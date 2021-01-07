package test.vertx.tsdb;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.Json;
import test.vertx.utilities.Warp10Utils;

import java.io.ByteArrayInputStream;

public class FileTsDbVerticle extends AbstractVerticle {
    Logger LOG = null;
    private MessageConsumer<String> consumer;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LoggerContext loggerContext = new LoggerContext();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        // Call context.reset() to clear any previous configuration, e.g. default
        // configuration. For multi-step configuration, omit calling context.reset().
        loggerContext.reset();
        FileSystem fs = vertx.fileSystem();
        String configFilePath = config().getString("config-file");
        fs.readFile(configFilePath)
            // Configure the Logging service
            .map(file -> {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(file.getBytes());
                try {
                    configurator.doConfigure(byteArrayInputStream);
                } catch (Exception e) {
                    throw new RuntimeException("Can't configure logging with " + configFilePath);
                }
                return null;
            })
            // Register the handler to the event bus
            .map(v -> {
                StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
                LOG = loggerContext.getLogger("GTS");
                EventBus eventBus = vertx.eventBus();
                consumer = eventBus.consumer("tsdb.data", this::onNewMessage);
                Promise<Void> promise = Promise.promise();
                consumer.completionHandler(ar -> {
                    if (ar.failed()) {
                        promise.fail(ar.cause());
                    } else {
                        promise.complete();
                    }
                });
                return promise.future();
            })
            .onComplete(hr -> {
                if (hr.succeeded()) {
                    startPromise.complete();
                } else {
                    startPromise.fail(hr.cause());
                }
            });
        // Set the result to fail or ok depending of the chain of the
//            .result()
//            .onComplete(asyncResult -> {
//                startPromise.handle(asyncResult);
//            });
//

    }

    private void onNewMessage(io.vertx.core.eventbus.Message<String> message) {
        TsDbRecord tsDbRecord = Json.decodeValue(message.body(), TsDbRecord.class);
        String warp10WSMessage = Warp10Utils.convertTsDbRecordToWSGTS(tsDbRecord);
        LOG.info(warp10WSMessage);
    }


    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        if (consumer != null) {
            consumer
                .unregister()
                .onComplete(stopPromise::handle);
        }
    }
}
