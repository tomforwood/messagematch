package org.forwoods.messagematch.junit.testrunstore;

import org.forwoods.messagematch.junit.SuccessfulScenarioTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class TestRunStore {
    public static TestRunStore INSTANCE = new TestRunStore();

    public final static String CREATE_TEST_TABLE = "CREATE TABLE IF NOT EXISTS successful_server_tests (" +
            "	test_name VARCHAR," +
            "	scenario_name VARCHAR NOT NULL," +
            "	scenario_hash VARCHAR(32) NOT NULL," +
            "   api_version VARCHAR(32) NULL," +
            "   runtime varchar(32)"
            + ");" +
            "CREATE INDEX scenario_id_idx IF NOT EXISTS on successful_tests (scenario_name, scenario_hash, runtime)  ";

    public final static String INSERT_TEST_RESULT = " INSERT INTO successful_server_tests VALUES (?, ?, ?, ?, ?)";
    private final Connection conn;
    private final PreparedStatement insertStatement;

    private TestRunStore() {
        String url = Optional.ofNullable(System.getenv("MESSAGEMATCH_TEST_DATABASE_URL"))
                .orElse("jdbc:sqlite:messagematch.testresult.db");
        try  {
            conn = DriverManager.getConnection(url);
            if (conn != null) {
                buildTable(conn);
                insertStatement = conn.prepareStatement(INSERT_TEST_RESULT);
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

    public void storeTestRun(SuccessfulScenarioTest testRecord) {
        try {
            insertStatement.setString(1, testRecord.testName());
            insertStatement.setString(2, testRecord.scenarioName());
            insertStatement.setString(3, testRecord.scenarioHash());
            insertStatement.setString(4, testRecord.apiVersion());
            insertStatement.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
            System.out.println("AQdded test result: " + testRecord);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
