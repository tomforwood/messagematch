package org.forwoods.messagematch.sample2;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class MessagematchSampleApplication2 extends Application<MessagematchSampleConfiguration> {

    public static void main(final String[] args) throws Exception {
        new MessagematchSampleApplication2().run(args);
    }

    @Override
    public String getName() {
        return "messagematch-sampleproject2";
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
