package org.forwoods.messagematch.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.forwoods.messagematch.generate.JsonGenerator;
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
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MessageMatchSwaggerChecker {

    private final Log log;
    private final Map<Validation, Level> levels;

    public MessageMatchSwaggerChecker(Log log, Map<Validation, Level> levels) {
        this.log = log;
        this.levels = levels;
    }

    protected boolean checkOpenApi(Collection<TestSpec> specPaths, Path openApiPath, List<String> excluded) {
        boolean passBuild = true;

        Predicate<String> exclusionMatcher = Optional.ofNullable(excluded)
                .stream().flatMap(Collection::stream)
                .map(s->"glob:"+s)
                .map(e1 -> FileSystems.getDefault().getPathMatcher(e1))
                .<Predicate<String>>map(p->(s1 ->p.matches(Path.of(s1))))
                .reduce(x->false, Predicate::or);
        try {
            List<URIChannel> specChannels = specPaths.stream()
                    .filter(Objects::nonNull)
                    .map(s->s.getCallUnderTest().getChannel())
                    .filter(s->s instanceof URIChannel)
                    .map(URIChannel.class::cast)
                    .collect(Collectors.toList());

            String openApiString = Files.readString(openApiPath);
            SwaggerParseResult result = new OpenAPIParser().readContents(openApiString, null, null);
            if (result.getMessages()!=null && result.getMessages().size()>0) {
                result.getMessages()
                        .forEach(m->log.error("Error reading openAPI, "+m));
                passBuild = false;
            }
            OpenAPI openApi = result.getOpenAPI();
            if (openApi!=null) {
                Paths apiPaths = openApi.getPaths();
                for (Map.Entry<String, PathItem> pathEntry:apiPaths.entrySet()) {
                    String path = pathEntry.getKey();
                    if(exclusionMatcher.test(path)) continue;//this path is ignored
                    UriTemplate template = new UriTemplate(path);
                    for(Map.Entry<PathItem.HttpMethod, Operation> operation: pathEntry.getValue().readOperationsMap().entrySet()) {
                        boolean matched;
                        List<URIChannel> uriMatches = specChannels.stream()
                                .filter(c->template.matches(c.getUri()))
                                .filter(c->c.getMethod().toUpperCase().equals(operation.getKey().toString()))
                                .collect(Collectors.toList());

                        matched = uriMatches.stream().map(c -> template.match(c.getUri()))
                                .anyMatch(m -> matchPathParams(m, operation.getValue().getParameters(), new HashSet<>()));
                        if (!matched) {
                            Level l = levels.get(Validation.UNTESTED_ENDPOINT);
                            l.log(log,"API path of "+operation.getKey()+":"+ path + " does not match any tested channel");
                            passBuild &= l!=Level.FAIL;
                        }

                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return passBuild;
    }

    public boolean checkOpenpi(CallExample call) {
        boolean passBuild=true;

        URL u = call.getVerifySchema();
        if (u==null) return false;
        if (!(call.getChannel() instanceof URIChannel)) return false;
        URIChannel channel = (URIChannel) call.getChannel();
        String s ;
        try {
            s = inputStreamToString(u.openStream());
            SwaggerParseResult parseResult = new OpenAPIParser().readContents(s, null, null);
            if (parseResult.getMessages()!=null && !parseResult.getMessages().isEmpty()) {
                parseResult.getMessages()
                        .forEach(m->log.error("Error reading openAPI, "+m));
                return false;
            }

            boolean matched = false;
            boolean matchedURI = false;
            OpenAPI openAPI = parseResult.getOpenAPI();
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
                    JsonNode requestMessageRaw = call.getRequestMessage();
                    JsonGenerator generator = new JsonGenerator(requestMessageRaw);
                    JsonNode requestMessage = requestMessageRaw==null?null:generator.generate();
                    if (params!=null && params.size()>0) {
                        if (!(requestMessage instanceof ObjectNode)) {
                            log.debug(channel.getUri() + " query params weren't supplied as a map of values");
                            continue;//if call asks for params they must be in the form of an array
                        }
                        boolean allMatch = true;
                        ObjectNode arrNode = (ObjectNode) requestMessage;
                        for (Parameter parameter : params) {
                            JsonNode paramVal = arrNode.get(parameter.getName());
                            if (paramVal == null && parameter.getRequired()) {
                                allMatch = false;
                                log.debug(channel.getUri() + " did not supply a value for the required parameter " + Json.pretty(parameter));
                            }
                            else if (paramVal!=null) {
                                allMatch &= new SchemaValidator(parameter.getSchema()).validate(paramVal);
                            }
                        }
                        if (!allMatch) {
                            log.debug(channel.getUri() + "query params of "+ requestMessage + " did not match schema params of "+ Json.pretty(params));
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
                                .map(v->v.validate(requestMessage))//if there eis a request body schema validate it
                                .orElse(true);//If there is no request body schema then retuen true
                        if (!requestBodyMatch) {
                            log.debug(channel.getUri() + "request body of "+ requestMessage +  " did not match schema body of "+Json.pretty(schema.get()));
                            continue;
                        }
                    }

                    //Now check the response object
                    Content content = op.getResponses().get("default").getContent();
                    Schema<?> schema = content.get("application/json").getSchema();
                    SchemaValidator schemaValidator = new SchemaValidator(schema);
                    boolean responseMatch = schemaValidator.validate(call.getResponseMessage());
                    if (!responseMatch) {
                        log.debug(channel.getUri() + "response body of "+ requestMessage +  " did not match schema body of "+Json.pretty(schema));
                        continue;
                    }

                    matched=true;
                }
            }
            if (matchedURI && !matched) {
                Level l  =levels.get(Validation.MISSMATCHED_SPEC);
                passBuild &= l != Level.FAIL;
                l.log(log,"call with channel "+channel + " and request body "+ requestRoString(call) + " did not match anything in the specified schema "+u
                + " see debug for things it nearly matched with");
            }
            if (!matchedURI) {
                Level l  =levels.get(Validation.MISSMATCHED_SPEC);
                passBuild &= l!=Level.FAIL;
                l.log(log,"call with channel "+channel + " did not match a uri in the specified schema "+u
                        + " see debug for things it didn't match with");
            }

        } catch (IOException e) {
            log.error("Cannot access "+u);
            passBuild = false;
        }
        return passBuild;
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
            SchemaValidator validator = new SchemaValidator(schemaParam.get().getSchema());
            if (!validator.validate(JsonNodeFactory.instance.textNode(param.getValue()))) return false;
        }
        return true;
    }

    public static String inputStreamToString(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

}
