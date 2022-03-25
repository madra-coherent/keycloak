package org.keycloak.models.jpa;

import org.keycloak.models.resource.Releasable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface JpaModel<T> extends Releasable {
    T getEntity();
}
