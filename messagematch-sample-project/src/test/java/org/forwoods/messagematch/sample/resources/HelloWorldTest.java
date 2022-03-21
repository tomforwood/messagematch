package org.forwoods.messagematch.sample.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCollection;
import org.forwoods.messagematch.junit.MessageSpec;
import org.forwoods.messagematch.junit.MessageSpecExtension;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.mongo.MongoBehaviourBuilder;
import org.forwoods.messagematch.mongo.MongoChannel;
import org.forwoods.messagematch.sample.api.GreetingTemplate;
import org.forwoods.messagematch.sample.db.GreetingDAO;
import org.forwoods.messagematch.sample.service.GreetingService;
import org.forwoods.messagematch.spec.TestSpec;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MessageSpecExtension.class)
@ExtendWith(MockitoExtension.class)
public class HelloWorldTest extends JerseyTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(new HelloWorldResource(new GreetingService(new GreetingDAO(()->collection))));
        return resourceConfig;
    }

    @Mock(lenient = true)
    MongoCollection<GreetingTemplate> collection;

    private MongoBehaviourBuilder mongo;

    @BeforeAll
    public static void initialiseMyTest() {
        new MongoChannel();
    }

    @BeforeEach
    public void setupMyTest()  {
        mongo = new MongoBehaviourBuilder();
    }

    @Test
    public void testHello(@MessageSpec("src/test/resources/org/forwoods/messagematch/sample/resources/sayHello") TestSpec event) throws IOException {
        mongo.addMocks(Map.of(MongoCollection.class, collection));
        mongo.addBehavior(event.getSideEffects());
        WebTarget target = target("/hello-world");
        target = addParams(target, event.getCallUnderTest().getRequestMessage());
        Response response = target.request()
                .get();


        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(),"Http Response should be 200: ");
        JsonMatcher jsonMatcher = new JsonMatcher(event.getCallUnderTest().getResponseMessage(), response.readEntity(String.class));
        assertTrue(jsonMatcher.matches(), jsonMatcher.getErrors().toString());
    }

    private WebTarget addParams(WebTarget target, JsonNode requestMessage) {
        ObjectNode node = (ObjectNode) requestMessage;
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while(it.hasNext()) {
            Map.Entry<String, JsonNode> param = it.next();
            target = target.queryParam(param.getKey(), param.getValue().textValue());
        }
        return target;
    }
}
