package test.vertx.tsdb;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.vertx.utilities.Warp10Utils;

import java.net.MalformedURLException;
import java.net.URL;

public class Warp10Verticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(Warp10Verticle.class);

    private volatile boolean running = true;
    private HttpClient client = null;
    private MessageConsumer<String> consumer = null;


    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        openWebsocket(0);
        EventBus eventBus = vertx.eventBus();
        DeliveryOptions options = new DeliveryOptions().setSendTimeout(100);
        consumer = eventBus.consumer("tsdb.data", message -> {
            TsDbRecord tsDbRecord = Json.decodeValue(message.body(), TsDbRecord.class);
            String warp10WSMessage = Warp10Utils.convertTsDbRecordToWSGTS(tsDbRecord);
            eventBus.request("tsdb.data.private", warp10WSMessage, options);
        });
        consumer.completionHandler(ar -> {
            if (ar.failed()) {
                startPromise.fail(ar.cause());
            } else {
                startPromise.complete();
            }
        });
    }


    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        if (consumer != null) {
            consumer.unregister();
            consumer = null;
        }
        if (client != null) {
            client.close(ar -> {
                if (ar.succeeded()) {
                    stopPromise.complete();
                } else {
                    LOG.error("Can't close the client", ar.cause());
                    stopPromise.fail(ar.cause());
                }
            });
        } else {
            stopPromise.complete();
        }
    }

    private void openWebsocket(long delay) throws MalformedURLException {
        Long reconnectDelay = config().getLong("reconnect-delay");
        URL hostUri = new URL(config().getString("host-uri"));

        Handler<Long> openClient = ar -> {
            if (client != null) {
                LOG.error("A client is already running");
                return;
            }
            HttpClientOptions httpOptions = new HttpClientOptions().setLogActivity(config().getBoolean("debug-ws"));
            client = vertx.createHttpClient(httpOptions);
            WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setPort(hostUri.getPort())
                .setHost(hostUri.getHost())
                .setURI(hostUri.getPath());
            client.webSocket(options, res -> {
                if (res.succeeded()) {
                    // Register a callback to reconnect when fail
                    WebSocket ws = res.result();
                    ws.frameHandler(hdl -> {
                        if (hdl.isText()) {
                            LOG.debug("Warp10 return message : {}", hdl.textData());
                        }
                    });
                    ws.closeHandler(hdl -> {
                        tryToOpenLater("closed", reconnectDelay);
                    });
                    // Write the token for Warp10
                    writeWS(ws, "ONERROR MESSAGE");
                    String token = config().getString("warp10-token");
                    String tokenMessage = "TOKEN " + token;
                    writeWS(ws, tokenMessage);
                    // Listen event to write on Warp10 WS
                    vertx.eventBus().<String>consumer("tsdb.data.private", message -> {
                        LOG.trace("Write data in Warp10 websocket: {}", message.body());
                        writeWS(ws, message.body());
                        message.reply("ok");
                    });

                    LOG.info("Websocked Connected on {}", hostUri);
                } else {
                    tryToOpenLater("not open", reconnectDelay);
                }
            });
        };
        if (delay > 0) {
            vertx.setTimer(delay, openClient);
        } else {
            openClient.handle(0L);
        }
    }

    private void tryToOpenLater(String raison, Long reconnectDelay) {
        if (client != null) {
            client.close();
            client = null;
        }
        if (running) {
            LOG.info("The websocket was {}, try to open it again in {} ms", raison, reconnectDelay);
            try {
                openWebsocket(reconnectDelay);
            } catch (Exception e) {
                // Not Possible
            }
        }
    }

    private void writeWS(WebSocket ws, String tokenMessage) {
        ws.writeTextMessage(tokenMessage, ar1 -> {
            if (ar1.failed()) {
                LOG.error("Can't write the message {}", tokenMessage, ar1.cause());
            }
        });
    }
}
