# Introduction

The purpose of this project is to reproduce the issue related to deploying new KJAR version. 
KJAR contains only ones BPM process "FindPersonProcess". This process consist of one custom WorkItemHandler 
and executes one search query onto "Person" table. 

# IDE configuration

One should install lombok plugin in Intellij IDEA (standard plugin installation procedure).
Plugin homepage: https://github.com/mplushnikov/lombok-intellij-plugin

# Setup database integration

- This project for tests use H2 database in memory .
- This project needs Oracle (or OracleXE) database onto production environment.

- Oracle SQL:
            
        --create person table
        CREATE TABLE person (
          ID                         NUMBER       NOT NULL,
          NAME                       VARCHAR(100) NOT NULL,
          PRIMARY KEY (ID)
        );
        
        CREATE UNIQUE INDEX PERSON_NAME_IDX
          ON person (NAME);
        
        CREATE SEQUENCE PERSON_SEQ
          START WITH 1000
          INCREMENT BY 1;
        
        -- add one row to person table  
          INSERT INTO person VALUES(1,'Mario'); 


# Deployment environment
- Java 1.8
- KIE-server 7.0.2
- JBoss EAP 7.1.0
- OracleXE 11

# How to reproduce the issue?

- deploy process

        curl -i  --user "admin:admin" -H "Content-Type: application/json" -X PUT -d \
            '{"container-id" : "com.mario.bpm.sample:sample-process:0.0.1-SNAPSHOT","container-name" : "sample-process","release-id" : {"group-id" :"com.mario.bpm.sample","artifact-id" : "sample-process","version" : "0.0.1-SNAPSHOT"},"configuration" : { },"status" : "STARTED"}' \
            http://localhost:8081/kie-server/services/rest/server/containers/com.mario.bpm.sample:sample-process:0.0.1-SNAPSHOT 
- start process

        curl -i  --user "admin:admin" -H "Content-Type: application/json" -X POST -d '{
         "job-command" : "org.jbpm.process.core.async.AsyncStartProcessCommand",
         "scheduled-date" : null,
         "request-data" : {
           "retries" : "0",
           "DeploymentId" : "com.mario.bpm.sample:sample-process:0.0.1-SNAPSHOT",
           "CorrelationKey" : "'${RANDOM}'",
           "ProcessId" : "com.mario.bpm.sample.FindPersonProcess"
         }
        }'  \
               http://localhost:8081/kie-server/services/rest/server/jobs

- expected output

         2018-09-28 16:39:03,011 INFO  [com.mario.bpm.sample.workitem.FindPersonWorkItemHandler] (Thread-0 (ActiveMQ-client-global-threads)) start executeWorkItem FindPersonWorkItemHandler
         2018-09-28 16:39:03,027 INFO  [com.mario.bpm.sample.workitem.FindPersonWorkItemHandler] (Thread-0 (ActiveMQ-client-global-threads)) found the person Person(id=1, name=Mario)
         
 - create new KJAR version (or undeploy previous one and deploy once again)
        
        curl -i --user "admin:admin" -X DELETE http://localhost:8081/kie-server/services/rest/server/containers/com.mario.bpm.sample:sample-process:0.0.1-SNAPSHOT
 - repeat step: "deploy process", "start process"
  
         
 - expected output (error)         
            
         ...   
         2018-09-28 16:48:11,797 WARN  [org.drools.persistence.PersistableRunner] (Thread-1 (ActiveMQ-client-global-threads)) Could not commit session: org.jbpm.workflow.instance.WorkflowRuntimeException: [com.mario.bpm.sample.FindPersonProcess:10 - FindPersonWorkItemHandler:3] -- com.mario.bpm.sample.workitem.Person cannot be cast to com.mario.bpm.sample.workitem.Person
            at org.jbpm.workflow.instance.node.WorkItemNodeInstance.internalTrigger(WorkItemNodeInstance.java:150)
            at org.jbpm.workflow.instance.impl.NodeInstanceImpl.trigger(NodeInstanceImpl.java:186)
            ...
         Caused by: java.lang.ClassCastException: com.mario.bpm.sample.workitem.Person cannot be cast to com.mario.bpm.sample.workitem.Person
         	at com.mario.bpm.sample.workitem.FindPersonWorkItemHandler.lambda$executeWorkItem$0(FindPersonWorkItemHandler.java:37)
         	at com.mario.bpm.sample.workitem.JpaRepository.doInTransaction(JpaRepository.java:31)
         	at com.mario.bpm.sample.workitem.FindPersonWorkItemHandler.executeWorkItem(FindPersonWorkItemHandler.java:36)
         	at org.drools.persistence.jpa.processinstance.JPAWorkItemManager.internalExecuteWorkItem(JPAWorkItemManager.java:69)
         	at org.jbpm.workflow.instance.node.WorkItemNodeInstance.internalTrigger(WorkItemNodeInstance.java:140)
         	... 117 more

