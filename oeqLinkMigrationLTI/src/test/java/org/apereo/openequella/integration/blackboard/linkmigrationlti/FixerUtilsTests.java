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

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;

public class FixerUtilsTests {
  public static final String OEQ_URL = "https://apereo.org/not/a/real/oeq/domain";
  public static final String TEST_BODY_1 =
	"<!--<item id=\"ee5ee6e4-2e2e-4444-4444-ee6c44543977\" itemdefid=\"ee5ee6e4-2e2e-4444-4444-ee6c44543979\" " +
	  "itemstatus=\"live\" key=\"11\" moderating=\"false\" version=\"1\"><name>Test name: is this real item name?</name>" +
	  "<newitem>false</newitem><owner>apereouser</owner><datecreated>2011-10-14T11:11:11-0100</datecreated>" +
	  "<datemodified>2011-12-14T15:03:53-0100</datemodified><dateforindex>2011-12-14T15:03:53-0100</dateforindex>" +
	  "<rating average=\"-1.0\"/><attachments selectedTitle=\"Test name: is this real att name?\"/>" +
	  "<badurls/><history><edit applies=\"false\" date=\"2011-12-14T15:03:53-0100\" state=\"draft\" " +
	  "user=\"cmdalzell\">apereouser</edit><resetworkflow applies=\"false\" date=\"2011-12-14T15:03:53-0100\" " +
	  "state=\"draft\" user=\"apereouser\">apereouser</resetworkflow><statechange applies=\"false\" " +
	  "date=\"2011-12-14T15:03:53-0100\" state=\"live\" user=\"apereouser\">apereouser</statechange>" +
	  "</history><moderation><liveapprovaldate>2011-12-14T15:03:53-0100</liveapprovaldate></moderation>" +
	  "</item>--><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"font-size:12pt\" " +
	  "width=\"450px\"><tr><td  colspan=\"2\"></td></tr><tr><td valign=\"top\"><table border=\"0\" " +
	  "cellspacing=\"0\" cellpadding=\"0\" style=\"font-size:12pt\" width=\"100%\"><tr><td></td></tr>" +
	  "<tr><td  colspan=\"2\"></td></tr><tr><td><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" " +
	  "style=\"font-size:12pt\" ><td ><img src=\"https://apereo.org/attachment.gif\" alt=\"*\" " +
	  "style=\"border:none;\" /></td><td>&nbsp;&nbsp;<a href=\"/webapps/dych-tle-BB4ee4eee444e44/ViewContent?" +
	  "type=default&content_id=@X@content.pk_string@X@&course_id=@X@course.pk_string@X@&page=\" " +
	  "class=\"info\">Test name: is this real bb name?</a></td></table></td></tr></table></td></tr></table>";
  public static final String TEST_BODY_1_CLEANED =
	"<!--<item id=\"ee5ee6e4-2e2e-4444-4444-ee6c44543977\" itemdefid=\"ee5ee6e4-2e2e-4444-4444-ee6c44543979\" " +
	  "itemstatus=\"live\" key=\"11\" moderating=\"false\" version=\"1\"><name>Test name: is this real item name?</name>" +
	  "<newitem>false</newitem><owner>apereouser</owner><datecreated>2011-10-14T11:11:11-0100</datecreated>" +
	  "<datemodified>2011-12-14T15:03:53-0100</datemodified><dateforindex>2011-12-14T15:03:53-0100</dateforindex>" +
	  "<rating average=\"-1.0\"/><attachments selectedTitle=\"Test name: is this real att name?\"/>" +
	  "<badurls/><history><edit applies=\"false\" date=\"2011-12-14T15:03:53-0100\" state=\"draft\" " +
	  "user=\"cmdalzell\">apereouser</edit><resetworkflow applies=\"false\" date=\"2011-12-14T15:03:53-0100\" " +
	  "state=\"draft\" user=\"apereouser\">apereouser</resetworkflow><statechange applies=\"false\" " +
	  "date=\"2011-12-14T15:03:53-0100\" state=\"live\" user=\"apereouser\">apereouser</statechange>" +
	  "</history><moderation><liveapprovaldate>2011-12-14T15:03:53-0100</liveapprovaldate></moderation>" +
	  "</item>--><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"font-size:12pt\" " +
	  "width=\"450px\"><tr><td  colspan=\"2\"></td></tr><tr><td valign=\"top\"><table border=\"0\" " +
	  "cellspacing=\"0\" cellpadding=\"0\" style=\"font-size:12pt\" width=\"100%\"><tr><td></td></tr>" +
	  "<tr><td  colspan=\"2\"></td></tr><tr><td/></tr></table></td></tr></table>";
  public static final String TEST_BODY_2 = "<!--<item id=\"ee5ee6e4-2e2e-4444-4444-ee6c44543971\" " +
	"itemdefid=\"ee5ee6e4-2e2e-4444-4444-ee6c12543977\" itemstatus=\"live\" key=\"2\" moderating=\"false\" " +
	"version=\"0\"><name>Test name - 2 item</name><newitem>false</newitem><owner>apereouser2</owner>" +
	"<datecreated>2012-08-14T14:30:05-0400</datecreated><datemodified>2012-08-19T14:50:05-0400</datemodified>" +
	"<dateforindex>2012-08-14T12:50:25-0400</dateforindex><rating average=\"-1.0\"/><attachments " +
	"selected=\"ee5ee6e4-2e2e-4444-4444-ee6c12543911\" selectedTitle=\"Test name - 2 att.zip\">" +
	"<attachment mapped=\"false\" type=\"zip\"><uuid>ee5ee6e4-2e2e-4444-4444-ee6c12543911</uuid>" +
	"<file>_zips/Test name - 2 att.zip</file>" +
	"<description>Test name - 2 att desc.zip</description></attachment></attachments>" +
	"<badurls/><history><edit applies=\"false\" date=\"2019-08-19T14:50:05-0400\" state=\"draft\" " +
	"user=\"apereo2\">apereo2</edit><resetworkflow applies=\"false\" date=\"2002-08-10T14:43:05-0400\" " +
	"state=\"draft\" user=\"apereo2\">apereo2</resetworkflow><statechange applies=\"false\" " +
	"date=\"2002-08-14T04:50:05-0400\" state=\"live\" user=\"apereo2\">apereo2</statechange></history>" +
	"<moderation><liveapprovaldate>2010-04-13T14:52:05-0400</liveapprovaldate></moderation></item>-->" +
	"<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"font-size:12pt\" width=\"450px\"><tr>" +
	"<td  colspan=\"2\"></td></tr><tr><td valign=\"top\"><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" " +
	"style=\"font-size:12pt\" width=\"100%\"><tr><td></td></tr><tr><td  colspan=\"2\"></td></tr><tr><td>" +
	"<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"font-size:12pt\" ><td >" +
	"<img src=\"https://apereo.org/oeq/icons/attachment.gif\" alt=\"*\" style=\"border:none;\" />" +
	"</td><td>&nbsp;&nbsp;<a href=\"/webapps/dych-tle-BB432345/ViewContent?type=default&content_id=" +
	"@X@content.pk_string@X@&course_id=@X@course.pk_string@X@&page=_zips/Test%20name%20-%202%20att.zip\" " +
	"class=\"info\">Test name - 2 att.zip</a></td></table></td></tr></table></td></tr></table>";
  public static final String TEST_BODY_2_CLEANED = "<!--<item id=\"ee5ee6e4-2e2e-4444-4444-ee6c44543971\" " +
	"itemdefid=\"ee5ee6e4-2e2e-4444-4444-ee6c12543977\" itemstatus=\"live\" key=\"2\" moderating=\"false\" " +
	"version=\"0\"><name>Test name - 2 item</name><newitem>false</newitem><owner>apereouser2</owner>" +
	"<datecreated>2012-08-14T14:30:05-0400</datecreated><datemodified>2012-08-19T14:50:05-0400</datemodified>" +
	"<dateforindex>2012-08-14T12:50:25-0400</dateforindex><rating average=\"-1.0\"/><attachments " +
	"selected=\"ee5ee6e4-2e2e-4444-4444-ee6c12543911\" selectedTitle=\"Test name - 2 att.zip\">" +
	"<attachment mapped=\"false\" type=\"zip\"><uuid>ee5ee6e4-2e2e-4444-4444-ee6c12543911</uuid>" +
	"<file>_zips/Test name - 2 att.zip</file>" +
	"<description>Test name - 2 att desc.zip</description></attachment></attachments>" +
	"<badurls/><history><edit applies=\"false\" date=\"2019-08-19T14:50:05-0400\" state=\"draft\" " +
	"user=\"apereo2\">apereo2</edit><resetworkflow applies=\"false\" date=\"2002-08-10T14:43:05-0400\" " +
	"state=\"draft\" user=\"apereo2\">apereo2</resetworkflow><statechange applies=\"false\" " +
	"date=\"2002-08-14T04:50:05-0400\" state=\"live\" user=\"apereo2\">apereo2</statechange></history>" +
	"<moderation><liveapprovaldate>2010-04-13T14:52:05-0400</liveapprovaldate></moderation></item>-->" +
	"<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"font-size:12pt\" width=\"450px\"><tr>" +
	"<td  colspan=\"2\"></td></tr><tr><td valign=\"top\"><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" " +
	"style=\"font-size:12pt\" width=\"100%\"><tr><td></td></tr><tr><td  colspan=\"2\"></td></tr><tr><td/>" +
	"</tr></table></td></tr></table>";
  public static final String TEST_BODY_3 = "<!--<item id=\"ee5ee6e4-2e2e-4444-4444-ff6c44543971\" " +
	"itemdefid=\"ee5ee6e4-2e2e-4444-4774-ee6c44543971\" itemstatus=\"live\" key=\"99\" moderating=\"false\" " +
	"version=\"0\"><name>Test body 3 name</name><newitem>false</newitem><thumbnail>default</thumbnail>" +
	"<owner>apereo1</owner><datecreated>2017-02-22T22:22:22-0400</datecreated><datemodified>" +
	"2017-02-22T22:22:22-0400</datemodified><dateforindex>2017-02-22T22:22:22-0400</dateforindex>" +
	"<rating average=\"-1.0\"/><attachments selected=\"https://apereo.org/integ/gen/" +
	"ee5ee6e4-2ff-4444-4444-ff6c44543971/0/?attachment.uuid=ee5ee6e4-2e2e-4444-4444-ff6e44543971\" " +
	"selectedMimeType=\"application/fanciness\" " +
	"selectedTitle=\"selected title name.docx\" selectedType=\"file\"><attachment type=\"local\">" +
	"<conversion>false</conversion><size>11411</size><uuid>ee5ee6e4-2e2e-4444-4444-ff6e44543971</uuid>" +
	"<file>att title file.docx</file><description>att name.docx</description><attributes>\n" +
	"  <entry>\n" +
	"    <string>wordcount</string>\n" +
	"    <string>4</string>\n" +
	"  </entry>\n" +
	"  <entry>\n" +
	"    <string>publisher</string>\n" +
	"    <string>Apereo</string>\n" +
	"  </entry>\n" +
	"  <entry>\n" +
	"    <string>pagecount</string>\n" +
	"    <string>1</string>\n" +
	"  </entry>\n" +
	"  <entry>\n" +
	"    <string>author</string>\n" +
	"    <string>A, Apereo</string>\n" +
	"  </entry>\n" +
	"  <entry>\n" +
	"    <string>lastmodified</string>\n" +
	"    <date>2017-09-22 22:22:00.0 EDT</date>\n" +
	"  </entry>\n" +
	"</attributes></attachment></attachments><badurls/><history><contributed applies=\"false\" " +
	"date=\"2017-02-22T22:22:22-0400\" state=\"draft\" user=\"apereo1\">apereo1</contributed>" +
	"</history><moderation><liveapprovaldate>2017-02-22T22:22:22-0400</liveapprovaldate>" +
	"</moderation></item>--><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" " +
	"style=\"font-size:12pt\" width=\"450px\"><tr><td colspan=\"2\">" +
	"<img src=\"https://.../prod/images/spacer.gif\" alt=\" \" style=\"border:none; width:0px; " +
	"height:4px;\"></td></tr><tr><td valign=\"top\"><table border=\"0\" cellspacing=\"0\" " +
	"cellpadding=\"0\" style=\"font-size:12pt\" width=\"100%\"><tr><td class=\"bbdesc\"></td>" +
	"</tr><tr><td colspan=\"2\"><img src=\"https://.../prod/images/spacer.gif\" alt=\" \" " +
	"style=\"border:none; width:0px; height:4px;\"></td></tr><tr><td><table border=\"0\" " +
	"cellspacing=\"0\" cellpadding=\"0\" style=\"font-size:12pt\" ><td >" +
	"<img src=\"https://apereo.org//icons/attachment.gif\" alt=\"*\" style=\"border:none;\" />" +
	"</td><td>&nbsp;&nbsp;<a href=\"/webapps/dych-tle-BB599999999/ViewContent?type=" +
	"default&content_id=@X@content.pk_string@X@&course_id=@X@course.pk_string@X@&page=" +
	"https://apereo.org/integ/gen/ee5ff6e4-2e2e-4444-4444-ff6c44543971/0/?attachment.uuid=" +
	"ee5ee6e4-2e2e-4444-4444-ff6ee4543971\" class=\"info\">class info test 3.docx</a></td>" +
	"</table></td></tr></table></td></tr></table>";
  public static final String TEST_BODY_3_CLEANED = "<!--<item id=\"ee5ee6e4-2e2e-4444-4444-ff6c44543971\" " +
	"itemdefid=\"ee5ee6e4-2e2e-4444-4774-ee6c44543971\" itemstatus=\"live\" key=\"99\" moderating=\"false\" " +
	"version=\"0\"><name>Test body 3 name</name><newitem>false</newitem><thumbnail>default</thumbnail>" +
	"<owner>apereo1</owner><datecreated>2017-02-22T22:22:22-0400</datecreated><datemodified>" +
	"2017-02-22T22:22:22-0400</datemodified><dateforindex>2017-02-22T22:22:22-0400</dateforindex>" +
	"<rating average=\"-1.0\"/><attachments selected=\"https://apereo.org/integ/gen/" +
	"ee5ee6e4-2ff-4444-4444-ff6c44543971/0/?attachment.uuid=ee5ee6e4-2e2e-4444-4444-ff6e44543971\" " +
	"selectedMimeType=\"application/fanciness\" " +
	"selectedTitle=\"selected title name.docx\" selectedType=\"file\"><attachment type=\"local\">" +
	"<conversion>false</conversion><size>11411</size><uuid>ee5ee6e4-2e2e-4444-4444-ff6e44543971</uuid>" +
	"<file>att title file.docx</file><description>att name.docx</description><attributes>\n" +
	"  <entry>\n" +
	"    <string>wordcount</string>\n" +
	"    <string>4</string>\n" +
	"  </entry>\n" +
	"  <entry>\n" +
	"    <string>publisher</string>\n" +
	"    <string>Apereo</string>\n" +
	"  </entry>\n" +
	"  <entry>\n" +
	"    <string>pagecount</string>\n" +
	"    <string>1</string>\n" +
	"  </entry>\n" +
	"  <entry>\n" +
	"    <string>author</string>\n" +
	"    <string>A, Apereo</string>\n" +
	"  </entry>\n" +
	"  <entry>\n" +
	"    <string>lastmodified</string>\n" +
	"    <date>2017-09-22 22:22:00.0 EDT</date>\n" +
	"  </entry>\n" +
	"</attributes></attachment></attachments><badurls/><history><contributed applies=\"false\" " +
	"date=\"2017-02-22T22:22:22-0400\" state=\"draft\" user=\"apereo1\">apereo1</contributed>" +
	"</history><moderation><liveapprovaldate>2017-02-22T22:22:22-0400</liveapprovaldate>" +
	"</moderation></item>--><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" " +
	"style=\"font-size:12pt\" width=\"450px\"><tr><td colspan=\"2\">" +
	"<img src=\"https://.../prod/images/spacer.gif\" alt=\" \" style=\"border:none; width:0px; " +
	"height:4px;\"></td></tr><tr><td valign=\"top\"><table border=\"0\" cellspacing=\"0\" " +
	"cellpadding=\"0\" style=\"font-size:12pt\" width=\"100%\"><tr><td class=\"bbdesc\"></td>" +
	"</tr><tr><td colspan=\"2\"><img src=\"https://.../prod/images/spacer.gif\" alt=\" \" " +
	"style=\"border:none; width:0px; height:4px;\"></td></tr><tr><td/></tr></table></td></tr></table>";
  public static String TEST_BODY_4 = "<!--<item id=\"ee5ee6e4-2f2e-4444-4444-ff6ee4543971\" " +
	"itemdefid=\"ee5ee6e4-2e2e-4446-4444-ff6ee4543971\" link=\"false\" live=\"false\" " +
	"modified=\"false\" notify=\"true\" version=\"0\"><name>test body 4 name metadata</name>" +
	"<description/><requestUuid/><attachments selected=\"2.%20Module%202%20SelectedAttByName%2021.pptx\" " +
	"selectedDescription=\"\" selectedTitle=\"selected title.pptx\"><attachment scorm=\"\">" +
	"<file>att name. in att file.pptx</file><description>att desc. in att desc.pptx</description>" +
	"</attachment></attachments></item>--><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" " +
	"style=\"font-size:12pt\" width=\"450px\"><tr><td  colspan=\"2\"><img " +
	"src=\"https://apereo.org/images/spacer.gif\"\t \r \n alt=\" \" style=\"border:none; " +
	"width:0px; height:4px;\" /></td></tr><tr><td valign=\"top\"><table border=\"0\" cellspacing=\"0\" " +
	"cellpadding=\"0\" style=\"font-size:12pt\" width=\"100%\"><tr><td></td></tr><tr>" +
	"<td  colspan=\"2\"><img src=\"https://apereo.org/images/spacer.gif\" alt=\"*\" style=\"border:none; " +
	"width:0px; height:5px;\" /></td></tr>" +
	"<tr><td><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" " +
	"style=\"font-size:12pt\" ><td ><img src=\"https://apereo.org//icons/attachment.gif\" " +
	"alt=\"*\" style=\"border:none;\" /></td><td>&nbsp;&nbsp;<a href=\"/webapps/dych-tle-BB599/" +
	"ViewContent?type=default&content_id=@X@content.pk_string@X@&course_id=@X@course.pk_string@X@&page=" +
	"randomness%20forSearch.pptx\" class=\"info\">2. Module 2 Selected Att by anchor class.pptx</a></td></table>" +
	"</td></tr></table></td><td ><img src=\"https://apereo.org//icons/attachment.gif\" alt=\"*\" style=\"border:none;\" /></td></tr></table>";
  public static String TEST_BODY_4_CLEANED = "<!--<item id=\"ee5ee6e4-2f2e-4444-4444-ff6ee4543971\" " +
	"itemdefid=\"ee5ee6e4-2e2e-4446-4444-ff6ee4543971\" link=\"false\" live=\"false\" " +
	"modified=\"false\" notify=\"true\" version=\"0\"><name>test body 4 name metadata</name>" +
	"<description/><requestUuid/><attachments selected=\"2.%20Module%202%20SelectedAttByName%2021.pptx\" " +
	"selectedDescription=\"\" selectedTitle=\"selected title.pptx\"><attachment scorm=\"\">" +
	"<file>att name. in att file.pptx</file><description>att desc. in att desc.pptx</description>" +
	"</attachment></attachments></item>--><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" " +
	"style=\"font-size:12pt\" width=\"450px\"><tr><td valign=\"top\"><table border=\"0\" cellspacing=\"0\" " +
	"cellpadding=\"0\" style=\"font-size:12pt\" width=\"100%\"><tr><td></td></tr><tr><td/></tr></table></td><td /></tr></table>";
  public static String TEST_BODY_5 = "<div class=\"equella-link contextItemDetailsHeaders\"><a target=\"_blank\" " +
	"href=\"/webapps/dych-tle-BB59999/ViewContent?content_id=@X@content.pk_string@X@&course_id=" +
	"@X@course.pk_string@X@\"><img alt=\"application.document\" src=\"https://apereo.org/icons/attachment.gif\" " +
	"style=\"margin-right:10px\">div title.docx</a></div>";
  public static String TEST_BODY_5_CLEANED = "<div class=\"equella-link\"></div>";

