<%@page import="
			java.util.*,
		  java.text.*,
			java.io.*,
			java.util.*,
			org.apereo.openequella.integration.blackboard.linkmigrationlti.*"
%>

<%@ taglib uri="/bbUI" prefix="ui"%>


	<%
	String error = null;
	String message = null;

	Fixer fixer = Fixer.instance();

	try
	{
		fixer.load();
		if(request.getMethod().equals( "POST" ))
		{
			fixer.submit(request);
		}
		if (fixer.hasCompleted())
		{
		message = "Migration has finished.";
	}
		else if (fixer.hasStarted())
		{
		message = "Migration has been started.  Press Submit again to refresh the page and view the log output.";
		}
		else
		{
		message = "Migration is ready to start.";
		}
	}
	catch(Exception e)
	{
		StringWriter s = new StringWriter();
		PrintWriter w = new PrintWriter(s);
		e.printStackTrace(w);
		error = s.toString();
	}

	String title = "openEQUELLA LTI Link Migration Plugin";
	%>

	<ui:docTemplate title="openEQUELLA Configuration">
		<ui:breadcrumbBar handle="admin_plugin_manage">
			<ui:breadcrumb><%=title%></ui:breadcrumb>
		</ui:breadcrumbBar>

		<ui:titleBar iconUrl="../images/openEquella.gif"><%=title%></ui:titleBar>
			<form action="config.jsp" method="POST">

				<ui:step title="openEQUELLA Plugin settings">
					<ui:dataElement label="Institution URL">
  						<span><%=fixer.getEquellaUrl()%></span>
  					</ui:dataElement>
  					<ui:dataElement label="Placement Handle">
            					<input type="text" size="75" name="<%=Fixer.PLACEMENT%>"/>
            </ui:dataElement>
            <ui:dataElement label="Course External ID (Empty for all the courses)">
                       <input type="text" size="75" name="<%=Fixer.COURSEID%>"/>
            </ui:dataElement>
            <ui:dataElement label="Image URLs to Remove (CSV)">
                       <input type="text" size="75" name="<%=Fixer.IMAGE_URL_CSV%>"/>
            </ui:dataElement>
				</ui:step>

				<%
				if (fixer.canReset())
				{
				%>
					<ui:step title="Reset">
            <ui:dataElement label="Check this box to reset the migration state when the Submit button is clicked">
                <input type="checkbox" name="<%=Fixer.RESET%>" />
              </ui:dataElement>
          </ui:step>
				<%
				} else if (!fixer.hasStarted() && !fixer.hasCompleted()) {
				%>
          <ui:step title="Start Migration">
            <ui:dataElement label="Check this box to begin execution when the Submit button is clicked">
                <input type="checkbox" name="<%=Fixer.EXECUTE%>" />
              </ui:dataElement>
          </ui:step>

          <ui:step title="Dry Run">
            <ui:dataElement label="Check this box to perform a DRY RUN when the Submit button is clicked">
                <input type="checkbox" name="<%=Fixer.DRYRUN%>" />
              </ui:dataElement>
          </ui:step>
        <%
				}
				%>

				<ui:step title="Status">

						<ui:dataElement label="Execution status:">
		  					<div><%=fixer.getStatus()%></div>
		  				</ui:dataElement>

				    	<%
						if(error != null)
						{
						%>
							<ui:dataElement label="Error" ><div style="{color:red}">
								<div><pre><%=error%></pre></div>
				   			</ui:dataElement>
						<%
						}

						if(message != null)
						{
						%>
						   	<ui:dataElement label="" >
						   		<div style="{color:blue}"><%=message%></div>
			   				</ui:dataElement>
						<%
						}
						%>
				</ui:step>
        <ui:stepSubmit title="Submit" />
			</form>

			<%
			final String log = fixer.getLog();
			if (log.length() > 0)
			{
			%><div style="font-family: monospace; font-size: larger;"><pre><%=log%></pre></div>
			<%
			}
			%>
	</ui:docTemplate>
