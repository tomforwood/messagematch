package org.forwoods.messagematch.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoClient;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.forwoods.messagematch.junit.MockBehaviourBuilder;
import org.forwoods.messagematch.match.JsonMatcher;
import org.mockito.ArgumentMatcher;

public class MessageArgumentMatcherBson<T> implements ArgumentMatcher<Bson> {

    private final JsonNode matcher;

    public MessageArgumentMatcherBson(JsonNode matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(Bson argument) {
        try {
            return new JsonMatcher(matcher,
                    MockBehaviourBuilder.objectMapper.readTree(argument.toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry()).toJson())).matches();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
