package net.trystram.scaletest.infinispan;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.quarkus.runtime.annotations.RegisterForReflection;

@ApplicationScoped
@RegisterForReflection
public class InfinispanMetrics {

    @Inject
    @RestClient
    Infinispan infinispan;

    @Gauge(name = "cache_entries", tags = {"cache=devices"}, unit = MetricUnits.NONE, description = "Total number of entries in the 'devices' cache")
    public Long cacheEntries() {
        return this.infinispan.cacheSize("devices");
    }

}
