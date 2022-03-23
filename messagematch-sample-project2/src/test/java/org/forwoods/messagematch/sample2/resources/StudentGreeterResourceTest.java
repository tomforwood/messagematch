package org.forwoods.messagematch.sample2.resources;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCollection;
import org.apache.http.client.HttpClient;
import org.forwoods.messagematch.apachehttp.HttpBehaviourBuilder;
import org.forwoods.messagematch.junit.MessageSpec;
import org.forwoods.messagematch.junit.MessageSpecExtension;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.mongo.MongoBehaviourBuilder;
import org.forwoods.messagematch.mongo.MongoChannel;
import org.forwoods.messagematch.sample2.api.StudentDetails;
import org.forwoods.messagematch.sample2.db.StudentDao;
import org.forwoods.messagematch.sample2.service.StudentService;
import org.forwoods.messagematch.spec.TestSpec;
import org.forwoods.messagematch.spec.URIChannel;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.constraints.Digits;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MessageSpecExtension.class)
@ExtendWith(MockitoExtension.class)
class StudentGreeterResourceTest extends JerseyTest {


    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig();
        StudentDao studentDao  = new StudentDao(()->collection);
        resourceConfig.register(new StudentGreeterResource(new StudentService(studentDao, ()->httpClient)));
        return resourceConfig;
    }
    @BeforeAll
    public static void initialiseMyTest() {
        new MongoChannel();
    }

    @Mock(lenient = true)
    MongoCollection<StudentDetails> collection;

    @Mock
    HttpClient httpClient;

    @BeforeEach
    public void setupMyTest()  {
        mongo = new MongoBehaviourBuilder();
    }

    private MongoBehaviourBuilder mongo;

    @Test
    void sayHelloTest(@MessageSpec("src/test/resources/org/forwoods/messagematch/sample2/resources/sayHello") TestSpec event) throws IOException {
        mongo.addMocks(Map.of(MongoCollection.class, collection));
        mongo.addBehavior(event.getSideEffects());
        HttpBehaviourBuilder httpBehaviour = new HttpBehaviourBuilder();
        httpBehaviour.addMocks(Map.of(HttpClient.class, httpClient));
        httpBehaviour.addBehavior(event.getSideEffects());

        URIChannel channel = (URIChannel) event.getCallUnderTest().getChannel();
        WebTarget target = target(channel.getUri());
        target = addParams(target, event.getCallUnderTest().getRequestMessage());

        Response response = target.request()
                .get();


        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(),"Http Response should be 200: ");

        String greeting = response.readEntity(String.class);
        JsonMatcher jsonMatcher = new JsonMatcher(event.getCallUnderTest().getResponseMessage(), JsonNodeFactory.instance.textNode(greeting));
        assertTrue(jsonMatcher.matches(), jsonMatcher.getErrors().toString());

        httpBehaviour.verifyBehaviour(event.getSideEffects());
    }

    //TODO add a test of the save method but Oh-no! our greetingTemplate object doesn't match!
    //This test will pass - we are internally consistant
    //Only the messagematch maven plugin can check against the swagger and save us now

    @Test
    public void saveAGreetingTest(@MessageSpec("src/test/resources/org/forwoods/messagematch/sample2/resources/saveGreeting") TestSpec event) throws IOException {
        HttpBehaviourBuilder httpBehaviour = new HttpBehaviourBuilder();
        httpBehaviour.addMocks(Map.of(HttpClient.class, httpClient));
        httpBehaviour.addBehavior(event.getSideEffects());

        URIChannel channel = (URIChannel) event.getCallUnderTest().getChannel();
        WebTarget target = target(channel.getUri());

        Response response = target.request()
                .post(Entity.json(event.getCallUnderTest().getResponseMessage().toString()));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(),"Http Response should be 200: ");

        String greetingTemplate = response.readEntity(String.class);
        JsonMatcher jsonMatcher = new JsonMatcher(event.getCallUnderTest().getResponseMessage(), greetingTemplate);
        assertTrue(jsonMatcher.matches(), jsonMatcher.getErrors().toString());

        httpBehaviour.verifyBehaviour(event.getSideEffects());
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