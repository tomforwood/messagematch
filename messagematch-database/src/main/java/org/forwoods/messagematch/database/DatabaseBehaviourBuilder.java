package org.forwoods.messagematch.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.junit.BehaviourVerificationException;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.spec.GenericChannel;
import org.forwoods.messagematch.spec.TriggeredCall;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DatabaseBehaviourBuilder extends BehaviourBuilder<GenericChannel> {

    final JdbcDataSource source;

    List<String> tables = new ArrayList<>();

    public DatabaseBehaviourBuilder(List<InputStream> databaseScript) {
        this(databaseScript, "jdbc:h2:mem:myDb;DB_CLOSE_DELAY=-1");
    }

    public DatabaseBehaviourBuilder(List<InputStream> databaseScript, String h2URL) {
        source = new JdbcDataSource();
        source.setURL(h2URL);
        databaseScript.forEach(s->{
            try {
                Connection connection = getConnection();
                RunScript.execute(connection, new InputStreamReader(s));

                Statement statement = connection.createStatement();
                ResultSet tablesResults = statement.executeQuery("show tables");
                while (tablesResults.next()) {
                    tables.add(tablesResults.getString(1).toUpperCase());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public DataSource getDataSource(){
        return source;
    }


    public Connection getConnection() throws SQLException {
        return source.getConnection();
    }

    //behaviour can be read or write
    //read sets up data before execution
    //write asserts that a row matching exists

    //request message is "table" object - name of table and optional column names
    //if the table has not already been created by the database script it is created with
    //columns all of type varchar (will this break?)

    //the response message is an array of table rows - a table row is an array of values
    //For read these get inserted
    //for write these get validated against


    @Override
    protected void addFilteredBehavior(Stream<TriggeredCall<GenericChannel>> calls) {
        calls.forEach(this::addBehavior);
    }

    private void addBehavior(TriggeredCall<GenericChannel> call) {
        Action action = Action.valueOf(call.getCall().getChannel().getProperties().get("action"));
        String table = call.getCall().getChannel().getProperties().get("table");
        if (!tables.contains(table.toUpperCase())) {
            createTable(table, call.getCall().getRequestMessage());
        }
        switch (action) {
            case READ:

                insertData(table, (ArrayNode) call.getCall().getResponseMessage());
                break;
            case WRITE:
                break;
        }
    }

    @Override
    public void verifyBehaviour(Collection<TriggeredCall<?>> calls) throws BehaviourVerificationException {
        //noinspection unchecked
        List<String> errors = calls.stream().filter(this::matchesChannel)
                .map(c->(TriggeredCall<GenericChannel>)c)
                .filter(TriggeredCall::hasTimes)
                .map(this::verifyBehaviour)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!errors.isEmpty()) {
            throw new BehaviourVerificationException(String.join("\n",errors));
        }
    }

    private String verifyBehaviour(TriggeredCall<GenericChannel> call) {
        Action action = Action.valueOf(call.getCall().getChannel().getProperties().get("action"));
        String table = call.getCall().getChannel().getProperties().get("table");
        switch (action) {
            case WRITE:
                ResultSet r = buildSelect(table,call.getCall().getRequestMessage());
                return findMatches(r, (ArrayNode)call.getCall().getResponseMessage(), table);
            case READ:
                return null;
        }
        return null;
    }

    private void createTable(String table, JsonNode requestMessage) {
        if (requestMessage==null) {
            throw new RuntimeException("table "+table+ " has not been scripted so columns must be specified");
        }
        StringBuilder createBuilder = new StringBuilder();
        createBuilder.append("Create table ").append(table);
        createBuilder.append("(\n");
        String cols = StreamSupport.stream(requestMessage.spliterator(), false).map(JsonNode::asText)
                .map(n->'`'+n+"` varchar").collect(Collectors.joining(",\n"));
        createBuilder.append(cols).append(");");
        String create = createBuilder.toString();
        try (Statement s = getConnection().createStatement()){
            s.execute(create);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        tables.add(table.toUpperCase());
    }

    private void insertData(String table, ArrayNode rowNodes) {
        try (Statement s = getConnection().createStatement()){
            List<String> colNames = readCols(table);
            for (JsonNode rowNode:rowNodes) {
                StringBuilder insertBuilder = new StringBuilder();
                insertBuilder.append("insert into ").append(table);
                rowNode = new JsonGenerator(rowNode).generate();
                if (rowNode instanceof ArrayNode) {
                    if (rowNode.size()!=colNames.size()) {
                        throw new RuntimeException("Error inserting values "+rowNode + " into "+table + " expected columns are "+colNames);
                    }
                    insertBuilder.append(" values (");
                    String cols = StreamSupport.stream(rowNode.spliterator(), false).map(JsonNode::asText)
                            .map(this::enquote)
                            .collect(Collectors.joining(",\n"));
                    insertBuilder.append(cols).append(");");
                }
                else if (rowNode instanceof ObjectNode) {
                    ObjectNode rowObjectNode = (ObjectNode) rowNode;
                    List<String> cols = new ArrayList<>();
                    List<String> vals = new ArrayList<>();
                    insertBuilder.append("(");
                    rowObjectNode.fields().forEachRemaining(f->{
                        if (!colNames.contains(f.getKey())) {
                            throw new RuntimeException("Error inserting column "+f.getKey()+" into "+table + " expected columns are "+colNames);
                        }
                        cols.add(f.getKey());
                        vals.add(f.getValue().textValue());
                    });
                    insertBuilder.append(String.join(",", cols)).append(")");
                    insertBuilder.append(vals.stream().map(this::enquote).collect(Collectors.joining(",", " values (", ");")));

                }
                s.execute(insertBuilder.toString());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> readCols(String table) throws SQLException {
        try (Statement statement = getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery("show columns from " + table)) {
            List<String> res = new ArrayList<>();
            while (resultSet.next()) {
                String s = resultSet.getString(1);
                res.add(s);
            }
            return res;
        }
    }

    private ResultSet buildSelect(String table, JsonNode requestMessage) {
        try (Statement s= getConnection().createStatement()){
            StringBuilder selectBuilder = new StringBuilder();
            selectBuilder.append("select ");
            if (requestMessage!=null && !requestMessage.isNull()) {
                String cols = StreamSupport.stream(requestMessage.spliterator(), false).map(JsonNode::asText)
                        .collect(Collectors.joining(",\n"));
                selectBuilder.append(cols);
            }
            else {
                selectBuilder.append("*");
            }
            selectBuilder.append(" from ").append(table);
            String select = selectBuilder.toString();
            return s.executeQuery(select);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String enquote(String n) {
        if (n.equals("null")) return "null";
        else return "'" + (n.replace("'", "''")) + "'";
    }

    private String findMatches(ResultSet r, ArrayNode node, String table){
        List<String> results = new ArrayList<>();
        try {
            List<String[]> data = readData(r);
            for (JsonNode rowNode:node) {
                if (rowNode instanceof ArrayNode) {
                    boolean matches = matches(rowNode, data);
                    if (!matches) results.add("expected a value matching "+node + " to be written to "+ table);
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return results.isEmpty()?null: String.join("\n", results);
    }

    private boolean matches(JsonNode rowNode, List<String[]> data) {
        for(String[] dataRow :data) {
            for (int i=0;i<rowNode.size();i++) {
                JsonMatcher matcher = new JsonMatcher(rowNode.get(i),TextNode.valueOf(dataRow[i]));
                if (matcher.matches()) return true;
            }
        }
        return false;
    }

    private List<String[]> readData(ResultSet r) throws SQLException {
        int count = r.getMetaData().getColumnCount();
       List<String[]> result = new ArrayList<>();
       while (r.next()) {
           String[] a = new String[count];
           for (int i = 0; i < count; i++) {
               a[i]=r.getString(i+1);
           }
           result.add(a);
       }
       return result;
    }

    @Override
    protected Stream<TriggeredCall<GenericChannel>> filteredCalls(Collection<TriggeredCall<?>> calls) {
        Stream<TriggeredCall<GenericChannel>> rightChannel =  super.filteredCalls(calls);
        return rightChannel.filter(c->c.getCall().getChannel().getTypeName().equals("database"));
    }

    @Override
    protected Class<GenericChannel> getChannelType() {
        return GenericChannel.class;
    }

    enum Action {
        READ, WRITE
    }
}
