package com.mario.bpm.sample.workitem.h2;

import com.google.common.base.MoreObjects;

import java.util.List;

public class H2DatabaseInstance {

  private String dbName;
  private String dbUser;
  private String dbPass;
  private String createDatabaseScriptPath;
  private List<String> initDatabaseScriptPaths;

  public H2DatabaseInstance(
      String dbName,
      String dbUser,
      String dbPass,
      String createDatabaseScriptPath,
      List<String> initDatabaseScriptPaths) {
    this.dbName = dbName;
    this.dbUser = dbUser;
    this.dbPass = dbPass;
    this.createDatabaseScriptPath = createDatabaseScriptPath;
    this.initDatabaseScriptPaths = initDatabaseScriptPaths;
  }

  String getDbName() {
    return dbName;
  }

  String getDbUser() {
    return dbUser;
  }

  String getDbPass() {
    return dbPass;
  }

  String getCreateDatabaseScriptPath() {
    return createDatabaseScriptPath;
  }

  List<String> getInitDatabaseScriptPaths() {
    return initDatabaseScriptPaths;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(H2DatabaseInstance.class)
        .add("dbName", dbName)
        .add("dbUser", dbUser)
        .add("dbPass", dbPass)
        .add("createDatabaseScriptPath", createDatabaseScriptPath)
        .add("initDatabaseScriptPaths", initDatabaseScriptPaths)
        .toString();
  }
}
