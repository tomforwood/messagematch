package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = URIChannel.class, name = "uri"),
        @JsonSubTypes.Type(value = MethodCallChannel.class, name = "method"),
        @JsonSubTypes.Type(value = GenericChannel.class, name = "generic")
})
public interface Channel {
}
