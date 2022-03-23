package org.forwoods.messagematch.sample2.resources;

import org.forwoods.messagematch.sample2.api.GreetingTemplate;
import org.forwoods.messagematch.sample2.service.StudentService;

import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

@Path("/student")
public class StudentGreeterResource {

    private final StudentService studentService;

    public StudentGreeterResource(StudentService studentService) {
        this.studentService = studentService;
    }

    @GET
    @Path("/getGreeting")
    public String sayHello(@QueryParam("userId") int userId) {
        return studentService.getGreeting(userId);
    }

    @POST
    @Path("/saveTranslation")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GreetingTemplate saveTranslation(GreetingTemplate template) {
        return studentService.persistGreeting(template);
    }
}
