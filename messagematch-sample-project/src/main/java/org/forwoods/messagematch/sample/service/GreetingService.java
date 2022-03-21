package org.forwoods.messagematch.sample.service;

import org.forwoods.messagematch.sample.api.Greeting;
import org.forwoods.messagematch.sample.api.GreetingTemplate;
import org.forwoods.messagematch.sample.db.GreetingDAO;

import java.util.Optional;

public class GreetingService {

    private final GreetingDAO greetingDAO;

    public GreetingService(GreetingDAO greetingDAO) {
        this.greetingDAO = greetingDAO;
    }

    public Greeting getGreeting(String name, Optional<String> language) {
        return language.map(g->buildGreeting(name, g)).orElseGet(()->new Greeting("Hello "+name));
    }

    private Greeting buildGreeting(String name, String language) {
        GreetingTemplate template= greetingDAO.lookup(language);
        if (template!=null) {
            return new Greeting(String.format(template.getGreetingTemplate(), name));
        }
        else {
            throw new RuntimeException("Template for "+language+" not found");
        }
    }

    public GreetingTemplate persistGreeting(GreetingTemplate template) {
        return greetingDAO.persistTemplate(template);
    }
}
