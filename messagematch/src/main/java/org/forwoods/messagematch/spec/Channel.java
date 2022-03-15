package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = URIChannel.class, name = "uri"),
        @JsonSubTypes.Type(value = MethodCallChannel.class, name = "method")
})
public interface Channel {
}
