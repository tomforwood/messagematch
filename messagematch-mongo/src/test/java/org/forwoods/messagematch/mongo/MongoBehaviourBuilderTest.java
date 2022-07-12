package org.forwoods.messagematch.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import org.forwoods.messagematch.junit.BehaviourVerificationException;
import org.forwoods.messagematch.junit.MessageSpec;
import org.forwoods.messagematch.junit.MessageSpecExtension;
import org.forwoods.messagematch.spec.TestSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MessageSpecExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MongoBehaviourBuilderTest {


    @Mock
    MongoCollection<?> collection;

    MongoBehaviourBuilder behaviourBuilder;


    @BeforeAll
    public static void before() {
        new MongoChannel();
    }

    @BeforeEach
    public void init(){
        behaviourBuilder = new MongoBehaviourBuilder();
        behaviourBuilder.addMocks(Map.of(MongoCollection.class, collection));
    }

    @Test
    void verifyMissedBehaviourCausesException(@MessageSpec("src/test/resources/mongoTest")TestSpec spec) {
        behaviourBuilder.addBehavior(spec.getSideEffects());

        Exception e = assertThrows(BehaviourVerificationException.class, ()->behaviourBuilder.verifyBehaviour(spec.getSideEffects()));
        assertEquals("Expected at least 1 calls to CallExample{channel=MongoChannel{mongoMethod=FIND, collectionType='org.bson.Document'}, requestMessage={\"id\":\"123\"}} but received 0\n" +
                "Expected at least 1 calls to CallExample{channel=MongoChannel{mongoMethod=UPDATE, collectionType='org.bson.Document'}, requestMessage=[{\"id\":\"123\"},{}]} but received 0", e.getMessage());

    }

    @Test
    void verifyTriggeredBehaviourVerifies(@MessageSpec("src/test/resources/mongoTest")TestSpec spec) {
        behaviourBuilder.addBehavior(spec.getSideEffects());

        collection.find(eq("id", "123"));
        collection.updateOne(eq("id", "123"), Updates.inc("count", 1));

        behaviourBuilder.verifyBehaviour(spec.getSideEffects());

    }
}