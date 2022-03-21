package org.forwoods.messagematch.junit;

import org.forwoods.messagematch.spec.TriggeredCall;
import org.mockito.verification.VerificationMode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public abstract class BehaviourBuilder {
    protected Map<Class<?>, Object> mocks = new HashMap<>();

    public void addMocks(Map<Class<?>, Object> mocks) {
        this.mocks.putAll(mocks);
    }

    public abstract void addBehavior(Collection<TriggeredCall> calls);

    public abstract void verifyBehaviour(Collection<TriggeredCall> calls);

    protected VerificationMode toMockitoTimes(TriggeredCall.Times times) {
        if (times.getMin()!=null && times.getMax()!=null) {//contructor ensures they are equal
            return times(times.getMin());
        }
        if (times.getMin()!=null) {
            return atLeast(times.getMin());
        }
        if (times.getMax()!=null) {
            return atMost(times.getMax());
        }
        throw new IllegalArgumentException("Min or max must be specified");
    }
}
