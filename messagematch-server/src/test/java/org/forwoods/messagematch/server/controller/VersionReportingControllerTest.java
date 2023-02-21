package org.forwoods.messagematch.server.controller;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.forwoods.messagematch.junit.MessageSpec;
import org.forwoods.messagematch.junit.MessageSpecExtension;
import org.forwoods.messagematch.junit.MockBehaviourBuilder;
import org.forwoods.messagematch.junit.MockMap;
import org.forwoods.messagematch.spec.TestSpec;
import org.forwoods.messagematch.spec.URIChannel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(MessageSpecExtension.class)
@ExtendWith(SpringExtension.class)
@Import(IntegrationTestConfiguration.class)
@WebMvcTest(VersionReportingController.class)
public class VersionReportingControllerTest {

    @MockMap
    public Map<Class<?>, Object> mocks;
    final MockBehaviourBuilder mocker = new MockBehaviourBuilder();
    @Autowired
    MockMvc mockMvc;

    @BeforeAll
    static void setupStatic(){
        TestSpec.specParser.registerModule(new JavaTimeModule());
    }

    @Test
    void replaceExistingVersionsReport(@MessageSpec("src/test/resources/version-reports/version-report") TestSpec spec) throws Exception {
        mocker.addMocks(mocks);
        mocker.addBehavior(spec.getSideEffects());

        JsonGenerator generator = new JsonGenerator(spec.getCallUnderTest().getRequestMessage());
        String content = generator.generateString();

        //when
        RequestBuilder post = post(((URIChannel) spec.getCallUnderTest().getChannel()).getUri())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .accept(MediaType.APPLICATION_JSON);
        mockMvc.perform(
                post
        ).andReturn().getResponse();

        mocker.verifyBehaviour(spec.getSideEffects());
    }

}