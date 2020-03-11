package net.trystram.scaletest.postgre;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.vertx.axle.sqlclient.Row;
import io.vertx.axle.sqlclient.RowSet;
import io.vertx.axle.sqlclient.Tuple;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.annotations.RegisterForReflection;

@ApplicationScoped
@RegisterForReflection
public class PostgreMetrics {

    private static final Logger logger = LoggerFactory.getLogger(PostgreMetrics.class);

    @Inject
    io.vertx.axle.pgclient.PgPool client;

    @Gauge(name = "table_entries", tags = {"table=devices"}, unit = MetricUnits.NONE, description = "Total number of entries in the 'devices' table")
    public Long tableEntries() {

        long count = -1;
        for(Row row : tableSizeQuery("devices")){
            count = row.getLong("count");
        }
        return count;
    }

    private RowSet<Row> tableSizeQuery(final String table) {
        return client.preparedQuery("select count(*) from $1", Tuple.of(table))
                .toCompletableFuture()
                .join();
    }
}
