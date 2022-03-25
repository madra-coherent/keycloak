package org.keycloak.models.cache.infinispan;

import java.util.Objects;
import java.util.function.Function;

import org.keycloak.models.resource.Releasable;

/**
 * A wrapping function which invokes the {@link Releasable#release()} method onto the
 * operand *after* the wrapped function has been applied.
 *
 * @param <T> the function operand type
 * @param <R> the function result type
 */
public class ReleaseResource<T extends Releasable, R> implements Function<T, R> {

    private final Function<T,R> function;
    
    public ReleaseResource(Function<T, R> function) {
        super();
        Objects.requireNonNull(function, "Argument 'function' cannot be null");
        this.function = function;
    }

    @Override
    public R apply(T operand) {
        try {
            return function.apply(operand);
        }
        finally {
            if (operand != null) operand.release();
        }
    }
    
    /**
     * Wraps the specified function and invokes the {@link Releasable#release()} method onto the
     * operand *after* the specified function has been applied.
     * Note that this implementation is null-safe (accepts null operands).
     * 
     * @param <T> the function operand type
     * @param <R> the function result type
     * @param op the operation to wrap (cannot be null)
     * @return the result of the wrapped operation
     */
    public static <T extends Releasable, R> ReleaseResource<T,R> after(Function<T, R> op) {
        return new ReleaseResource<>(op);
    }
}
