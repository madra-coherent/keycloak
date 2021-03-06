/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import javax.ws.rs.NotFoundException;
import org.keycloak.Config;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Version;
import org.keycloak.common.util.UriUtils;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.MediaType;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AdminConsole {
    protected static final Logger logger = Logger.getLogger(AdminConsole.class);

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected HttpRequest request;

    @Context
    protected HttpResponse response;

    @Context
    protected KeycloakSession session;

    @Context
    protected Providers providers;

    protected RealmModel realm;

    public AdminConsole(RealmModel realm) {
        this.realm = realm;
    }

    public static class WhoAmI {
        protected String userId;
        protected String realm;
        protected String displayName;
        protected Locale locale;

        @JsonProperty("createRealm")
        protected boolean createRealm;
        @JsonProperty("realm_access")
        protected Map<String, Set<String>> realmAccess = new HashMap<String, Set<String>>();

        public WhoAmI() {
        }

        public WhoAmI(String userId, String realm, String displayName, boolean createRealm, Map<String, Set<String>> realmAccess, Locale locale) {
            this.userId = userId;
            this.realm = realm;
            this.displayName = displayName;
            this.createRealm = createRealm;
            this.realmAccess = realmAccess;
            this.locale = locale;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public boolean isCreateRealm() {
            return createRealm;
        }

        public void setCreateRealm(boolean createRealm) {
            this.createRealm = createRealm;
        }

        public Map<String, Set<String>> getRealmAccess() {
            return realmAccess;
        }

        public void setRealmAccess(Map<String, Set<String>> realmAccess) {
            this.realmAccess = realmAccess;
        }

        public Locale getLocale() {
            return locale;
        }

        public void setLocale(Locale locale) {
            this.locale = locale;
        }

        @JsonProperty(value = "locale")
        public String getLocaleLanguageTag() {
            return locale != null ? locale.toLanguageTag() : null;
        }
    }

    /**
     * Adapter configuration for the admin console for this realm
     *
     * @return
     */
    @Path("config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ClientManager.InstallationAdapterConfig config() {
        ClientModel consoleApp = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if (consoleApp == null) {
            throw new NotFoundException("Could not find admin console client");
        }
        return new ClientManager(new RealmManager(session)).toInstallationRepresentation(realm, consoleApp, session.getContext().getUri().getBaseUri());    }

    @Path("whoami")
    @OPTIONS
    public Response whoAmIPreFlight() {
        return new AdminCorsPreflightService(request).preflight();
    }

    /**
     * Permission information
     *
     * @param headers
     * @return
     */
    @Path("whoami")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response whoAmI(final @Context HttpHeaders headers) {
        RealmManager realmManager = new RealmManager(session);
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            return Response.status(401).build();
        }
        UserModel user= authResult.getUser();
        String displayName;
        if ((user.getFirstName() != null && !user.getFirstName().trim().equals("")) || (user.getLastName() != null && !user.getLastName().trim().equals(""))) {
            displayName = user.getFirstName();
            if (user.getLastName() != null) {
                displayName = displayName != null ? displayName + " " + user.getLastName() : user.getLastName();
            }
        } else {
            displayName = user.getUsername();
        }

        RealmModel masterRealm = getAdminstrationRealm(realmManager);
        Map<String, Set<String>> realmAccess;
        if (masterRealm == null)
            throw new NotFoundException("No realm found");
        boolean createRealm = false;
        if (realm.equals(masterRealm)) {
            logger.debug("setting up realm access for a master realm user");
            createRealm = user.hasRole(masterRealm.getRole(AdminRoles.CREATE_REALM));
            realmAccess = getMasterRealmAccess(user);
        } else {
            logger.debug("setting up realm access for a realm user");
            realmAccess = getRealmAccess(realm, user); 
        }

        Locale locale = session.getContext().resolveLocale(user);

        Cors.add(request).allowedOrigins(authResult.getToken()).allowedMethods("GET").auth()
                .build(response);

        return Response.ok(new WhoAmI(user.getId(), realm.getName(), displayName, createRealm, realmAccess, locale)).build();
    }

    /**
     * Builds the user's admin roles collection for the specified realm  
     * @param realms the realm to get the user's admin roles for
     * @param user the user to get roles for
     * @param realmAdminClientSupplier the function to apply to retrieve the admin client for a realm
     * @return the map of <realm name, set of role names)
     */
    private Map<String, Set<String>> getPerRealmAccess(Stream<RealmModel> realms, UserModel user, Function<RealmModel,ClientModel> realmAdminClientSupplier) {
        Collection<RealmModel> collectedRealms = realms.collect(Collectors.toList());

        // Cannot rely of the roles' realmid as all realm admin roles are detained by the master realm
        // Maintains the mapping between the client id and the realm it's related to
        Map<String, RealmModel> realmsByAdminClientId =  new HashMap<>();
        Set<ClientModel> realmAdminClients = new HashSet<>();
        collectedRealms.forEach(realm -> {
            ClientModel client = realmAdminClientSupplier.apply(realm);
            if (client != null) {
                realmAdminClients.add(client);
                realmsByAdminClientId.put(client.getId(), realm);
            }
        });
        
        Map<String, Set<String>> result = session.roles().getClientsRolesStream(realmAdminClients.stream())
                .filter(user::hasRole)
                .collect(Collectors.groupingBy(
                        role -> realmsByAdminClientId.get(role.getContainerId()).getName(),
                        Collectors.mapping(RoleModel::getName, Collectors.toSet())
                        ));
        
        return result;
    }
    
    private Map<String, Set<String>> getRealmAccess(RealmModel realm, UserModel user) {
        RealmManager realmManager = new RealmManager(session);
        return getPerRealmAccess(Stream.of(realm), user, r -> r.getClientByClientId(realmManager.getRealmAdminClientId(r)));
    }

    private Map<String, Set<String>> getMasterRealmAccess(UserModel user) {
        return getPerRealmAccess(session.realms().getRealmsStream(), user, RealmModel::getMasterAdminClient);
    }

    /**
     * Logout from the admin console
     *
     * @return
     */
    @Path("logout")
    @GET
    @NoCache
    public Response logout() {
        URI redirect = AdminRoot.adminConsoleUrl(session.getContext().getUri(UrlType.ADMIN)).build(realm.getName());

        return Response.status(302).location(
                OIDCLoginProtocolService.logoutUrl(session.getContext().getUri(UrlType.ADMIN)).queryParam("redirect_uri", redirect.toString()).build(realm.getName())
        ).build();
    }

    protected RealmModel getAdminstrationRealm(RealmManager realmManager) {
        return realmManager.getKeycloakAdminstrationRealm();
    }

    /**
     * Main page of this realm's admin console
     *
     * @return
     * @throws URISyntaxException
     */
    @GET
    @NoCache
    public Response getMainPage() throws IOException, FreeMarkerException {
        if (!session.getContext().getUri(UrlType.ADMIN).getRequestUri().getPath().endsWith("/")) {
            return Response.status(302).location(session.getContext().getUri(UrlType.ADMIN).getRequestUriBuilder().path("/").build()).build();
        } else {
            Theme theme = AdminRoot.getTheme(session, realm);

            Map<String, Object> map = new HashMap<>();

            URI adminBaseUri = session.getContext().getUri(UrlType.ADMIN).getBaseUri();
            String adminBaseUrl = adminBaseUri.toString();
            if (adminBaseUrl.endsWith("/")) {
                adminBaseUrl = adminBaseUrl.substring(0, adminBaseUrl.length() - 1);
            }

            URI authServerBaseUri = session.getContext().getUri(UrlType.FRONTEND).getBaseUri();
            String authServerBaseUrl = authServerBaseUri.toString();
            if (authServerBaseUrl.endsWith("/")) {
                authServerBaseUrl = authServerBaseUrl.substring(0, authServerBaseUrl.length() - 1);
            }

            map.put("authServerUrl", authServerBaseUrl);
            map.put("authUrl", adminBaseUrl);
            map.put("consoleBaseUrl", Urls.adminConsoleRoot(adminBaseUri, realm.getName()).getPath());
            map.put("resourceUrl", Urls.themeRoot(adminBaseUri).getPath() + "/admin/" + theme.getName());
            map.put("resourceCommonUrl", Urls.themeRoot(adminBaseUri).getPath() + "/common/keycloak");
            map.put("masterRealm", Config.getAdminRealm());
            map.put("resourceVersion", Version.RESOURCES_VERSION);
            map.put("loginRealm", realm.getName());
            map.put("properties", theme.getProperties());

            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);
            Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.TEXT_HTML_UTF_8).language(Locale.ENGLISH).entity(result);

            // Replace CSP if admin is hosted on different URL
            if (!adminBaseUri.equals(authServerBaseUri)) {
                session.getProvider(SecurityHeadersProvider.class).options().allowFrameSrc(UriUtils.getOrigin(authServerBaseUri));
            }

            return builder.build();
        }
    }

    @GET
    @Path("{indexhtml: index.html}") // this expression is a hack to get around jaxdoclet generation bug.  Doesn't like index.html
    public Response getIndexHtmlRedirect() {
        return Response.status(302).location(session.getContext().getUri(UrlType.ADMIN).getRequestUriBuilder().path("../").build()).build();
    }

    @GET
    @Path("messages.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Properties getMessages(@QueryParam("lang") String lang) {
        return AdminRoot.getMessages(session, realm, lang, "admin-messages");
    }

}
