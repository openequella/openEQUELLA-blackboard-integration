<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page contentType="text/html;charset=UTF-8" %>
<%@page
	import="java.util.*,
			java.text.*,
			java.io.*,
			org.apereo.openequella.integration.blackboard.oeqAudit.Audit"%>

<%@ taglib uri="/bbUI" prefix="ui"%>


<%

	Audit audit = Audit.instance();

	String ex = null;
	String message = null;

	try
	{
		message = audit.getAudit();
	}
	catch( Exception e )
	{
		StringWriter s = new StringWriter();
		PrintWriter w = new PrintWriter(s);
		e.printStackTrace(w);
		ex = s.toString();
	}
	String error = (audit.getError() == null ? ex : audit.getError());

	String title = "openEQUELLA B2 Audit Plugin";
	
	List<String> auditForClasses = audit.auditClasses();
%>

<ui:docTemplate title="openEQUELLA Audit">
	<ui:breadcrumbBar handle="admin_plugin_manage">
		<ui:breadcrumb><%=title%></ui:breadcrumb>
	</ui:breadcrumbBar>
	<h2>Audit of B2 oEQ links</h2>
	<%
		if(error != null)
		{
	%>
			<ui:dataElement label="Error" >
				<div style="{color:red}">
					<div><pre><%=error%></pre>
				</div>
	  		</ui:dataElement>
	 <%  }
    if(message != null)
    {
   %>
      <ui:dataElement label="" >
          <div style="{color:blue}">
            <%=message%>
          </div>
      </ui:dataElement>

   <% }
    if(audit != null)
    {
    for(String auditBit : auditForClasses) {
   %>
      <div style="{color:blue}">
            <%=auditBit%>
      </div>
      

   <% } }%>
	 
	 
	 
</ui:docTemplate>