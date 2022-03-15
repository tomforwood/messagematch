package org.forwoods.messagematch.plugin;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.maven.plugin.logging.Log;
import org.forwoods.messagematch.spec.TestSpec;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageMatchSwaggerChecker {


    protected void checkOpenApi(Collection<TestSpec> specPaths, Path openApiPath, Log log) {
        try {
            List<String> specChannels = specPaths.stream()
                    .filter(s->s!=null)
                    .map(s->s.getCallUnderTest().getChannel().toString())
                    .collect(Collectors.toList());

            String openApiString = Files.readString(openApiPath);
            SwaggerParseResult result = new OpenAPIParser().readContents(openApiString, null, null);
            if (result.getMessages()!=null) {
                result.getMessages()
                        .forEach(m->log.warn("Error reading openAPI, "+m));
            }
            OpenAPI openApi = result.getOpenAPI();
            if (openApi!=null) {
                Paths apiPaths = openApi.getPaths();
                for (Map.Entry<String, PathItem> pathEntry:apiPaths.entrySet()) {
                    System.out.println(pathEntry.getKey());
                    UriTemplate template = new UriTemplate(pathEntry.getKey());
                    boolean matched = specChannels.stream().map(c->template.match(c))
                            .anyMatch(m->typeMatch(m, pathEntry.getValue()));
                    if (!matched) {
                        log.warn("API path of "+pathEntry.getKey()+ " does not match any tested channel");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean typeMatch(Map<String, String> m, PathItem value) {

        //TODO check bindings match the schema
        return true;
    }
}
