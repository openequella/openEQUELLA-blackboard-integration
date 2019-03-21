<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page contentType="text/html;charset=UTF-8" errorPage="/error.jsp" %>
<%@page import="java.util.Collection"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.net.URL"%>
<%@page import="java.net.MalformedURLException"%>
<%@page import="java.util.Iterator"%>
<%@page import="blackboard.platform.plugin.PlugInUtil"%>
<%@page import="blackboard.platform.session.BbSession"%>
<%@page import="blackboard.platform.security.AccessManagerService"%>
<%@page import="blackboard.platform.session.BbSessionManagerService"%>
<%@page import="blackboard.platform.BbServiceManager"%>
<%@page import="blackboard.platform.context.ContextManager"%>
<%@page import="org.apereo.openequella.integration.blackboard.buildingblock.data.WrappedUser"%>
<%@page import="org.apereo.openequella.integration.blackboard.buildingblock.Configuration"%>
<%@page import="org.apereo.openequella.integration.blackboard.common.BbUtil"%>
<%@page import="org.apereo.openequella.integration.blackboard.common.BbLogger"%>
<%@ taglib uri="/bbNG" prefix="bbng"%>

<%! 
WrappedUser user;
private String link(String url) throws Exception
{
	try
	{
		String token = user.getToken();
		if (url.indexOf('?') == -1)
		{
			url += "?";
		}
		else
		{
			url += "&";
		}
		url = new URL(new URL(Configuration.instance().getEquellaUrl()), url).toString();
		return url+"token="+URLEncoder.encode(token,"utf-8");
	}
	catch (MalformedURLException mal)
	{
		return "#";
	}
	catch (Exception e)
	{
		if (e.getCause() instanceof MalformedURLException)
		{
			return "#";
		}
		else
		{
			throw e;
		}
	}
}
%>
<%
ContextManager ctxMgr = (ContextManager)BbServiceManager.lookupService( ContextManager.class );
ctxMgr.setContext( request );
BbSessionManagerService sessionService = BbServiceManager.getSessionManagerService();
BbSession bbSession = sessionService.getSession( request );
AccessManagerService accessManager = (AccessManagerService) BbServiceManager.lookupService( AccessManagerService.class );
if (! bbSession.isAuthenticated()) {
    accessManager.sendLoginRedirect(request,response);
    return;
}

PlugInUtil.authorizeForSystemAdmin(request, response);
String error = "";
String message = "";
Configuration configuration = Configuration.instance();
user = WrappedUser.getUser(request);
try
{
	if(request.getMethod().equals( "POST" ))
	{
		configuration.modify(request);
		configuration.save();
		message = "Your settings have been saved";
	}
}
catch(Exception e)
{
	error = "Error saving settings: " + e.getMessage();
}

configuration.load();
String equellaurl = configuration.getEquellaUrl();
String clientId = configuration.getOauthClientId();
String clientSecret = configuration.getOauthClientSecret();
String secretId = configuration.getSecretId();
String secret = configuration.getSecret();
String restriction = configuration.getRestriction();
String logLevel = configuration.getLogLevel();
boolean newWindowOrTab = configuration.isNewWindowOrTab();
String loggingDetails = BbLogger.instance().getLoggingDetails();

