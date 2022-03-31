/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.common.util.reflections.Types;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleCompositionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.storage.role.RoleLookupProvider;
import org.keycloak.storage.role.RoleStorageProvider;
import org.keycloak.storage.role.RoleStorageProviderFactory;
import org.keycloak.storage.role.RoleStorageProviderModel;
import org.keycloak.utils.ServicesUtils;

public class RoleStorageManager implements RoleProvider {
    private static final Logger logger = Logger.getLogger(RoleStorageManager.class);

    protected KeycloakSession session;

    private final long roleStorageProviderTimeout;

    public RoleStorageManager(KeycloakSession session, long roleStorageProviderTimeout) {
        this.session = session;
        this.roleStorageProviderTimeout = roleStorageProviderTimeout;
    }

    public static boolean isStorageProviderEnabled(RealmModel realm, String providerId) {
        RoleStorageProviderModel model = getStorageProviderModel(realm, providerId);
        return model.isEnabled();
    }

    public static RoleStorageProviderModel getStorageProviderModel(RealmModel realm, String componentId) {
        ComponentModel model = realm.getComponent(componentId);
        if (model == null) return null;
        return new RoleStorageProviderModel(model);
    }

    public static RoleStorageProvider getStorageProvider(KeycloakSession session, RealmModel realm, String componentId) {
        ComponentModel model = realm.getComponent(componentId);
        if (model == null) return null;
        RoleStorageProviderModel storageModel = new RoleStorageProviderModel(model);
        RoleStorageProviderFactory factory = (RoleStorageProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(RoleStorageProvider.class, model.getProviderId());
        if (factory == null) {
            throw new ModelException("Could not find RoletStorageProviderFactory for: " + model.getProviderId());
        }
        return getStorageProviderInstance(session, storageModel, factory);
    }


    public static <T> Stream<RoleStorageProviderModel> getStorageProviders(RealmModel realm, KeycloakSession session, Class<T> type) {
        return realm.getRoleStorageProvidersStream()
                .filter(model -> {
                    RoleStorageProviderFactory factory = getRoleStorageProviderFactory(model, session);
                    if (factory == null) {
                        logger.warnv("Configured RoleStorageProvider {0} of provider id {1} does not exist in realm {2}",
                                model.getName(), model.getProviderId(), realm.getName());
                        return false;
                    } else {
                        return Types.supports(type, factory, RoleStorageProviderFactory.class);
                    }
                });
    }

    private static RoleStorageProviderFactory getRoleStorageProviderFactory(RoleStorageProviderModel model, KeycloakSession session) {
        return (RoleStorageProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(RoleStorageProvider.class, model.getProviderId());
    }

    public static RoleStorageProvider getStorageProviderInstance(KeycloakSession session, RoleStorageProviderModel model, RoleStorageProviderFactory factory) {
        RoleStorageProvider instance = (RoleStorageProvider)session.getAttribute(model.getId());
        if (instance != null) return instance;
        instance = factory.create(session, model);
        if (instance == null) {
            throw new IllegalStateException("RoleStorageProvideFactory (of type " + factory.getClass().getName() + ") produced a null instance");
        }
        session.enlistForClose(instance);
        session.setAttribute(model.getId(), instance);
        return instance;
    }


    public static <T> Stream<T> getStorageProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        return getStorageProviders(realm, session, type)
                .map(model -> type.cast(getStorageProviderInstance(session, model, getRoleStorageProviderFactory(model, session))));
    }


    public static <T> Stream<T> getEnabledStorageProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        return getStorageProviders(realm, session, type)
                .filter(RoleStorageProviderModel::isEnabled)
                .map(model -> type.cast(getStorageProviderInstance(session, model, getRoleStorageProviderFactory(model, session))));
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String name) {
        return session.roleLocalStorage().addRealmRole(realm, name);
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        return session.roleLocalStorage().addRealmRole(realm, id, name);
    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        RoleModel realmRole = session.roleLocalStorage().getRealmRole(realm, name);
        if (realmRole != null) return realmRole;
        return getEnabledStorageProviders(session, realm, RoleLookupProvider.class)
                .map(provider -> provider.getRealmRole(realm, name))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Optional<String> getRoleStorageProviderIdForRoleId(String roleId) {
        return Optional.ofNullable(new StorageId(roleId).getProviderId());
    }
    
    /**
     * Resolves a provider identifier against the specified realms into {@link RoleLookupProvider} instances,
     * along with the related realms they were resolved against (the same provider ID can theoretically lead
     * to distinct instances)
     * @param providerId the provider ID to resolve
     * @param realms the realms to resolve the provider ID against
     * @return the resolved {@link RoleLookupProvider}s, with their related {@link RealmModel}s they apply to
     */
    private Map<RoleLookupProvider, Set<RealmModel>> resolveProviderPerRealm(Optional<String> providerId, Set<RealmModel> realms) {
        RoleLookupProviderResolver providerResolver = new RoleLookupProviderResolver(providerId, session);
        return realms.stream()
                .map(providerResolver::resolve)
                .filter(RoleLookupProviderResolution::isResolved)
                .collect(Collectors.groupingBy(RoleLookupProviderResolution::getProvider,
                        Collectors.mapping(RoleLookupProviderResolution::getRealm, Collectors.toSet())
                        ));
    }

    private Stream<RealmRestrictedRoleLookup<String>> buildRoleLookups(Optional<String> providerId, Set<RealmModel> realms, List<String> roleIds) {
        return resolveProviderPerRealm(providerId, realms).entrySet().stream()
                .map(resolvedProvider -> new RealmRestrictedRoleLookup<>(resolvedProvider.getKey(), realms, roleIds));
    }

    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        // TODO: align with getRolesByIds()
        StorageId storageId = new StorageId(id);
        if (storageId.getProviderId() == null) {
            return session.roleLocalStorage().getRoleById(realm, id);
        }
        RoleLookupProvider provider = (RoleLookupProvider)getStorageProvider(session, realm, storageId.getProviderId());
        if (provider == null) return null;
        if (! isStorageProviderEnabled(realm, storageId.getProviderId())) return null;
        return provider.getRoleById(realm, id);
    }

