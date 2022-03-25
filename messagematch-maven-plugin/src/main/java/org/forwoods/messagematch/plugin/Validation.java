package org.forwoods.messagematch.plugin;

public enum Validation {
    UNUSED_SPEC(Level.ERROR),
    UNTESTED_ENDPOINT(Level.WARN),
    MISSMATCHED_SPEC(Level.FAIL);

    private final Level defaultLevel;

    Validation(Level defaultLevel) {
        this.defaultLevel = defaultLevel;
    }

    public static Validation parse(String v) {
        return valueOf(v.toUpperCase());
    }

    public Level getDefault() {
        return defaultLevel;
    }
}
