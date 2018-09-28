package com.mario.bpm.sample.workitem;

import com.mario.bpm.sample.workitem.h2.H2DatabaseRule;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;


public class FindPersonProcessIT extends JbpmJUnitBaseTestCase {

    private static final String SHOW_MESSAGE_PROCESS_ID = "com.mario.bpm.sample.FindPersonProcess";
    private static final String SHOW_MESSAGE_BPM_FILE = "FindPersonProcess.bpmn2";
    private static final boolean SETUP_DATASOURCE = true;
    private static final boolean SETUP_PERSISTENCE = true;
    private static final String SHOW_MESSAGE_WORK_ITEM_NAME = "FindPersonWorkItemHandler";
    private static final String APP_PERSISTENCE_UNIT_NAME = "jbpmapptest";
    private static final String ETL_ENTITY_MANAGER_FACTORY_KEY = "etlEntityManagerFactory";
    @Rule
    public H2DatabaseRule h2Database =
            new H2DatabaseRule(
                    "9092",
                    "dbTest",
                    "dbUser",
                    "dbPass",
                    "sql/create_schema_h2.sql",
                    newArrayList("sql/init.sql"));
    private EntityManagerFactory entityManagerFactory;
    private RuntimeManager manager;
    private RuntimeEngine kieEngine;
    private KieSession kieSession;


    public FindPersonProcessIT() {
        super(SETUP_DATASOURCE, SETUP_PERSISTENCE);
    }

    @Before
    public void setup() throws Exception {
        entityManagerFactory = Persistence.createEntityManagerFactory(APP_PERSISTENCE_UNIT_NAME);
        addEnvironmentEntry(ETL_ENTITY_MANAGER_FACTORY_KEY, entityManagerFactory);
        manager = createRuntimeManager(SHOW_MESSAGE_BPM_FILE);
        kieEngine = getRuntimeEngine(null);
        kieSession = kieEngine.getKieSession();
        kieSession
                .getWorkItemManager()
                .registerWorkItemHandler(SHOW_MESSAGE_WORK_ITEM_NAME, new FindPersonWorkItemHandler(kieSession));
    }

    @After
    public void tearDown() {
        if (nonNull(entityManagerFactory)) {
            entityManagerFactory.close();
        }
    }

    @Test
    public void shouldRunProcess() {
        // given
        String expectedFindPersonNodeName = "FindPersonWorkItemHandler";

        // when
        ProcessInstance showMessageProcessInstance = kieSession.startProcess(SHOW_MESSAGE_PROCESS_ID);

        // then
        assertProcessInstanceCompleted(showMessageProcessInstance.getId());
        assertNodeTriggered(showMessageProcessInstance.getId(), expectedFindPersonNodeName);
    }
}
