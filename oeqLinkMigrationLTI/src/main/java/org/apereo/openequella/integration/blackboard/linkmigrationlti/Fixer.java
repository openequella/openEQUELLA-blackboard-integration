package org.apereo.openequella.integration.blackboard.linkmigrationlti;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

import blackboard.base.BbList;
import blackboard.data.content.Content;
import blackboard.data.course.Course;
import blackboard.data.navigation.CourseToc;
import blackboard.data.blti.BasicLTIContent;
import blackboard.data.BbAttribute;
import blackboard.data.ExtendedData;
import blackboard.persist.Id;
import blackboard.persist.PersistenceException;
import blackboard.persist.content.ContentDbLoader;
import blackboard.persist.content.ContentDbPersister;
import blackboard.persist.course.CourseDbLoader;
import blackboard.persist.navigation.CourseTocDbLoader;
import blackboard.platform.BbServiceManager;
import blackboard.platform.config.BbConfig;
import blackboard.platform.context.ContextManager;
import blackboard.platform.log.LogService;
import blackboard.platform.plugin.PlugInConfig;
import blackboard.platform.plugin.PlugInException;
import blackboard.platform.plugin.PlugInUtil;
import blackboard.platform.vxi.data.VirtualHost;
import blackboard.platform.vxi.data.VirtualInstallation;
import blackboard.platform.vxi.service.VirtualInstallationManager;
import org.apache.commons.lang.StringUtils;
import org.apereo.openequella.integration.blackboard.common.BbContext;

/**
 * @author aholland
 */
@SuppressWarnings("nls")
public class Fixer extends AbstractFixer {
  public static final String EXECUTE = "execute";
  public static final String PLACEMENT = "placementid";
  public static final String EQUELLA_URL = "equellaurl";
  public static final String CONFIG_FILE = "config.properties";

  private volatile static Fixer instance;
  protected final ContentDbLoader contentDbLoader;
  protected final CourseDbLoader courseDbLoader;
  protected final CourseTocDbLoader courseTocDbLoader;
  protected final LogService bbLogs;
  private final ContextManager context;
  private ContentDbPersister contentDbPersister;
  private final String bbUriStem;
  protected boolean completed;
  protected boolean started;
  protected boolean errored;
  protected int percent;
  protected int lookedAt;
  protected int equellaLookedAt;
  protected int fixedItems;
  private int blackboardVersion;
  private String placementId;

  protected Fixer() throws Exception {
	super();

	BbServiceManager.initFromSystemProps();

	final VirtualInstallationManager vim = (VirtualInstallationManager) BbServiceManager
	  .lookupService(VirtualInstallationManager.class);
	final VirtualHost vh = vim.getVirtualHost(""); //$NON-NLS-1$
	context = (ContextManager) BbServiceManager.lookupService(ContextManager.class);
	context.setContext(vh);

	bbUriStem = getBbUriStem(vim.getVirtualInstallationById(vh.getVirtualInstallationId()));

	bbLogs = BbContext.instance().getPersistenceManager().getLogService();
	contentDbLoader = (ContentDbLoader) BbContext.instance().getPersistenceManager().getLoader(ContentDbLoader.TYPE);
	contentDbPersister = (ContentDbPersister) BbContext.instance().getPersistenceManager().getPersister(ContentDbPersister.TYPE);
	courseDbLoader = (CourseDbLoader) BbContext.instance().getPersistenceManager().getLoader(CourseDbLoader.TYPE);
	courseTocDbLoader = (CourseTocDbLoader) BbContext.instance().getPersistenceManager().getLoader(CourseTocDbLoader.TYPE);

  }

  public static Fixer instance() throws Exception {
	if (instance != null) {
	  return instance;
	}

	instance = new Fixer();
	return instance;
  }

