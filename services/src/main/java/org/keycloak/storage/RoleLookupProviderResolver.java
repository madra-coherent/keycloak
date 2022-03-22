package org.keycloak.storage;

import java.util.Objects;
import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.role.RoleLookupProvider;

/**
 * Performs the resolution of {@link RoleLookupProvider} instances by their ID against a realm
 */
public class RoleLookupProviderResolver {
    
    private final Optional<String> providerId;
    private final KeycloakSession session;

    /**
     * Builds a resolver which will resolve the provided ID.
     * Note that the default local storage provider will be used when the provided ID is absent.
     * @param providerId the ID to resolve
     * @param session
     */
    public RoleLookupProviderResolver(Optional<String> providerId, KeycloakSession session) {
        super();
        Objects.requireNonNull(providerId, "Argument 'providerId' cannot be null");
        Objects.requireNonNull(session, "Argument 'session' cannot be null");
        this.providerId = providerId;
        this.session = session;
    }

    /**
     * Resolves the provider ID against the specified realm
     * @param realm the realm to resolve the ID against
     * @return the resolved {@link RoleLookupProvider} instance ({@link Optional#isPresent()} returning true if found, false if not or disabled)
     */
    public RoleLookupProviderResolution resolve(RealmModel realm) {
        return new RoleLookupProviderResolution(realm, getEnabledRoleLookupProviderForRealm(realm));
    }
    
    private Optional<RoleLookupProvider> getEnabledRoleLookupProviderForRealm(RealmModel realm) {
        return providerId.isPresent() ?
                Optional.ofNullable( (RoleLookupProvider) RoleStorageManager.getStorageProvider(session, realm, providerId.get()) )
                        .filter(provider -> RoleStorageManager.isStorageProviderEnabled(realm, providerId.get()))
                : Optional.of(session.roleLocalStorage());
    }

}
