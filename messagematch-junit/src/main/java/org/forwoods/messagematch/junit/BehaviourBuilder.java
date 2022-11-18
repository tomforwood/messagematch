package org.forwoods.messagematch.junit;

import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.Channel;
import org.forwoods.messagematch.spec.TriggeredCall;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BehaviourBuilder<C extends Channel> {
    protected final Map<Class<?>, Object> mocks = new HashMap<>();

    protected final Map<CallExample<C>, List<Invocation>> callsMatched = new HashMap<>();

    public void addMocks(Map<Class<?>, Object> mocks) {
        this.mocks.putAll(mocks);
    }

    public void addBehavior(Collection<TriggeredCall<?>> calls) {
        addFilteredBehavior(filteredCalls(calls));
    }

    protected Stream<TriggeredCall<C>> filteredCalls(Collection<TriggeredCall<?>> calls) {
        //noinspection unchecked
        return calls.stream().filter(this::matchesChannel)
                .map(c->(TriggeredCall<C>)c);
    }

    protected abstract void addFilteredBehavior(Stream<TriggeredCall<C>> calls);

    public void verifyBehaviour(Collection<TriggeredCall<?>> calls) throws BehaviourVerificationException{
        //For all the calls tht have a times specified
        List<String> errors = filteredCalls(calls).filter(TriggeredCall::hasTimes).flatMap(c->{
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

    protected boolean matchesChannel(TriggeredCall<?> c) {
        return getChannelType().isAssignableFrom(c.getCall().getChannel().getClass());
    }

    protected abstract Class<C> getChannelType();

    public static class Invocation {
        public final Map<String, Object> bindings;

        public Invocation(Map<String, Object> bindings) {
            this.bindings = bindings;
        }
    }
}