  private final String getBbUriStem(VirtualInstallation vi) {
	String path = ""; //$NON-NLS-1$
	try {
	  path = PlugInUtil.getUriStem(EQUELLA_BLOCK_VENDOR, EQUELLA_BLOCK_HANDLE, vi);
	} catch (final Exception t) {
	  logMessage(0, "Error getting relative path " + t.getMessage()); //$NON-NLS-1$
	}

	// see Jira Defect TLE-996 :
	// http://apps.dytech.com.au/jira/browse/TLE-996
	// This is only a temporary fix. We can't assume the VI is bb_bb60.
	if (path.length() == 0) {
	  path = "/webapps/" + EQUELLA_BLOCK_VENDOR + "-" + EQUELLA_BLOCK_HANDLE + "-bb_bb60/";
	}
	return path;
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

  @Override
  public synchronized void submit(HttpServletRequest request) throws Exception {
	placementId = request.getParameter(PLACEMENT);
	if (request.getParameter(EXECUTE) != null && StringUtils.isNotEmpty(placementId)) {
	  if (!started && !completed) {
		started = true;
		Runnable runner = new Runnable() {
		  @Override
		  public void run() {
			try {
			  BbList courseList = courseDbLoader.loadAllCourses();
			  int courseCount = courseList.size();
			  for (int i = 0; i < courseCount; i++) {
				percent = (int) (100.0 * i / courseCount);
				Course course = (Course) courseList.get(i);

				logMessage(0, "Looking at Course '" + course.getTitle() + "' ("
				  + course.getId().toExternalString() + ")");

				BbList courseTocs = courseTocDbLoader.loadByCourseId(course.getId());
				for (int j = 0; j < courseTocs.size(); j++) {
				  CourseToc courseToc = (CourseToc) courseTocs.get(j);

				  //logMessage(1, "Looking at CourseToc '" + courseToc.getLabel() + "' ("
				  //	+ courseToc.getId().toExternalString() + ")");

				  recurseContent(contentDbLoader, courseToc,
					getChildren(contentDbLoader, courseToc.getContentId()), placementId, 3);
				}

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
			  bbLogs.logError("An error occurred trying to fix EQUELLA content links", e);
			}
		  }
		};
		new Thread(runner, "Link Fixer Thread").start();
	  }
	}
  }

  protected void recurseContent(ContentDbLoader contentLoader, CourseToc courseToc, BbList contentList, String placementId, int level)
	throws Exception {
	for (int j = 0; j < contentList.size(); j++) {

	  Content content = (Content) contentList.get(j);
	  lookedAt++;

	  String handler = content.getContentHandler();

	  boolean tleResource = handler.equals("resource/tle-resource") || handler.equals("resource/tle-myitem")
		|| handler.equals("resource/tle-plan");
	  if (tleResource) {

		logMessage(level, "Title: " + content.getTitle());
		try {
		  logMessage(level, "getDataType().getName(): " + content.getDataType().getName());
		} catch (Exception exc) {
		  logMessage(1, "Error with getDataType: " + exc.getMessage());
		}

			  /*if (content.getParentId()!=null){
				logMessage(level, "getParentId" + content.getParentId().toExternalString());
			  }else {
				logMessage(level, "getParentId = NULL");
			  }


			  if (content.getParent()!=null){
				logMessage(level, "getParent" + content.getParent().getUrl());
			  }else {
				logMessage(level, "getParent = NULL");
			  }

			  if (content.getExtensionAttributes()!=null){

				for (String attribute:content.getExtensionAttributes().keySet()){
				  Map map2 = content.getExtensionAttributes().get(attribute);
				  logMessage(level+1, "map2: " + attribute);
				  for (Object attribute2:map2.keySet()){
					logMessage(level+2, "attribute2: " + attribute2.toString() + " : " + map2.get(attribute2.toString()).toString());
				  }
				}
			  }else {
				logMessage(level, "getExtensionAttributes = NULL");
			  }

			  if (content.getBbAttributes()!=null){
				List<BbAttribute> listBB = content.getBbAttributes().getBbAttributeList();
				for (BbAttribute bbAttribute:listBB){
				  if (bbAttribute.getValue()!=null) {
					logMessage(level + 1, "getBbAttributes" + bbAttribute.getName() + " : " + bbAttribute.getValue().toString());
				  }else{
					logMessage(level + 1, "getBbAttributes" + bbAttribute.getName() + " : null");
				  }
				}
			  }else {
				logMessage(level, "getBbAttributes = NULL");
			  }
			  logMessage(level, "getLaunchInNewWindow" + content.getLaunchInNewWindow());

			  logMessage(level, "getAllowGuests: " + content.getAllowGuests());

			   */

		ExtendedData extendedData2 = content.getExtendedData();
		if (extendedData2 != null) {
		  mapToString(extendedData2.getValues(), level, "Extended Data");
		}
	  }

			/*if ((handler.equals("resource/x-bb-blti-link")) && (content.getClass().getCanonicalName().equals("blackboard.data.blti.BasicLTIContent"))){
				BasicLTIContent basicLTIContent = (BasicLTIContent)content;
				try {




				  if (basicLTIContent.getParentContent()!=null){
					logMessage(level, "getParentContent" + basicLTIContent.getParentContent().getTitle());
				  }else {
					logMessage(level, "getParentContent = NULL");
				  }



				  if (basicLTIContent.getDomainConfig()!=null){
					logMessage(level, "getDomainConfig" + basicLTIContent.getDomainConfig().getKey());
				  }else {
					logMessage(level, "getDomainConfig = NULL");
				  }

				  if (basicLTIContent.getLinkCredentials()!=null){
					logMessage(level, "getLinkCredentials:key:  " + basicLTIContent.getLinkCredentials().getKey());
					logMessage(level, "getLinkCredentials: contentId: " + basicLTIContent.getLinkCredentials().getContentId().toExternalString());
					logMessage(level, "getLinkCredentials: secret: " + basicLTIContent.getLinkCredentials().getSecret());
					logMessage(level, "getLinkCredentials: id: " + basicLTIContent.getLinkCredentials().getId().toExternalString());
				  }else {
					logMessage(level, "getLinkCredentials = NULL");
				  }

				  if (basicLTIContent.getVendorInfo()!=null){
					logMessage(level, "getVendorInfo" + basicLTIContent.getVendorInfo().getUrl());
				  }else {
					logMessage(level, "getVendorInfo = NULL");
				  }

				  logMessage(level, "Alternate Url " + basicLTIContent.getAlternateUrl());

				  Map<String,String> customParameters = basicLTIContent.getCustomParameters();
				  if (customParameters != null) {
					mapToString(customParameters,level, "customParameters");
				  }
				}catch (Exception exc) {
				  logMessage(1, "ANOTHER ERROR: " + exc.getMessage());
				}

			}*/

	  if (tleResource) {
		logMessage(0, "Updating:  " + contentDisplay(content));
		try {
		  fixContent(courseToc, content, level);
		  logMessage(0, "Success");
		} catch (Exception ex) {
		  logMessage(0, "Updating ERROR: " + ex.getMessage());
		  for (StackTraceElement element : ex.getStackTrace()) {
			logMessage(0, element.toString());
		  }
		}
	  }
	  recurseContent(contentLoader, courseToc, getChildren(contentLoader, content.getId()), placementId, level + 1);
	}
  }

  private void mapToString(Map<String, String> map, int level, String label) {
	try {

	  logMessage(level, "MAP SIZE: " + map.size());
	  Iterator<String> i = map.keySet().iterator();
	  while (i.hasNext()) {
		String attribute = i.next();
		logMessage(level, attribute + " : " + map.get(attribute));
	  }
	} catch (Exception exc) {
	  logMessage(level, label + " error : " + exc.getMessage());
	}
  }

  protected BbList getChildren(ContentDbLoader contentLoader, Id bbContentId) throws PersistenceException {
	return contentLoader.loadChildren(bbContentId);
  }

  private void fixContent(CourseToc courseToc, Content ltiContent, int level) throws Exception {
	equellaLookedAt++;

	ExtendedData extendedData = ltiContent.getExtendedData();
	String newUrl = getEquellaUrl() + extendedData.getValue("url");
	logMessage(level, "NewUrl: " + newUrl);
	ltiContent.setUrl(newUrl);
	String newUrlHost = getDomainName(getEquellaUrl());
	logMessage(level, "NewUrlHost: " + newUrlHost);
	ltiContent.getBbAttributes().setString("UrlHost", newUrlHost);
	Map<String, String> values = new HashMap<>();
	ltiContent.setContentHandler("resource/x-bb-blti-link");
	values.put("customParameters", "");
	values.put("cimPlacementId", placementId);
	logMessage(level, "PlacementId: " + placementId);
	values.put("itemOrigin", "CIM");
	extendedData.setValues(values);
	// persist it
	contentDbPersister.persist(ltiContent);
	fixedItems++;

  }

  private String contentDisplay(Content content) {
	return "'" + content.getTitle() + "' (" + content.getId().toExternalString() + ")";
  }

  /**
   * This is a rather hacky way to get the version.
   *
   * @return
   */
  @Override
  protected int getBlackboardVersion() {
	if (blackboardVersion == 0) {
	  String vers = BbServiceManager.getConfigurationService().getBbProperty(BbConfig.LIBRARY_VERSION);
	  // don't use vers.contains (not in Java 1.4)
	  if (vers.indexOf('.') > -1) //$NON-NLS-1$
	  {
		vers = vers.split("\\.")[0]; //$NON-NLS-1$
	  }
	  try {
		blackboardVersion = Integer.parseInt(vers);
	  } catch (NumberFormatException nfe) {
		blackboardVersion = 6;
	  }
	}
	return blackboardVersion;
  }

  public String getStatus() {
	if (errored) {
	  return "An error occurred when trying to fix EQUELLA links.  See Blackboard log " + bbLogs.getLogFileName()
		+ " for more details.";
	} else if (completed) {
	  return "Execution of fixer has finished!  Looked at " + lookedAt + " items (" + equellaLookedAt
		+ " EQUELLA items) and converted to LTI " + fixedItems + " items .  The building block can now be safely removed.";
	} else if (hasStarted()) {
	  return "Execution of fixer has started.  Approx " + percent + " complete.";
	} else {
	  return "Ready to start.  It is <b>HIGHLY</b> recommended that you backup your Blackboard database before pressing the Submit button.";
	}
  }

  @Override
  protected void logMessage(int lvl, String msg) {
	super.logMessage(lvl, msg);
	bbLogs.logWarning("[EQUELLA Link Fixer] " + msg);
  }

  public synchronized boolean hasStarted() {
	return started;
  }

  public boolean hasCompleted() {
	return completed;
  }

  @Override
  public synchronized String getEquellaUrl() {
	if (equellaUrl.length() == 0) {
	  File configFile = new File(getConfigDirectory(), CONFIG_FILE);
	  if (!configFile.exists()) {
		throw new RuntimeException("Cannot find EQUELLA Plugin Building Block configuration file");
	  }

	  Properties props = new Properties();
	  try (InputStream inStream = new FileInputStream(configFile)) {
		props.load(inStream);
	  } catch (Exception e) {
		throw new RuntimeException("Error loading configuration", e);
	  }
	  if (props.containsKey(EQUELLA_URL)) {
		equellaUrl = props.getProperty(EQUELLA_URL);
		EQUELLA_URL_REGEX = null;
	  } else {
		throw new RuntimeException("EQUELLA Url in EQUELLA Plugin Building Block is not set");
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

  @Override
  protected String getRelativePath() {
	return bbUriStem;
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

  public String getPlacementId() {
	return placementId;
  }

  public void setPlacementId(String placementId) {
	this.placementId = placementId;
  }
}
