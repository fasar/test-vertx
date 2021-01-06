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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Warp10Verticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(Warp10Verticle.class);

    private volatile boolean running = true;
    private HttpClient client = null;
    private MessageConsumer<String> consumer = null;

    private List<String> toWrite = new ArrayList<>();

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        openWebsocket(0);
        EventBus eventBus = vertx.eventBus();
        DeliveryOptions options = new DeliveryOptions().setSendTimeout(100);
        consumer = eventBus.consumer("tsdb.data", message -> {
            TsDbRecord tsDbRecord = Json.decodeValue(message.body(), TsDbRecord.class);
            String warp10WSMessage = convertTsDbRecordToWSMessage(tsDbRecord);
            eventBus.request("tsdb.data.private", warp10WSMessage, options, ar -> {
                if (ar.failed()) {
                    toWrite.add(message.body());
                    while (toWrite.size() > 10000) {
                        toWrite.remove(0);
                    }
                }
            });
        });
    }

    String convertTsDbRecordToWSMessage(TsDbRecord record) {
        List<String> data = record.getData();
        if (data.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String signalName = record.getSignalName();
        long l = record.getStartInstant().toEpochMilli();
        sb.append(l);
        sb.append("// ");
        sb.append(record.signalName);
        String labels = null;
        if (record.labels != null) {
            labels = record.labels.stream().map(lab -> lab.key + "=" + lab.value).collect(Collectors.joining(",", "{", "}"));
        }
        if (StringUtils.isBlank(labels)) {
            sb.append("{}");
        } else {
            sb.append(labels);
        }
        sb.append(" ");
        sb.append(toUnint(data.get(0), record.unit));
        sb.append("\n");
        for (int i = 1; i < data.size(); i++) {
            l = l + record.stepDuration.toMillis();
            sb.append("=");
            sb.append(l);
            sb.append("// ");
            sb.append(toUnint(data.get(i), record.unit));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String toUnint(String data, TsDbUnit unit) {
        switch (unit) {
            case STRING:
                return "'" + data.replace("'", "\\'") + "'";
            case NUMBER:
            case BOOLEAN:
            default:
                return data;
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        if (consumer != null) {
            consumer.unregister();
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
                            LOG.info("Warp10 return message : {}", hdl.textData());
                        }
                    });
                    ws.closeHandler(hdl -> {
                        client.close();
                        client = null;
                        if (running) {
                            LOG.info("The websocket is closed, try to open it again in {} ms", reconnectDelay);
                            try {
                                openWebsocket(reconnectDelay);
                            } catch (Exception e) {
                                // Not Possible
                            }
                        }
                    });
                    // Write the token for Warp10
                    writeWS(ws, "ONERROR MESSAGE");
                    String token = config().getString("warp10-token");
                    String tokenMessage = "TOKEN " + token;
                    writeWS(ws, tokenMessage);
                    // Listen event to write on Warp10 WS
                    vertx.eventBus().<String>consumer("tsdb.data.private", message -> {
                        LOG.info("Write data in Warp10 websocket: {}", message.body());
                        writeWS(ws, message.body());
                        message.reply("ok");
                    });

                    // Replay event not sent
                    List<String> tmp = toWrite;
                    toWrite = new ArrayList<>();
                    for (String tsDbRecord : tmp) {
                        vertx.eventBus().publish("tsdb.data", tsDbRecord);
                    }

                    LOG.info("Websocked Connected on {}", hostUri);
                } else {
                    client.close();
                    client = null;
                    LOG.error("Can't connected websocket on {}", hostUri);
                    if (running) {
                        LOG.info("The websocket was not open, try to open it again in {}", reconnectDelay, res.cause());
                        try {
                            openWebsocket(reconnectDelay);
                        } catch (Exception e) {
                            // Not Possible
                        }
                    }
                }
            });
        };
        if (delay > 0) {
            vertx.setTimer(delay, openClient);
        } else {
            openClient.handle(0L);
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
