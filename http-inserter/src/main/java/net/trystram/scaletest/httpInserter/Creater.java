package net.trystram.scaletest.httpInserter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.glutamate.lang.Exceptions;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Creater implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Creater.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    /**
     * The RNG for the salt. As this is just for testing, we don't need to use {@link SecureRandom}.
     */
    private static final Random R = new Random();

    private final ObjectMapper mapper = new ObjectMapper();
    private final Config config;
    private final boolean plain;
    private final boolean dynamic;
    private final Statistics stats;

    private OkHttpClient client;
    private HttpUrl registerUrl;
    private HttpUrl credentialsUrl;

    public Creater(final Config config) {
        System.out.format("Running with config: %s%n", config);

        this.config = config;
        this.plain = config.isPlainPasswords();
        this.dynamic = config.isDynamicPasswords();
        this.stats = new Statistics(System.out, Duration.ofSeconds(10));
        var builder = new OkHttpClient.Builder();

        if ( config.isDisableConnectionPool() ) {
            builder.connectionPool(new ConnectionPool(0, 1, TimeUnit.MILLISECONDS));
        }

        if (config.isInsecureTls()) {
            Tls.makeOkHttpInsecure(builder);
        }

        this.client = builder.build();

        final HttpUrl base = config.getRegistryUrl();

        this.registerUrl = base.newBuilder()
                .addPathSegment("devices")
                .addPathSegment(config.getTenantId())
                .build();

        this.credentialsUrl = base.newBuilder()
                .addPathSegment("credentials")
                .addPathSegment(config.getTenantId())
                .build();

        System.out.println("Register URL: " + this.registerUrl);
        System.out.println("Credentials URL: " + this.credentialsUrl);
        System.out.println("Device ID example value:" + this.config.getDeviceIdPrefix() + 0);
        System.out.println("Credential Example JSON: " + credentialJson(0));
    }

    @Override
    public void close() {
        this.stats.close();
    }

    private Request.Builder newRequest() {
        return new Request.Builder()
                .header("Authorization", "Bearer " + this.config.getAuthToken());
    }

    public void run() {

        final long max = this.config.getDevicesToCreate();
        for (long i = 0; i < max; i++) {
            try {
                createDevice(i);
            } catch (final Exception e) {
                handleError(e);
            }
        }

        System.out.format("Finished creating %s devices.%n", max);

    }

    private void createDevice(final long i) throws Exception {

        final String deviceId = this.config.getDeviceIdPrefix() + Long.toString(i);

        final Instant start = Instant.now();
        final Request register = newRequest()
                .url(this.registerUrl
                        .newBuilder()
                        .addPathSegment(deviceId)
                        .build())
                .post(RequestBody.create("{}", JSON))
                .build();

        try (Response response = this.client.newCall(register).execute()) {
            if (!response.isSuccessful()) {
                handleRegistrationFailure(response);
                return;
            }
        }

        final Instant endReg = Instant.now();
        if (!config.isOnlyRegister()) {
            final Request credentials = newRequest()
                    .url(this.credentialsUrl
                            .newBuilder()
                            .addPathSegment(deviceId)
                            .build())
                    .put(RequestBody.create(credentialJson(i), JSON))
                    .build();

            try (Response response = client.newCall(credentials).execute()) {
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

    private String credentialJson(long i) {
        final Map<String, Object> result = new HashMap<>(3);
        result.put("type", "hashed-password");
        result.put("auth-id", this.config.getDeviceIdPrefix() + "auth-" + i);

        final Map<String, String> secret = new HashMap<>(2);
        final String password = "longerThanUsualPassword-" + i;
        if (this.plain) {
            secret.put("pwd-plain", password);
        } else if (!this.dynamic) {
            secret.put("pwd-hash", "GO/C0ZqOnMFs2QnCUxFR92pu1uPe4fdMgVCXGjnH7uk=");
            secret.put("salt", "YvajSViCAW8=");
            secret.put("hash-function", "sha-256");
        } else {
            final byte[] salt = new byte[8];
            R.nextBytes(salt);
            secret.put("pwd-hash", encodePassword(salt, password));
            secret.put("hash-function", "sha-256");
            secret.put("salt", Base64.getEncoder().encodeToString(salt));
        }
        result.put("secrets", Collections.singletonList(secret));

        return Exceptions.wrap(() -> this.mapper.writeValueAsString(Collections.singletonList(result)));
    }

    private static String encodePassword(byte[] salt, final String password) {
        final MessageDigest digest = Exceptions.wrap(() -> MessageDigest.getInstance("SHA-256"));
        if (salt != null) {
            digest.update(salt);
        }
        return Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
    }
}
