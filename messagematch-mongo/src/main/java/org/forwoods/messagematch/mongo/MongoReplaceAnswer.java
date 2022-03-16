package org.forwoods.messagematch.mongo;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonString;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;

public class MongoReplaceAnswer implements Answer<UpdateResult> {
    private final long selectedCount;
    private final long updatedCount;
    private final String id;

    public MongoReplaceAnswer(JsonNode responseMessage) {
        selectedCount = Long.parseLong(responseMessage.get(0).toString());
        updatedCount  = Long.parseLong(responseMessage.get(1).toString());
        try {
            id = new JsonGenerator(responseMessage.get(2).toString()).generate().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UpdateResult answer(InvocationOnMock invocation) {
        return UpdateResult.acknowledged(selectedCount, updatedCount, new BsonString(id));
    }
}
