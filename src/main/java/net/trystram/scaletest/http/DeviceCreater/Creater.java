package net.trystram.scaletest.http.DeviceCreater;

import net.trystram.scaletest.http.HttpConfigValues;
import net.trystram.util.CsvLogger;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import io.vertx.ext.web.client.WebClientOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Creater {

    private HttpConfigValues config;
    private long devicesToCreate;

    private final Logger log = LoggerFactory.getLogger(
            Creater.class);
    private final Vertx vertx;

    private final String pathToConfig;

    private CsvLogger csv;
    private AtomicLong progress = new AtomicLong(0);

    public Creater(String pathToConfig) {
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

        csv.setBeginTime(System.currentTimeMillis());

        Buffer deviceIds = Buffer.buffer();
        List<Future> futureList = new ArrayList<>();

       for (long i = 0; i < devicesToCreate; i++) {

           Future requestFuture = Future.future();
           futureList.add(requestFuture);

           Future<String> deviceFuture = Future.future();
           client.post(String.format("/v1/devices/%s", config.getTenantId()))
                   .putHeader("Content-Type", " application/json")
                   .putHeader("Authorization", "Bearer "+config.getPassword())
                   .send(res -> {
                       if (res.succeeded()){
                           if (res.result().statusCode() == 201) {
                               deviceFuture.complete(res.result().bodyAsJsonObject().getString("id"));
                           } else {
                               log.error("Cannot create device : HTTP "+ res.result().statusCode());
                               requestFuture.fail(res.cause());
                           }
                       } else {
                            log.error("HTTP request failed", res.cause());
                            requestFuture.fail(res.cause());
                       }
                    });

           deviceFuture.setHandler(id -> {
               client.put(String.format("/v1/credentials/%s/%s", config.getTenantId(), id.result()))
                       .putHeader("Content-Type", " application/json")
                       .putHeader("Authorization", "Bearer "+config.getPassword())
                       .sendJson(credentialJson(id.result()), res2 -> {
                           asyncLogger();
                           deviceIds.appendString(id.result()).appendString("\n");
                           requestFuture.complete();
                       });
           });
       }

       CompositeFuture.join(futureList).setHandler(res -> {
           writeIdsToFile(deviceIds, config.getCreatedIdsFile());
           csv.saveFile();
           startPromise.complete();
       });

        return startPromise;
    }


    private void asyncLogger(){
        csv.log(progress.incrementAndGet());
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

                devicesToCreate = config.getNumberOfDevicesToCreate();
                String verif = config.verify();
                if (verif != null){
                    log.error(verif);
                    configured.fail(new IllegalArgumentException(verif));
                } else {
                    this.config = config;
                    csv = new CsvLogger(vertx, config.getCsvLogFile());
                    configured.complete();
                }
            }
        });
        return configured;
    }

    private void writeIdsToFile(Buffer buffer, String filename){

        if (filename == null){
            filename  = String.format("/tmp/enmasse_%s_devices", config.getTenantId());
            log.warn(String.format("Missing createdIdsFile setting. The deviceIds will be saved as %s", filename));
        }

        vertx.fileSystem().writeFileBlocking(filename, buffer);
    }

    private static JsonArray credentialJson(String password){
        return new JsonArray().add(new JsonObject()
                .put("type", "hashed-password")
                .put("auth-id", "sensor1")
                .put("secrets", new JsonArray()
                        .add(new JsonObject().put("pwd-plain", password))));
    }
}