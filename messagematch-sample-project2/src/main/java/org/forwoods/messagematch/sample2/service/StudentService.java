package org.forwoods.messagematch.sample2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.forwoods.messagematch.sample2.api.Greeting;
import org.forwoods.messagematch.sample2.api.GreetingTemplate;
import org.forwoods.messagematch.sample2.api.StudentDetails;
import org.forwoods.messagematch.sample2.db.StudentDao;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Supplier;

public class StudentService {

    private final StudentDao studentDao;
    private final Supplier<HttpClient> httpClient;
    ObjectMapper mapper = new ObjectMapper();

    public StudentService(StudentDao studentDao, Supplier<HttpClient> httpClient) {
        this.studentDao = studentDao;
        this.httpClient = httpClient;
    }

    public String getGreeting(int userId) {
        StudentDetails student = studentDao.getStudentDetails(userId);

        try {
            URIBuilder builder = new URIBuilder("hello-world");
            builder.setHost("localhost");
            builder.addParameter("name",student.getName());
            builder.addParameter("language", student.getLanguage());
            HttpGet request = new HttpGet(builder.build());
            HttpResponse response = httpClient.get().execute(request);
            Greeting g = mapper.readValue(response.getEntity().getContent(), Greeting.class);
            return g.getGreeting();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    public GreetingTemplate persistGreeting(GreetingTemplate template) {
        try {
            URIBuilder builder = new URIBuilder("hello-world/saveTranslation");
            builder.setHost("localhost");
            HttpPost request = new HttpPost(builder.build());
            request.setEntity(new StringEntity(mapper.writeValueAsString(template)));
            HttpResponse response = httpClient.get().execute(request);
            GreetingTemplate g = mapper.readValue(response.getEntity().getContent(), GreetingTemplate.class);
            return g;
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
