package org.forwoods.messagematch.apiscenario.spec;

import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.URIChannel;

import java.util.List;

public class State
{
    CallExample<URIChannel> stateTrigger;
    List<CallExample<URIChannel>> expectedCalls;

    public CallExample<URIChannel> getStateTrigger()
    {
        return stateTrigger;
    }

    public void setStateTrigger(final CallExample<URIChannel> stateTrigger)
    {
        this.stateTrigger = stateTrigger;
    }

    public List<CallExample<URIChannel>> getExpectedCalls()
    {
        return expectedCalls;
    }

    public void setExpectedCalls(final List<CallExample<URIChannel>> expectedCalls)
    {
        this.expectedCalls = expectedCalls;
    }
}