  @Test
  public void testGetDomainName() {
	try {
	  Assert.assertEquals("asdf", FixerUtils.getDomainName("asdf"));
	  Assert.assertEquals("apereo.org", FixerUtils.getDomainName("www.apereo.org"));
	  Assert.assertEquals("apereo.org", FixerUtils.getDomainName("https://www.apereo.org"));
	  Assert.assertEquals("apereo.org", FixerUtils.getDomainName("http://www.apereo.org"));

	} catch (MalformedURLException e) {
	  Assert.fail(e.getMessage());
	}
  }

  @Test
  public void testIsUrlPresent() {
	FixerUtils utils = new FixerUtils(null);
	Assert.assertEquals(false, utils.isUrlPresent(0, null));
	Assert.assertEquals(false, utils.isUrlPresent(0, ""));
	Assert.assertEquals(true, utils.isUrlPresent(0, "asdf"));
  }

  @Test
  public void testFindItemId() {
	FixerUtils utils = new FixerUtils(null);
	Assert.assertEquals(null, FixerUtils.findItemId(""));
	Assert.assertEquals(null, FixerUtils.findItemId("item id"));
	Assert.assertEquals("ee5ee6e4-2e2e-4444-4444-ee6c44543977", FixerUtils.findItemId(TEST_BODY_1));
  }

