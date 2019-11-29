package net.trystram.scaletest.httpReader;

import java.time.Duration;
import java.time.Instant;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reader {

    private static final Logger log = LoggerFactory.getLogger(Reader.class);

    private final Config config;
    private final Statistics stats;

    private OkHttpClient client;
    private HttpUrl registrationUrl;
    private HttpUrl credentialsUrl;

    public Reader(Config config) {
        this.config = config;
        this.stats = new Statistics(System.out, Duration.ofSeconds(10));
        var builder = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(0,1, TimeUnit.MILLISECONDS));

        if (config.isInsecureTls()) {
            Tls.makeOkHttpInsecure(builder);
        }

        this.client = builder.build();

        final HttpUrl base = config.getRegistryUrl();

        this.registrationUrl = base.newBuilder()
                .addPathSegment("debug")
                .addPathSegment("registration")
                .addPathSegment(config.getTenantId())
                .build();

        this.credentialsUrl = base.newBuilder()
                .addPathSegment("debug")
                .addPathSegment("credentials")
                .addPathSegment(config.getTenantId())
                .addPathSegment("hashed-password")
                .build();

        System.out.println("Registration URL: " + this.registrationUrl);
        System.out.println("Credentials URL: " + this.credentialsUrl);
    }


    private Request.Builder newRequest() {
        return new Request.Builder()
                .header("Authorization", "Bearer " + this.config.getAuthToken());
    }

    public void run() {

        for (long i = 0; i < config.getDevicesToRead(); i++) {
            try {
                readDeviceRegistration(ThreadLocalRandom.current().nextLong(config.getMaxDevicesCreated()));
            } catch (final Exception e) {
                handleError(e);
            }
        }
        System.exit(0);
    }

    private void readDeviceRegistration(final long i) throws Exception {

        final String deviceId = getRandomDevicePrefix() + Long.toString(i);

        final Instant start = Instant.now();
        final Request registration = newRequest()
                .url(this.registrationUrl
                        .newBuilder()
                        .addPathSegment(deviceId)
                        .build())
                .get()
                .build();

        try (Response response = this.client.newCall(registration).execute()) {
            if (!response.isSuccessful()) {
                handleRegistrationFailure(response);
                return;
            }
        }

        final Instant endReg = Instant.now();
        if (!config.isOnlyRegister()) {
            final String authId = "device-" + Long.toString(i);

            final Request credentials = newRequest()
                    .url(this.credentialsUrl
                            .newBuilder()
                            .addPathSegment(authId)
                            .build())
                    .get()
                    .build();

            try (Response response = this.client.newCall(credentials).execute()) {
                if (!response.isSuccessful()) {
                    handleCredentialsFailure(response);
                    return;
                }
            }
        }
        final Instant end = Instant.now();

        handleSuccess(
                Duration.between(start, endReg),
                this.config.isOnlyRegister() ? Optional.empty() : Optional.of(Duration.between(endReg, end)));

    }

    private void handleError(final Exception e) {
        log.warn("Failed to process", e);
    }

    private void handleCredentialsFailure(final Response response) {
        this.stats.errorCredentials();
    }

    private void handleRegistrationFailure(final Response response) {
        this.stats.errorRegister();
    }

    private void handleSuccess(final Duration r, final Optional<Duration> c) {
        this.stats.success(r, c);
    }

    private String getRandomDevicePrefix(){
        final int size = config.getDeviceIdPrefixes().size()-1;

        return (config.getDeviceIdPrefixes().get(ThreadLocalRandom.current().nextInt(size)));
    }
}