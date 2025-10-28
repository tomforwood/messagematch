package org.forwoods.messagematch.apiTester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class NumberStoreServer
{
    private final Map<Integer, List<Integer>> storedNumbers = new ConcurrentHashMap<>();
    private int port;
    ObjectMapper mapper = new ObjectMapper();

    public static Future<NumberStoreServer> deploy(final Vertx vertx)
    {
        NumberStoreServer application = new NumberStoreServer();
        Router router = Router.router(vertx);


        router.route().handler(BodyHandler.create());
        router.get("/:accountId/storedNumbers").handler(application::getNumbers);
        router.put("/:accountId/storedNumbers").handler(application::storeNumbers);

        HttpServer httpServer = vertx.createHttpServer();
        return httpServer.requestHandler(router).listen(-1).map(s -> {
            application.port = s.actualPort();
            System.out.println("NumberStoreServer deployed with port " + s.actualPort());
            return application;
        });

    }

    private void storeNumbers(RoutingContext ctx)
    {
        try
        {

            Integer accountId = Integer.valueOf(ctx.pathParam("accountId"));
            List<Integer> store = storedNumbers.computeIfAbsent(accountId, i -> new CopyOnWriteArrayList<>());
            final String string = ctx.body().asString();
            List<Integer> toAdd = mapper.readValue(string, new TypeReference<>() {});
            if (toAdd.stream().anyMatch(i->i<0))
            {
                ctx.response().setStatusCode(400).setStatusMessage("I don't support negative numbers").end();
                return;
            }
            store.addAll(toAdd);
            String res = mapper.writeValueAsString(store);
            ctx.response().send(res);
        } catch (JsonProcessingException e)
        {
            e.printStackTrace();
            ctx.response().setStatusCode(400).end();
        }
    }

    private void getNumbers(RoutingContext ctx)
    {
        try
        {
            Integer accountId = Integer.valueOf(ctx.pathParam("accountId"));
            List<Integer> result = storedNumbers.computeIfAbsent(accountId, i -> new CopyOnWriteArrayList<>());
            String res = mapper.writeValueAsString(result);
            ctx.response().send(res);
        }catch (JsonProcessingException e)
        {
            e.printStackTrace();
            ctx.response().setStatusCode(400).end();
        }
    }

    public int getPort()
    {
        return port;
    }
}
