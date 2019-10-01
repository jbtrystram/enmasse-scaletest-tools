package net.trystram.scaletest.InfinispanDevicesCreator;

import io.enmasse.iot.service.base.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.service.base.infinispan.config.InfinispanProperties;

import io.enmasse.iot.service.base.infinispan.device.DeviceInformation;
import io.enmasse.iot.service.base.infinispan.device.DeviceKey;
import io.enmasse.iot.service.base.infinispan.tenant.TenantHandle;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

import net.trystram.util.CsvLogger;
import org.infinispan.client.hotrod.RemoteCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Populator {

    private String tenantIdToPopulate;
    private InfinispanProperties infinispanProperties;
    private long devicesToCreate;

    private RemoteCache<DeviceKey, DeviceInformation> devicesCache;

    DeviceManagementCacheProvider mgmtProvider;

    private final Logger log = LoggerFactory.getLogger(
            Populator.class);
    private final Vertx vertx;

    private final String pathToConfig;

    private CsvLogger csv;

    public Populator(String pathToConfig) {
        VertxOptions vxOptions = new VertxOptions().setBlockedThreadCheckInterval(2000000);
        vertx = Vertx.vertx(vxOptions);
        this.pathToConfig = pathToConfig;
    }

    public Future<Void> run(Future<Void> startPromise) {

        try {
            mgmtProvider = new DeviceManagementCacheProvider(infinispanProperties);
            mgmtProvider.start();
            devicesCache = mgmtProvider.getDeviceManagementCache();

        } catch (Exception e) {
            log.error("Unable to access Infinispan caches.", e.getCause());
            startPromise.fail(e);
            return startPromise;
        }

        csv.setBeginTime(System.currentTimeMillis());

       for (long i = 0; i < devicesToCreate; i++) {

           DeviceInformation deviceInfo = new DeviceInformation();
           deviceInfo.setTenantId(tenantIdToPopulate);
           deviceInfo.setVersion("version");
           deviceInfo.setDeviceId(UUID.randomUUID().toString());

           DeviceKey key = DeviceKey.deviceKey(
                   TenantHandle.of(tenantIdToPopulate.split("/")[0],
                           tenantIdToPopulate), deviceInfo.getDeviceId());

           devicesCache.putIfAbsent(key, deviceInfo);

           csv.log(i);
           //log.info(String.valueOf(i));
       }

       stop();
       csv.saveFile();
       startPromise.complete();

        return startPromise;
    }

    public void stop() {
        // Stop the cache managers and release all resources
        try {
            mgmtProvider.stop();
        } catch (Exception n) {
            log.warn("Could not properly release cache manager resources in infinispan");
        }
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
                CreatorConfigValues config = json.result().mapTo(CreatorConfigValues.class);

                devicesToCreate = config.getNumberOfDevicesToCreate();

                String verif = config.verify();
                if (verif != null){
                    log.error(verif);
                    configured.fail(new IllegalArgumentException(String.format(verif)));
                } else {
                    tenantIdToPopulate = config.getTenantId();
                    infinispanProperties = CreatorConfigValues.createInfinispanProperties(config);
                    csv = new CsvLogger(vertx, config.getCsvLogFile());
                    configured.complete();
                }
            }
        });
        return configured;
    }
}