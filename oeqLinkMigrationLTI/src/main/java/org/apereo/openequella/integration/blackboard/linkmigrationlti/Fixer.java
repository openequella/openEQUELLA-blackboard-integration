/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0, (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apereo.openequella.integration.blackboard.linkmigrationlti;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

import blackboard.base.BbList;
import blackboard.base.FormattedText;
import blackboard.data.blti.BasicLTIPlacement;
import blackboard.data.content.Content;
import blackboard.data.course.Course;
import blackboard.data.navigation.CourseToc;
import blackboard.data.ExtendedData;
import blackboard.persist.Id;
import blackboard.persist.KeyNotFoundException;
import blackboard.persist.PersistenceException;
import blackboard.persist.content.ContentDbLoader;
import blackboard.persist.content.ContentDbPersister;
import blackboard.persist.course.CourseDbLoader;
import blackboard.persist.navigation.CourseTocDbLoader;
import blackboard.platform.BbServiceManager;
import blackboard.platform.blti.BasicLTIPlacementManager;
import blackboard.platform.context.ContextManager;
import blackboard.platform.log.LogService;
import blackboard.platform.plugin.PlugInConfig;
import blackboard.platform.plugin.PlugInException;
import blackboard.platform.vxi.data.VirtualHost;
import blackboard.platform.vxi.service.VirtualInstallationManager;
import org.apache.commons.lang.StringUtils;
import org.apereo.openequella.integration.blackboard.common.BbContext;
import org.apereo.openequella.integration.blackboard.common.BbLogger;
import org.apereo.openequella.integration.blackboard.common.content.ContentUtil;
import org.apereo.openequella.integration.blackboard.common.content.ItemInfo;
import org.apereo.openequella.integration.blackboard.common.content.ItemKey;
import org.apereo.openequella.integration.blackboard.common.content.RegistrationUtil;

@SuppressWarnings("nls")
public class Fixer {
  public static final String EXECUTE = "execute";
  public static final String RESET = "reset";
  public static final String COURSEID = "courseId";
  public static final String PLACEMENT = "placementhandle";
  public static final String EQUELLA_URL = "equellaurl";
  public static final String CONFIG_FILE = "config.properties";
  protected static final String EQUELLA_BLOCK_VENDOR = "dych";
  protected static final String EQUELLA_BLOCK_HANDLE = "tle";

  private volatile static Fixer instance;
  protected final ContentDbLoader contentDbLoader;
  protected final CourseDbLoader courseDbLoader;
  protected final CourseTocDbLoader courseTocDbLoader;
  protected final LogService bbLogs;
  private final ContextManager context;
  private ContentDbPersister contentDbPersister;
  protected boolean completed;
  protected boolean started;
  protected boolean errored;
  protected int percent;
  protected int lookedAt;
  protected int equellaLookedAt;
  protected int fixedItems;
  private String placementHandle;
  private String courseId;
  protected StringBuffer log;
  protected String equellaUrl = "";

  protected Fixer() throws Exception {
	resetState();

	BbServiceManager.initFromSystemProps();

	final VirtualInstallationManager vim = (VirtualInstallationManager) BbServiceManager
	  .lookupService(VirtualInstallationManager.class);
	final VirtualHost vh = vim.getVirtualHost(""); //$NON-NLS-1$
	context = (ContextManager) BbServiceManager.lookupService(ContextManager.class);
	context.setContext(vh);
	bbLogs = BbContext.instance().getPersistenceManager().getLogService();
	contentDbLoader = (ContentDbLoader) BbContext.instance().getPersistenceManager().getLoader(ContentDbLoader.TYPE);
	contentDbPersister = (ContentDbPersister) BbContext.instance().getPersistenceManager().getPersister(ContentDbPersister.TYPE);
	courseDbLoader = (CourseDbLoader) BbContext.instance().getPersistenceManager().getLoader(CourseDbLoader.TYPE);
	courseTocDbLoader = (CourseTocDbLoader) BbContext.instance().getPersistenceManager().getLoader(CourseTocDbLoader.TYPE);

  }

  protected synchronized void resetState() {
    completed = false;
    started = false;
    errored = false;

	log = new StringBuffer();
  }

  public static Fixer instance() throws Exception {
	if (instance != null) {
	  return instance;
	}
	instance = new Fixer();
	return instance;
  }

  @Override
  protected void finalize() throws Throwable {
	context.releaseContext();
	super.finalize();
  }

  public void load() {
	// force re-evaluation of the Equella URL on page load
	// (it may be have been changed in the other Building Block since)
	setEquellaUrl("");
  }

  public synchronized void submit(HttpServletRequest request) throws Exception {
	if((request.getParameter(RESET) != null) && canReset()) {
	  resetState();
	  logMessage(0, "State reset.");
	  return;
	}

    placementHandle = request.getParameter(PLACEMENT);
	courseId = request.getParameter(COURSEID);
	if (request.getParameter(EXECUTE) != null && StringUtils.isNotEmpty(placementHandle)) {
	  if (!started && !completed) {
		started = true;
		Runnable runner = new Runnable() {
		  @Override
		  public void run() {
			try {
			  BasicLTIPlacement placement = null;

			  logMessage(0, "Loading placement...");
			  try {
			  	placement = BasicLTIPlacementManager.Factory.getInstance().loadByHandle(placementHandle);
			  } catch (KeyNotFoundException ex) {
				logMessage(1, "---------------------------------------------------");
				logMessage(1, "An error occurred: The placement handle does not exist. Please introduce a valid placement handle code");
				started = false;
				completed = false;
				errored = true;
				throw ex;
			  }
			  logMessage(1, "Name: " + placement.getName());
			  logMessage(1, "Url: " + placement.getUrl());
			  logMessage(1, "Handle: " + placement.getHandle());
			  logMessage(1, "Id: " + placement.getId().toExternalString());

			  mapToString(placement.getCustomParameters(), 0, "placement parameters");
			  BbList courseList = new BbList();
			  if (StringUtils.isNotEmpty(courseId) && courseDbLoader.doesCourseIdExist(courseId)) {
				courseList.add(courseDbLoader.loadByCourseId(courseId));
				logMessage(0, "Filtering by course: " + courseId);
			  } else if (StringUtils.isNotEmpty(courseId)) {
				logMessage(0, "---------------------------------------------------");
				logMessage(0, "An error occurred: The course ID does not exist. Please provide a valid course ID or leave empty for ALL courses");
				started = false;
				completed = false;
				errored = true;
				throw new Exception("Unknown course filter ID of " + courseId);
			  } else {
				courseList = courseDbLoader.loadAllCourses();
				logMessage(0, "No course filter specified - migrating ALL courses");
			  }

			  int courseCount = courseList.size();
			  for (int i = 0; i < courseCount; i++) {
				percent = (int) (100.0 * i / courseCount);
				Course course = (Course) courseList.get(i);

				logMessage(0, "Beginning review of course '" + course.getTitle() + "' ("
				  + course.getId().toExternalString() + ")");


				BbList courseTocs = courseTocDbLoader.loadByCourseId(course.getId());
				for (int j = 0; j < courseTocs.size(); j++) {
				  CourseToc courseToc = (CourseToc) courseTocs.get(j);

				  recurseContent(contentDbLoader, courseToc, course,
					getChildren(contentDbLoader, courseToc.getContentId()), placement.getId().toExternalString(), 0);
				}

				logMessage(0, "Finished review of course '" + course.getTitle() + "' ("
				  + course.getId().toExternalString() + ")");
			  }
			  started = false;
			  completed = true;
			  errored = false;
			} catch (Exception e) {
			  started = false;
			  completed = false;
			  errored = true;
			  logMessage(0, "---------------------------------------------------");
			  logMessage(0, "An error occurred: " + e.getMessage());
			  logMessage(0, "See Blackboard service logs for details");
			  bbLogs.logError("An error occurred trying to migrate openEQUELLA content links", e);
			}
		  }
		};
		new Thread(runner, "Link Migrator Thread").start();
	  }
	}
  }

  protected void recurseContent(ContentDbLoader contentLoader, CourseToc courseToc, Course course, BbList contentList, String placementExternalId, int level)
	throws Exception {
	for (int j = 0; j < contentList.size(); j++) {

	  Content content = (Content) contentList.get(j);
	  lookedAt++;

	  String handler = content.getContentHandler();

	  boolean tleResource = handler.equals("resource/tle-resource") || handler.equals("resource/tle-myitem")
		|| handler.equals("resource/tle-plan");
	  if (tleResource) {
		logMessage(level, content, "content to migrate");

		try {
		  fixContent(courseToc, course, content, level, placementExternalId);
		  logMessage(level, content, "migrated content");

		} catch (Exception ex) {
		  logMessage(level, ex, "Updating ERROR");
		}
	  } else {
		logMessage(level, content, "content with unknown handle (unable to migrate)");

	  }
	  recurseContent(contentLoader, courseToc, course, getChildren(contentLoader, content.getId()), placementExternalId, level + 1);
	}
  }

  private void mapToString(Map<String, String> map, int level, String label) {
	try {
	  Iterator<String> i = map.keySet().iterator();
	  while (i.hasNext()) {
		String attribute = i.next();
		logMessage(level, label + " : " + attribute + " : " + map.get(attribute));
	  }
	} catch (Exception exc) {
	  logMessage(level, label + " error : " + exc.getMessage());
	}
  }

  protected BbList getChildren(ContentDbLoader contentLoader, Id bbContentId) throws PersistenceException {
	return contentLoader.loadChildren(bbContentId);
  }

  private void fixContent(CourseToc courseToc, Course course,  Content ltiContent, int level, String placementExternalId) throws Exception {
	logMessage(level, "Link migration notes for " + ltiContent.getTitle());
	equellaLookedAt++;

	ExtendedData extendedData = ltiContent.getExtendedData();
	String equellaURL = getEquellaUrl();

	ItemInfo itemInfo = null;
	try {
	  itemInfo = ContentUtil.instance().ensureProperties(ltiContent, course, ltiContent.getParentId(), equellaURL);
	  if( itemInfo == null) {
		logMessage(level+1, "oEQ item info is null - unable to migrate link");
		return;
	  }
	} catch (Exception t) {
	  logMessage(level+1, t, "Error retrieving item info - unable to migrate link");
	  return;
	}

	final ItemKey itemkey = itemInfo.getItemKey();
	if( itemkey == null) {
	  logMessage(level+1, "oEQ item key is null - unable to migrate link");
	  return;
	}

	//Update the URL
	final String xdUrl = extendedData.getValue("url");
	if(xdUrl == null) {
	  logMessage(level+1, "oEQ link is null - unable to migrate link");
	  return;
	} else if(StringUtils.isEmpty(xdUrl)) {
	  logMessage(level+1, "oEQ link is empty - unable to migrate link");
	  return;
	}

	String newUrl = equellaURL + xdUrl;
	logMessage(level+1, "NewUrl: " + newUrl);
	ltiContent.setUrl(newUrl);

	//Update the Host
	String newUrlHost = getDomainName(equellaURL);
	logMessage(level+1, "NewUrlHost: " + newUrlHost);
	ltiContent.getBbAttributes().setString("UrlHost", newUrlHost);

	//Update the Type
	ltiContent.setContentHandler("resource/x-bb-blti-link");

	//Update the content body (remove the link)
	String htmlContent = ltiContent.getBody().getFormattedText();
	String toRemove = StringUtils.substringBetween(htmlContent, "<div class=\"equella-link", "</div>");
	htmlContent = StringUtils.remove(htmlContent,toRemove);
	FormattedText newFormattedText = new FormattedText(htmlContent,FormattedText.Type.HTML);
	ltiContent.setBody(newFormattedText);

	//Update the extended data
	Map<String, String> values = new HashMap<>();
	values.put("customParameters", "");
	values.put("cimPlacementId", placementExternalId);
	logMessage(level+1, "PlacementId: " + placementExternalId);
	values.put("itemOrigin", "CIM");
	extendedData.setValues(values);
	mapToString(values,level+1, "New Extended Data");

	// persist it
	contentDbPersister.persist(ltiContent);
	fixedItems++;
	logMessage(level+1, "Migrated link");
  }

  public String getStatus() {
	if (errored) {
	  return "An error occurred when trying to migrate openEQUELLA links.  See Blackboard log " + bbLogs.getLogFileName()
		+ " for more details.";
	} else if (completed) {
	  return "Migration has finished!  Looked at " + lookedAt + " links (" + equellaLookedAt
		+ " openEQUELLA items) and converted " + fixedItems + " links to LTI.";
	} else if (hasStarted()) {
	  return "Migration has started.  Approx " + percent + " complete.";
	} else {
	  return "Ready to start.  It is <b>HIGHLY</b> recommended that you backup your Blackboard database before pressing the Submit button.";
	}
  }

  public synchronized boolean canReset() {
	return errored || completed;
  }

  public synchronized boolean hasStarted() {
	return started;
  }

  public boolean hasCompleted() {
	return completed;
  }

  public synchronized String getEquellaUrl() {
	if (equellaUrl.length() == 0) {
	  File configFile = new File(getConfigDirectory(), CONFIG_FILE);
	  if (!configFile.exists()) {
		throw new RuntimeException("Cannot find openEQUELLA integration Building Block configuration file");
	  }

	  Properties props = new Properties();
	  try (InputStream inStream = new FileInputStream(configFile)) {
		props.load(inStream);
	  } catch (Exception e) {
		throw new RuntimeException("Error loading configuration", e);
	  }
	  if (props.containsKey(EQUELLA_URL)) {
		equellaUrl = props.getProperty(EQUELLA_URL);
	  } else {
		throw new RuntimeException("openEQUELLA Url in openEQUELLA integration Building Block is not set");
	  }
	}
	return equellaUrl;
  }

  private synchronized void setEquellaUrl(String equellaUrl) {
	this.equellaUrl = equellaUrl;
  }

  private File getConfigDirectory() {
	try {
	  return new PlugInConfig(EQUELLA_BLOCK_VENDOR, EQUELLA_BLOCK_HANDLE).getConfigDirectory();
	} catch (PlugInException e) {
	  throw new RuntimeException(e);
	}
  }

  public String getLog()
  {
	return log.toString();
  }

  protected void logMessage(int lvl, String msg)
  {
	log.append(loggerPadding(lvl) + msg + "\n");
	bbLogs.logWarning("[oEQ Link Migrator] " + msg);
  }

  protected void logMessage(int lvl, Content c, String label) {
	logMessage(lvl, "Beginning display of " + label + " - " + c.getTitle());
	logMessage(lvl+1, "ID: " + c.getId().toExternalString());
	final Content pContent = c.getParent();
	if(pContent == null) {
	  logMessage(lvl+1, "Parent ID: null");
	} else {
	  logMessage(lvl+1, "Parent ID: " + pContent.getId().toExternalString() + " (" + pContent.getTitle() + ")");
	}
	logMessage(lvl+1, "Course ID: " + c.getCourseId().toExternalString());
	logMessage(lvl+1, "Content handler: " + c.getContentHandler());
	logMessage(lvl+1, "Link ref: " + c.getLinkRef());
	logMessage(lvl+1, "Body ([lt] == <): " + c.getBody().getText().replaceAll("<", "[lt]"));
	logMessage(lvl+1, "Short Description: " + c.getShortDescription());
	logMessage(lvl+1, "URL: " + c.getUrl());
	logMessage(lvl+1, "URL Host: " + c.getUrlHost());

	try {
	  logMessage(lvl+1, "Data type: " + c.getDataType().getName());
	} catch (Exception exc) {
	  logMessage(lvl+1, exc,"Error with retrieving data type");
	}
	ExtendedData xData = c.getExtendedData();
	if (xData != null) {
	  mapToString(xData.getValues(), lvl+1, "Extended data");
	} else {
	  logMessage(lvl+1, "Extended data is null for '" + label + "'");
	}
	logMessage(lvl, "Completed display of " + label + " - " + c.getTitle());
  }

  protected void logMessage(int lvl, Exception e, String label) {
	logMessage(0, e, label + ": " + e.getMessage());
	for (StackTraceElement element : e.getStackTrace()) {
	  logMessage(0, element.toString());
	}
  }

  private String loggerPadding(int level)
  {
    String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	String date = simpleDateFormat.format(new Date());
    String pad = "[" + date + "]";
	for( int lvl = 0; lvl < level; lvl++ )
	{
	  pad += "  ";
	}
	return pad;
  }

  public static String getDomainName(String url) throws MalformedURLException {
	if (!url.startsWith("http") && !url.startsWith("https")) {
	  url = "http://" + url;
	}
	URL netUrl = new URL(url);
	String host = netUrl.getHost();
	if (host.startsWith("www")) {
	  host = host.substring("www".length() + 1);
	}
	return host;
  }

  public String getPlacementHandle() {
	return placementHandle;
  }

  public void setPlacementHandle(String placementHandle) {
	this.placementHandle = placementHandle;
  }
}
