package org.forwoods.messagematch.server.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.forwoods.messagematch.junit.MessageSpec;
import org.forwoods.messagematch.junit.MessageSpecExtension;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.server.dao.VersionedArtifactDao;
import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.forwoods.messagematch.server.model.compatibility.TestRecord;
import org.forwoods.messagematch.server.persist.TestRecordDaoWrapper;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.TestSpec;
import org.forwoods.messagematch.spec.URIChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(IntegrationTestConfiguration.class)
@SpringBootTest
@AutoConfigureDataJpa
@AutoConfigureMockMvc
@ExtendWith(MessageSpecExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class IntegrationTest {


    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestRecordDaoWrapper testRecordDao;

    @Autowired
    VersionedArtifactDao underlyingVerDao;


    @Test
    public void testDeployReport() throws Exception {
        URL versionReportURL = IntegrationTest.class.getResource("/version-reports/versionReport.CallExample");
        CallExample<URIChannel> versionReportSubmit =TestSpec.specParser.readValue(versionReportURL, new TypeReference<>() {
        });

        RequestBuilder requestBuilder = buildRequest(versionReportSubmit);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult).extracting(r->r.getResponse().getStatus()).isEqualTo(200);

        //this saving should result in two artifacts
        var artifacts = new ArrayList<>((Collection<VersionedArtifact>) underlyingVerDao.findAll());
        assertThat(artifacts).size().isEqualTo(2);

        //There should also be a test record as we assume that the build passed
        List<TestRecord> tests = testRecordDao.findByArtifactUnderTest(artifacts.get(0));
        assertThat(tests).size().isGreaterThanOrEqualTo(1);

        //Call it again with te same args shouldn't create any more artifacts
        requestBuilder = buildRequest(versionReportSubmit);
        mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult).extracting(r->r.getResponse().getStatus()).isEqualTo(200);
        artifacts = new ArrayList<>((Collection<VersionedArtifact>) underlyingVerDao.findAll());
        assertThat(artifacts).size().isEqualTo(2);

    }

    /**
     * After the depended upon artifact has been deployed we can show the depending-artifacts compatibility report
     */
    @Test
    public void testCompatibilityReport(@MessageSpec("src/test/resources/version-reports/CompatibleSucess")TestSpec spec) throws Exception {
        setupUntested();
        RequestBuilder requestBuilder;

        //no get the compatibility report

        CallExample<URIChannel> callUnderTest = spec.getCallUnderTest();
        URI compatibilityRequest = URI.create(callUnderTest.getChannel().getUri());
        requestBuilder = MockMvcRequestBuilders.get(compatibilityRequest).accept(MediaType.APPLICATION_JSON);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult).extracting(r->r.getResponse().getStatus()).isEqualTo(200);

        JsonMatcher matcher = new JsonMatcher(callUnderTest.getResponseMessage(), mvcResult.getResponse().getContentAsString());
        matcher.matches();
        assertThat(matcher.getErrors()).isEmpty();
    }

    private void setupUntested() throws Exception {
        testDeployReport();//deploy the first artifact

        //Deploy the depended-on artifact
        URL versionReportURL = IntegrationTest.class.getResource("/version-reports/versionReportArtifact2.CallExample");
        CallExample<URIChannel> versionReportSubmit =TestSpec.specParser.readValue(versionReportURL, new TypeReference<>() {
        });
        RequestBuilder requestBuilder = buildRequest(versionReportSubmit);

        mockMvc.perform(requestBuilder).andReturn();
    }

    /**
     * This test changes the deployed version of the dependencies - the resulting compatibility report should be untested
     * The version is in fact downgraded - to a version that definitely exists and when we do a test build will pass
     */
    @Test
    public void testDeployedVersionChanged(@MessageSpec("src/test/resources/version-reports/CompatibleUntested")TestSpec spec) throws Exception {
        testDeployReport();//deploy the first artifact

        URL versionReportURL = IntegrationTest.class.getResource("/version-reports/versionReportArtifact2-2.CallExample");
        CallExample<URIChannel> versionReportSubmit = TestSpec.specParser.readValue(versionReportURL, new TypeReference<>() {
        });
        RequestBuilder requestBuilder = buildRequest(versionReportSubmit);
        mockMvc.perform(requestBuilder).andReturn();

        CallExample<URIChannel> callUnderTest = spec.getCallUnderTest();
        URI compatibilityRequest = URI.create(callUnderTest.getChannel().getUri());
        requestBuilder = MockMvcRequestBuilders.get(compatibilityRequest).accept(MediaType.APPLICATION_JSON);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult).extracting(r->r.getResponse().getStatus()).isEqualTo(200);

        JsonMatcher matcher = new JsonMatcher(callUnderTest.getResponseMessage(), mvcResult.getResponse().getContentAsString());
        matcher.matches();
        assertThat(matcher.getErrors()).isEmpty();
    }

    @Test
    public void testBuildFixes() throws Exception {
        setupUntested();


    }


    private RequestBuilder buildRequest(CallExample<URIChannel> call) {
        String uri= call.getChannel().getUri();
        String content = new JsonGenerator(call.getRequestMessage()).generateString();
        return switch(call.getChannel().getMethod()) {
            case "GET", "get" -> MockMvcRequestBuilders.get(uri).contentType(MediaType.APPLICATION_JSON)
                    .content(content)
                    .accept(MediaType.APPLICATION_JSON);
            case "POST", "post" -> MockMvcRequestBuilders.post(uri).contentType(MediaType.APPLICATION_JSON)
                    .content(content)
                    .accept(MediaType.APPLICATION_JSON);
            default -> throw new RuntimeException("Unknown http verb");
        };
    }

}
