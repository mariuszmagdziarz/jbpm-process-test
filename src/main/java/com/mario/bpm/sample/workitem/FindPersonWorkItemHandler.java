package com.mario.bpm.sample.workitem;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import static com.google.common.collect.Maps.newHashMap;

@Slf4j
public class FindPersonWorkItemHandler implements WorkItemHandler {
  private static final String ETL_ENTITY_MANAGER_FACTORY_KEY = "etlEntityManagerFactory";
  private KieSession kieSession;

  public FindPersonWorkItemHandler() {
    log.info("create FindPersonWorkItemHandler");
  }

  public FindPersonWorkItemHandler(KieSession kieSession) {
    log.info("create FindPersonWorkItemHandler kieSession {}", kieSession);
    this.kieSession = kieSession;
  }

  @Override
  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

    log.info("start executeWorkItem FindPersonWorkItemHandler");
    EntityManagerFactory entityManagerFactory =
        (EntityManagerFactory) kieSession.getEnvironment().get(ETL_ENTITY_MANAGER_FACTORY_KEY);
    JpaRepository jpaRepository = new JpaRepository(entityManagerFactory);
    Person person =
        jpaRepository.doInTransaction(
            (EntityManager entityManager) -> entityManager.find(Person.class, Long.valueOf(1)));
    log.info("found the person {}", person);
    manager.completeWorkItem(workItem.getId(), newHashMap());
  }

  @Override
  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    manager.abortWorkItem(workItem.getId());
  }
}
