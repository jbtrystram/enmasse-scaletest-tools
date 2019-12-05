package net.trystram.scaletest.infinispan;

import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;

@ApplicationScoped
@RegisterForReflection
@Path("/info")
public class InfinispanMetrics {

    private static final Logger logger = LoggerFactory.getLogger(InfinispanMetrics.class);

    @Inject
    @RestClient
    Infinispan infinispan;

    private String authHeader;

    void onStart(@Observes StartupEvent ev) {
        logger.info("The application is starting...");
        final String credentials = ConfigProvider.getConfig().getValue("infinispan.credentials", String.class);
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    @GET
    @Path("/devices")
    @Produces("text/plain")
    public Long devices() {
        return this.infinispan.cacheSize(this.authHeader, "devices");
    }

    @Gauge(name = "cache_entries", tags = {"cache=devices"}, unit = MetricUnits.NONE, description = "Total number of entries in the 'devices' cache")
    public Long cacheEntries() {
        return this.infinispan.cacheSize(this.authHeader, "devices");
    }

}