    @Override
    public Stream<RoleModel> getRolesByIds(Set<RealmModel> realms, Stream<String> ids) {
        return ids.collect(Collectors.groupingBy(this::getRoleStorageProviderIdForRoleId)).entrySet().stream()
                .flatMap(providerIdAndRoleIds -> buildRoleLookups(providerIdAndRoleIds.getKey(), realms, providerIdAndRoleIds.getValue()))
                .flatMap(lookup -> lookup.getProvider().getRolesByIds(lookup.getRealms(), lookup.getRoleIds().stream()));
    }

    private Optional<String> getRoleStorageProviderIdForRoleComposition(RoleCompositionModel roleComposition) {
        return Optional.ofNullable(new StorageId(roleComposition.getRoleId()).getProviderId());
    }

    private Stream<RealmRestrictedRoleLookup<RoleCompositionModel>> buildRoleCompositionLookups(
            Optional<String> providerId, Set<RealmModel> realms, List<RoleCompositionModel> roleCompositions) {
        return resolveProviderPerRealm(providerId, realms).entrySet().stream()
                .map(resolvedProvider -> new RealmRestrictedRoleLookup<>(resolvedProvider.getKey(), realms, roleCompositions));
    }

    @Override
    public Stream<RoleModel> getRolesByCompositions(Set<RealmModel> realms, Stream<RoleCompositionModel> roleCompositions) {
        return roleCompositions.collect(Collectors.groupingBy(this::getRoleStorageProviderIdForRoleComposition)).entrySet().stream()
                .flatMap(providerIdAndRoleIds -> buildRoleCompositionLookups(providerIdAndRoleIds.getKey(), realms, providerIdAndRoleIds.getValue()))
                .flatMap(lookup -> lookup.getProvider().getRolesByCompositions(lookup.getRealms(), lookup.getRoleIds().stream()));
    }

    @Override
    public Stream<RoleCompositionModel> getDeepRoleCompositionsStream(RealmModel realm, Stream<String> ids, Set<String> excludedIds) {
        return ids.collect(Collectors.groupingBy(id -> Optional.ofNullable(new StorageId(id).getProviderId()))) // Map<Optional<String>, List<String>>
                .entrySet().stream()
                .map(entry -> getDeepCompositeRoleIdsFromProvider(realm, entry.getValue().stream(), entry.getKey(), excludedIds))
                .flatMap(Function.identity());
    }

