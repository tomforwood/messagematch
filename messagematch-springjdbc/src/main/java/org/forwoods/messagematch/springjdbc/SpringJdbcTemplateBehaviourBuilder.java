package org.forwoods.messagematch.springjdbc;

import com.fasterxml.jackson.databind.JavaType;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.spec.*;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import java.util.*;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class SpringJdbcTemplateBehaviourBuilder extends BehaviourBuilder<GenericChannel> {


    private JdbcTemplate template;

    final Map<CallExample<GenericChannel>, JavaType> calls = new HashMap<>();

    //This behaviour insists on being allowed to construct its own mock so it can set the default behaviour for any method
    @SuppressWarnings({"deprecation", "unchecked"})
    public JdbcTemplate getTemplate() {
        if (template == null) {
            template= mock(JdbcTemplate.class, RETURNS_SMART_NULLS);

        }
        return template;
    }

    @Override
    protected Stream<TriggeredCall<GenericChannel>> filteredCalls(Collection<TriggeredCall<?>> calls) {
        Stream<TriggeredCall<GenericChannel>> rightChannel =  super.filteredCalls(calls);
        return rightChannel.filter(this::isTemplateCall);
    }

    @SuppressWarnings("unchecked")
    private void addMocking() {
        when(template.execute(anyString(), any(PreparedStatementCallback.class)))
                .thenAnswer(new JDBCAnswer<>(calls,
                        callsMatched,JDBCAnswer.STRING_QUERY_EXTRACTOR, JDBCAnswer.prepStatParamExtractor(1),
                        JDBCAnswer.BOOLEAN_RESULT));
        when(template.update(any(PreparedStatementCreator.class), any(KeyHolder.class)))
                .thenAnswer(new JDBCAnswer<>(calls, callsMatched,
                        JDBCAnswer.PREP_STAT_CREATOR_QUERY_EXTRACTOR,
                        JDBCAnswer.PREP_STAT_CREATOR_PARAM_EXTRACTOR,
                        JDBCAnswer.KEY_HOLDER_RESULT));
        when(template.query(anyString(), nullable(RowMapper.class),any())).thenAnswer(
                new JDBCAnswer<>(calls, callsMatched, JDBCAnswer.STRING_QUERY_EXTRACTOR,
                        JDBCAnswer.varArgParamExtractor(2), JDBCAnswer.LIST_RESULT)
        );
        when(template.query(anyString(), any(Object[].class),nullable(RowMapper.class))).thenAnswer(
                new JDBCAnswer<>(calls, callsMatched, JDBCAnswer.STRING_QUERY_EXTRACTOR,
                        i->i.getArgument(1), JDBCAnswer.LIST_RESULT)
        );
        when(template.queryForObject(anyString(), nullable(RowMapper.class),any())).thenAnswer(
                new JDBCAnswer<>(calls, callsMatched, JDBCAnswer.STRING_QUERY_EXTRACTOR,
                        JDBCAnswer.varArgParamExtractor(2), JDBCAnswer.LIST_RESULT)
        );
        when(template.queryForObject(anyString(), any(Object[].class),nullable(RowMapper.class))).thenAnswer(
                new JDBCAnswer<>(calls, callsMatched, JDBCAnswer.STRING_QUERY_EXTRACTOR,
                        i->i.getArgument(1), JDBCAnswer.LIST_RESULT)
        );
        when(template.queryForObject(anyString(), any(Object[].class),any(Class.class))).thenAnswer(
                new JDBCAnswer<>(calls, callsMatched, JDBCAnswer.STRING_QUERY_EXTRACTOR,
                        i->i.getArgument(1), JDBCAnswer.LIST_RESULT)
        );
    }

    @Override
    public void addFilteredBehavior(Stream<TriggeredCall<GenericChannel>> calls) {
        Mockito.reset(template);
        addMocking();
        calls.forEach(this::addBehavior);
    }

    private void addBehavior(TriggeredCall<GenericChannel> call) {
        GenericChannel channel = call.getCall().getChannel();
        String method = channel.getProperties().get("returns");
        switch (method) {
            case "object" :
                try {
                    String objectType = channel.getProperties().get("objectType");
                    Class<?> clazz = Class.forName(objectType);
                    JavaType type = TestSpec.specParser.getTypeFactory().constructType(clazz);
                    calls.put(call.getCall(), type);
                }
                catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "list" :
                try {
                    String objectType = channel.getProperties().get("objectType");
                    Class<?> clazz = Class.forName(objectType);
                    JavaType type = TestSpec.specParser.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
                    calls.put(call.getCall(), type);
                }
                catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "int" :
                JavaType type = TestSpec.specParser.getTypeFactory().constructType(Integer.TYPE);
                calls.put(call.getCall(), type);
                break;

        }
    }

    private boolean isTemplateCall(TriggeredCall<GenericChannel> triggeredCall) {
        return triggeredCall.getCall().getChannel().getTypeName().equals("jdbcTemplate");
    }

    @Override
    protected Class<GenericChannel> getChannelType() {
        return GenericChannel.class;
    }


    protected static class BindingAnswer implements Answer<Object> {
        final Map<Integer, String> values = new HashMap<>();
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            if (invocation.getMethod().getName().startsWith("set")) {
                values.put(invocation.getArgument(0), Objects.toString(invocation.getArgument(1), null));
            }
            return RETURNS_SMART_NULLS.answer(invocation);
        }

        protected static Object[] toObjArray(Map<Integer, String> setParams) {
            int size = setParams.keySet().stream().mapToInt(i->i).max().orElse(0);
            Object[] result = new Object[size];
            setParams.forEach((key, value) -> result[key - 1] = value);
            return result;
        }
    }
}
