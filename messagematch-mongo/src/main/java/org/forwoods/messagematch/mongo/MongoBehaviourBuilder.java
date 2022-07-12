package org.forwoods.messagematch.mongo;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.codecs.configuration.CodecRegistry;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.junit.MessageArgumentMatcher;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.TriggeredCall;
import org.mockito.invocation.InvocationOnMock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class MongoBehaviourBuilder extends BehaviourBuilder {

    private final CodecRegistry codecs;

    public MongoBehaviourBuilder() {
        this.codecs = MongoClientSettings.getDefaultCodecRegistry();
    }

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
                        when(mongo.find(argThat(new MessageArgumentMatcherBson(c.getRequestMessage(), codecs)), any(Class.class)))
                                .thenAnswer(new MongoFindAnswerWithRuntimeClass<>(c.getResponseMessage(), c));
                    } else {
                        when(mongo.find(argThat(new MessageArgumentMatcherBson(c.getRequestMessage(), codecs))))
                                .thenAnswer(new MongoFindAnswerWithCompileClass<>(c.getResponseMessage(), channel.getCollectionType(), c));
                    }
                    break;
                case REPLACE:

                    when(mongo.replaceOne(argThat(new MessageArgumentMatcherBson(c.getRequestMessage().get(0), codecs)),
                            argThat(new MessageArgumentMatcher<>(c.getRequestMessage().get(1))),
                            any(ReplaceOptions.class)))
                            .thenAnswer(new MongoReplaceAnswer(c.getResponseMessage(), c, callsMatched));
                    break;
                case UPDATE:
                    when(mongo.updateOne(argThat(new MessageArgumentMatcherBson(c.getRequestMessage().get(0), codecs)),
                            argThat(new MessageArgumentMatcherBson(c.getRequestMessage().get(1), codecs))))
                            .thenAnswer(new MongoUpdateAnser(c.getResponseMessage(), c, callsMatched));
                    when(mongo.updateMany(argThat(new MessageArgumentMatcherBson(c.getRequestMessage().get(0), codecs)),
                            argThat(new MessageArgumentMatcherBson(c.getRequestMessage().get(1), codecs))))
                            .thenAnswer(new MongoUpdateAnser(c.getResponseMessage(), c, callsMatched));
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
    protected Class<MongoChannel> getChannelType() {
        return MongoChannel.class;
    }

    private class MongoFindAnswerWithRuntimeClass<T> extends MongoFindAnswer<T> {

        public MongoFindAnswerWithRuntimeClass(JsonNode responseMessage, CallExample call) {
            super(responseMessage, call);
        }

        @Override
        public FindIterable<T> answer(InvocationOnMock invocation) throws Throwable {
            callsMatched.computeIfAbsent(call, c->new ArrayList<>()).add(new BehaviourBuilder.Invocation(null) );
            Class<T> returnClass = invocation.getArgument(1);
            return getFindIterable(returnClass);
        }

    }

    private class MongoFindAnswerWithCompileClass<T> extends MongoFindAnswer<T> {
        private final Class<T> returnClass;

        public MongoFindAnswerWithCompileClass(JsonNode requestMessage, String collectionType, CallExample call) {
            super(requestMessage, call);
            try {
                //noinspection unchecked
                this.returnClass = (Class<T>)Class.forName(collectionType);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public FindIterable<T> answer(InvocationOnMock invocation) throws Throwable {
            callsMatched.computeIfAbsent(call, c->new ArrayList<>()).add(new BehaviourBuilder.Invocation(null) );
            return getFindIterable(returnClass);
        }
    }
}