    private Stream<RoleCompositionModel> getDeepCompositeRoleIdsFromProvider(RealmModel realm, Stream<String> ids, Optional<String> providerId, Set<String> excludedIds) {
        if (!providerId.isPresent()) {
            return session.roleLocalStorage().getDeepRoleCompositionsStream(realm, ids, excludedIds);
        }
        RoleLookupProvider provider = (RoleLookupProvider)getStorageProvider(session, realm, providerId.get());
        if (provider == null) return Stream.empty();
        if (! isStorageProviderEnabled(realm, providerId.get())) return Stream.empty();
        return provider.getDeepRoleCompositionsStream(realm, ids, excludedIds);
    }
    
    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm, Integer first, Integer max) {
        return session.roleLocalStorage().getRealmRolesStream(realm, first, max);
    }

    @Override
    public Stream<RoleModel> getRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        return session.roleLocalStorage().getRolesStream(realm, ids, search, first, max);
    }

    /**
     * Obtaining roles from an external role storage is time-bounded. In case the external role storage
     * isn't available at least roles from a local storage are returned. For this purpose
     * the {@link org.keycloak.services.DefaultKeycloakSessionFactory#getRoleStorageProviderTimeout()} property is used.
     * Default value is 3000 milliseconds and it's configurable.
     * See {@link org.keycloak.services.DefaultKeycloakSessionFactory} for details.
     */
    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        Stream<RoleModel> local = session.roleLocalStorage().searchForRolesStream(realm, search, first, max);
        Stream<RoleModel> ext = getEnabledStorageProviders(session, realm, RoleLookupProvider.class)
                .flatMap(ServicesUtils.timeBound(session,
                        roleStorageProviderTimeout,
                        p -> ((RoleLookupProvider) p).searchForRolesStream(realm, search, first, max)));

        return Stream.concat(local, ext);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        if (!StorageId.isLocalStorage(role.getId())) {
            throw new RuntimeException("Federated roles do not support this operation");
        }
        return session.roleLocalStorage().removeRole(role);
    }

    @Override
    public void removeRoles(RealmModel realm) {
        session.roleLocalStorage().removeRoles(realm);
    }

    @Override
    public void removeRoles(ClientModel client) {
        session.roleLocalStorage().removeRoles(client);
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String name) {
        return session.roleLocalStorage().addClientRole(client, name);
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String id, String name) {
        return session.roleLocalStorage().addClientRole(client, id, name);
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        RoleModel clientRole = session.roleLocalStorage().getClientRole(client, name);
        if (clientRole != null) return clientRole;
        return getEnabledStorageProviders(session, client.getRealm(), RoleLookupProvider.class)
                .map(provider -> provider.getClientRole(client, name))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client) {
        return session.roleLocalStorage().getClientRolesStream(client);
    }

    @Override
    public Stream<RoleModel> getClientsRolesStream(Stream<ClientModel> clients) {
        return session.roleLocalStorage().getClientsRolesStream(clients);
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max) {
        return session.roleLocalStorage().getClientRolesStream(client, first, max);
    }

    /**
     * Obtaining roles from an external role storage is time-bounded. In case the external role storage
     * isn't available at least roles from a local storage are returned. For this purpose
     * the {@link org.keycloak.services.DefaultKeycloakSessionFactory#getRoleStorageProviderTimeout()} property is used.
     * Default value is 3000 milliseconds and it's configurable.
     * See {@link org.keycloak.services.DefaultKeycloakSessionFactory} for details.
     */
    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        Stream<RoleModel> local = session.roleLocalStorage().searchForClientRolesStream(client, search, first, max);
        Stream<RoleModel> ext = getEnabledStorageProviders(session, client.getRealm(), RoleLookupProvider.class)
                .flatMap(ServicesUtils.timeBound(session,
                        roleStorageProviderTimeout,
                        p -> ((RoleLookupProvider) p).searchForClientRolesStream(client, search, first, max)));

        return Stream.concat(local, ext);
    }

    @Override
    public void close() {
    }
}
