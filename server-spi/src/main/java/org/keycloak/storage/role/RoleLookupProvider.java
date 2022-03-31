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
package org.keycloak.storage.role;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleCompositionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

/**
 * Abstraction interface for lookup of both realm roles and client roles by id, name and description.
 */
public interface RoleLookupProvider {

    /**
     * Exact search for a role by given name.
     * @param realm Realm.
     * @param name String name of the role.
     * @return Model of the role, or {@code null} if no role is found.
     */
    RoleModel getRealmRole(RealmModel realm, String name);

    /**
     * Exact search for a role by its internal ID..
     * @param realm Realm.
     * @param id Internal ID of the role.
     * @return Model of the role.
     */
    RoleModel getRoleById(RealmModel realm, String id);

    /**
     * Exact search for multiple roles by their internal ID within the specified realm.
     * Important: no ordering is specified, so sorting must performed onto the result if appropriate 
     * @param realm the {@link RealmModel} to restrict the roles to
     * @param ids the Internal IDs of the roles.
     * @return Stream of {@link RoleModel}. Never null.
     */
    default Stream<RoleModel> getRolesByIds(RealmModel realm, Stream<String> ids) {
        return realm == null ? Stream.empty() : getRolesByIds(Collections.singleton(realm), ids);
    }

    /**
     * Exact search for multiple roles by their internal ID within a specified set of realms.
     * Important: no ordering is specified, so sorting must performed onto the result if appropriate 
     * @param realms the {@link RealmModel}s to restrict the roles to
     * @param ids the Internal IDs of the roles.
     * @return Stream of {@link RoleModel}. Never null.
     */
    Stream<RoleModel> getRolesByIds(Set<RealmModel> realms, Stream<String> ids);

    /**
     * Specialized exact search for multiple roles by their internal ID, with their already known composition
     * (which saves loading them from the provider again).
     * Important: no ordering is specified, so sorting must performed onto the result if appropriate 
     * @param realm Realm.
     * @param roleCompositions the stream of role compositions, containing both their own ID and their child role IDs.
     * @return Stream of {@link RoleModel}.
     */
    Stream<RoleModel> getRolesByCompositions(Set<RealmModel> realms, Stream<RoleCompositionModel> roleCompositions);

    /**
     * Resolves the specified role IDs with the entire set of children role IDs (expanding composites),
     * retaining the composition relationship between role and its children (if any).
     *
     * @param realm Realm. Cannot be {@code null}.
     * @param ids non-null Stream of role compositions. Returns empty {@code Stream} when {@code null}.
     * @return non-null Stream of {@link RoleCompositionModel}.
     */
    default Stream<RoleCompositionModel> getDeepRoleCompositionsStream(RealmModel realm, Stream<String> ids) {
        return getDeepRoleCompositionsStream(realm, ids, Collections.emptySet());
    }

    /**
     * Resolves the specified role IDs with the entire set of children role IDs (expanding composites),
     * retaining the composition relationship between role and its children (if any), and excluding
     * some IDs from the composition lookup (useful when some parts of the composition graph is already known).
     *
     * @param realm Realm. Cannot be {@code null}.
     * @param ids Stream of role compositions. Returns empty {@code Stream} when {@code null}.
     * @param excludedIds set of role IDs to exclude from the deep lookup 
     * @return non-null Stream of {@link RoleCompositionModel}.
     */
    Stream<RoleCompositionModel> getDeepRoleCompositionsStream(RealmModel realm, Stream<String> ids, Set<String> excludedIds);

    /**
     * Case-insensitive search for roles that contain the given string in their name or description.
     * @param realm Realm.
     * @param search Searched substring of the role's name or description.
     * @param first First result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return Stream of the realm roles their name or description contains given search string. 
     * Never returns {@code null}.
     */
    Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max);

    /**
     * Exact search for a client role by given name.
     * @param client Client.
     * @param name String name of the role.
     * @return Model of the role, or {@code null} if no role is found.
     */
    RoleModel getClientRole(ClientModel client, String name);

    /**
     * Case-insensitive search for client roles that contain the given string in their name or description.
     * @param client Client.
     * @param search String to search by role's name or description.
     * @param first First result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return Stream of the client roles their name or description contains given search string. 
     * Never returns {@code null}.
     */
    Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max);
}
