package org.forwoods.messagematch.mongo;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonString;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.spec.CallExample;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MongoUpdateAnser implements Answer<UpdateResult> {
    private final long selectedCount;
    private final long updatedCount;
    private BsonString updatedId;
    private final CallExample<MongoChannel> call;
    private final Map<CallExample<MongoChannel>, List<BehaviourBuilder.Invocation>> invocations;

    public MongoUpdateAnser(JsonNode responseMessage, CallExample<MongoChannel> call, Map<CallExample<MongoChannel>, List<BehaviourBuilder.Invocation>> invocations) {
        selectedCount = Long.parseLong(responseMessage.get(0).toString());
        updatedCount  = Long.parseLong(responseMessage.get(1).toString());
        this.call = call;
        this.invocations = invocations;
        try {
            if (responseMessage.has(2)) {
                String id = new JsonGenerator(responseMessage.get(2).toString()).generate().toString();

                updatedId = new BsonString(id);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UpdateResult answer(InvocationOnMock invocation) {
        invocations.computeIfAbsent(call, c->new ArrayList<>()).add(new BehaviourBuilder.Invocation(null) );
        return UpdateResult.acknowledged(selectedCount, updatedCount, updatedId);
    }
}
