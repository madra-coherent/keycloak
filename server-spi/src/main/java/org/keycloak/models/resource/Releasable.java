package org.keycloak.models.resource;

/**
 * Contract for model entities which allow to release resources they detain.
 */
public interface Releasable {

    /**
     * Releases the underlying resources detained by this instance.
     * Default implementation does nothing.
     */
    default void release() {}

}
