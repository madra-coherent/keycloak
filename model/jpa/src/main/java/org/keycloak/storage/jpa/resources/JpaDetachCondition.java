package org.keycloak.storage.jpa.resources;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Detach condition for JPA entities which IDs are known upfront: the IDs which are eligible for detach/evict
 * are evaluated at construction time.
 * 
 * Used to detect which entities may be released after performing a JPA operation: the JPA operation may load
 * entities either from the persistence unit (database) or from the persistence context cache,
 * in which case releasing them may create side effect (loosing uncommitted changes, or code already detaining
 * a reference onto such an entity later throwing exception because it was detached in the meanwhile).
 * Building such an instance prior to executing the JPA operation captures this information for a conditional
 * detach/evict after the operation was performed.
 */
public class JpaDetachCondition implements Predicate<Serializable> {

    private final Set<Serializable> idsEligibleForRelease;
    
    public JpaDetachCondition(Collection<? extends Serializable> resourceIds, Predicate<Serializable> condition) {
        super();
        Objects.requireNonNull(resourceIds, "Argument 'resourceIds' cannot be null");
        Objects.requireNonNull(condition, "Argument 'condition' cannot be null");
        this.idsEligibleForRelease = resourceIds.stream().filter(condition).collect(Collectors.toSet());
    }

    @Override
    public boolean test(Serializable resourceId) {
        return idsEligibleForRelease.contains(resourceId);
    }

}
