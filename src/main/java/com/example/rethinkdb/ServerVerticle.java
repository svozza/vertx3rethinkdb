package com.example.rethinkdb;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

public class ServerVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> fut) {

        EventBus eb = vertx.eventBus();
        vertx.deployVerticle("com.example.rethinkdb.ScoreUpdateVerticle");

        DeploymentOptions deployOpts = new DeploymentOptions().setWorker(true).setMultiThreaded(true).setInstances(4);
        vertx.deployVerticle("com.example.rethinkdb.ScoreWorkerVerticle", deployOpts, res -> {
            Router router = Router.router(vertx);

            router.route("/scores/:id").handler(rc -> {
                final DeliveryOptions opts = new DeliveryOptions()
                        .setSendTimeout(2000);
                opts.addHeader("method", "getScore")
                        .addHeader("id", rc.request().getParam("id"));
                eb.send("com.example.score", null, opts, reply -> {
                    rc.response()
                            .setStatusMessage("OK")
                            .setStatusCode(200)
                            .end(reply.result().body().toString());
                });
            });

            // Allow events for the designated addresses in/out of the event bus bridge
            BridgeOptions opts = new BridgeOptions()
                    .addOutboundPermitted(new PermittedOptions().setAddress("com.example.score.updates"));

            // Create the event bus bridge and add it to the router.
            SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
            router.route("/eventbus/*").handler(ebHandler);

            vertx.eventBus().consumer("com.example.score.updates")
                    .handler(msg -> System.out.println(msg.body().toString()));

            router.route("/*").handler(StaticHandler.create().setWebRoot("web"));

            vertx
                    .createHttpServer()
                    .requestHandler(router::accept)
                    .listen(8080, result -> {
                        if (result.succeeded()) {
                            fut.complete();
                        } else {
                            fut.fail(result.cause());
                        }
                    });
        });
    }
}
