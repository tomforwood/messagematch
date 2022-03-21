package org.forwoods.messagematch.plugin;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.maven.plugin.logging.Log;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.TestSpec;
import org.forwoods.messagematch.spec.URIChannel;
import org.springframework.web.util.UriTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MessageMatchSwaggerChecker {


    protected void checkOpenApi(Collection<TestSpec> specPaths, Path openApiPath, Log log) {
        try {
            List<URIChannel> specChannels = specPaths.stream()
                    .filter(Objects::nonNull)
                    .map(s->s.getCallUnderTest().getChannel())
                    .filter(s->s instanceof URIChannel)
                    .map(URIChannel.class::cast)
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
                    UriTemplate template = new UriTemplate(pathEntry.getKey());
                    for(Map.Entry<PathItem.HttpMethod, Operation> operation: pathEntry.getValue().readOperationsMap().entrySet()) {
                        boolean matched;
                        List<URIChannel> uriMatches = specChannels.stream()
                                .filter(c->template.matches(c.getUri()))
                                .filter(c->c.getMethod().toUpperCase().equals(operation.getKey().toString()))
                                .collect(Collectors.toList());

                        matched = uriMatches.stream().map(c -> template.match(c.getUri()))
                                .anyMatch(m -> typeMatch(m, operation.getValue().getParameters()));
                        if (!matched) {
                            log.warn("API path of "+operation.getKey()+":"+pathEntry.getKey()+ " does not match any tested channel");
                        }

                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void checkOpenpi(CallExample call, Log log) {
        URL u = call.getVerifySchema();
        String s ;
        try {
            s = inputStreamToString(u.openStream());
            SwaggerParseResult result = new OpenAPIParser().readContents(s, null, null);
            if (result.getMessages()!=null) {
                result.getMessages()
                        .forEach(m->log.warn("Error reading openAPI, "+m));
            }
            //TODO
        } catch (IOException e) {
            log.error("Cannot access "+u);
        }
    }

    public static String inputStreamToString(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private boolean typeMatch(Map<String, String> m, List<Parameter> value) {

        //TODO check bindings match the schema
        return true;
    }

}
