package org.forwoods.messagematch.junit.testrunstore;

import org.forwoods.messagematch.junit.ScenarioTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class TestRunStore {
    public static TestRunStore INSTANCE = new TestRunStore();

    public final static String CREATE_TEST_TABLE = "CREATE TABLE IF NOT EXISTS successful_server_tests (" +
            "   result integer NOT NULL," +
            "	test_name VARCHAR," +
            "	scenario_name VARCHAR NOT NULL," +
            "	scenario_hash VARCHAR(32) NOT NULL," +
            "   api_name VARCHAR(64) NULL," +
            "   api_version VARCHAR(32) NOT NULL," +
            "   runtime varchar(32)"
            + ");"
            //+ "CREATE INDEX scenario_id_idx IF NOT EXISTS on successful_tests (scenario_hash, api_version, runtime)  "
            ;

    public final static String INSERT_TEST_RESULT = " INSERT INTO successful_server_tests " +
            "(result, test_name, scenario_name, scenario_hash, api_name, api_version, runtime)" +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    public final static String DELETE_API_RESULTS = " DELETE FROM successful_server_tests where api_name=? and api_version=?";
    private final PreparedStatement insertStatement;
    private final PreparedStatement deleteStatement;

    private TestRunStore() {
        final String messagematchTestDatabaseUrl = System.getenv("MESSAGEMATCH_TEST_DATABASE_URL");
        String url = Optional.ofNullable(messagematchTestDatabaseUrl)
                .orElse("jdbc:sqlite:messagematch.testresult.db");
        try  {
            final Connection conn = DriverManager.getConnection(url);
            if (conn != null) {
                buildTable(conn);
                insertStatement = conn.prepareStatement(INSERT_TEST_RESULT);
                deleteStatement = conn.prepareStatement(DELETE_API_RESULTS);
            }
            else {
                throw new RuntimeException("Unable to connect to test run store");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to database", e);
        }
    }

    private void buildTable(final Connection conn) {
        try (final Statement create = conn.createStatement()){
            create.execute(CREATE_TEST_TABLE);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void storeTestRun(ScenarioTest testRecord) {
        try {
//result, test_name, scenario_name, scenario_hash, api_name, api_version, runtime
            insertStatement.setBoolean(1, testRecord.result());
            insertStatement.setString(2, testRecord.testName());
            insertStatement.setString(3, testRecord.scenarioName());
            insertStatement.setString(4, testRecord.scenarioHash());
            insertStatement.setString(5, testRecord.api());
            insertStatement.setString(6, testRecord.apiVersion());
            insertStatement.setTimestamp(7, new java.sql.Timestamp(System.currentTimeMillis()));
            insertStatement.executeUpdate();
            System.out.println("Added test result: " + testRecord);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearApiVersion(String api, String version) {
        try {
            deleteStatement.setString(1, api);
            deleteStatement.setString(2, version);
            deleteStatement.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
