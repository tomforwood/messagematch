package org.forwoods.messagematch.junit;

import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.Channel;
import org.forwoods.messagematch.spec.TriggeredCall;
import org.mockito.verification.VerificationMode;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

public abstract class BehaviourBuilder {
    protected Map<Class<?>, Object> mocks = new HashMap<>();

    protected Map<CallExample, List<Invocation>> callsMatched = new HashMap<>();

    public void addMocks(Map<Class<?>, Object> mocks) {
        this.mocks.putAll(mocks);
    }

    public abstract void addBehavior(Collection<TriggeredCall> calls);

    public void verifyBehaviour(Collection<TriggeredCall> calls) throws BehaviourVerificationException{
        //For all the calls tht have a times specified
        List<String> errors = calls.stream().filter(TriggeredCall::hasTimes).filter(c-> getChannelType().isAssignableFrom(c.getCall().getChannel().getClass())).flatMap(c->{
            TriggeredCall.Times times = c.getTimes();
            int minCalls = times.getMin()==null? 0 : times.getMin();
            int maxCalls = times.getMax()==null? Integer.MAX_VALUE : times.getMax();
            List<Invocation> invocations = callsMatched.getOrDefault(c.getCall(), List.of());
            List<String> callErrors = new ArrayList<>();
            if (invocations.size()<minCalls) {
                callErrors.add(String.format("Expected at least %d calls to %s but received %d", minCalls, c.getCall().toString(), invocations.size()));
            }
            if (invocations.size()>maxCalls) {
                callErrors.add(String.format("Expected at most %d calls to %s but received %d", maxCalls, c.getCall().toString(), invocations.size()));
            }
            return callErrors.stream();
        }).collect(Collectors.toList());
        if (!errors.isEmpty()) {
            throw new BehaviourVerificationException(String.join("\n",errors));
        }
    }

    protected abstract Class<? extends Channel> getChannelType();

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

    public static class Invocation {
        public final Map<String, Object> bindings;

        public Invocation(Map<String, Object> bindings) {
            this.bindings = bindings;
        }
    }
}