  @Test
  public void testFindItemVersion() {
	FixerUtils utils = new FixerUtils(null);
	Assert.assertEquals(null, FixerUtils.findItemVersion(""));
	Assert.assertEquals(null, FixerUtils.findItemVersion("item version"));
	Assert.assertEquals("1", FixerUtils.findItemVersion(TEST_BODY_1));
  }

  @Test
  public void testFindSelectedItemAttachment() {
	FixerUtils utils = new FixerUtils(null);
	Assert.assertEquals(null, FixerUtils.findSelectedItemAttachment(""));
	Assert.assertEquals(null, FixerUtils.findSelectedItemAttachment("item version"));
	Assert.assertEquals(null, FixerUtils.findSelectedItemAttachment(TEST_BODY_1));
	Assert.assertEquals("ee5ee6e4-2e2e-4444-4444-ee6c12543911", FixerUtils.findSelectedItemAttachment(TEST_BODY_2));
  }

  @Test
  public void testMigrateWithExtendedDataUrl() {
	FixerUtils utils = new FixerUtils(null);
	FixerResponse resp = utils.migrate(0, OEQ_URL, "integ/gen/uuid/version/",
	  "xd title", "xd desc", TEST_BODY_5, new String[0]);
	Assert.assertEquals(utils.getLog(), true, resp.isValidResponse());
	Assert.assertEquals(utils.getLog(), OEQ_URL + "/integ/gen/uuid/version/", resp.getNewUrl());
	Assert.assertEquals(utils.getLog(), "xd title", resp.getNewName());
	Assert.assertEquals(utils.getLog(), "xd desc", resp.getNewDescription());
	Assert.assertEquals(utils.getLog(), TEST_BODY_5_CLEANED, resp.getNewBody());
  }

