package org.forwoods.messagematch.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.parser.OpenAPIParser;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.util.ResolverFully;
import org.apache.maven.plugin.logging.Log;
import org.forwoods.messagematch.match.fieldmatchers.IntTypeMatcher;
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
import java.util.*;
import java.util.stream.Collectors;

public class MessageMatchSwaggerChecker {

    private final Log log;

    public MessageMatchSwaggerChecker(Log log) {
        this.log = log;
    }

    protected void checkOpenApi(Collection<TestSpec> specPaths, Path openApiPath) {
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
                                .anyMatch(m -> matchPathParams(m, operation.getValue().getParameters(), new HashSet<>()));
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
        if (u==null) return;
        if (!(call.getChannel() instanceof URIChannel)) return;
        URIChannel channel = (URIChannel) call.getChannel();
        String s ;
        try {
            s = inputStreamToString(u.openStream());
            SwaggerParseResult result = new OpenAPIParser().readContents(s, null, null);
            if (result.getMessages()!=null) {
                result.getMessages()
                        .forEach(m->log.warn("Error reading openAPI, "+m));
            }

            boolean matched = false;
            boolean matchedURI = false;
            OpenAPI openAPI = result.getOpenAPI();
            new ResolverFully().resolveFully(openAPI);
            for (Map.Entry<String, PathItem> paths: openAPI.getPaths().entrySet()) {
                UriTemplate template = new UriTemplate(paths.getKey());
                if (template.matches(channel.getUri())) {
                    matchedURI = true;

                    Set<Parameter> paramsMatched = new HashSet<>();
                    PathItem.HttpMethod httpMethod = PathItem.HttpMethod.valueOf(channel.getMethod().toUpperCase());
                    Operation op = paths.getValue().readOperationsMap().get(httpMethod);
                    if (op==null) continue;//openapi doesn't have the right verb
                    Map<String, String> pathMatch = template.match(channel.getUri());
                    List<Parameter> parameters = new ArrayList<>();
                    if (op.getParameters()!=null) parameters.addAll(op.getParameters());
                    if (paths.getValue().getParameters()!=null) parameters.addAll(paths.getValue().getParameters());
                    boolean pathMatches = matchPathParams(pathMatch, parameters, paramsMatched);
                    if (!pathMatches)  {
                        log.debug(channel +" failed to match the path params of " + Json.pretty(parameters));
                        continue;
                    }

                    //Now check the query params
                    List<Parameter> params = op.getParameters();
                    if (params!=null && params.size()>0) {
                        if (!(call.getRequestMessage() instanceof ArrayNode)) {
                            log.debug(channel.getUri() + " query params weren't supplied as an array of values");
                            continue;//if call asks for params they must be in the form of an array
                        }
                        boolean allMatch = true;
                        ArrayNode arrNode = (ArrayNode) call.getRequestMessage();
                        for (int i=0;i<params.size();i++) {
                            allMatch &= new SchemaValidator(params.get(i).getSchema()).validate(arrNode.get(i));
                        }
                        if (!allMatch) {
                            log.debug(channel.getUri() + "query params of "+ call.getRequestMessage().toString() + " did not match schema params of "+ Json.pretty(params));
                            continue;
                        }
                    }
                    else { //or request body
                        Optional<Schema<?>> schema = Optional.of(op)
                                .map(Operation::getRequestBody)
                                .map(RequestBody::getContent)
                                .map(o -> o.get("application.json"))
                                .map(o->(Schema<?>)o.getSchema());
                        boolean requestBodyMatch = schema
                                .map(SchemaValidator::new)
                                .map(v->v.validate(call.getRequestMessage()))//if there eis a request body schema validate it
                                .orElse(true);//If there is no request body schema then retuen true
                        if (!requestBodyMatch) {
                            log.debug(channel.getUri() + "request body of "+ call.getRequestMessage().toString() +  " did not match schema body of "+Json.pretty(schema.get()));
                            continue;
                        }
                    }

                    //Now check the response object
                    Content content = op.getResponses().get("default").getContent();
                    Schema<?> schema = content.get("application/json").getSchema();
                    SchemaValidator schemaValidator = new SchemaValidator(schema);
                    boolean responseMatch = schemaValidator.validate(call.getResponseMessage());
                    if (!responseMatch) {
                        log.debug(channel.getUri() + "response body of "+ call.getRequestMessage() +  " did not match schema body of "+Json.pretty(schema));
                        continue;
                    }

                    matched=true;
                }
            }
            if (matchedURI && !matched) {
                log.error("call with channel "+channel + " and request body "+ requestRoString(call) + " did not match anything in the specified schema "+u
                + " see debug for things it nearly matched with");
            }
            if (!matchedURI) {
                log.error("call with channel "+channel + " did not match a uri in the specified schema "+u
                        + " see debug for things it didn't match with");
            }

        } catch (IOException e) {
            log.error("Cannot access "+u);
        }
    }

    private String requestRoString(CallExample call) {
        JsonNode requestMessage = call.getRequestMessage();
        if (requestMessage==null) return null;
        return requestMessage.toString();
    }

    private boolean matchPathParams(Map<String, String> pathMatch, List<Parameter> parameters, Set<Parameter> matched) {
        for (Map.Entry<String, String> param:pathMatch.entrySet()) {
            Optional<Parameter> schemaParam = parameters.stream().filter(p -> p.getIn().equals("path")).filter(p -> p.getName().equals(param.getKey())).findFirst();
            if (schemaParam.isEmpty()) return false;
            if (!matchedType(schemaParam.get().getSchema(), param.getValue())) return false;
        }
        return true;
    }

    private boolean matchedType(Schema<?> schema, String value) {
        switch(schema.getType()) {
            case "integer" : return IntTypeMatcher.isInt(value);
            default :
                log.error("Unsupported openapi schema tpye of "+schema.getType() + " this feature is probably unfinished");
        }
        return false;
    }

    public static String inputStreamToString(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

}
