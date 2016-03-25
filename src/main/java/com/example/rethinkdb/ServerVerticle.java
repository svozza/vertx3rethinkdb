package com.example.rethinkdb;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ServerVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> fut) {

        vertx.deployVerticle("com.example.rethinkdb.ScoreUpdateVerticle");

        DeploymentOptions deployOpts = new DeploymentOptions().setWorker(true).setMultiThreaded(true).setInstances(4);
        vertx.deployVerticle("com.example.rethinkdb.ScoreWorkerVerticle", deployOpts, res -> {
            Router router = Router.router(vertx);

            router.route("/scores/:id").handler(rc -> {
                final DeliveryOptions opts = new DeliveryOptions()
                        .setSendTimeout(2000);
                opts.addHeader("method", "getScore")
                        .addHeader("id", rc.request().getParam("id"));
                vertx.eventBus().send("com.example.score", null, opts, reply -> {
                    rc.response()
                            .setStatusMessage("OK")
                            .setStatusCode(200)
                            .end(reply.result().body().toString());
                });
            });

            vertx.eventBus().consumer("com.example.score.updates")
                    .handler(msg -> System.out.println(msg.body().toString()));

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
