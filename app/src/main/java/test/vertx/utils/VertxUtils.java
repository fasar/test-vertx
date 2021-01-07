package test.vertx.utils;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class VertxUtils {
    private static Logger LOG = LoggerFactory.getLogger(VertxUtils.class);
    public static ConfigRetriever loadConfiguration(Vertx vertx) {
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
            }
        });
        retriever.configStream()
            .endHandler(v -> {
                // retriever closed
                LOG.info("Configuration closed ");
            })
            .exceptionHandler(t -> {
                // an error has been caught while retrieving the configuration
                LOG.error("Configuration error ", t);
            })
            .handler(conf -> {
                // the configuration
                LOG.info("Configuration changed:" + conf);
                LOG.info("Terminate verticles");
                List<Future<Void>> toWait = vertx.deploymentIDs().stream().map(vertx::undeploy).collect(Collectors.toList());
                LOG.info(toWait.size() + " verticles ended");
            });
        return retriever;
    }

}
