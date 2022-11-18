package org.forwoods.messagematch.springjdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.GenericChannel;
import org.forwoods.messagematch.spec.TestSpec;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JDBCAnswer<T> implements Answer<T> {
    private final Map<CallExample<GenericChannel>, JavaType> calls;
    private final Map<CallExample<GenericChannel>, List<BehaviourBuilder.Invocation>> callsMatched;

    private final QueryExtractor queryExtractor;
    private final ResultBuilder<T> resultBuilder;
    private final ParamExtractor paramExtractor;

    public JDBCAnswer(Map<CallExample<GenericChannel>, JavaType> calls, Map<CallExample<GenericChannel>,
            List<BehaviourBuilder.Invocation>> callsMatched, QueryExtractor queryExtractor,
                      ParamExtractor paramExtractor,
                      ResultBuilder<T> resultBuilder) {
        this.calls = calls;
        this.callsMatched = callsMatched;
        this.queryExtractor = queryExtractor;
        this.resultBuilder = resultBuilder;
        this.paramExtractor = paramExtractor;
    }

    @Override
    public T answer(InvocationOnMock invocation) throws Throwable {

        String query = queryExtractor.extractQuery(invocation);
        for (Map.Entry< CallExample<GenericChannel>, JavaType > entry : calls.entrySet()) {
            ObjectNode requestNode = (ObjectNode) entry.getKey().getRequestMessage();
            String queryToMatch = requestNode.get("query").asText();
            ArrayNode paramsToMatch = (ArrayNode) requestNode.get("params");
            boolean matches;
            if (queryToMatch.startsWith("^")) {
                //treat this as a regex - sadly you can't get the names of named groups out of the pattern without reflection
                Pattern p = Pattern.compile(queryToMatch);
                Matcher matcher = p.matcher(query);
                matches = matcher.matches();
            } else {
                matches = queryToMatch.equals(query);
            }
            if (matches) {
                Object[] objects = paramExtractor.extractParameters(invocation);
                JsonNode paramsNode = TestSpec.specParser.valueToTree(objects);
                JsonMatcher paraMatcher = new JsonMatcher(paramsToMatch, paramsNode);
                if (paraMatcher.matches()) {
                    Map<String, Object> bindings = new HashMap<>(paraMatcher.getBindings());
                    callsMatched.computeIfAbsent(entry.getKey(), k->new ArrayList<>()).add(new BehaviourBuilder.Invocation(bindings));
                    return resultBuilder.buildResult(entry.getKey().getResponseMessage(), bindings, entry.getValue(), invocation);
                }
            }
        }
        //noinspection unchecked
        return (T)Answers.RETURNS_SMART_NULLS.answer(invocation);
    }


    protected interface QueryExtractor{
        String extractQuery(InvocationOnMock invocation);
    }

    protected interface ResultBuilder<T>{
        T buildResult(JsonNode resultNode, Map<String, Object> bindings, JavaType resultType, InvocationOnMock invocation);
    }

    protected interface ParamExtractor {
        Object[] extractParameters(InvocationOnMock invocation);
    }

    protected final static QueryExtractor STRING_QUERY_EXTRACTOR = invocation -> invocation.getArgument(0).toString();

    protected final static ResultBuilder<Boolean> BOOLEAN_RESULT = (resultNode, bindings, resultType, invocation) -> {
        JsonGenerator generator = new JsonGenerator(resultNode, bindings);
        JsonNode resNode = generator.generate();
        return Boolean.parseBoolean(resNode.textValue());
    };

    protected static ParamExtractor prepStatParamExtractor(int index) {
        return invocation -> {
            PreparedStatementCallback<?> callback = invocation.getArgument(index);
            SpringJdbcTemplateBehaviourBuilder.BindingAnswer bindingAnswer = new SpringJdbcTemplateBehaviourBuilder.BindingAnswer();
            PreparedStatement statement = mock(PreparedStatement.class, bindingAnswer);
            try {
                callback.doInPreparedStatement(statement);
                return SpringJdbcTemplateBehaviourBuilder.BindingAnswer.toObjArray(bindingAnswer.values);
            } catch (SQLException ignored) {
            }
            return null;
        };
    }

    protected final static QueryExtractor PREP_STAT_CREATOR_QUERY_EXTRACTOR = invocation -> {
        try {
            PreparedStatementCreator psc = invocation.getArgument(0);
            Connection con = mock(Connection.class);
            SpringJdbcTemplateBehaviourBuilder.BindingAnswer bindingAnswer = new SpringJdbcTemplateBehaviourBuilder.BindingAnswer();
            PreparedStatement statement = mock(PreparedStatement.class, bindingAnswer);
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            when(con.prepareStatement(queryCaptor.capture(), any(String[].class))).thenReturn(statement);
            when(con.prepareStatement(queryCaptor.capture(), any(int[].class))).thenReturn(statement);
            psc.createPreparedStatement(con);
            return queryCaptor.getValue();
        } catch (SQLException ignored) {
        }
        return null;
    };

    protected final static ParamExtractor PREP_STAT_CREATOR_PARAM_EXTRACTOR = invocation -> {
        try {
            PreparedStatementCreator psc = invocation.getArgument(0);
            Connection con = mock(Connection.class);
            SpringJdbcTemplateBehaviourBuilder.BindingAnswer bindingAnswer = new SpringJdbcTemplateBehaviourBuilder.BindingAnswer();
            PreparedStatement statement = mock(PreparedStatement.class, bindingAnswer);
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            when(con.prepareStatement(queryCaptor.capture(), any(String[].class))).thenReturn(statement);
            when(con.prepareStatement(queryCaptor.capture(), any(int[].class))).thenReturn(statement);
            psc.createPreparedStatement(con);
            return SpringJdbcTemplateBehaviourBuilder.BindingAnswer.toObjArray(bindingAnswer.values);

        } catch (SQLException ignored) {
        }
        return null;
    };

    protected final static ResultBuilder<Integer> KEY_HOLDER_RESULT = (resultNode, bindings, resultType, invocation) -> {
        try {
            JsonGenerator generator = new JsonGenerator(resultNode, bindings);
            JsonNode resNode = generator.generate();
            TypeReference<List<Map<String, Object>>> tr = new TypeReference<>() {};
            JavaType type = TestSpec.specParser.getTypeFactory().constructType(tr);
            List<Map<String, Object>> keys = TestSpec.specParser.treeToValue(resNode, type);
            ((KeyHolder) invocation.getArgument(1)).getKeyList().addAll(keys);
            return keys.size();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    };

    public static ParamExtractor varArgParamExtractor(int skip){
        return invocation -> {
            Object[] args = invocation.getArguments();
            //This has split varargs into separate arguments - I need to put them back together
            return Arrays.copyOfRange(args, skip, args.length);
        };
    }

    public static final ResultBuilder<?> LIST_RESULT = listResultBuilder();

    public static ResultBuilder<Object> listResultBuilder() {
        return (resultNode, bindings, resultType, invocation) -> {
            JsonGenerator generator = new JsonGenerator(resultNode, bindings);
            JsonNode resNode = generator.generate();
            try {
                return TestSpec.specParser.treeToValue(resNode, resultType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
