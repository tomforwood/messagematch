package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.databind.jsontype.NamedType;

public class OtherChannel implements Channel {
    static {
        TestSpec.specParser.registerSubtypes(new NamedType(OtherChannel.class, "other"));
    }
    private final String text;

    public OtherChannel(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
