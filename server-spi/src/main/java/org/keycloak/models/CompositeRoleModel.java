package org.keycloak.models;

import java.util.Objects;

public class CompositeRoleModel {

    private final String compositeId;
    private final String childRoleId;
    
    public CompositeRoleModel(String compositeId, String childRoleId) {
        super();
        Objects.requireNonNull(compositeId, "Argument 'compositeId' cannot be null");
        Objects.requireNonNull(childRoleId, "Argument 'childRoleId' cannot be null");
        this.compositeId = compositeId;
        this.childRoleId = childRoleId;
    }

    /**
     * The parent role ID
     * @return the composite parent role ID
     */
    public String getCompositeId() {
        return compositeId;
    }

    /**
     * The child role ID part of the parent composite role
     * @return the child role ID
     */
    public String getChildRoleId() {
        return childRoleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CompositeRoleModel)) return false;

        CompositeRoleModel that = (CompositeRoleModel) o;

        if (!compositeId.equals(that.compositeId)) return false;
        if (!childRoleId.equals(that.childRoleId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(compositeId, childRoleId);
    }
    
}
