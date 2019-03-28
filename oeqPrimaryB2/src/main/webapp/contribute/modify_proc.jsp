<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page contentType="text/html;charset=UTF-8" errorPage="/error.jsp" %>
<%@page	import="org.apereo.openequella.integration.blackboard.buildingblock.data.WrappedContent" %>
<%@page	import="org.apereo.openequella.integration.blackboard.common.BbUtil" %>
<%@page	import="com.google.common.base.Strings" %>

<%@ taglib uri="/bbNG" prefix="bbng"%>

<%-- Handles the 'save' from edit a resource content object (fancy way of saying oEQ link in Bb) --%>

<%
  WrappedContent content = new WrappedContent(request.getParameter(BbUtil.COURSE_ID), request.getParameter(BbUtil.CONTENT_ID));
  if(Strings.isNullOrEmpty(request.getParameter("name"))) {
    // Assume this is a refresh request
    content.loadOnly(request);
  } else {
    // Assume this is the original request to save the object link
    content.modify(request);
    content.persist(request);
  }

%>

<bbng:learningSystemPage title="Modified openEQUELLA Object Link">
	<bbng:breadcrumbBar environment="COURSE" isContent="true" />

	<bbng:receipt type="SUCCESS" title="Content Updated"
		recallUrl="<%=content.getReferrer(true)%>">
		<h1><%=content.getTitle()%></h1>
		<%=content.getHtml(request, true)%>
		<br>
		<br>
	</bbng:receipt>
</bbng:learningSystemPage>
