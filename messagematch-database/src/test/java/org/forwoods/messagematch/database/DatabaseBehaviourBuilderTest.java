package org.forwoods.messagematch.database;

import org.forwoods.messagematch.junit.MessageSpec;
import org.forwoods.messagematch.junit.MessageSpecExtension;
import org.forwoods.messagematch.spec.TestSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MessageSpecExtension.class)
public class DatabaseBehaviourBuilderTest {

    DatabaseBehaviourBuilder database;

    @Test
    void readSucceeds(@MessageSpec("src/test/resources/database-passes") TestSpec spec, TestInfo testInfo) throws SQLException {
        database = new DatabaseBehaviourBuilder(List.of(DatabaseBehaviourBuilderTest.class.getResourceAsStream("/database.sql")),
                "jdbc:h2:mem:"+testInfo.getTestMethod().map(Method::toString).orElse("read")+";DB_CLOSE_DELAY=-1;MODE=MySQL");
        database.addBehavior(spec.getSideEffects());

        Connection connection = database.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select * from scripted1");
        resultSet.next();
        assertEquals("ABC", resultSet.getString(1));
        assertEquals(1, resultSet.getInt(2));
        assertEquals(1.1, resultSet.getFloat(3), 1e-5);

        resultSet = statement.executeQuery("select * from unscripted1");
        resultSet.next();
        assertEquals("ABC", resultSet.getString(1));
        assertEquals(1, resultSet.getInt(2));
        assertEquals(1.1, resultSet.getFloat(3), 1e-5);
    }

    @Test
    void writeSucceeds(@MessageSpec("src/test/resources/database-passes") TestSpec spec, TestInfo testInfo) throws SQLException {
        database = new DatabaseBehaviourBuilder(List.of(DatabaseBehaviourBuilderTest.class.getResourceAsStream("/database.sql")),
                "jdbc:h2:mem:"+testInfo.getTestMethod().map(Method::toString).orElse("write")+";DB_CLOSE_DELAY=-1;MODE=MySQL");
        database.addBehavior(spec.getSideEffects());

        Connection connection = database.getConnection();
        Statement statement = connection.createStatement();
        statement.execute("insert into scripted1 values ('DEF', 2, 2.2)");
        statement.execute("insert into unscripted1 values ('DEF', 2, 2.2)");
        database.verifyBehaviour(spec.getSideEffects());
    }
}