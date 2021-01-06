package test.vertx.app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

import java.net.MalformedURLException;
import java.net.URL;

public class HttpVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        HttpServerOptions httpOptions = new HttpServerOptions(config().getJsonObject("config"));
        httpOptions.setSsl(false);
        HttpServer server = vertx.createHttpServer(httpOptions);

        //Start
        server
            .requestHandler(r -> {
                URL url = null;
                try {
                    url = new URL(r.absoluteURI());
                    url = new URL("https", url.getHost(), config().getInteger("port-ssl"), url.getFile());
                } catch (MalformedURLException e) {
                    r.response().setStatusCode(404)
                        .end("HTTP is not available");
                }

                r.response()
                    .setStatusCode(301)
                    .putHeader("Location", url.toString())
                    .end();
            })
            .listen(res -> {
                if (res.failed()) {
                    startPromise.fail(res.cause());
                } else {
                    startPromise.complete();
                }
            });
    }

}
