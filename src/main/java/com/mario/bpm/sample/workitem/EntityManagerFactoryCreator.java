package com.mario.bpm.sample.workitem;

import lombok.extern.slf4j.Slf4j;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

@Slf4j
public class EntityManagerFactoryCreator {

  public EntityManagerFactory create(String persistenceName, ClassLoader classLoader) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    EntityManagerFactory entityManagerFactory;

    log.debug("Creating entity manager factory for persistenceName={}", persistenceName);
    try {
      Thread.currentThread().setContextClassLoader(classLoader);
      entityManagerFactory = Persistence.createEntityManagerFactory(persistenceName);
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    log.debug("Created entity manager factory for persistenceName={}", persistenceName);
    return entityManagerFactory;
  }
}