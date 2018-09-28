package com.mario.bpm.sample.workitem.h2;

import org.h2.tools.Server;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.sql.DriverManager.getConnection;
import static java.util.Collections.singletonList;
import static org.awaitility.Awaitility.await;

/**
 * This is a rule (or class rule) that starts a H2 database engine in "in memory" mode. Specific
 * databases are created on demand during first connection. Data in such a database is persisted as
 * long as there is at least one active connection to that database.
 * <p>
 * <p>For details see H2 documentation: http://www.h2database.com/html/features.html
 * <p>
 * <p>Example usage in test:
 * <pre>
 *     @ClassRule
 *     public static H2DatabaseRule h2DatabaseRule = new H2DatabaseRule(
 *     "9092", "dbTest", "dbUser", "dbPass",
 *     newArrayList("/ftp/dbScripts/create_tables.sql", "/ftp/dbScripts/ftp_download_init_1.sql"));
 * <pre>
 *
 * Requires dependency:
 * <pre>
 *   <dependency>
 *      <groupId>com.h2database</groupId>
 *      <artifactId>h2</artifactId>
 *      <version>${h2.version}</version>
 *      <scope>test</scope>
 *   </dependency>
 * </pre>
 */
@SuppressWarnings("unused")
public class H2DatabaseRule extends ExternalResource {

  private static final Logger log = LoggerFactory.getLogger(H2DatabaseRule.class);

  private Server h2Server;
  private HashMap<String, Connection> bootstrappingConnections;
  private String dbPort;
  private List<H2DatabaseInstance> databaseInstances;

  public H2DatabaseRule(String dbPort, List<H2DatabaseInstance> databaseInstances) {
    this.dbPort = dbPort;
    this.databaseInstances = databaseInstances;
    bootstrappingConnections = new HashMap<>();
  }

  public H2DatabaseRule(
      String dbPort,
      String dbName,
      String dbUser,
      String dbPass,
      String createDatabaseScriptPath,
      List<String> initDatabaseScriptPaths) {
    this.dbPort = dbPort;
    H2DatabaseInstance h2DatabaseInstance =
        new H2DatabaseInstance(
            dbName, dbUser, dbPass, createDatabaseScriptPath, initDatabaseScriptPaths);
    databaseInstances = singletonList(h2DatabaseInstance);
    bootstrappingConnections = new HashMap<>();
  }

  public static String createDatabaseUrl(String dbName, String dbPort) {
    return String.format(
        "jdbc:h2:tcp://localhost:%s/mem:%s;MV_STORE=FALSE;MVCC=FALSE;LOCK_TIMEOUT=10000",
        dbPort, dbName);
  }

  @Override
  protected void before() throws Exception {

    log.info("Starting H2 server...");

    h2Server = Server.createTcpServer();
    h2Server.start();

    for (H2DatabaseInstance databaseInstance : databaseInstances) {
      Connection bootstrappingConnection = createAndInit(databaseInstance, dbPort);
      bootstrappingConnections.put(databaseInstance.getDbName(), bootstrappingConnection);
    }
  }

