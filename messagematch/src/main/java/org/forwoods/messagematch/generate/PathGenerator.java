package org.forwoods.messagematch.generate;

import org.forwoods.messagematch.generate.nodegenerators.NodeGenerator;
import org.forwoods.messagematch.generate.nodegenerators.ValueProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.forwoods.messagematch.generate.JsonGenerator.createValueProvider;

public class PathGenerator
{
    private final String matcherPath;
    private final Map<String, ValueProvider> bindings;

    public PathGenerator(String matcherPath, Map<String, Object> bindings)
    {
        this.matcherPath = matcherPath;
        this.bindings = bindings.entrySet().stream()
                .collect(
                        Collectors.toMap(Map.Entry::getKey, e-> createValueProvider(e.getValue().toString())));
    }

    public String generate()
    {
        final PathTemplate pathTemplate = new PathTemplate(matcherPath);
        Object[] generated = new Object[pathTemplate.bindingExpressions.size()];
        for  (int i = 0; i < pathTemplate.bindingExpressions.size(); i++) {
            final NodeGenerator nodeGenerator = JsonGenerator.parseMatcher(pathTemplate.bindingExpressions.get(i), bindings);
            generated[i] = nodeGenerator.generate();
        }
        return String.format(pathTemplate.format, generated);
    }

    private static class PathTemplate
    {
        private final static Pattern matcherDeconstruct = Pattern.compile("\\{[^}]+\\}");
        private final String format;
        private final List<String> bindingExpressions = new ArrayList<>();

        public PathTemplate(String matcherPath)
        {
            StringBuilder formatBuilder = new StringBuilder();
            Matcher matcher = matcherDeconstruct.matcher(matcherPath);
            int pos = 0;
            while (matcher.find()) {
                formatBuilder.append(matcherPath.substring(pos, matcher.start()));
                formatBuilder.append("%s");
                final String group = matcher.group();
                bindingExpressions.add(group.substring(1, group.length()-1));
                pos = matcher.end();
            }
            formatBuilder.append(matcherPath.substring(pos));
            format = formatBuilder.toString();

        }
    }
}
