package net.trystram.scaletest.httpInserter;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Statistics implements AutoCloseable {

    private long success;
    private long errorRegister;
    private long errorCredentials;

    private long timeRegister;
    private long timeCredentials;

    private ScheduledExecutorService executor;
    private PrintStream out;

    private Instant last = Instant.now();
    private long lastSuccess;

    public Statistics(final PrintStream out, final Duration print) {
        this.out = out;
        this.executor = Executors.newScheduledThreadPool(1);
        this.executor.scheduleAtFixedRate(this::tick, print.toMillis(), print.toMillis(), TimeUnit.MILLISECONDS);
        this.out.println("Time;Total;Created;Rate;ErrorsR;ErrorsC;AvgR;AvgC");
    }

    public Statistics(final OutputStream out, final Duration print) {
        this(new PrintStream(out), print);
    }

    @Override
    public void close() throws Exception {
        this.executor.shutdown();
        this.out.close();
    }

    private synchronized void tick() {

        final long currentSuccess = this.success;
        final long diff = currentSuccess - this.lastSuccess;
        this.lastSuccess = currentSuccess;

        final Instant now = Instant.now();
        final Duration period = Duration.between(this.last, now);
        this.last = now;

        final double rate = ((double) diff) / ((double) period.toMillis()) * 1000.0;

        final Long avgReg = this.timeRegister > 0 ? this.timeRegister / diff : null;
        final Long avgCred = this.timeCredentials > 0 ? this.timeCredentials / diff : null;
        timeRegister = 0L;
        timeCredentials = 0L;

        this.out.format("\"%s\";%s;%s;%.2f;%s;%s;%.0f;%.0f%n",
                Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME),
                diff,
                currentSuccess,
                rate,
                errorRegister, errorCredentials,
                avgReg, avgCred
                );
        this.out.flush();
    }

    public synchronized void success(final Duration register, final Optional<Duration> credentials) {
        success++;
        timeRegister += register.toMillis();
        timeCredentials += register.toMillis();
    }

    public synchronized void errorRegister() {
        errorRegister++;
    }

    public synchronized void errorCredentials() {
        errorCredentials++;
    }

}