  private Connection createAndInit(H2DatabaseInstance databaseInstance, String dbPort) {

    String dbNameTmp = databaseInstance.getDbName();
    String dbUserTmp = databaseInstance.getDbUser();
    String dbPassTmp = databaseInstance.getDbPass();
    StringBuilder dbUrlBuilder = new StringBuilder(createDatabaseUrl(dbNameTmp, dbPort));
    String createDatabaseScriptPath = databaseInstance.getCreateDatabaseScriptPath();
    List<String> initDatabaseScriptPaths = databaseInstance.getInitDatabaseScriptPaths();

    // Append create sql script to database connection URL.
    // H2 will execute this script when it receives this connection.
    boolean addingFirstElement = true;
    if (!isNullOrEmpty(createDatabaseScriptPath)) {
      dbUrlBuilder.append(";INIT=runscript from 'classpath:");
      dbUrlBuilder.append(createDatabaseScriptPath);
      dbUrlBuilder.append("'");
      addingFirstElement = false;
    }
    // Append init sql scripts to database connection URL.
    // H2 will execute those scripts when it receives this connection.
    if (!initDatabaseScriptPaths.isEmpty()) {
      for (String initScript : initDatabaseScriptPaths) {
        if (addingFirstElement) {
          dbUrlBuilder.append(";INIT=runscript from 'classpath:");
          dbUrlBuilder.append(initScript);
          dbUrlBuilder.append("'");
        } else {
          dbUrlBuilder.append("\\;runscript from 'classpath:");
          dbUrlBuilder.append(initScript);
          dbUrlBuilder.append("'");
        }
        addingFirstElement = false;
      }
    }

    // This database is created in "in memory mode". In this mode database's state is persisted as
    // long as there is at least one active connection to this database.
    // That is why we establish this artificail connection in "before" method and close it in
    // "after" method when the tests finish.
    // That way we make sure that at least one connection is active and that this database will be
    // persisted for the duration of tests.
    log.info(
        "Establishing bootstrapping connection to H2 server dbUrl={} dbUser={} dbPassTmp={}",
        dbUrlBuilder,
        dbUserTmp,
        dbPassTmp);
    try {
      return getConnection(dbUrlBuilder.toString(), dbUserTmp, dbPassTmp);
    } catch (SQLException e) {
      throw new H2DatabaseRuleException("Failed to get connection", e);
    }
  }

  @Override
  protected void after() {
    for (H2DatabaseInstance databaseInstance : databaseInstances) {
      await().atMost(10, TimeUnit.SECONDS).until(() -> destroy(databaseInstance));
    }
    if (h2Server != null && h2Server.isRunning(true)) {
      await().atMost(1, TimeUnit.MINUTES).until(this::shutDownTcpServer);
      log.info("Is H2 server running: {}", h2Server.isRunning(true));
    }
  }

  private boolean shutDownTcpServer() {
    String tcpServerUrl = "tcp://localhost:" + dbPort;

    try {
      log.info("Force closing H2 Server");
      Server.shutdownTcpServer(tcpServerUrl, "", true, true);
      log.info("Force closing H2 Server completed");
    } catch (SQLException ex) {
      throw new H2DatabaseRuleException("Failed to force closing H2 Server", ex);
    }

    return true;
  }

  private boolean destroy(H2DatabaseInstance databaseInstance) {

    String databaseName = databaseInstance.getDbName();

    Connection bootstrappingConnection = bootstrappingConnections.get(databaseName);
    if (bootstrappingConnection == null) {
      log.warn(
          "Failed to destroy database '{}' - bootstrapping connection is not available",
          databaseName);
      return true;
    }

    log.info("Shutting down database '{}'", databaseName);
    try {
      shutdownDatabase(bootstrappingConnection);
    } catch (Exception ex) {
      log.warn("Failed to shutdown database '{}'", databaseName, ex);
    }

    log.info("Closing bootstrapping connection for database '{}'", databaseName);
    try {
      bootstrappingConnection.close();
    } catch (Exception ex) {
      log.warn("Failed to close bootstrapping connection for database '{}'", databaseName, ex);
    }

    return true;
  }

  private void shutdownDatabase(Connection connection) throws SQLException {
    boolean connectionIsClosed;
    try {
      connectionIsClosed = connection == null || connection.isClosed();
    } catch (SQLException ex) {
      throw new H2DatabaseRuleException("Failed to check connection", ex);
    }
    if (connectionIsClosed) {
      log.warn(
          "Can't clean database, database bootstrapping connection is closed, database is probably down...");
      return;
    }
    try (Statement statement = connection.createStatement()) {
      execute(statement, "SHUTDOWN");
    }
  }

  private void execute(Statement statement, String sqlScriptLine) {
    try {
      log.info("Executing sql script line [{}]", sqlScriptLine);
      statement.execute(sqlScriptLine);
    } catch (SQLException ex) {
      throw new H2DatabaseRuleException("Failed to execute statement", ex);
    }
  }
}