  @Test
  public void testMigrateWithEmptyUrlAndItemSummary() {
	FixerUtils utils = new FixerUtils(null);
	FixerResponse resp = utils.migrate(0, OEQ_URL, null, null, null, TEST_BODY_1, new String[0]);
	Assert.assertEquals(utils.getLog(), true, resp.isValidResponse());
	Assert.assertEquals(utils.getLog(), OEQ_URL + "/integ/gen/ee5ee6e4-2e2e-4444-4444-ee6c44543977/1/", resp.getNewUrl());
	Assert.assertEquals(utils.getLog(), "Test name: is this real bb name?", resp.getNewName());
	Assert.assertEquals(utils.getLog(), "", resp.getNewDescription());
	Assert.assertEquals(utils.getLog(), TEST_BODY_1_CLEANED, resp.getNewBody());
  }

  @Test
  public void testMigrateWithEmptyUrlAndAttachmentByUuidSelected() {
	FixerUtils utils = new FixerUtils(null);
	FixerResponse resp = utils.migrate(0, OEQ_URL, null, null, null, TEST_BODY_2, new String[0]);
	System.out.println(utils.getLog());
	Assert.assertEquals(utils.getLog(), true, resp.isValidResponse());
	Assert.assertEquals(utils.getLog(), OEQ_URL + "/integ/gen/ee5ee6e4-2e2e-4444-4444-ee6c44543971/0/" +
	  "?attachment.uuid=ee5ee6e4-2e2e-4444-4444-ee6c12543911", resp.getNewUrl());
	Assert.assertEquals(utils.getLog(), "Test name - 2 att.zip", resp.getNewName());
	Assert.assertEquals(utils.getLog(), "", resp.getNewDescription());
	Assert.assertEquals(utils.getLog(), TEST_BODY_2_CLEANED, resp.getNewBody());
  }

