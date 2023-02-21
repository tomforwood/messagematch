package org.forwoods.messagematch.sample.resources;

import io.swagger.v3.oas.annotations.Parameter;
import org.forwoods.messagematch.sample.api.Greeting;
import org.forwoods.messagematch.sample.api.GreetingTemplate;
import org.forwoods.messagematch.sample.service.GreetingService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Optional;

@Path("/hello-world")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {

    private final GreetingService greetService;

    public HelloWorldResource(GreetingService greetService) {
        this.greetService = greetService;
    }

    @GET
    public Greeting sayHello(
            @Parameter(required = true)
            @QueryParam("name") String name,
            @Parameter()
            @QueryParam("language") String language) {
        return greetService.getGreeting(name, Optional.ofNullable(language));
    }

    @POST
    @Path("/saveTranslation")
    public GreetingTemplate saveTranslation(GreetingTemplate template) {
        return greetService.persistGreeting(template);
    }
}
