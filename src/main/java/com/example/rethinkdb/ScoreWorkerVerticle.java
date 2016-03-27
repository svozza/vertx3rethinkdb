package com.example.rethinkdb;

import io.vertx.core.json.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;

import java.util.HashMap;

public class ScoreWorkerVerticle extends AbstractVerticle {

    public static final RethinkDB r = RethinkDB.r;
    public static final String DBHOST = "172.17.0.3";

    @Override
    public void start() {

        vertx.eventBus().consumer("com.example.score").handler(this::handleDbRequest);
    }

    public void handleDbRequest(Message<Object> msg) {
        Connection conn = r.connection()
                .hostname(DBHOST).port(28015).connect();
        String method = msg.headers().get("method");
        switch(method) {
            case "getScore":
                int id = Integer.parseInt(msg.headers().get("id"));
                HashMap m = r.db("test").table("scores").get(id).run(conn);
                msg.reply(Json.encode(m));
        }
        conn.close();
    }

}
