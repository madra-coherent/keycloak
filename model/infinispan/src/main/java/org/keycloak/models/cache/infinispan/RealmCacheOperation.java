package org.keycloak.models.cache.infinispan;

import java.util.Objects;
import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.CachedRealmModel;
import org.keycloak.models.cache.infinispan.entities.CachedRealm;

/**
 * Implements {@link RealmModel} cache get and put operations.
 * This implementation does check for invalidations, and publishes cache events as well when appropriate.
 */
public class RealmCacheOperation {

    private final RealmCacheSession realmCacheSession;

    public RealmCacheOperation(RealmCacheSession realmCacheSession) {
        super();
        Objects.requireNonNull(realmCacheSession, "Argument 'realmCacheSession' cannot be null");
        this.realmCacheSession = realmCacheSession;
    }

    protected Optional<RealmAdapter> getRealmFromCache(String id) {
        return Optional.ofNullable(realmCacheSession.cache.get(id, CachedRealm.class))
                .map(cached -> new RealmAdapter(realmCacheSession.session, cached, realmCacheSession));
    }

    protected RealmAdapter addRealmToCache(RealmModel realm) {
        Long loaded = realmCacheSession.cache.getCurrentRevision(realm.getId());
        CachedRealm cached = new CachedRealm(loaded, realm);
        realmCacheSession.cache.addRevisioned(cached, realmCacheSession.startupRevision);
        RealmAdapter adapter = new RealmAdapter(realmCacheSession.session, cached, realmCacheSession);
        publishRealmCachedEvent(adapter);
        realm.release();
        return adapter;
    }
    
    protected void publishRealmCachedEvent(RealmAdapter adapter) {
        CachedRealmModel.RealmCachedEvent event = new CachedRealmModel.RealmCachedEvent() {
            @Override
            public CachedRealmModel getRealm() {
                return adapter;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return realmCacheSession.session;
            }
        };
        realmCacheSession.session.getKeycloakSessionFactory().publish(event);
    }

    protected RealmAdapter getManagedRealm(String id) {
        return realmCacheSession.managedRealms.get(id);
    }

    protected RealmAdapter addManagedRealm(RealmAdapter realm) {
        realmCacheSession.managedRealms.put(realm.getId(), realm);
        return realm;
    }

    /**
     * Perform a cache lookup for the specified id
     * @param id the realm ID to look for
     * @return the realm found in the cache, or {@code empty} if id is invalidated or absent from cache
     */
    public Optional<RealmModel> get(String id) {
        if (realmCacheSession.invalidations.contains(id)) return Optional.empty();
        Optional<RealmModel> alreadyManagedRealm = Optional.ofNullable(getManagedRealm(id));
        return alreadyManagedRealm.isPresent() ? alreadyManagedRealm
                : getRealmFromCache(id).map(this::addManagedRealm);
    }

    /**
     * Adds the specified realm into the cache if not null and not invalidated
     * @param realm the realm to add to cache
     * @return the newly cached realm, or the provided realm if it could not be added to cache
     */
    public Optional<RealmModel> put(RealmModel realm) {
        if (realm == null || realmCacheSession.invalidations.contains(realm.getId())) return Optional.ofNullable(realm);
        return Optional.of(realm)
                .map(this::addRealmToCache)
                .map(this::addManagedRealm);
    }

}
