package com.example.rethinkdb;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;

import java.util.HashMap;

public class ScoreUpdateVerticle extends AbstractVerticle {

    public static final RethinkDB r = RethinkDB.r;
    public static final String DBHOST = "172.17.0.3";

    @Override
    public void start() {
        new Thread(() -> {
            Connection conn = null;

            try {
                conn =  r.connection()
                        .hostname(DBHOST).port(28015).connect();
                Cursor<HashMap> cur = r.db("test").table("scores").changes()
                        .getField("new_val").run(conn);

                for (HashMap item : cur)
                    vertx.eventBus().publish("com.example.score.updates", Json.encode(item));
            }
            catch (Exception e) {
                System.err.println("Error: changefeed failed");
            }
            finally {
                conn.close();
            }
        }).start();
    }
}
