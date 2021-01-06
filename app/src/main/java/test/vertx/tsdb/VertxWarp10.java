package test.vertx.tsdb;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class VertxWarp10 {

    public static void main(String[] args) {
        // Configure jackson
        JavaTimeModule module = new JavaTimeModule();
        DatabindCodec.mapper().registerModule(module);
        DatabindCodec.prettyMapper().registerModule(module);


        Vertx vertx = Vertx.vertx();
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
            .setType("file")
            .setConfig(new JsonObject().put("path", "conf/config.json"));

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
            .setScanPeriod(1000)
            .addStore(fileStore);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);


        // Force to read configuration
        retriever.getConfig(ar -> {
            if (ar.failed()) {
                // Failed to retrieve the configuration
                ar.cause().printStackTrace();
            } else {
                JsonObject config = ar.result();
                System.out.println("Loaded Config is : " + config);
            }
        });

        retriever.configStream()
            .endHandler(v -> {
                // retriever closed
                System.out.println("Configuration closed ");
            })
            .exceptionHandler(t -> {
                // an error has been caught while retrieving the configuration
                System.out.println("Configuration error ");
                t.printStackTrace();
            })
            .handler(conf -> {
                // the configuration
                System.out.println("Configuration changed:" + conf);
                System.out.println("Terminate verticles");
                List<Future<Void>> toWait = vertx.deploymentIDs().stream().map(vertx::undeploy).collect(Collectors.toList());
                System.out.println(toWait.size() + " verticles ended");
                System.out.println("Starting verticles");
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
            System.out.println("Write to " + obj.getSignalName());
            vertx.eventBus().send("tsdb.data", Json.encode(obj));
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
    }
}
