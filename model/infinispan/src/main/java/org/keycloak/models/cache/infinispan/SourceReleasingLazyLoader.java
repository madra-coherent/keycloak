package org.keycloak.models.cache.infinispan;

import java.util.function.Function;
import java.util.function.Supplier;

import org.keycloak.models.resource.Releasable;

/**
 * Specialized flavour of {@link DefaultLazyLoader} which releases loaded entities
 *
 * @param <S> the type of the {@link Releasable} source entity to load from
 * @param <D> the type of data being supplied
 */
public class SourceReleasingLazyLoader<S extends Releasable, D> extends DefaultLazyLoader<S, D> {

    public SourceReleasingLazyLoader(Function<S, D> loader, Supplier<D> fallback) {
        super(ReleaseResource.after(loader), fallback);
    }

}
