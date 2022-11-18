package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class CallExample<C extends Channel> {
    //Can be a reference to an example elsewhere
    private URL reference;
    private String relative;

    //or an inline example
    private String name;
    private C channel;
    private JsonNode requestMessage;
    private JsonNode responseMessage;
    @JsonProperty("schema") private URL verifySchema;

    @SuppressWarnings("unchecked")
    public void resolve(URL base) {
        if (channel == null) {
            CallExample<C> remote;
            if (relative != null) {
                try {
                    reference = new URL(base, relative);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                if (reference.toString().endsWith("testSpec")) {
                    TestSpec spec = TestSpec.specParser.readValue(reference, TestSpec.class);
                    remote = (CallExample<C>) spec.callUnderTest;
                }
                else {
                    remote = TestSpec.specParser.readValue(reference, CallExample.class);
                }
                this.channel = remote.channel;
                this.requestMessage = remote.requestMessage;
                this.responseMessage = remote.responseMessage;
                this.verifySchema = remote.verifySchema;
            } catch (IOException e) {
                throw new RuntimeException("Error resolving referred spec " + reference, e);
            }
        }
    }

    public C getChannel() {
        return channel;
    }

    public JsonNode getRequestMessage() {
        return requestMessage;
    }

    public JsonNode getResponseMessage() {
        return responseMessage;
    }

    public URL getVerifySchema() {
        return verifySchema;
    }

    public URL getReference() {
        return reference;
    }

    public String getRelative() {
        return relative;
    }

    public void setReference(URL reference) {
        this.reference = reference;
    }

    public void setRelative(String relative) {
        this.relative = relative;
    }

    public void setChannel(C channel) {
        this.channel = channel;
    }

    public void setRequestMessage(JsonNode requestMessage) {
        this.requestMessage = requestMessage;
    }

    public void setResponseMessage(JsonNode responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSchema(URL verifySchema) {
        this.verifySchema = verifySchema;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CallExample{");
        if (name!=null) {sb.append("name='").append(name).append("',");}
        sb.append("channel=").append(channel);
        sb.append(", requestMessage=").append(requestMessage);
        sb.append('}');
        return sb.toString();
    }
}
