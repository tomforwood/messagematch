package org.forwoods.messagematch.apachehttp;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.TestSpec;
import org.forwoods.messagematch.spec.TriggeredCall;
import org.forwoods.messagematch.spec.URIChannel;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpBehaviourBuilder extends BehaviourBuilder {
    @Override
    public void addBehavior(Collection<TriggeredCall> calls) {
        HttpClient httpClient = getHttpClient();
        calls.stream().map(TriggeredCall::getCall).filter(c->c.getChannel() instanceof URIChannel).forEach(c-> {

            try {
                when(httpClient.execute(argThat(new RequestMatcher(c)))).thenAnswer(new HttpAnswer(c.getResponseMessage()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void verifyBehaviour(Collection<TriggeredCall> calls) {
        HttpClient httpClient = getHttpClient();
        calls.stream().filter(TriggeredCall::hasTimes).filter(c->c.getCall().getChannel() instanceof URIChannel).forEach(c-> {
            VerificationMode mockitoTimes = toMockitoTimes(c.getTimes());
            try {
                verify(httpClient).execute(argThat(new RequestMatcher(c.getCall())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private HttpClient getHttpClient() {
        return (HttpClient) this.mocks.entrySet().stream()
                .filter(e->HttpClient.class.isAssignableFrom(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst().orElseThrow(()->new RuntimeException("Mock implementing apache HttpClient not found"));
    }

    private static class HttpAnswer implements Answer<HttpResponse> {

        private final JsonNode responseMessage;

        public HttpAnswer(JsonNode responseMessage) {
            this.responseMessage = responseMessage;
        }

        @Override
        public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
            StringEntity entity = new StringEntity(responseMessage.toString());
            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, ""));
            basicHttpResponse.setEntity(entity);
            return basicHttpResponse;
        }
    }

    private static class RequestMatcher implements ArgumentMatcher<HttpUriRequest> {
        private final CallExample call;
        private final URIChannel channel;
        public RequestMatcher(CallExample c) {
            this.call=c;
            this.channel=(URIChannel)c.getChannel();
        }

        @Override
        public boolean matches(HttpUriRequest argument) {
            switch (channel.getMethod()) {
                case "get":
                    HttpGet get = (HttpGet) argument;
                    URI u = get.getURI();
                    if (!u.getPath().equals(channel.getUri())) return false;
                    URIBuilder builder = new URIBuilder(u);
                    List<NameValuePair> queryParams = builder.getQueryParams();
                    Map<String, Object> map = queryParams.stream()
                            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
                    JsonNode nodeParams = TestSpec.specParser.valueToTree(map);
                    JsonMatcher matcher = new JsonMatcher(call.getRequestMessage(), nodeParams);
                    return matcher.matches();

                case "post" :
                    HttpPost post = (HttpPost) argument;
                    u = post.getURI();
                    if (!u.getPath().equals(channel.getUri())) return false;
                    try {
                        matcher = new JsonMatcher(call.getRequestMessage(),TestSpec.specParser.readTree(post.getEntity().getContent()));
                        return matcher.matches();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                default:{
                    throw new UnsupportedOperationException("method "+channel.getMethod() + " not currently supported");
                }
            }

        }
    }
}
