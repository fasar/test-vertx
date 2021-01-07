/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package test.vertx.app;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import test.vertx.utils.VertxUtils;

public class App {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ConfigRetriever retriever = VertxUtils.loadConfiguration(vertx);
        retriever.configStream()
            .handler(conf -> {
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
