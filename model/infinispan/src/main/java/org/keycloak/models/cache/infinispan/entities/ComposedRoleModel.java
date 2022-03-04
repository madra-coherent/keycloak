package org.keycloak.models.cache.infinispan.entities;

import java.util.Objects;
import java.util.Set;

import org.keycloak.models.CompositeRoleIdentifiersModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.RoleModelDelegate;

/**
 * Helper class wrapping an existing {@link RoleModel} but providing the composite directly.
 */
public class ComposedRoleModel extends RoleModelDelegate {
    
    private final Set<String> composites;

    public ComposedRoleModel(RoleModel delegate, CompositeRoleIdentifiersModel compositeIds) {
        super(delegate);
        Objects.requireNonNull(compositeIds,"Argument 'compositeIds' cannot be null");
        if (!compositeIds.getRoleId().equals(delegate.getId())) {
            throw new IllegalArgumentException("Argument 'compositeIds' is inconsistent with argument 'delegate'"
                    + " (role id is " + compositeIds.getRoleId() + ", but expected " + delegate.getId() + ")");
        }
        this.composites = compositeIds.getChildRoleIds();
    }

    @Override
    public Set<String> getCompositeRoleIds() {
        return composites;
    }
    
}
