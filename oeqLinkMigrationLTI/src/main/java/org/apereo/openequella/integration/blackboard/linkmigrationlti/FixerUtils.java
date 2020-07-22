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

import blackboard.platform.log.LogService;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FixerUtils {

  public static final int UUID_LENGTH = 36;

  protected StringBuffer logger = new StringBuffer();
  LogService bbLogs = null;


  public FixerUtils(LogService bbLogs) {
    this.bbLogs = bbLogs;
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

  public boolean isUrlPresent(int level, String url) {
	if(url == null) {
	  log(level+1, "oEQ link is null - unable to migrate link");
	  return false;
	} else if(StringUtils.isEmpty(url)) {
	  log(level+1, "oEQ link is empty - unable to migrate link");
	  return false;
	}

	return true;
  }

  public static String buildOeqIntegUrl(String oequrl, String itemId, String itemVersion) {
    // Avoid a double slash
    final String separator = oequrl.endsWith("/") ? "" : "/";
    return oequrl + separator + "integ/gen/" + itemId + "/" + itemVersion + "/";
  }

  //
  // Logging Utils
  //

  public String getLog() {
    return logger.toString();
  }

  public void log(int lvl, String msg) {
	logger.append(loggerPadding(lvl) + msg + "\n");
	if(bbLogs != null) {
	  bbLogs.logWarning("[oEQ Link Migrator] " + msg);
	}
  }

  public void log(int lvl, Exception e, String label) {
	log(0, e, label + ": " + e.getMessage());
	for (StackTraceElement element : e.getStackTrace()) {
	  log(0, element.toString());
	}
  }

  private String loggerPadding(int level) {
	String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	String date = simpleDateFormat.format(new Date());
	String pad = "[" + date + "]";
	for (int lvl = 0; lvl < level; lvl++) {
	  pad += "  ";
	}
	return pad;
  }

  public static String findItemId(String str) {
	return StringUtils.substringBetween(str, "<!--<item id=\"", "\" itemdefid=\"");
  }

  public static String findItemVersion(String str) {
	return StringUtils.substringBetween(str, " version=\"", "\"");
  }

  public static String findSelectedItemAttachment(String str) {
	return StringUtils.substringBetween(str, "<attachments selected=\"", "\"");
  }

  public static String findName(String str) {
	return StringUtils.substringBetween(str, "class=\"info\">", "</a></td></table></td>");
  }

  public static String findDescription(String str) {
    String retVal = StringUtils.substringBetween(str, "<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"font-size:12pt\" width=\"100%\"><tr><td>", "</td>");
	if(retVal == null) {
	  retVal = StringUtils.substringBetween(str, "<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"font-size:12pt\" width=\"100%\"><tr><td class=\"bbdesc\">", "</td>");
	}
	return retVal;
  }

  /**
   * To find the correct link, name, and description:
   *
   * 1) ExtendedData[url] has a valid URL.
   *   To find the name (if there isn't a content name already), use the attachmentName value
   *   To find the description (if there isn't a content description already) use the description value
   *   Body: comment out <div class="equella-link...</div> and add a filler div...
   *   <!--<div class="equella-link"...</div>--><div></div>
   * 2) Item Attachment by selected uuid
   *   Regex:  Body contains <!--<item id="[uuid]"...version="[version]"...<attachments selected="[att-uuid]"...-->
   *     note - [att-uuid] must be 36 characters long
   *   URL:  lti-placement/items/[uuid]/[version]/?attachment.uuid=[att-uuid]
   *   Name / Description:  See below
   *   Body:
   *     Replace: <td><table...href="/webapps/dych-tle-.../ViewContent..."...</table></td>
   *       with: <td/>
   * 3) Item Attachment by selected integration link
   *   Regex:  Body contains <!--<item id="[uuid]"...version="[version]"...<attachments selected="[att-integ-link]"...-->
   *     note - [att-integ-link] must have a length greater then 0,
   *            start with https:// or http:// ,
   *            and contain /integ/gen/ ,
   *            and end with attachment.uuid=[att-uuid] where att-uuid is 36 characters long
   *   URL:  [att-integ-link]
   *   Name / Description:  See below
   *   Body:
   *     Consider:  class="info">[link name</a></td></table></td>
   *     Replace: <td><table...href="/webapps/dych-tle-.../ViewContent..."...</table></td>
   *       with: <td/>
   * 4) Item Attachment by selected filename
   *   Regex:  Body contains <!--<item id="[uuid]"...version="[version]"...<attachments selected="[att-filename]"...-->
   *     note - [att-uuid] must have a length greater then 0 and not start with https:// nor http://
   *   URL:  lti-placement/items/[uuid]/[version]/[att-filename]
   *   Name / Description:  See below
   *   Body:
   *     Consider:  class="info">[link name</a></td></table></td>
   *     Replace: <td><table...href="/webapps/dych-tle-.../ViewContent..."...</table></td>
   *       with: <td/>
   * 5) Item Summary without selection
   *   Regex:  Body contains <!--<item id="[uuid]"...version="[version]"...-->
   *   URL:  lti-placement/items/[uuid]/[version]
   *   Name / Description:  See below
   *   Body:
   *     Consider:  class="info">[link name</a></td></table></td>
   *     Replace: <td><table...href="/webapps/dych-tle-.../ViewContent..."...</table></td>
   *       with: <td/>
   *
   * To find the link name and description for links without extended data:
   * 1) If the name or description already exists on the content link, use it.
   * 2) For the name, use the value in the body:
   *   ...class="info">[link name]</a></td></table></td>...
   * 3) For the description, use the value in the body:
   *   ...<table border="0" cellspacing="0" cellpadding="0" style="font-size:12pt" width="100%"><tr><td>[link description]</td>...
   */
  public FixerResponse migrate(int level, String oequrl, String xDUrl, String xDTitle, String xDDesc, String htmlBody, String[] urls) {
	// Try various migration methods to see if the content is in a known format
	FixerResponse fixed = migrateViaExtendedData(level, oequrl, xDUrl, xDTitle, xDDesc, htmlBody, urls);
	if(fixed.isValidResponse()) {
	  return fixed;
	}
	fixed = migrateViaAttachmentBySelectedUuid(level, oequrl, htmlBody, urls);

	if(fixed.isValidResponse()) {
	  return fixed;
	}
	fixed = migrateViaAttachmentBySelectedIntegLink(level, htmlBody, urls);

	if(fixed.isValidResponse()) {
	  return fixed;
	}

	fixed = migrateViaAttachmentBySelectedFilename(level, oequrl, htmlBody, urls);
	if(fixed.isValidResponse()) {
	  return fixed;
	}

	fixed = migrateViaOnlySummary(level, oequrl, htmlBody, urls);
	if(fixed.isValidResponse()) {
	  return fixed;
	}

	log(level+1, "oEQ link has an unknown format - unable to migrate link");
	return fixed;
  }

  private FixerResponse migrateViaExtendedData(int level, String oequrl, String xDUrl, String xDTitle, String xDDesc, String htmlBody, String[] urls) {
	final String flow = "ExtendedData";
	FixerResponse fr = new FixerResponse();
    fr.setValidResponse(isUrlPresent(level, xDUrl));

    if(!fr.isValidResponse()) {
	  log(level+1, "Unable to migrate via " + flow + ".");
	  return fr;
	}
	// Ensure there is only one slash between the base oEQ URL and the xDUrl
	final String separator = oequrl.endsWith("/") ? "" : "/";
    final String cleanedXDUrl = xDUrl.startsWith("/") ? xDUrl.substring(1) : xDUrl;
	fr.setNewUrl(oequrl + separator + cleanedXDUrl);

	// Update the content body (remove the link)
	fr.setNewBody(scrubDescription(htmlBody, level, urls));

	fr.setNewName(xDTitle);

	fr.setNewDescription(xDDesc);

	log(level+1, "Migrating via " + flow + ".");

	return fr;
  }

  private FixerResponse migrateViaAttachmentBySelectedUuid(int level, String oequrl, String htmlBody, String[] urls) {
	final String flow = "AttachmentBySelectedUuid";
	FixerResponse fr = new FixerResponse();

	final String itemId = findItemId(htmlBody);
	if(itemId == null) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - no item uuid found.");
	  return fr;
	}

	final String itemVersion = findItemVersion(htmlBody);
	if(itemVersion == null) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - no item version found.");
	  return fr;
	}

	final String attUuid = findSelectedItemAttachment(htmlBody);
	if(StringUtils.isEmpty(attUuid)) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - no attachment selected.");
	  return fr;
	}
	if(attUuid.length() != UUID_LENGTH) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - attachment selected value is [" + attUuid.length() + "].  Should be 36.");
	  return fr;
	}
	fr.setNewUrl(buildOeqIntegUrl(oequrl, itemId, itemVersion) + "?attachment.uuid=" + attUuid);
	fr.setValidResponse(true);

	fr.setNewBody(scrubDescription(htmlBody, level, urls));

	setNameAndDescription(fr, htmlBody, level, flow);

	if(fr.isValidResponse()) {
	  log(level + 1, "Migrating via " + flow + ".");
	}

	return fr;
  }

  private FixerResponse migrateViaAttachmentBySelectedIntegLink(int level, String htmlBody, String[] urls) {
	final String flow = "AttachmentBySelectedIntegLink";
	FixerResponse fr = new FixerResponse();

	final String itemId = findItemId(htmlBody);
	if(itemId == null) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - no item uuid found.");
	  return fr;
	}

	final String itemVersion = findItemVersion(htmlBody);
	if(itemVersion == null) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - no item version found.");
	  return fr;
	}

	final String attIntegLink = findSelectedItemAttachment(htmlBody);
	if(StringUtils.isEmpty(attIntegLink)) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - no attachment selected.");
	  return fr;
	}
	if(!attIntegLink.startsWith("https://") && !attIntegLink.startsWith("http://")) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - attachment selected value doesn't start with 'https://' nor 'http://'.");
	  return fr;
	}
	if(!attIntegLink.contains("/integ/gen/")) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - attachment selected value doesn't contain '/integ/gen/'.");
	  return fr;
	}
	final String attUuid = StringUtils.substringAfter(attIntegLink, "attachment.uuid=");
	if(StringUtils.isEmpty(attUuid)) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - no uuid found in the selected attachment.");
	  return fr;
	}
	if(attUuid.length() != UUID_LENGTH) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - uuid in the selected attachment link is [" + attUuid.length() + "].  Should be 36.");
	  return fr;
	}
	fr.setNewUrl(attIntegLink);
	fr.setValidResponse(true);

	fr.setNewBody(scrubDescription(htmlBody, level, urls));

	setNameAndDescription(fr, htmlBody, level, flow);

	if(fr.isValidResponse()) {
	  log(level + 1, "Migrating via " + flow + ".");
	}

	return fr;
  }

  private FixerResponse migrateViaAttachmentBySelectedFilename(int level, String oequrl, String htmlBody, String[] urls) {
	final String flow = "AttachmentBySelectedFilename";

	FixerResponse fr = new FixerResponse();

	final String itemId = findItemId(htmlBody);
	if(itemId == null) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - no item uuid found.");
	  return fr;
	}

	final String itemVersion = findItemVersion(htmlBody);
	if(itemVersion == null) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - no item version found.");
	  return fr;
	}

	final String attFilename = findSelectedItemAttachment(htmlBody);
	if(StringUtils.isEmpty(attFilename)) {
	  fr.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + flow + " - no attachment selected.");
	  return fr;
	}
	fr.setNewUrl(buildOeqIntegUrl(oequrl, itemId, itemVersion) + attFilename);
	fr.setValidResponse(true);

	fr.setNewBody(scrubDescription(htmlBody, level, urls));

	setNameAndDescription(fr, htmlBody, level, flow);

	if(fr.isValidResponse()) {
	  log(level + 1, "Migrating via " + flow + ".");
	}

	return fr;
  }

  private FixerResponse migrateViaOnlySummary(int level, String oequrl, String htmlBody, String[] imageUrls) {
    final String flow = "OnlySummary";
	FixerResponse fr = new FixerResponse();

	final String itemId = findItemId(htmlBody);
	if(itemId == null) {
	  log(level+1, "Unable to migrate via " + flow + " - no item uuid found.");
	  fr.setValidResponse(false);
	  return fr;
	}

	final String itemVersion = findItemVersion(htmlBody);
	if(itemVersion == null) {
	  log(level+1, "Unable to migrate via " + flow + " - no item version found.");
	  fr.setValidResponse(false);
	  return fr;
	}
	fr.setNewUrl(buildOeqIntegUrl(oequrl, itemId, itemVersion));
	fr.setValidResponse(true);

	fr.setNewBody(scrubDescription(htmlBody, level, imageUrls));

	setNameAndDescription(fr, htmlBody, level, flow);

	if(fr.isValidResponse()) {
	  log(level + 1, "Migrating via " + flow + ".");
	}


	return fr;
  }

  private void setNameAndDescription(FixerResponse resp, String textToSearch, int level, String method) {
	resp.setNewName(findName(textToSearch));
	if(resp.getNewName() == null) {
	  resp.setValidResponse(false);
	  log(level+1, "Unable to migrate via " + method + " - cannot find the name.");
	} else {
	  // Description is optional - null is given if no description found
	  resp.setNewDescription(findDescription(textToSearch));
	}
  }

  private String scrubDescription(String str, int level, String[] urls) {
    Document d = Jsoup.parse(str);
    d.outputSettings().prettyPrint(false);

    // Remove all old integration links
    Elements elems = d.select("a[href^=\"/webapps/dych-tle-\"]");
    for(Element e : elems) {
        e.remove();
        log(level + 1, "Removing the element ([lt] == <): " + e.outerHtml().replaceAll("<", "[lt]"));
	}

	// Remove all old image spacers
	elems = d.select("img");
	for(Element e : elems) {
	  scrubForUrls(e, level, urls);
	}

	StringBuilder result = new StringBuilder();
	for(Node n : d.childNodes()) {
	  // Keep any top level comments
	  if (n instanceof Comment) {
	    result.append(n.outerHtml());
	  }
	}

	// Keep the inner html of the body
	result.append(d.body().html());
    return result.toString();

  }

  private void scrubForUrls(Element e, int level, String[] urls) {
	for(String urlToTest : urls) {
	  if (e.attr("src").equals(urlToTest)) {
	    if(e.hasParent() &&
		  e.parent().hasParent() &&
		  e.parent().tagName().equals("td") &&
		  e.parent().parent().tagName().equals("tr") &&
		  e.parent().text().trim().length() == 0 &&
		  e.parent().parent().text().trim().length() == 0 &&
		  (e.parent().childNodeSize() == 1) &&
		  (e.parent().parent().childNodeSize() == 1)) {
			// If the img is the only element in the form <tr><td><img .../></td></tr> , remove the tr
		  log(level + 1, "Removing the element ([lt] == <): " + e.parent().parent().outerHtml().replaceAll("<", "[lt]"));
		  e.parent().parent().remove();
		} else {
		  log(level + 1, "Removing the element ([lt] == <): " + e.outerHtml().replaceAll("<", "[lt]"));
		  e.remove();
		}

		return;
	  }
	}
  }
}
