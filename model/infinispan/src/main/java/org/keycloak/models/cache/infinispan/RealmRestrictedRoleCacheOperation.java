package org.keycloak.models.cache.infinispan;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleCompositionModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.entities.CachedClientRole;
import org.keycloak.models.cache.infinispan.entities.CachedRealmRole;
import org.keycloak.models.cache.infinispan.entities.CachedRole;

/**
 * Implements {@link RoleModel} cache get and put operations restricted to a limited set of realms.
 * This implementation does check for invalidations.
 */
public class RealmRestrictedRoleCacheOperation {

    private final RealmCacheSession realmCacheSession;
    private final Map<String, RealmModel> realmsById;
    
    public RealmRestrictedRoleCacheOperation(RealmCacheSession realmCacheSession, Set<RealmModel> realms) {
        super();
        Objects.requireNonNull(realmCacheSession, "Argument 'realmCacheSession' cannot be null");
        Objects.requireNonNull(realms, "Argument 'realms' cannot be null");
        this.realmCacheSession = realmCacheSession;
        this.realmsById = realms.stream().collect(Collectors.toMap(RealmModel::getId, Function.identity()));
    }
    
    protected Optional<RoleAdapter> getRoleFromCache(String id) {
        CachedRole cached = realmCacheSession.cache.get(id, CachedRole.class);
        if (cached == null) return Optional.empty(); 
        RealmModel realm = realmsById.get(cached.getRealm());
        if (realm == null) return Optional.empty();
        return Optional.of(new RoleAdapter(cached, realmCacheSession, realm));
    }

    protected RoleAdapter addRoleToCache(RealmModel realm, RoleModel model) {
        Long loaded = realmCacheSession.cache.getCurrentRevision(model.getId());
        CachedRole cached;
        if (model.isClientRole()) {
            cached = new CachedClientRole(loaded, model.getContainerId(), model, realm);
        } else {
            cached = new CachedRealmRole(loaded, model, realm);
        }
        realmCacheSession.cache.addRevisioned(cached, realmCacheSession.startupRevision);
        return new RoleAdapter(cached, realmCacheSession, realm);
    }

    protected RoleModel addManagedRole(RoleAdapter role) {
        realmCacheSession.managedRoles.put(role.getId(), role);
        return role;
    }
    
    /**
     * Perform a cache lookup for the specified id
     * @param id the role ID to look for
     * @return the role found in the cache, or {@code empty} if id is invalidated, absent from cache, or not in one of the specified realms
     */
    public Optional<RoleModel> get(String id) {
        if (realmCacheSession.invalidations.contains(id)) return Optional.empty();
        Optional<RoleModel> alreadyManagedRole = Optional.ofNullable(realmCacheSession.managedRoles.get(id));
        return alreadyManagedRole.isPresent() ? alreadyManagedRole
                : getRoleFromCache(id).map(this::addManagedRole);
    }

    /**
     * Perform a cache lookup for the specified role composition structure
     * @param id the role composition to look for
     * @return the role found in the cache, or {@code empty} if role composition is null, id is invalidated or not in cache
     */
    public Optional<RoleModel> get(RoleCompositionModel roleComposition) {
        if (roleComposition == null) return Optional.empty();
        return get(roleComposition.getRoleId());
    }

    /**
     * Adds the specified role into the cache if not null, not invalidated and in one of the specified realms
     * @param role the role to add to cache
     * @return the newly cached role, or the provided role if it could not be added to cache
     */
    public Optional<RoleModel> put(RoleModel role) {
        if (role == null || role.getRealmId() == null
                || realmCacheSession.invalidations.contains(role.getId())) {
            return Optional.ofNullable(role);
        }
        RealmModel realm = realmsById.get(role.getRealmId());
        if (realm == null) return Optional.of(role);
        return Optional.of(addManagedRole(addRoleToCache(realm, role)));
    }


}
