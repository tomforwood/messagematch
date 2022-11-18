package org.forwoods.messagematch.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Getter
@Setter
@ConfigurationProperties("messagematch")
public class MessagematchServerConfig {
    List<String> monitoredEnvironments;
    String mavenHome;
}
