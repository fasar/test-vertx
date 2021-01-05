package test.vertx.app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class HttpsVerticle extends AbstractVerticle {
    private HttpServer server;

    public static void main(String[] args) {
        HttpServerOptions httpServerOptions = new HttpServerOptions();
        httpServerOptions.setSsl(true)
                .setKeyStoreOptions(
            new JksOptions()
                .setPath("certificates.keystore")   // (2)
                .setPassword("localhost")           // (3)
        );
        System.out.println(httpServerOptions.toJson());

    }
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        Router router = Router.router(vertx);
        router.get("/api/message")
            .handler(rc -> {
                String message = config().getString("message");
                rc.response().end(message);
            });
        router.get().handler(StaticHandler
            .create().setAllowRootFileSystemAccess(true).setDirectoryListing(true)
            .setWebRoot("/home/sartor/work/Sandbox/Java/test-vertx/app/src/main/resources/conf")
        );

        //router.get().handler(StaticHandler.create());
        HttpServerOptions httpServerOptions = new HttpServerOptions(config().getJsonObject("config"));
        server = vertx.createHttpServer(httpServerOptions).requestHandler(router);
        // Now bind the server:
        server.listen(res -> {
            if (res.succeeded()) {
                startPromise.complete();
                System.out.println("HTTP Server listen on " + config().getJsonObject("config").getInteger("port"));
            } else {
                startPromise.fail(res.cause());
            }
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        super.stop(stopPromise);
    }

}
