package org.apereo.openequella.integration.blackboard.buildingblock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apereo.openequella.integration.blackboard.common.BbContext;
import org.apereo.openequella.integration.blackboard.common.BbLogger;
import org.apereo.openequella.integration.blackboard.common.BbLogger.LogLevel;
import org.apereo.openequella.integration.blackboard.common.BbUtil;
import org.apereo.openequella.integration.blackboard.common.PathUtils;
import org.apereo.openequella.integration.blackboard.common.content.PlacementUtil;
import org.apereo.openequella.integration.blackboard.common.content.PlacementUtil.LoadPlacementResponse;

import com.google.common.base.Strings;
import com.google.common.io.Closeables;

import blackboard.data.blti.BasicLTIDomainConfig;
import blackboard.data.blti.BasicLTIDomainConfig.SendUserData;
import blackboard.data.blti.BasicLTIDomainConfig.Status;
import blackboard.data.blti.BasicLTIDomainHost;
import blackboard.data.blti.BasicLTIPlacement;
import blackboard.persist.Id;
import blackboard.platform.blti.BasicLTIDomainConfigManager;
import blackboard.platform.blti.BasicLTIDomainConfigManagerFactory;
import blackboard.platform.gradebook2.ScoreProvider;
import blackboard.platform.gradebook2.impl.ScoreProviderDAO;
import blackboard.platform.plugin.ContentHandler;
import blackboard.platform.plugin.ContentHandlerDbLoader;
import blackboard.platform.plugin.ContentHandlerDbPersister;
import blackboard.platform.plugin.PlugInException;
import blackboard.platform.plugin.PlugInUtil;
import blackboard.platform.vxi.data.VirtualHost;
import blackboard.platform.vxi.service.VirtualInstallationManager;
import blackboard.platform.vxi.service.VirtualInstallationManagerFactory;

@SuppressWarnings("nls")
// @NonNullByDefault
public class Configuration {

    /* @Nullable */
    private static Configuration instance;
    private static final Object instanceLock = new Object();

    private static final String CONFIG_FILE = "config.properties";
    private static final String CONTENT_HANDLER_HANDLE = "resource/tle-resource";

    // DEPRECATED CONFIG ELEMENTS
    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String CONTEXT = "context";
    private static final String INSTITUTION = "institution";
    private static final String SECURE = "secure";
    private static final String MOCK_PORTAL_ROLES = "mock.portal.roles";

    // CONFIG ELEMENTS (referred to in the JSP)
    public static final String EQUELLA_URL = "equellaurl";
    public static final String SECRET = "secret";
    public static final String SECRETID = "secretid";
    public static final String OAUTH_CLIENT_ID = "oauth.clientid";
    public static final String OAUTH_CLIENT_SECRET = "oauth.clientsecret";
    public static final String RESTRICTIONS = "restrictions";
    public static final String LOG_LEVEL = "loglevel";
    public static final String NEWWINDOW = "newwindow";

    private static final String DEFAULT_SECRET = "";
    private static final String DEFAULT_RESTRICTION = "none";
    private static final LogLevel DEFAULT_LOG_LEVEL = BbLogger.LogLevel.Info;

    private final BbContext context;
    private File configDirectory;
    /* @Nullable */
    private ContentHandler contentHandler;
    private final Object contentHandlerLock = new Object();

    /* @Nullable */
    private String equellaUrl;
    /* @Nullable */
    private String secret;
    /* @Nullable */
    private String secretid;
    /* @Nullable */
    private String oauthClientId;
    /* @Nullable */
    private String oauthClientSecret;
    /* @Nullable */
    private String restriction;
    /* @Nullable */
    private String logLevel;
    private boolean newWindow;
    /* @Nullable */
    private Set<String> mockPortalRoles;

    private Date lastModified = Calendar.getInstance().getTime();

