package org.keycloak.storage.jpa.resources;

import java.io.Serializable;
import java.util.function.Predicate;

import javax.persistence.EntityManager;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Predicate for retaining entity ids which are absent from the Hibernate persistence context.
 * Useful in deciding whether to release (detach/evict) entities after an operation has been performed onto them,
 * only when they were not previously present into the persistence context prior to the operation.
 * 
 * Note #1: this implementation is session-specific and MUST NOT be shared across Hibernate sessions or even threads.
 * Note #2: this implementation only checks the presence of root level entities,
 *          and does not make any assumption or check onto the dependent graph of entities
 * Note #3: this implementation is Hibernate specific and using Hibernate SPI, since JPA does not provide any mechanism to do so
 */
public class HibernateContextAbsencePredicate implements Predicate<Serializable> {
    
    private final EntityPersister persister;
    private final PersistenceContext context;
    
    public HibernateContextAbsencePredicate(Class<?> clazz, EntityManager em) {
        super();
        SessionImplementor hibernateSession = em.unwrap( org.hibernate.engine.spi.SessionImplementor.class );
        this.persister = hibernateSession.getSessionFactory().getMetamodel().entityPersister(clazz);
        this.context = hibernateSession.getPersistenceContext();
    }

    @Override
    public boolean test(Serializable id) {
        return !context.containsEntity(new EntityKey(id, persister));
    }
    
}