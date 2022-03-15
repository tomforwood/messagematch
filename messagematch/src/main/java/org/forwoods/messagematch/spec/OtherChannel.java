package org.forwoods.messagematch.spec;

public class OtherChannel implements Channel {
    private final String text;

    public OtherChannel(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
