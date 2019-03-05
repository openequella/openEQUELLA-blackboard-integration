package org.apereo.openequella.integration.blackboard.oeqAudit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//import javax.servlet.http.HttpServletRequest;
import blackboard.platform.BbServiceManager;

public class Audit {

	public static void main(String[] args) {

	}

	public static List<String> auditClasses() {
		List<String> audit = new ArrayList<>();
		try {
			audit.add("Available:  " + blackboard.data.blti.BasicLTIDomainConfig.class.getName());
		} catch (Exception e) {
			audit.add("MISSING:  blackboard.data.blti.BasicLTIDomainConfig");
		}

		try {
			audit.add("Available:  " + blackboard.platform.blti.BasicLTIDomainConfigManager.class.getName());
		} catch (Exception e) {
			audit.add("MISSING:  blackboard.platform.blti.BasicLTIDomainConfigManager");
		}

		try {
			audit.add("Available:  " + blackboard.data.blti.BasicLTIPlacement.class.getName());
		} catch (Exception e) {
			audit.add("MISSING:  blackboard.data.blti.BasicLTIPlacement");
		}

		try {
			audit.add("Available:  " + blackboard.data.blti.BasicLTIDomainHost.class.getName());
		} catch (Exception e) {
			audit.add("MISSING:  blackboard.data.blti.BasicLTIDomainHost");
		}

		// "blackboard.data.blti.BasicLTIDomainConfig.SendUserData",
		// "blackboard.data.blti.BasicLTIDomainConfig.Status",
		// "blackboard.data.blti.BasicLTIPlacement.Type",
		// "blackboard.data.gradebook.impl.OutcomeDefinition",
		// "blackboard.db.BbDatabase", "blackboard.db.ConnectionManager",
		// "blackboard.db.DbUtil",
		// "blackboard.persist.gradebook.impl.OutcomeDefinitionDbLoader",
		// "blackboard.persist.impl.SimpleSelectQuery",
		// "blackboard.platform.blti.BasicLTIDomainConfigManager",
		// "blackboard.platform.blti.BasicLTIDomainConfigManagerFactory",
		// "blackboard.platform.blti.BasicLTIPlacementManager",
		// "blackboard.platform.ContentWrapperHelper",
		// "blackboard.platform.gradebook2.GradableItem",
		// "blackboard.platform.gradebook2.GradableItemManager",
		// "blackboard.platform.gradebook2.GradebookManagerFactory",
		// "blackboard.platform.gradebook2.impl.GradableItemDAO",
		// "blackboard.platform.gradebook2.impl.ScoreProviderDAO",
		// "blackboard.platform.gradebook2.ScoreProvider",
		// "blackboard.platform.plugin.ContentHandler",
		// "blackboard.platform.plugin.ContentHandlerDbLoader",
		// "blackboard.platform.plugin.ContentHandlerDbPersister",
		// "blackboard.platform.plugin.ContentHandlerType.ActionType",
		// "blackboard.platform.plugin.PlugInException",
		// "blackboard.platform.query.Criteria",
		// "blackboard.platform.query.CriterionBuilder",
		// "blackboard.platform.security.AccessManagerService",
		// "blackboard.platform.session.BbSessionManagerServiceExFactory",
		// "blackboard.platform.vxi.data.VirtualHost",
		// "blackboard.platform.vxi.data.VirtualInstallation",
		// "blackboard.platform.vxi.service.VirtualInstallationManager",
		// "blackboard.platform.ws.AxisHelpers", "blackboard.platform.ws.SessionVO",
		// "blackboard.platform.ws.WebserviceContext",
		// "blackboard.platform.ws.WebserviceException",
		// "blackboard.platform.ws.WebserviceLogger" };

		return audit;
	}

	private volatile static Audit instance;

	protected Audit() {

	}

	public static Audit instance() {
		if (instance != null) {
			return instance;
		}

		instance = new Audit();
		return instance;
	}

	public String getAudit() {
		// Test to pull in the dep.
		String state = BbServiceManager.getState().name();
		return "A successful test on " + (new Date()) + " State: " + state;
	}

	public String getError() {
		return null;// "An error msg here";
	}

}
