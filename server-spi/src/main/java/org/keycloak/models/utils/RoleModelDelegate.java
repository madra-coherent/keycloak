package org.keycloak.models.utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;

/**
 * Wraps a RoleModel to which all operations are delegated.
 * Extending this class allows to easily override only a subset of these operations.
 */
public class RoleModelDelegate implements RoleModel {

    private final RoleModel delegate;

    public RoleModelDelegate(RoleModel delegate) {
        super();
        Objects.requireNonNull(delegate, "Argument 'delegate' cannot be null");
        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    @Override
    public String getContainerId() {
        return delegate.getContainerId();
    }

    @Override
    public RoleContainerModel getContainer() {
        return delegate.getContainer();
    }
    
    @Override
    public String getRealmId() {
        return delegate.getRealmId();
    }

    @Override
    public boolean isComposite() {
        return delegate.isComposite();
    }

    @Override
    public Set<String> getCompositeRoleIds() {
        return delegate.getCompositeRoleIds();
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        delegate.addCompositeRole(role);
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        delegate.removeCompositeRole(role);
    }

    @Override
    public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) {
        return delegate.getCompositesStream(search, first, max);
    }

    @Override
    public boolean isClientRole() {
        return delegate.isClientRole();
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return delegate.hasRole(role);
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return delegate.getAttributeStream(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return delegate.getAttributes();
    }
    
    @Override
    public void setSingleAttribute(String name, String value) {
        delegate.setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        delegate.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    @Override
    public void release() {
        delegate.release();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RoleModel)) return false;

        RoleModel that = (RoleModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
