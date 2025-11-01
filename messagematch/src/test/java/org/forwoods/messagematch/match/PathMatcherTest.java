package org.forwoods.messagematch.match;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathMatcherTest
{
    @Test
    public void shouldMatchSimple()
    {
        String matcherPath = "/storedNumbers";
        String concrete = "/storedNumbers";
        final PathMatcher pathMatcher = new PathMatcher(matcherPath, concrete, Map.of("from", "Bristol"));
        assertTrue(pathMatcher.matches());
    }

    @Test
    public void shouldMatchIntPathParam()
    {
        String matcherPath = "/{$Int=accountId,5}/storedNumbers";
        String concrete = "/7/storedNumbers";
        final PathMatcher pathMatcher = new PathMatcher(matcherPath, concrete, Map.of("from", "Bristol"));
        assertTrue(pathMatcher.matches());
        assertEquals("7", pathMatcher.getBindings().get("accountId"));
    }
    @Test
    public void shouldMatchComplexMatcher()
    {
        String matcherPath = "/travel/{$String=from}-{$String=to}";
        String concrete = "/travel/London-Edinburgh";
        final PathMatcher pathMatcher = new PathMatcher(matcherPath, concrete, Map.of());
        assertTrue(pathMatcher.matches());
        assertEquals("London", pathMatcher.getBindings().get("from"));
        assertEquals("Edinburgh", pathMatcher.getBindings().get("to"));
    }

    @Test
    public void shouldNotMatch()
    {
        String matcherPath = "/travel/{$String=from}-{$String=to}";
        String concrete = "/traveling/London-Edinburgh";
        final PathMatcher pathMatcher = new PathMatcher(matcherPath, concrete, Map.of());
        assertFalse(pathMatcher.matches());
    }

    @Test
    public void shouldRespectExistingBindings()
    {
        String matcherPath = "/travel/{$String=from}-{$String=to}";
        String concrete = "/travel/London-Edinburgh";
        final PathMatcher pathMatcher = new PathMatcher(matcherPath, concrete, Map.of("from", "Bristol"));
        assertFalse(pathMatcher.matches());
    }

    @Test
    public void shouldMatchWholePath()
    {
        String matcherPath = "/storedNumbers";
        String concrete = "/storedNumbersBroken";
        final PathMatcher pathMatcher = new PathMatcher(matcherPath, concrete, Map.of());
        assertFalse(pathMatcher.matches());
    }
}
