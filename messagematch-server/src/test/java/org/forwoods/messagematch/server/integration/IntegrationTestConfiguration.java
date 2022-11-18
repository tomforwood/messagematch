package org.forwoods.messagematch.server.integration;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.forwoods.messagematch.server.config.MessagematchServerConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

@ComponentScan("org.forwoods.messagematch.server.dao")
@ComponentScan(basePackages = {"org.forwoods.messagematch.server.dao", "org.forwoods.messagematch.server.persist"})
@TestConfiguration
public class IntegrationTestConfiguration {
    @Bean
    MessagematchServerConfig config() {
        MessagematchServerConfig config = new MessagematchServerConfig();
        config.setMonitoredEnvironments(List.of("qa"));
        return config;
    }

    @Bean
    public Module javaTimeModule() {
        return new JavaTimeModule();
    }
}
