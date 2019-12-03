package net.trystram.scaletest.httpReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.glutamate.lang.Exceptions;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

import java.util.Base64;

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

public class Reader implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Reader.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Config config;
    private final Statistics stats;

    private OkHttpClient client;
    private HttpUrl registrationUrl;
    private HttpUrl credentialsUrl;

    private final int max;

    public Reader(Config config) {
        this.config = config;
        this.stats = new Statistics(System.out, Duration.ofSeconds(10));
        var builder = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.MILLISECONDS));

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
        this.max = config.getDeviceIdPrefixes().size() -1;
    }

    @Override
    public void close() {
        this.stats.close();
    }

    public void run() {

        long max = config.getDevicesToRead();
        for (long i = 0; i < max; i++) {
            try {
                readDeviceRegistration(ThreadLocalRandom.current().nextLong(config.getMaxDevicesCreated()));
            } catch (final Exception e) {
                handleError(e);
            }
        }
        System.out.format("Finished reading %s devices.%n", max);
    }

    private void readDeviceRegistration(final long i) throws Exception {

        final String prefix = getRandomDevicePrefix();
        final String deviceId = prefix + Long.toString(i);

        final Instant start = Instant.now();

        final Request registration = new Request.Builder()
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
            final String authId = prefix + "auth-" + Long.toString(i);

            final Request credentials = new Request.Builder()
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

                // verify the credential
                final JsonNode credential = mapper.readTree(response.body().bytes());
                final JsonNode secrets = credential.get("secrets");

                if (! verifyPassword(secrets.get(0).get("salt").asText(), secrets.get(0).get("pwd-hash").asText(), i)){
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

    private String getRandomDevicePrefix() {
        return this.config
                .getDeviceIdPrefixes()
                .get(ThreadLocalRandom
                        .current()
                        .nextInt(this.max));
    }

    private static String encodePassword(byte[]salt, final String password) {
        final MessageDigest digest = Exceptions.wrap(() -> MessageDigest.getInstance("SHA-256"));
        if (salt != null) {
            digest.update(salt);
        }
        return Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
    }

    private static boolean verifyPassword(final String saltAsString, final String hash, long deviceId) {

        final String expectedPassword = "longerThanUsualPassword-" + deviceId;
        final byte[] salt = Base64.getDecoder().decode(saltAsString);
        final String expectedHash = encodePassword(salt, expectedPassword);

        return hash.equals(expectedHash);
    }
}
