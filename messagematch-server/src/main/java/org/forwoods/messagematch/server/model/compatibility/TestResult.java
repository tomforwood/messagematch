package org.forwoods.messagematch.server.model.compatibility;

import com.google.common.collect.Comparators;

public enum TestResult {
    SUCCEEDED,
    UNTESTED,
    FAILED;

    public TestResult merge(TestResult other) {
        return Comparators.max(this,other);
    }
}
