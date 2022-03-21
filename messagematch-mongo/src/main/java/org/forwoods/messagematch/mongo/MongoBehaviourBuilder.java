package org.forwoods.messagematch.mongo;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.junit.MessageArgumentMatcher;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.TriggeredCall;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.verification.VerificationMode;

import java.util.Collection;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class MongoBehaviourBuilder extends BehaviourBuilder {


    @Override
    @SuppressWarnings("unchecked")
    public void addBehavior(Collection<TriggeredCall> calls) {

        MongoCollection<?> mongo = getMongoCollection();
        calls.stream().map(TriggeredCall::getCall).filter(c->c.getChannel() instanceof MongoChannel).forEach(c->{
            MongoChannel channel = (MongoChannel) c.getChannel();
            MongoMethod method = channel.getMethod();
            switch(method) {
                case FIND:
                    if (channel.getCollectionType()==null) {
                        when(mongo.find(argThat(new MessageArgumentMatcher<Document>(c.getRequestMessage())), any(Class.class)))
                                .thenAnswer(new MongoFindAnswerWithRuntimeClass<>(c.getResponseMessage()));
                    } else {
                        when(mongo.find(argThat(new MessageArgumentMatcher<Document>(c.getRequestMessage()))))
                                .thenAnswer(new MongoFindAnswerWithCompileClass<>(c.getResponseMessage(), channel.getCollectionType()));
                    }
                    break;
                case REPLACE:

                    when(mongo.replaceOne(argThat(new MessageArgumentMatcherBson<Bson>(c.getRequestMessage().get(0))),
                            argThat(new MessageArgumentMatcher<>(c.getRequestMessage().get(1))),
                            any(ReplaceOptions.class)))
                            .thenAnswer(new MongoReplaceAnswer(c.getResponseMessage()));
                    break;
                default: throw new UnsupportedOperationException("Mongo operation not supported");
            }
        });
    }

    private MongoCollection<?> getMongoCollection() {
        return (MongoCollection<?>) this.mocks.entrySet().stream()
                .filter(e->MongoCollection.class.isAssignableFrom(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst().orElseThrow(()->new RuntimeException("Mock implementing MongoCollection not found"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void verifyBehaviour(Collection<TriggeredCall> calls) {
        MongoCollection<?> mongo = getMongoCollection();
        calls.stream().filter(TriggeredCall::hasTimes).filter(c->c.getCall().getChannel() instanceof MongoChannel).forEach(c->{
            CallExample call = c.getCall();
            MongoChannel channel = (MongoChannel) call.getChannel();
            MongoMethod method = channel.getMethod();
            VerificationMode mockitoTimes = toMockitoTimes(c.getTimes());
            switch(method) {
                case FIND:
                    if (channel.getCollectionType()==null) {
                        verify(mongo, mockitoTimes).find(argThat(new MessageArgumentMatcher<Document>(call.getRequestMessage())), any(Class.class));
                    } else {
                        verify(mongo, mockitoTimes).find(argThat(new MessageArgumentMatcher<Document>(call.getRequestMessage())));
                    }
                    break;
                case REPLACE:
                    verify(mongo, mockitoTimes).replaceOne(argThat(new MessageArgumentMatcherBson<Bson>(call.getRequestMessage().get(0))),
                            argThat(new MessageArgumentMatcher<>(call.getRequestMessage().get(1))),
                            any(ReplaceOptions.class));
                    break;
                default: throw new UnsupportedOperationException("Mongo operation not supported");
            }
        });
    }

    private static class MongoFindAnswerWithRuntimeClass<T> extends MongoFindAnswer<T> {

        public MongoFindAnswerWithRuntimeClass(JsonNode responseMessage) {
            super(responseMessage);
        }

        @Override
        public FindIterable<T> answer(InvocationOnMock invocation) throws Throwable {
            Class<T> returnClass = invocation.getArgument(1);
            return getFindIterable(returnClass);
        }

    }

    private static class MongoFindAnswerWithCompileClass<T> extends MongoFindAnswer<T> {
        private final Class<T> returnClass;

        public MongoFindAnswerWithCompileClass(JsonNode requestMessage, String collectionType) {
            super(requestMessage);
            try {
                //noinspection unchecked
                this.returnClass = (Class<T>)Class.forName(collectionType);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public FindIterable<T> answer(InvocationOnMock invocation) throws Throwable {
            return getFindIterable(returnClass);
        }
    }
}
