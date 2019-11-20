package net.trystram.scaletest.httpInserter;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Statistics implements AutoCloseable {

    private AtomicLong success = new AtomicLong();
    private AtomicLong error = new AtomicLong();

    private ScheduledExecutorService executor;
    private PrintStream out;

    private Instant last = Instant.now();
    private long lastSuccess;

    public Statistics(final PrintStream out) {
        this.out = out;
        this.executor = Executors.newScheduledThreadPool(1);
        this.executor.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
        this.out.println("Time;Total;Created;Rate;Errors");
    }

    public Statistics(final OutputStream out) {
        this(new PrintStream(out));
    }

    @Override
    public void close() throws Exception {
        this.executor.shutdown();
        this.out.close();
    }

    private void tick() {

        long currentSuccess = this.success.get();
        long diff = currentSuccess - this.lastSuccess;
        this.lastSuccess = currentSuccess;

        Instant now = Instant.now();
        Duration period = Duration.between(this.last, now);
        this.last = now;

        double rate = ((double) diff) / ((double) period.getSeconds());

        this.out.format("\"%s\";%s;%s;%.2f;%s%n",
                Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME),
                diff,
                currentSuccess,
                rate,
                this.error.get());
        this.out.flush();
    }

    public void success() {
        success.incrementAndGet();
    }

    public void error() {
        error.incrementAndGet();
    }

}