    public static Configuration instance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new Configuration();
                }
            }
        }
        return instance;
    }

    private Configuration() {
        try {
            configDirectory = PlugInUtil.getConfigDirectory(BbUtil.VENDOR, BbUtil.HANDLE);

            context = BbContext.instance();

            final VirtualInstallationManager vim = VirtualInstallationManagerFactory.getInstance();
            final VirtualHost vhost = vim.getVirtualHost("");
            context.getContextManager().setContext(vhost);

            load();
            ensureLtiPlacement();
            ensureScoreProvider();
            // Fix up dodgy Blind SSL
            // HttpsURLConnection.setDefaultSSLSocketFactory(new
            // sun.security.ssl.SSLSocketFactoryImpl());
        } catch (PlugInException e) {
            BbLogger.instance().logError(
                    "Unable to obtain the plugin config directory for " + BbUtil.VENDOR + "-" + BbUtil.HANDLE, e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            BbLogger.instance().logError("Couldn't init building block", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        context.getContextManager().releaseContext();
        super.finalize();
    }

    public synchronized void modify(HttpServletRequest request) throws Exception {
        setEquellaUrl(request.getParameter(EQUELLA_URL));
        setSecret(request.getParameter(SECRET));
        setSecretId(request.getParameter(SECRETID));
        setOauthClientId(request.getParameter(OAUTH_CLIENT_ID));
        setOauthClientSecret(request.getParameter(OAUTH_CLIENT_SECRET));
        setRestriction(request.getParameter(RESTRICTIONS));
        setLogLevel(request.getParameter(LOG_LEVEL));
        String newWindowParam = request.getParameter(NEWWINDOW);
        if (newWindowParam == null || newWindowParam.equals("")) {
            newWindowParam = "false";
        }
        setNewWindow(Boolean.parseBoolean(newWindowParam));
        lastModified = Calendar.getInstance().getTime();

    }

    public synchronized void load() {
        final File configFile = new File(getConfigDirectory(), CONFIG_FILE);
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                BbLogger.instance().logTrace("Successfully created configuration file");
            } catch (IOException e) {
                BbLogger.instance().logError("Error creating configuration file", e);
                throw new RuntimeException(e);
            }
            return;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(configFile);
            final Properties props = new Properties();
            props.load(fis);
            if (props.containsKey(EQUELLA_URL)) {
                setEquellaUrl(props.getProperty(EQUELLA_URL));
            } else {
                try {
                    setEquellaUrl(buildEquellaUrlFromDeprecatedConfigParams(props));
                } catch (MalformedURLException mal) {
                    BbLogger.instance().logTrace("Failed to load equella URL from deprecated props");
                }
            }
            setSecret(props.getProperty(SECRET));
            setSecretId(props.getProperty(SECRETID));
            setOauthClientId(props.getProperty(OAUTH_CLIENT_ID));
            setOauthClientSecret(props.getProperty(OAUTH_CLIENT_SECRET));
            setMockPortalRoles(commaSplit(props.getProperty(MOCK_PORTAL_ROLES)));
            setRestriction(props.getProperty(RESTRICTIONS));
            setLogLevel(props.getProperty(LOG_LEVEL));
            setNewWindow(Boolean.parseBoolean(props.getProperty(NEWWINDOW, "true")));
        } catch (Exception e) {
            BbLogger.instance().logError("Error loading configuration", e);
            throw new RuntimeException(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public synchronized void save() {
        final File configFile = new File(getConfigDirectory(), CONFIG_FILE);
        FileOutputStream fos = null;
        try {
            ensureLtiPlacement();
            ensureScoreProvider();

            fos = new FileOutputStream(configFile);

            Properties props = new Properties();
            props.setProperty(EQUELLA_URL, equellaUrl);
            props.setProperty(SECRET, secret);
            props.setProperty(SECRETID, secretid);
            props.setProperty(OAUTH_CLIENT_ID, oauthClientId);
            props.setProperty(OAUTH_CLIENT_SECRET, oauthClientSecret);
            props.setProperty(MOCK_PORTAL_ROLES, commaJoin(mockPortalRoles));
            props.setProperty(RESTRICTIONS, restriction);
            props.setProperty(LOG_LEVEL, logLevel);
            props.setProperty(NEWWINDOW, Boolean.toString(newWindow));
            props.store(fos, null);
        } catch (Exception e) {
            BbLogger.instance().logError("Error saving configuration", e);
            throw new RuntimeException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * @Nullable
     * @return
     */
    private synchronized BasicLTIPlacement ensureLtiPlacement() {
        try {
            BbLogger.instance().logTrace("Entering ensureLtiPlacement");
            if (Strings.isNullOrEmpty(oauthClientId) || Strings.isNullOrEmpty(oauthClientSecret)
                    || Strings.isNullOrEmpty(equellaUrl)) {
                BbLogger.instance().logTrace("Not creating a placement since a property was blank");
                return null;
            }

            BasicLTIDomainConfig domainConfig = null;
            boolean newPlacement = false;
            BasicLTIPlacement placement = PlacementUtil.loadFromHandle(CONTENT_HANDLER_HANDLE);
            if (placement == null) {
                BbLogger.instance().logTrace("Loading placement via URL " + equellaUrl);
                final LoadPlacementResponse loadPlacement = PlacementUtil.loadPlacementByUrl(equellaUrl);
                placement = loadPlacement.getPlacement();
                domainConfig = loadPlacement.getDomainConfig();
                if (placement == null) {
                    BbLogger.instance().logTrace("No existing placement for URL " + equellaUrl);
                    final Id placementId = getContentHandler().getBasicLTIPlacementId();
                    if (!Id.isValid(placementId)) {
                        BbLogger.instance().logTrace("No existing placement associated with ContentHandler");

                        if (domainConfig == null) {
                            domainConfig = createDomainConfig();
                        }
                        placement = PlacementUtil.createNewPlacement(domainConfig, getContentHandler());
                        newPlacement = true;
                    } else {
                        BbLogger.instance().logTrace("Loading existing placement from ContentHandler");
                        placement = PlacementUtil.loadFromId(placementId);
                        if (placement == null) {
                            BbLogger.instance().logTrace("Content handler pointing to invalid placement...");
                            if (domainConfig == null) {
                                domainConfig = createDomainConfig();
                            }
                            placement = PlacementUtil.createNewPlacement(domainConfig, getContentHandler());
                            newPlacement = true;
                        }
                    }
                } else {
                    BbLogger.instance().logTrace("Loaded placement via URL");
                }
            } else {
                BbLogger.instance().logTrace("Loaded placement via handle");
            }

            // ensure domainConfig
            if (domainConfig == null) {
                BbLogger.instance().logTrace("No domain config loaded");

                // get any current domain config for this domain
                domainConfig = PlacementUtil.loadDomainConfigByUrl(new URL(equellaUrl));

                if (domainConfig != null) {
                    BbLogger.instance().logTrace("Domain config loaded via URL " + equellaUrl);
                    if (populateDomainConfig(domainConfig)) {
                        BbLogger.instance().logTrace("Saving dirty domain config");
                        final BasicLTIDomainConfigManager fac = BasicLTIDomainConfigManagerFactory.getInstance();
                        fac.save(domainConfig);
                    } else {
                        BbLogger.instance().logTrace("Not saving doman config (not dirty)");
                    }
                } else {
                    BbLogger.instance().logTrace("No domain config for URL " + equellaUrl);
                    final Id domainConfigId = placement.getBasicLTIDomainConfigId();
                    if (Id.isValid(domainConfigId)) {
                        BbLogger.instance().logTrace("Loading domain config based on placement pointer");
                        final BasicLTIDomainConfigManager fac = BasicLTIDomainConfigManagerFactory.getInstance();
                        domainConfig = fac.loadById(domainConfigId);
                        if (populateDomainConfig(domainConfig)) {
                            BbLogger.instance().logTrace("Saving dirty domain config");
                            fac.save(domainConfig);
                        } else {
                            BbLogger.instance().logTrace("Not saving domain config (not dirty)");
                        }
                    } else {
                        BbLogger.instance().logTrace("Creating new domain config");
                        domainConfig = createDomainConfig();
                    }
                }
            }

            BbLogger.instance().logTrace("newPlacement = " + newPlacement);
            BbLogger.instance().logTrace("placement = " + placement.getId().toExternalString());

            boolean saveHandler = newPlacement;
            // delete the old placement associated with this handler if
            // there is one
            final ContentHandler handler = getContentHandler();
            if (handlerPlacementMismatch(placement, handler)) {
                if (Id.isValid(handler.getBasicLTIPlacementId())) {
                    BbLogger.instance().logTrace("Deleting existing placement associated with this handler "
                            + handler.getBasicLTIPlacementId().toExternalString());
                    PlacementUtil.deleteById(handler.getBasicLTIPlacementId());
                }
                saveHandler = true;
            }

            // Save placement?
            // if( newPlacement || requiresSave(placement, domainConfig) )
            // {
            placement.setBasicLTIDomainConfigId(domainConfig.getId());
            placement.setUrl(equellaUrl);
            PlacementUtil.save(placement);
            // }

            // Save handler?
            if (saveHandler) {
                BbLogger.instance().logTrace(
                        "Saving updated ContentHandler with placement " + placement.getId().toExternalString());

                handler.setBasicLTIPlacementId(placement.getId());
                final ContentHandlerDbPersister contentHandlerPersister = (ContentHandlerDbPersister) BbContext
                        .instance().getPersistenceManager().getPersister(ContentHandlerDbPersister.TYPE);
                contentHandlerPersister.persist(handler);
            } else {
                BbLogger.instance().logTrace("Not saving ContentHandler");
            }

            return placement;
        } catch (Exception e) {
            BbLogger.instance().logError("Error ensuring LTI placement", e);
            throw new RuntimeException(e);
        }
    }

    private boolean handlerPlacementMismatch(BasicLTIPlacement placement, ContentHandler handler) {
        final Id currentPlacementId = handler.getBasicLTIPlacementId();
        if (!currentPlacementId.equals(placement.getId())) {
            BbLogger.instance().logTrace("ContentHandler is pointing at a different (or null) placement "
                    + handler.getBasicLTIPlacementId().toExternalString());
            return true;
        }
        BbLogger.instance().logTrace("ContentHandler is pointing to correct placement");
        return false;
    }

    private synchronized ScoreProvider ensureScoreProvider() {
        final ScoreProviderDAO dao = ScoreProviderDAO.get();
        ScoreProvider provider = dao.getByHandle(BbUtil.CONTENT_HANDLER);
        if (provider == null) {
            // The boolean values are cloned from what I could find about
            // resource/x-bb-blti-link
            // in score_provider.txt in BB installer
            provider = new ScoreProvider();
            provider.setName("Equella");
            provider.setHandle(BbUtil.CONTENT_HANDLER);
            provider.setAllowAttemptGrading(true);
            provider.setAllowMultiple(false);
            provider.setAttemptBased(false);
            provider.setGradeAction(PathUtils.urlPath(BbUtil.getBlockRelativePath(), "ViewGradebook"));
            provider.setReviewAction(PathUtils.urlPath(BbUtil.getBlockRelativePath(), "ViewGradebook"));
            dao.persist(provider);
        }

        return provider;
    }

    private BasicLTIDomainConfig createDomainConfig() {
        final BasicLTIDomainConfig domainConfig = new BasicLTIDomainConfig();
        populateDomainConfig(domainConfig);
        domainConfig.setSendEmail(true);
        domainConfig.setSendName(true);
        domainConfig.setSendRole(true);
        domainConfig.setUseSplash(false);
        domainConfig.setSendUserData(SendUserData.Always);
        domainConfig.setStatus(Status.Approved);
        final BasicLTIDomainConfigManager fac = BasicLTIDomainConfigManagerFactory.getInstance();
        fac.save(domainConfig);
        return domainConfig;
    }

    /**
     * @param domainConfig
     * @return Was changed
     */
    private boolean populateDomainConfig(BasicLTIDomainConfig domainConfig) {
        try {
            boolean dirty = false;
            final String key = domainConfig.getKey();
            if (!Strings.nullToEmpty(key).equals(Strings.nullToEmpty(oauthClientId))) {
                domainConfig.setKey(oauthClientId);
                dirty = true;
            }
            final String secret = domainConfig.getSecret();
            if (!Strings.nullToEmpty(secret).equals(Strings.nullToEmpty(oauthClientSecret))) {
                domainConfig.setSecret(oauthClientSecret);
                dirty = true;
            }

            BasicLTIDomainHost host = domainConfig.getPrimaryHost();
            final String newDomain = new URL(equellaUrl).getHost();
            if (host == null || !Strings.nullToEmpty(host.getDomain()).equals(Strings.nullToEmpty(newDomain))) {
                host = (host == null ? new BasicLTIDomainHost() : host);
                host.setDomain(newDomain);
                host.setPrimary(true);
                domainConfig.setPrimaryHost(host);
                dirty = true;
            }
            return dirty;
        } catch (Exception mal) {
            BbLogger.instance().logError("Error populating domainConfig", mal);
            throw new RuntimeException(mal);
        }
    }

    private ContentHandler getContentHandler() throws Exception {
        if (contentHandler == null) {
            synchronized (contentHandlerLock) {
                if (contentHandler == null) {
                    final ContentHandlerDbLoader contentHandlerLoader = (ContentHandlerDbLoader) BbContext.instance()
                            .getPersistenceManager().getLoader(ContentHandlerDbLoader.TYPE);
                    contentHandler = contentHandlerLoader.loadByHandle(CONTENT_HANDLER_HANDLE);
                    final Id basicLTIPlacementId = contentHandler.getBasicLTIPlacementId();
                    BbLogger.instance().logTrace("Loaded content handler from DB, placement = "
                            + (basicLTIPlacementId == null ? "null" : basicLTIPlacementId.toExternalString()));
                }
            }
        }
        return contentHandler;
    }

    public File getConfigDirectory() {
        return configDirectory;
    }

    private String buildEquellaUrlFromDeprecatedConfigParams(Properties props) throws MalformedURLException {
        final boolean secure = Boolean.valueOf(props.getProperty(SECURE));
        final String host = props.getProperty(HOST, "localhost");
        final int port = Integer.parseInt(props.getProperty(PORT, "80"));
        final String context = props.getProperty(CONTEXT, "/");
        final String inst = props.getProperty(INSTITUTION, "");

        return new URL(new URL(secure ? "https" : "http", host, port, context), inst).toString();
    }

    public boolean hasBeenModified(Date lastUpdate) {
        return lastUpdate.before(lastModified);
    }

    private Set<String> commaSplit(/* @Nullable */String value) {
        final Set<String> result = new HashSet<String>();
        if (value != null) {
            final String[] vs = value.split(",");
            for (int i = 0; i < vs.length; i++) {
                result.add(vs[i].trim());
            }
        }
        return result;
    }

    private String commaJoin(/* @Nullable */Collection<String> values) {
        final StringBuilder roles = new StringBuilder();
        if (values != null) {
            for (Iterator<String> iter = values.iterator(); iter.hasNext();) {
                if (roles.length() > 0) {
                    roles.append(',');
                }
                roles.append(iter.next());
            }
        }
        return roles.toString();
    }

    /* @Nullable */
    public String getEquellaUrl() {
        return equellaUrl;
    }

    public void setEquellaUrl(/* @Nullable */String equellaUrl) {
        this.equellaUrl = (equellaUrl == null ? null : equellaUrl.trim());
        if (!Strings.isNullOrEmpty(this.equellaUrl) && !this.equellaUrl.endsWith("/")) {
            this.equellaUrl += '/';
        }
    }

    /* @Nullable */
    public String getSecret() {
        return secret;
    }

    public void setSecret(/* @Nullable */String secret) {
        if (secret == null || secret.length() == 0) {
            this.secret = DEFAULT_SECRET;
        } else {
            this.secret = secret;
        }
    }

    /* @Nullable */
    public String getSecretId() {
        return secretid;
    }

    public void setSecretId(/* @Nullable */String secretid) {
        if (secretid == null || secretid.length() == 0) {
            this.secretid = DEFAULT_SECRET;
        } else {
            this.secretid = secretid;
        }
    }

    /* @Nullable */
    public String getOauthClientId() {
        return oauthClientId;
    }

    public void setOauthClientId(/* @Nullable */String oauthClientId) {
        this.oauthClientId = oauthClientId;
    }

    /* @Nullable */
    public String getOauthClientSecret() {
        return oauthClientSecret;
    }

    public void setOauthClientSecret(/* @Nullable */String oauthClientSecret) {
        this.oauthClientSecret = oauthClientSecret;
    }

    public void setMockPortalRoles(/* @Nullable */Set<String> mockPortalRoles) {
        this.mockPortalRoles = mockPortalRoles;
    }

    /* @Nullable */
    public Set<String> getMockPortalRoles() {
        return mockPortalRoles;
    }

    /* @Nullable */
    public String getRestriction() {
        return restriction;
    }

    public void setRestriction(/* @Nullable */String restriction) {
        if (restriction == null) {
            this.restriction = DEFAULT_RESTRICTION;
        } else {
            this.restriction = restriction;
        }
    }

    /* @Nullable */
    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(/* @Nullable */String level) {
        if (level == null) {
            this.logLevel = DEFAULT_LOG_LEVEL.name();
        } else {
            this.logLevel = level;
        }
        BbLogger.instance().setLoggingLevel(LogLevel.valueOf(this.logLevel));
    }

    public void setNewWindow(boolean newWindow) {
        this.newWindow = newWindow;
    }

    public boolean isNewWindow() {
        return newWindow;
    }
}
