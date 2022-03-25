package org.keycloak.models.cache.infinispan;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.entities.CachedClientRole;
import org.keycloak.models.cache.infinispan.entities.CachedRealmRole;
import org.keycloak.models.cache.infinispan.entities.CachedRole;

/**
 * Implements {@link RoleModel} cache get and put operations restricted to a limited set of realms.
 * This implementation does NOT check for invalidations.
 */
public class RealmRestrictedRoleCacheOperation {

    private final RealmCacheSession session;
    private final Map<String, RealmModel> realmsById;
    
    public RealmRestrictedRoleCacheOperation(RealmCacheSession session, Set<RealmModel> realms) {
        super();
        Objects.requireNonNull(session, "Argument 'session' cannot be null");
        Objects.requireNonNull(realms, "Argument 'realms' cannot be null");
        this.session = session;
        this.realmsById = realms.stream().collect(Collectors.toMap(RealmModel::getId, Function.identity()));
    }
    
    protected Optional<RoleAdapter> getRoleFromCache(String id) {
        CachedRole cached = session.cache.get(id, CachedRole.class);
        if (cached == null) return Optional.empty(); 
        RealmModel realm = realmsById.get(cached.getRealm());
        if (realm == null) return Optional.empty();
        return Optional.of(new RoleAdapter(cached, session, realm));
    }

    protected RoleAdapter addRoleToCache(RealmModel realm, RoleModel model) {
        Long loaded = session.cache.getCurrentRevision(model.getId());
        CachedRole cached;
        if (model.isClientRole()) {
            cached = new CachedClientRole(loaded, model.getContainerId(), model, realm);
        } else {
            cached = new CachedRealmRole(loaded, model, realm);
        }
        session.cache.addRevisioned(cached, session.startupRevision);
        return new RoleAdapter(cached, session, realm);
    }

    protected RoleModel addManagedRole(RoleAdapter role) {
        session.managedRoles.put(role.getId(), role);
        return role;
    }
    
    public Optional<RoleModel> get(String id) {
        Optional<RoleModel> alreadyManagedRole = Optional.ofNullable(session.managedRoles.get(id));
        return alreadyManagedRole.isPresent() ? alreadyManagedRole
                : getRoleFromCache(id).map(this::addManagedRole);
    }

    public Optional<RoleModel> put(RoleModel role) {
        if (role == null || role.getRealmId() == null) return Optional.ofNullable(role);
        RealmModel realm = realmsById.get(role.getRealmId());
        if (realm == null) return Optional.of(role);
        return Optional.of(addManagedRole(addRoleToCache(realm, role)));
    }
    
}
