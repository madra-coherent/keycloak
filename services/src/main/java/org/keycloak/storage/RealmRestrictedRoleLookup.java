package org.keycloak.storage;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.keycloak.models.RealmModel;
import org.keycloak.storage.role.RoleLookupProvider;

/**
 * Represents a role lookup operation to perform within a specified set of realms.
 * @param <ID> the type of the identifiers used by the lookup operation
 */
public class RealmRestrictedRoleLookup<ID> {

    private final RoleLookupProvider provider;
    private final Set<RealmModel> realms;
    private final Collection<ID> roleIds;
    
    public RealmRestrictedRoleLookup(RoleLookupProvider provider, Set<RealmModel> realms, Collection<ID> roleIds) {
        super();
        Objects.requireNonNull(provider, "Argument 'provider' cannot be null");
        Objects.requireNonNull(realms, "Argument 'realms' cannot be null");
        Objects.requireNonNull(roleIds, "Argument 'roleIds' cannot be null");
        this.provider = provider;
        this.realms = realms;
        this.roleIds = roleIds;
    }

    /**
     * The {@link RoleLookupProvider} instance to use to perform the lookup operation
     * @return the role lookup provider
     */
    public RoleLookupProvider getProvider() {
        return provider;
    }
    
    /**
     * The realms to restrict the lookup operation to
     * @return the set of realms
     */
    public Set<RealmModel> getRealms() {
        return realms;
    }

    /**
     * The collection of role IDs to look for
     * @return the collection of 
     */
    public Collection<ID> getRoleIds() {
        return roleIds;
    }

}
