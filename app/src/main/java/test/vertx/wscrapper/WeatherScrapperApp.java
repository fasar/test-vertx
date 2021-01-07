package test.vertx.wscrapper;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import test.vertx.utils.VertxUtils;

import java.util.List;
import java.util.stream.Collectors;

public class WeatherScrapperApp {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ConfigRetriever retriever = VertxUtils.loadConfiguration(vertx);

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
    }

    private static void startVerticles(Vertx vertx, JsonObject config) {
        // Deploy HTTP Verticle
        JsonObject httpConfig = config.getJsonObject("http-server");
        vertx
            .deployVerticle("test.vertx.app.HttpsVerticle", new DeploymentOptions().setConfig(httpConfig))
            .onSuccess(verticleId -> {
                System.out.println("HTTP Verticle deployed with id : " + verticleId);
            }).onFailure(e -> e.printStackTrace())
        ;

        JsonObject httpRedirectConfig = config.getJsonObject("http-redirect");
        if (httpRedirectConfig.getBoolean("enable")) {
            vertx
                .deployVerticle("test.vertx.app.HttpVerticle", new DeploymentOptions().setConfig(httpRedirectConfig))
                .onSuccess(verticleId -> {
                    System.out.println("HTTP-Redirect Verticle deployed with id : " + verticleId);
                }).onFailure(e -> e.printStackTrace())
            ;
        } else {
            System.out.println("HTTPS Redirect is disable");
        }
    }
}