String title = "EQUELLA Server Configuration";
int number = 1;
%>
<bbng:genericPage title="openEQUELLA Configuration">
	<bbng:breadcrumbBar navItem="admin_plugin_manage" environment="SYS_ADMIN">
		<bbng:breadcrumb title="<%=title%>" />
	</bbng:breadcrumbBar>
	<bbng:pageHeader>
		<bbng:pageTitleBar title="<%=title%>" iconUrl="../images/tle.gif" />
	</bbng:pageHeader>

	<form action="config.jsp" method="POST">
		<bbng:dataCollection>
			<bbng:step title="EQUELLA Server Details">
				<bbng:dataElement label="EQUELLA URL" isRequired="true">
					<bbng:textElement isRequired="true" size="100" name="<%=Configuration.EQUELLA_URL%>" value="<%=equellaurl%>"/>
				</bbng:dataElement>
				
				<bbng:dataElement label="LTI Consumer ID" isRequired="true">
					<bbng:textElement isRequired="true" size="40" name="<%=Configuration.OAUTH_CLIENT_ID%>" value="<%=clientId%>"/>
				</bbng:dataElement>
				<bbng:dataElement label="LTI Consumer Secret" isRequired="true">
					<bbng:textElement isRequired="true" size="40" name="<%=Configuration.OAUTH_CLIENT_SECRET%>" value="<%=clientSecret%>"/>
				</bbng:dataElement>
				
				<bbng:dataElement label="Shared Secret ID" isRequired="true">
					<bbng:textElement isRequired="true" size="40" name="<%=Configuration.SECRETID%>" value="<%=secretId%>"/>
				</bbng:dataElement>
				<bbng:dataElement label="Shared Secret Value" isRequired="true">
					<bbng:textElement isRequired="true" size="40" name="<%=Configuration.SECRET%>" value="<%=secret%>"/>
				</bbng:dataElement>

				<% if(message.length() > 0) { %>
					<bbng:dataElement label="" >
						<div style="color:blue"><%=message%></div>
					</bbng:dataElement>
				<% } %>

				<% if(error.length() == 0) { %>
					<bbng:dataElement label="">
						<div><a href="<%=link("jnlp/admin.jnlp?rand="+System.currentTimeMillis())%>" target="_blank">Administration Console</a></div>
					</bbng:dataElement>
				<% } else { %>
					<bbng:dataElement label="Error" >
						<span style="color:red"><%=error%></span>
					</bbng:dataElement>
				<% } %>
			</bbng:step>
			
			<bbng:step title="Options">
        <bbng:dataElement label="Restrict selection of openEQUELLA content">
          <bbng:selectElement name="<%=Configuration.RESTRICTIONS %>" >
            <bbng:selectOptionElement value="none" optionLabel="No restrictions" isSelected="<%=restriction.equals(\"none\") %>"/>
            <bbng:selectOptionElement value="itemonly" optionLabel="Items only" isSelected="<%=restriction.equals(\"itemonly\") %>"/>
            <bbng:selectOptionElement value="attachmentonly" optionLabel="Attachments only" isSelected="<%=restriction.equals(\"attachmentonly\") %>"/>
            <bbng:selectOptionElement value="packageonly" optionLabel="Packages only" isSelected="<%=restriction.equals(\"packageonly\") %>"/>  
          </bbng:selectElement>
        </bbng:dataElement>
      
        <bbng:dataElement label="Override Logging Level of Integration">
          <bbng:selectElement name="<%=Configuration.LOG_LEVEL %>" >
            <bbng:selectOptionElement value="NotSet" optionLabel="Not Set" isSelected="<%=logLevel.equals(\"NotSet\") %>"/>
            <bbng:selectOptionElement value="Warn" optionLabel="Warn" isSelected="<%=logLevel.equals(\"Warn\") %>"/>
            <bbng:selectOptionElement value="Info" optionLabel="Info" isSelected="<%=logLevel.equals(\"Info\") %>"/>
            <bbng:selectOptionElement value="Debug" optionLabel="Debug" isSelected="<%=logLevel.equals(\"Debug\") %>"/>  
            <bbng:selectOptionElement value="SqlTrace" optionLabel="SQL Trace" isSelected="<%=logLevel.equals(\"SqlTrace\") %>"/>
            <bbng:selectOptionElement value="Trace" optionLabel="Trace" isSelected="<%=logLevel.equals(\"Trace\") %>"/>
          </bbng:selectElement>
        </bbng:dataElement>
        
        <bbng:dataElement label="Default new openEQUELLA content to open in a new window or tab">
          <bbng:checkboxElement name="<%=Configuration.NEW_WINDOW_OR_TAB%>" value="true" isSelected="<%=newWindowOrTab%>" />
        </bbng:dataElement>
      </bbng:step>
      
      <bbng:stepSubmit cancelUrl="../../blackboard/admin/manage_plugins.jsp" />
		</bbng:dataCollection>
	</form>

  <div style="font-size: 8pt; color: #C0C0C0">
    Logging:  <%=loggingDetails%>
  </div>
</bbng:genericPage>
