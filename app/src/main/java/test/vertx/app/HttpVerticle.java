package test.vertx.app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;

public class HttpVerticle extends AbstractVerticle {
    private HttpServer server;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        Integer port = config().getInteger("port");

        server = vertx.createHttpServer().requestHandler(req -> {
            req.response()
                .putHeader("content-type", "text/plain")
                .end(config().getString("message"));
        });

        // Now bind the server:
        server.listen(port, res -> {
            if (res.succeeded()) {
                startPromise.complete();
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
