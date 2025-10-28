package org.forwoods.messagematch.match;

import org.forwoods.messagematch.match.fieldmatchers.FieldMatcher;
import org.forwoods.messagematch.spec.URIChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathMatcher
{
    private final String matcherPath;
    private final String concretPath;
    private final Map<String, Object> bindings;

    public PathMatcher(final String matcherPath, final String concretePath, final Map<String, Object> inputBindings)
    {
        this.matcherPath = matcherPath;
        this.bindings = new HashMap<>(inputBindings);
        this.concretPath = concretePath;
    }

    public boolean matches()
    {
        PathMatcherPattern pattern = new PathMatcherPattern(matcherPath);
        final Matcher matcher = pattern.pattern.matcher(concretPath);
        if (matcher.matches()) {
            List<String> bindingExpressions = pattern.bindingExpressions;
            for (int i = 0; i < bindingExpressions.size(); i++)
            {
                final String bindingExpression = bindingExpressions.get(i);
                FieldMatcher<?> fieldMatcher = JsonMatcher.parseMatcher(bindingExpression);
                String concreteValue = matcher.group(i+1);
                final boolean matches = fieldMatcher.matches(concreteValue, bindings);
                if (!matches) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public Map<String, Object> getBindings()
    {
        return bindings;
    }

    private static class PathMatcherPattern {
        private final Pattern pattern;
        private final List<String> bindingExpressions = new ArrayList<>();
        private final static Pattern matcherDeconstruct = Pattern.compile("\\{[^}]+\\}");
        public PathMatcherPattern(String matcherPath) {
            StringBuilder patternBuilder = new StringBuilder();
            Matcher matcher = matcherDeconstruct.matcher(matcherPath);
            int pos = 0;
            while (matcher.find()) {
                patternBuilder.append(matcherPath.substring(pos, matcher.start()));
                patternBuilder.append("([^/]+)");
                final String group = matcher.group();
                bindingExpressions.add(group.substring(1, group.length()-1));
                pos = matcher.end();
            }
            patternBuilder.append(matcherPath.substring(pos));
            pattern = Pattern.compile(patternBuilder.toString());
        }
    }
}
