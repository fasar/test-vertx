package test.vertx.tsdb;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import test.vertx.utils.VertxUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VertxTsDbApp {

    public static void main(String[] args) {
        // Configure jackson
        JavaTimeModule module = new JavaTimeModule();
        DatabindCodec.mapper().registerModule(module);
        DatabindCodec.prettyMapper().registerModule(module);

        Vertx vertx = Vertx.vertx();
        ConfigRetriever retriever = VertxUtils.loadConfiguration(vertx);
        retriever.configStream()
            .handler(conf -> {
                startVerticles(vertx, conf);
            });

        List<String> data = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            data.add("" + i);
        }
        TsDbRecord obj = new TsDbRecord("vertx.test", null, Instant.parse("2020-01-01T00:00:00Z"), Duration.ofMinutes(1), TsDbUnit.NUMBER, data);
        Random ran = new Random();
        vertx.setPeriodic(5000, hd -> {
            obj.setSignalName("vertx.test" + ran.nextInt(5));
            System.out.println("MAIN THREAD SEND: " + obj.getSignalName());
            vertx.eventBus().publish("tsdb.data", Json.encode(obj));
        });
    }

    private static void startVerticles(Vertx vertx, JsonObject config) {
        // Deploy HTTP Verticle
        JsonObject warp10Config = config.getJsonObject("warp10");
        vertx
            .deployVerticle("test.vertx.tsdb.Warp10Verticle", new DeploymentOptions().setConfig(warp10Config))
            .onSuccess(verticleId -> {
                System.out.println("Warp10 Verticle deployed with id : " + verticleId);
            }).onFailure(e -> e.printStackTrace())
        ;
        JsonObject fileTsdbConf = config.getJsonObject("tsdb-file");
        vertx
            .deployVerticle("test.vertx.tsdb.FileTsDbVerticle", new DeploymentOptions().setConfig(fileTsdbConf))
            .onSuccess(verticleId -> {
                System.out.println("FileTsDb Verticle deployed with id : " + verticleId);
            }).onFailure(e -> e.printStackTrace())
        ;
    }
}
