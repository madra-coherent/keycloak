package org.keycloak.storage;

import java.util.Objects;
import java.util.Optional;

import org.keycloak.models.RealmModel;
import org.keycloak.storage.role.RoleLookupProvider;

/**
 * Represents the outcome of the resolution of a {@link RoleLookupProvider} by its ID against a realm.
 * See {@link RoleLookupProviderResolver}.
 */
public class RoleLookupProviderResolution {

    private final RealmModel realm;
    private final Optional<RoleLookupProvider> provider;

    public RoleLookupProviderResolution(RealmModel realm, Optional<RoleLookupProvider> provider) {
        super();
        Objects.requireNonNull(realm, "Argument 'realm' cannot be null");
        Objects.requireNonNull(provider, "Argument 'provider' cannot be null");
        this.realm = realm;
        this.provider = provider;
    }
    
    public boolean isResolved() {
        return provider.isPresent();
    }
    
    public RealmModel getRealm() {
        return realm;
    }
    
    public RoleLookupProvider getProvider() {
        return provider.orElse(null);
    }
    
}
