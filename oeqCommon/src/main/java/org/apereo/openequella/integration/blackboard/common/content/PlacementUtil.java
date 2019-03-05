package org.apereo.openequella.integration.blackboard.common.content;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import blackboard.data.blti.BasicLTIDomainConfig;
import blackboard.data.blti.BasicLTIPlacement;
import blackboard.data.blti.BasicLTIPlacement.Type;
import blackboard.persist.Id;
import blackboard.persist.KeyNotFoundException;
import blackboard.platform.blti.BasicLTIDomainConfigManager;
import blackboard.platform.blti.BasicLTIDomainConfigManagerFactory;
import blackboard.platform.blti.BasicLTIPlacementManager;
import blackboard.platform.plugin.ContentHandler;
import blackboard.platform.plugin.ContentHandlerType.ActionType;

import com.google.common.base.Throwables;

import org.apereo.openequella.integration.blackboard.common.BbLogger;
import org.apereo.openequella.integration.blackboard.common.BbUtil;
import org.apereo.openequella.integration.blackboard.common.SqlUtil;
import org.apereo.openequella.integration.blackboard.common.SqlUtil.ResultProcessor;

/**
 * @author Aaron
 */
@SuppressWarnings("nls")
// @NonNullByDefault
public abstract class PlacementUtil {
	private static final String LTI_PLACEMENT_TITLE = "openEQUELLA LTI Integration";

	public PlacementUtil() {
		throw new Error();
	}

	public static BasicLTIPlacement getDefaultPlacement() {
		return PlacementUtil.loadFromHandle("resource/tle-resource");
	}

	/* @Nullable */
	public static BasicLTIPlacement loadFromHandle(String handle) {
		final String sql = "SELECT pk1 FROM blti_placement WHERE handle=?";
		final List<Id> ids = SqlUtil.runSql(sql.toString(), new ResultProcessor<Id>() {
			@Override
			public List<Id> getResults(ResultSet results) throws SQLException {
				final List<Id> idList = new ArrayList<Id>();
				while (results.next()) {
					final Long contentId = results.getLong("pk1");
					final Id id = Id.toId(BasicLTIPlacement.DATA_TYPE, Long.toString(contentId));
					idList.add(id);
				}
				return idList;
			}
		}, handle);
		if (ids.size() == 0) {
			BbLogger.instance().logTrace("No placements found for handle " + handle);
			return null;
		}
		BbLogger.instance().logTrace("Placements found for handle " + handle);
		return loadFromId(ids.get(0));
	}

	/* @Nullable */
	public static BasicLTIPlacement loadFromId(Id placementId) {
		final BasicLTIPlacementManager basicLTIPlacementManager = BasicLTIPlacementManager.Factory.getInstance();
		try {
			return basicLTIPlacementManager.loadById(placementId, false);
		} catch (KeyNotFoundException e) {
			return null;
		}
	}

	public static LoadPlacementResponse loadPlacementByUrl(String urlString) throws Exception {
		final LoadPlacementResponse response = new LoadPlacementResponse();
		// Handle badly formatted old URLs
		final URL url;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException mal) {
			return response;
		}

		final BasicLTIDomainConfig domainConfig = loadDomainConfigByUrl(url);
		response.setDomainConfig(domainConfig);
		if (domainConfig != null) {
			BbLogger.instance().logTrace("Existing BasicLTIDomainConfig, loading placements");
			final BasicLTIPlacementManager basicLTIPlacementManager = BasicLTIPlacementManager.Factory.getInstance();
			final List<BasicLTIPlacement> placements = basicLTIPlacementManager.loadByDomainConfigId(domainConfig.getId());
			if (placements == null || placements.size() == 0) {
				BbLogger.instance().logTrace("No existing BasicLTIPlacements for domain config");
			} else {
				// what if more than one?
				BbLogger.instance()
						.logTrace("Found " + placements.size() + " existing BasicLTIPlacements for host " + url.getHost());
				response.setPlacement(placements.get(0));
			}
		} else {
			BbLogger.instance().logTrace("No domain config for this URL");
		}
		return response;
	}

	/* @Nullable */
	public static BasicLTIDomainConfig loadDomainConfigByUrl(URL url) {
		final BasicLTIDomainConfigManager fac = BasicLTIDomainConfigManagerFactory.getInstance();
		try {
			return fac.loadByDomain(url.getHost());
		} catch (KeyNotFoundException knfUhHuhUhHuh) {
			return null;
		}
	}

	public static BasicLTIPlacement createNewPlacement(BasicLTIDomainConfig domainConfig, ContentHandler contentHandler) {
		BbLogger.instance().logTrace("Creating new BasicLTIPlacement");
		try {
			final BasicLTIPlacement placement = new BasicLTIPlacement();
			placement.setType(Type.ContentHandler);
			placement.setAllowGrading(true);
			placement.setContentHandler(contentHandler);
			placement.setName(LTI_PLACEMENT_TITLE);
			placement.setHandle(contentHandler.getHandle());
			placement.setBasicLTIDomainConfigId(domainConfig.getId());
			return placement;
		} catch (Exception e) {
			BbLogger.instance().logError("Error creating placement", e);
			throw Throwables.propagate(e);
		}
	}

	public static void deleteById(Id id) {
		BbLogger.instance().logTrace("Deleting placement " + id.toExternalString());
		final BasicLTIPlacementManager basicLTIPlacementManager = BasicLTIPlacementManager.Factory.getInstance();
		basicLTIPlacementManager.deleteById(id);
	}

	public static void save(BasicLTIPlacement placement) {
		BbLogger.instance().logTrace("Saving placement " + placement.getId().toExternalString());
		final BasicLTIPlacementManager basicLTIPlacementManager = BasicLTIPlacementManager.Factory.getInstance();
		basicLTIPlacementManager.saveContentHandlerPlacement(placement, ActionType.none);
	}

	// @NonNullByDefault(false)
	public static class LoadPlacementResponse {
		private BasicLTIPlacement placement;
		private BasicLTIDomainConfig domainConfig;

		public BasicLTIPlacement getPlacement() {
			return placement;
		}

		public void setPlacement(BasicLTIPlacement placement) {
			this.placement = placement;
		}

		public BasicLTIDomainConfig getDomainConfig() {
			return domainConfig;
		}

		public void setDomainConfig(BasicLTIDomainConfig domainConfig) {
			this.domainConfig = domainConfig;
		}
	}
}