  @Test
  public void testMigrateWithEmptyUrlAndAttachmentByIntegLinkSelected() {
	FixerUtils utils = new FixerUtils(null);
	FixerResponse resp = utils.migrate(0, OEQ_URL, null, null, null, TEST_BODY_3, new String[0]);
	utils.log(0, "PRE....:" + TEST_BODY_3);
	utils.log(0, "POST...:" + TEST_BODY_3_CLEANED);
	utils.log(0, "CLEANED:" + resp.getNewBody());
	Assert.assertEquals(utils.getLog(), true, resp.isValidResponse());
	Assert.assertEquals(utils.getLog(), "https://apereo.org/integ/gen/ee5ee6e4-2ff-" +
	  "4444-4444-ff6c44543971/0/?attachment.uuid=ee5ee6e4-2e2e-4444-4444-ff6e44543971", resp.getNewUrl());
	Assert.assertEquals(utils.getLog(), "class info test 3.docx", resp.getNewName());
	Assert.assertEquals(utils.getLog(), "", resp.getNewDescription());
	Assert.assertEquals(utils.getLog(), TEST_BODY_3_CLEANED, resp.getNewBody());
  }

  @Test
  public void testMigrateWithEmptyUrlAndViaAttachmentBySelectedFilename() {
	FixerUtils utils = new FixerUtils(null);
	String[] urls = "https://apereo.org//icons/attachment.gif,https://apereo.org//images/spacer.gif,https://apereo.org/images/spacer.gif".split(",");

	FixerResponse resp = utils.migrate(0, OEQ_URL, null, null, null, TEST_BODY_4, urls);
	System.out.println(utils.getLog());
	Assert.assertEquals(utils.getLog(), true, resp.isValidResponse());
	Assert.assertEquals(utils.getLog(), "https://apereo.org/not/a/real/oeq/domain/integ/gen/ee5ee6e4-2f2e" +
	  "-4444-4444-ff6ee4543971/0/2.%20Module%202%20SelectedAttByName%2021.pptx", resp.getNewUrl());
	Assert.assertEquals(utils.getLog(), "2. Module 2 Selected Att by anchor class.pptx", resp.getNewName());
	Assert.assertEquals(utils.getLog(), "", resp.getNewDescription());
	Assert.assertEquals(utils.getLog(), TEST_BODY_4_CLEANED, resp.getNewBody());
  }
}
