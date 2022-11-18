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
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class MongoBehaviourBuilder extends BehaviourBuilder<MongoChannel> {

    private final CodecRegistry codecs;

    public MongoBehaviourBuilder() {
        this.codecs = MongoClientSettings.getDefaultCodecRegistry();
    }

    @Override
    public void addFilteredBehavior(Stream<TriggeredCall<MongoChannel>> calls) {

        MongoCollection<?> mongo = getMongoCollection();
        calls.map(TriggeredCall::getCall).forEach(c->{
            MongoChannel channel = c.getChannel();
            MongoMethod method = channel.getMethod();
            switch(method) {
                case FIND:
                    if (channel.getCollectionType()==null) {
                        //noinspection unchecked
                        doAnswer(new MongoFindAnswerWithRuntimeClass<>(c.getResponseMessage(), c)).when(mongo).find(argThat(new MessageArgumentMatcherBson(c.getRequestMessage(), codecs)), any(Class.class));
                    } else {
                        doAnswer(new MongoFindAnswerWithCompileClass<>(c.getResponseMessage(), channel.getCollectionType(), c)).when(mongo).find(argThat(new MessageArgumentMatcherBson(c.getRequestMessage(), codecs)));
                    }
                    break;
                case REPLACE:
                    doAnswer(new MongoReplaceAnswer(c.getResponseMessage(), c, callsMatched)).when(mongo)
                            .replaceOne(argThat(new MessageArgumentMatcherBson(c.getRequestMessage().get(0), codecs)),
                                argThat(new MessageArgumentMatcher<>(c.getRequestMessage().get(1))),
                                any(ReplaceOptions.class));
                    break;
                case UPDATE:
                    doAnswer(new MongoUpdateAnser(c.getResponseMessage(), c, callsMatched)).when(mongo)
                            .updateOne(argThat(new MessageArgumentMatcherBson(c.getRequestMessage().get(0), codecs)),
                            argThat(new MessageArgumentMatcherBson(c.getRequestMessage().get(1), codecs)));
                    doAnswer(new MongoUpdateAnser(c.getResponseMessage(), c, callsMatched)).
                    when(mongo).updateMany(argThat(new MessageArgumentMatcherBson(c.getRequestMessage().get(0), codecs)),
                            argThat(new MessageArgumentMatcherBson(c.getRequestMessage().get(1), codecs)));
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

        public MongoFindAnswerWithRuntimeClass(JsonNode responseMessage, CallExample<MongoChannel> call) {
            super(responseMessage, call);
        }

        @Override
        public FindIterable<T> answer(InvocationOnMock invocation) throws Throwable {
            callsMatched.computeIfAbsent(call, c -> new ArrayList<>()).add(new BehaviourBuilder.Invocation(null) );
            Class<T> returnClass = invocation.getArgument(1);
            return getFindIterable(returnClass);
        }

    }

    private class MongoFindAnswerWithCompileClass<T> extends MongoFindAnswer<T> {
        private final Class<T> returnClass;

        public MongoFindAnswerWithCompileClass(JsonNode requestMessage, String collectionType, CallExample<MongoChannel> call) {
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
