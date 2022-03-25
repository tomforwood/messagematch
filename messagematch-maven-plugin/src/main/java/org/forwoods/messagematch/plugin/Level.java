package org.forwoods.messagematch.plugin;

import org.apache.maven.plugin.logging.Log;

import java.util.function.BiConsumer;

public enum Level {
    WARN(Log::warn),
    ERROR(Log::error),
    FAIL(Log::error);

    Level(BiConsumer<Log, String> logger) {
        this.logger = logger;
    }

    private final BiConsumer<Log, String> logger;

    public void log(Log l, String s) {
        logger.accept(l,s);
    }

    public static Level parse(String l) {
        return valueOf(l.toUpperCase());
    }
}
