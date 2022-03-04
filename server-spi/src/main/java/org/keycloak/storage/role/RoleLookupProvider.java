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

import java.util.stream.Stream;
import org.keycloak.models.ClientModel;
import org.keycloak.models.CompositeRoleIdentifiersModel;
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
     * Exact search for multiple roles by their internal ID.
     * Important: no ordering is specified, so sorting must performed onto the result if appropriate 
     * @param realm Realm.
     * @param ids the Internal IDs of the roles.
     * @return Stream of {@link RoleModel}.
     */
    Stream<RoleModel> getRolesByIds(RealmModel realm, Stream<String> ids);

    /**
     * Specialized exact search for multiple roles by their internal ID, with already known composite child ids
     * (which saves loading them from the provider again).
     * discarding the known composite child IDs entirely.
     * Important: no ordering is specified, so sorting must performed onto the result if appropriate 
     * @param realm Realm.
     * @param compositeRoleIds the stream of roles identifiers, containing both their own ID and their child role IDs.
     * @return Stream of {@link RoleModel}.
     */
    Stream<RoleModel> getCompositeRolesByIds(RealmModel realm, Stream<CompositeRoleIdentifiersModel> compositeRoleIds);

    /**
     * Augments the specified role IDs with the entire set of children role IDs (expanding composites).
     *
     * @param realm Realm. Cannot be {@code null}.
     * @param ids Stream of ids. Returns empty {@code Stream} when {@code null}.
     * @return Stream of expanded role IDs. Never returns {@code null}.
     */
    Stream<String> getDeepRoleIdsStream(RealmModel realm, Stream<String> ids);

    /**
     * Resolves the specified role IDs with the entire set of children role IDs (expanding composites),
     * retaining the relationship between role and its children (if any).
     *
     * @param realm Realm. Cannot be {@code null}.
     * @param ids non-null Stream of composite role identifiers. Returns empty {@code Stream} when {@code null}.
     * @return non-null Stream of {@link CompositeRoleIdentifiersModel}.
     */
    Stream<CompositeRoleIdentifiersModel> getDeepCompositeRoleIdsStream(RealmModel realm, Stream<String> ids);
    
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
