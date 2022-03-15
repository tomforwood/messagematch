package org.forwoods.messagematch.mongo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.forwoods.messagematch.spec.Channel;
import org.forwoods.messagematch.spec.TestSpec;

public class MongoChannel implements Channel {

    static {
        TestSpec.specParser.registerSubtypes(new NamedType(MongoChannel.class, "mongo"));
    }

    @JsonProperty("mongoMethod")
    private MongoMethod mongoMethod;

    public MongoMethod getMethod() {
        return mongoMethod;
    }
}
