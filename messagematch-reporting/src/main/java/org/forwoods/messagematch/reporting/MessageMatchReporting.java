package org.forwoods.messagematch.reporting;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.forwoods.messagematch.reporting.api.CompatMatrix;
import org.forwoods.messagematch.reporting.dao.ReportingDAO;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Set;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.OPTIONS;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

public class MessageMatchReporting {
    public static final JsonMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    private final ReportingDAO reportingDao;

    public MessageMatchReporting(final MessageMatchReportingConfig config) {
        reportingDao = new ReportingDAO(config.database);
    }

    public static Future<?> deploy(final Vertx vertx, MessageMatchReportingConfig config) {
        MessageMatchReporting reporting = new MessageMatchReporting(config);
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.route().handler(CorsHandler.create().allowedMethods(Set.of(GET, PUT, OPTIONS, POST, DELETE)));
        router.get("/api/clients").handler(reporting::getClients);
        router.get("/api/matrix").handler(reporting::getMatrix);
        return vertx.createHttpServer()
                .requestHandler(router)
                .listen(8456)
                .onSuccess(server -> System.out.println("Server at http://localhost:"+ server.actualPort()))
                .onFailure(err -> System.out.println("Failed to start server"));

    }

    private void getMatrix(RoutingContext routingContext) {
        final String client = routingContext.request().getParam("client");
        final String clientVersion = routingContext.request().getParam("clientVersion");
        final int cols = Integer.parseInt(routingContext.request().getParam("versionCount", "5"));
        final CompatMatrix apiCompatibilities = reportingDao.getApiCompatibilities(client, clientVersion, cols);
        final String body = MAPPER.writeValueAsString(apiCompatibilities);
        routingContext.response().send(body);
    }

    private void getClients(RoutingContext routingContext) {
        final String body = MAPPER.writeValueAsString(reportingDao.getClients());
        routingContext.response().send(body);
    }

    public record MessageMatchReportingConfig(String database){}
}
