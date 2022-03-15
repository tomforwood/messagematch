package org.forwoods.messagematch.junit;

import org.forwoods.messagematch.spec.CallExample;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class BehaviourBuilder {
    protected Map<Class, Object> mocks = new HashMap<>();

    public void addMocks(Map<Class, Object> mocks) {
        this.mocks.putAll(mocks);
    }

    public abstract void addBehavior(Collection<CallExample> calls);
}
