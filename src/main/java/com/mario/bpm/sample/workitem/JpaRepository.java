package com.mario.bpm.sample.workitem;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

class JpaRepository {
    private final EntityManagerFactory entityManagerFactory;

    JpaRepository(EntityManagerFactory entityManagerFactory) {
        checkArgument(!isNull(entityManagerFactory), "Expected non-null entityManagerFactory");

        this.entityManagerFactory = entityManagerFactory;
    }

    <T> T doInTransaction(JpaRepository.JPATransactionFunction<T> function) {
        checkArgument(nonNull(function), "Expected non-null function");
        T result;
        EntityManager entityManager = null;
        EntityTransaction transaction = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            transaction = entityManager.getTransaction();
            transaction.begin();
            result = function.apply(entityManager);
            transaction.commit();
        } catch (Exception ex) {

            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw ex;
        } finally {

            if (entityManager != null) {
                entityManager.close();
            }
        }
        return result;
    }

    @FunctionalInterface
    interface JPATransactionFunction<T> extends Function<EntityManager, T> {}
}