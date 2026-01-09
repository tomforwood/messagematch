package org.forwoods.messagematch.reporting;

import io.vertx.core.Vertx;

public class MessageMatchReportingMain
{
    public static void main(String[] args )
    {
        System.out.println("Starting server");
        final Vertx vertx = Vertx.vertx();
        MessageMatchReporting.deploy(vertx, new MessageMatchReporting.MessageMatchReportingConfig("jdbc:sqlite:/home/forwoodt/code/messagematch/messagematch-junit/messagematch.testresult.db"));
    }
}
