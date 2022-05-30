package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URL;

public class TriggeredCall {

    public static class Times {
        private final Integer min;
        private final Integer max;

        public Times(@JsonProperty(value = "min")Integer min, @JsonProperty(value = "max")Integer max) {
            if (min!=null && max!=null && !min.equals(max)) throw new IllegalArgumentException("min and max must be the same if both are specified");
            this.min = min;
            this.max = max;
        }

        public Integer getMin() {
            return min;
        }

        public Integer getMax() {
            return max;
        }
    }

    private final Times times;
    private final CallExample call;

    @JsonCreator
    public TriggeredCall(@JsonProperty(value = "times")Times times, @JsonProperty(value = "call")CallExample call,
                         @JsonProperty("reference")URL reference,
                         @JsonProperty("relative")String relative,
                         @JsonProperty("channel") Channel channel,
                         @JsonProperty("requestMessage") JsonNode requestMessage,
                         @JsonProperty("responseMessage") JsonNode responseMessage,
                         @JsonProperty("schema") URL verifySchema) {
        this.times = times;
        if (call!=null)
            this.call = call;
        else {
            this.call = new CallExample();
            this.call.setReference(reference);
            this.call.setRelative(relative);
            this.call.setChannel(channel);
            this.call.setRequestMessage(requestMessage);
            this.call.setResponseMessage(responseMessage);
            this.call.setSchema(verifySchema);
        }
    }

    public Times getTimes() {
        return times;
    }

    public CallExample getCall() {
        return call;
    }

    public boolean hasTimes() {
        return times!=null && times.min!=null && times.min > 0;
    }
}


