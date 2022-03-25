package org.keycloak.models.cache.infinispan.entities;

import java.util.Objects;
import java.util.Set;

import org.keycloak.models.RoleCompositionModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.RoleModelDelegate;

/**
 * Helper class wrapping an existing {@link RoleModel} but providing the composite directly.
 */
public class ComposedRoleModel extends RoleModelDelegate {
    
    private final Set<String> composites;

    public ComposedRoleModel(RoleModel delegate, RoleCompositionModel roleComposition) {
        super(delegate);
        Objects.requireNonNull(roleComposition,"Argument 'roleComposition' cannot be null");
        if (!roleComposition.getRoleId().equals(delegate.getId())) {
            throw new IllegalArgumentException("Argument 'roleComposition' is inconsistent with argument 'delegate'"
                    + " (role id is " + roleComposition.getRoleId() + ", but expected " + delegate.getId() + ")");
        }
        this.composites = roleComposition.getChildRoleIds();
    }

    @Override
    public Set<String> getCompositeRoleIds() {
        return composites;
    }
    
}
