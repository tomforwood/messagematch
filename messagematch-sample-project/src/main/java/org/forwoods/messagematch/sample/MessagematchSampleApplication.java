package org.forwoods.messagematch.sample;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.ws.rs.ApplicationPath;

public class MessagematchSampleApplication extends Application<MessagematchSampleConfiguration> {

    public static void main(final String[] args) throws Exception {
        new MessagematchSampleApplication().run(args);
    }

    @Override
    public String getName() {
        return "messagematch-sample-project";
    }

    @Override
    public void initialize(final Bootstrap<MessagematchSampleConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final MessagematchSampleConfiguration configuration,
                    final Environment environment) {
        // TODO: implement application
    }

}
