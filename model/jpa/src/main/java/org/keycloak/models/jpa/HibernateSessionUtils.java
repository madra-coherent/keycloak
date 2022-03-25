package org.keycloak.models.jpa;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.persister.entity.EntityPersister;
import org.jboss.logging.Logger;


public class HibernateSessionUtils {

    protected static final Logger logger = Logger.getLogger(HibernateSessionUtils.class);

    public static void inspect(EntityManager em) {
        org.hibernate.engine.spi.SessionImplementor hibernateSession = em.unwrap( org.hibernate.engine.spi.SessionImplementor.class );
        org.hibernate.engine.spi.PersistenceContext pc = hibernateSession.getPersistenceContext();
        Map.Entry<Object,org.hibernate.engine.spi.EntityEntry>[] entityEntries = pc.reentrantSafeEntityEntries();
        Map<org.hibernate.engine.spi.Status,Map<String,Long>> countEntitiesPerStatusPerClass = Stream.of(entityEntries)
                .map(Map.Entry::getValue)
                .collect(Collectors.groupingBy(
                        org.hibernate.engine.spi.EntityEntry::getStatus,
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.stream().collect(
                                Collectors.groupingBy(
                                        entry -> StringUtils.substringAfterLast(entry.getEntityName(), "."),
                                        Collectors.counting()
                                       )
                                )
                        )
                        ));
        logger.infof("HHH session => %d : %s", entityEntries.length, countEntitiesPerStatusPerClass.toString());
        
    }

    public static void inspect(EntityManager em, Serializable entityKey) {
        org.hibernate.engine.spi.SessionImplementor hibernateSession = em.unwrap( org.hibernate.engine.spi.SessionImplementor.class );
        org.hibernate.engine.spi.PersistenceContext pc = hibernateSession.getPersistenceContext();
        Map.Entry<Object,org.hibernate.engine.spi.EntityEntry>[] entityEntries = pc.reentrantSafeEntityEntries();
        Optional<Map.Entry<Object,org.hibernate.engine.spi.EntityEntry>> entry = Stream.of(entityEntries)
                .filter(e -> e.getValue().getEntityKey().getIdentifier().equals(entityKey))
                .findFirst();
        logger.infof("HHH session => key %s : %s", entityKey,
                entry.isPresent() ? StringUtils.substringAfterLast(entry.get().getValue().getEntityName(), ".") : "<not found>"
                    );
    }

    public static boolean existsInContext(EntityManager em, Class<?> clazz, Serializable id) {
        org.hibernate.engine.spi.SessionImplementor hibernateSession = em.unwrap( org.hibernate.engine.spi.SessionImplementor.class );
//        ClassMetadata metadata = hibernateSession.getSessionFactory().getClassMetadata(clazz);
//        EntityPersister persister = hibernateSession.getFactory().getEntityPersister(metadata.getEntityName());
        EntityPersister persister = hibernateSession.getSessionFactory().getMetamodel().entityPersister(clazz);
        PersistenceContext context = hibernateSession.getPersistenceContext();

        EntityKey entityKey = new EntityKey(id, persister);
        return context.containsEntity(entityKey);
    }

}
