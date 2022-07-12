package org.forwoods.messagematch.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.bson.BsonDocument;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.forwoods.messagematch.junit.MockBehaviourBuilder;
import org.forwoods.messagematch.match.JsonMatcher;
import org.mockito.ArgumentMatcher;

public class MessageArgumentMatcherBson implements ArgumentMatcher<Bson> {

    private final JsonNode matcher;
    private final CodecRegistry codecs;

    public MessageArgumentMatcherBson(JsonNode matcher, CodecRegistry codecs) {
        this.matcher = matcher;
        this.codecs = codecs;
    }

    @Override
    public boolean matches(Bson argument) {
        try {
            return new JsonMatcher(matcher,
                    MockBehaviourBuilder.objectMapper.readTree(argument.toBsonDocument(BsonDocument.class, codecs).toJson())).matches();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
