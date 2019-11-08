package net.trystram.scaletest.http.DeviceReader;


import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import io.vertx.ext.web.handler.impl.HttpStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.trystram.scaletest.http.HttpConfigValues;
import net.trystram.util.CsvLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reader {

    private HttpConfigValues config;
    private List<String> devicesIds;

    private final Logger log = LoggerFactory.getLogger(Reader.class);
    private final Vertx vertx;

    private final String pathToConfig;

    private CsvLogger csv;
    private AtomicLong progress = new AtomicLong(0);
    private AtomicLong errors = new AtomicLong(0);

    public Reader(String pathToConfig) {
        VertxOptions vxOptions = new VertxOptions().setBlockedThreadCheckInterval(2000000);
        vertx = Vertx.vertx(vxOptions);
        this.pathToConfig = pathToConfig;
    }

    public Future<Void> run(Future<Void> startPromise) {

        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(config.getHost())
                .setDefaultPort(config.getPort())
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false)
                // for debugging
                //.setLogActivity(true)
        );

        List<Future> futureList = new ArrayList<>();

        for (String id : devicesIds) {

           Future requestFuture = Future.future();
           futureList.add(requestFuture);

           client.get(String.format("/v1/devices/%s/%s", config.getTenantId(), id))
                   .putHeader("Content-Type", " application/json")
                   .putHeader("Authorization", "Bearer "+config.getPassword())
                   .send(res -> {
                       if (res.succeeded()) {
                           if (res.result().statusCode() == 200) {
                               asyncLogger();
                               requestFuture.complete();
                           } else {
                               log.error("Cannot read device : HTTP "+ res.result().statusCode());
                               errors.incrementAndGet();
                               requestFuture.fail(new HttpStatusException(res.result().statusCode()));
                           }
                       } else {
                            log.error("HTTP request failed");
                            log.error(res.cause().getMessage());
                            errors.incrementAndGet();
                            requestFuture.fail(res.cause());
                       }
                    });
       }

       CompositeFuture.join(futureList).setHandler(res -> {
           csv.saveFile();
           startPromise.complete();
       });

        return startPromise;
    }


    private void asyncLogger(){
        csv.log(progress.incrementAndGet(), errors.get());
    }

    public Future<Void> configure() {

        Future<Void> configured = Future.future();

        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
                .addStore(new ConfigStoreOptions()
                        .setType("file")
                        .setFormat("yaml")
                        .setConfig(new JsonObject().put("path", pathToConfig)))
                .addStore(new ConfigStoreOptions()
                        .setType("env")));

        retriever.getConfig(json -> {
            if (json.failed()) {
                log.error("Failed to read configuration.", json.cause());
                configured.fail(json.cause());
            } else {
                HttpConfigValues config = json.result().mapTo(HttpConfigValues.class);

                String verif = config.verify();
                if (verif != null) {
                    log.error(verif);
                    configured.fail(new IllegalArgumentException(verif));
                } else if (config.getCreatedIdsFile() == null){
                    log.error("Missing configuration value: createdIdsFile");
                    configured.fail(new IllegalArgumentException("Missing configuration value: createdIdsFile"));
                } else {
                    this.config = config;
                    csv = new CsvLogger(vertx, config.getCsvLogFile());
                    devicesIds = readIdsFromFile(config.getCreatedIdsFile());
                    configured.complete();
                }
            }
        });
        return configured;
    }

    private List<String> readIdsFromFile(String filename){

        List<String> ids = new ArrayList<>();
        RecordParser parser = RecordParser.newDelimited("\n", handler -> {
            ids.add(handler.toString());
        });

        parser.handle(vertx.fileSystem().readFileBlocking(filename));
        return ids;
    }
}