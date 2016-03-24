package com.example.rethinkdb;

import io.vertx.core.AbstractVerticle;

public class ScoreWorkerVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        vertx.eventBus().consumer("com.example.score").handler(msg -> msg.reply("received!"));
    }
}
