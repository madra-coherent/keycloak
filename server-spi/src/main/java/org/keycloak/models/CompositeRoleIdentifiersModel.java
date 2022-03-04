package org.keycloak.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the relationship between a role and its children (composite role) 
 */
public class CompositeRoleIdentifiersModel {

    private final String roleId;
    private final Set<String> childRoleIds;
    
    public CompositeRoleIdentifiersModel(String roleId, Set<String> childRoleIds) {
        super();
        Objects.requireNonNull(roleId, "Argument 'roleId' cannot be null");
        Objects.requireNonNull(childRoleIds, "Argument 'childRoleIds' cannot be null");
        this.roleId = roleId;
        this.childRoleIds = Collections.unmodifiableSet(new HashSet<>(childRoleIds));
    }
    
    /**
     * The composite role identifier
     * @return the role identifier
     */
    public String getRoleId() {
        return roleId;
    }

    /**
     * The set of child role identifiers of this role
     * @return the non-null set of child role identifiers (empty when no children)
     */
    public Set<String> getChildRoleIds() {
        return childRoleIds;
    }
    
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CompositeRoleIdentifiersModel)) return false;

        CompositeRoleIdentifiersModel that = (CompositeRoleIdentifiersModel) o;

        if (!roleId.equals(that.roleId)) return false;

        return true;
    }

    @Override
    public final int hashCode() {
        return roleId.hashCode();
    }
}