package net.trystram.scaletest.httpInserter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.glutamate.lang.Exceptions;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Creater {

    private static final Logger log = LoggerFactory.getLogger(Creater.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ObjectMapper mapper = new ObjectMapper();
    private final Config config;
    private final Statistics stats;

    private OkHttpClient client;
    private HttpUrl registerUrl;
    private HttpUrl credentialsUrl;

    public Creater(Config config) {
        this.config = config;
        this.stats = new Statistics(System.out);
        var builder = new OkHttpClient.Builder();

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
        System.out.println("Credential Example JSON: " + credentialJson(0));
    }


    private Request.Builder newRequest() {
        return new Request.Builder()
                .header("Authorization", "Bearer " + this.config.getAuthToken());
    }

    public void run() {

        for (long i = 0; i < config.getDevicesToCreate(); i++) {
            try {
                createDevice(i);
            } catch (final Exception e) {
                handleError(e);
            }
        }

    }

    private void createDevice(final long i) throws Exception {

        final String deviceId = this.config.getDeviceIdPrefix() + Long.toString(i);

        final Request register = newRequest()
                .url(this.registerUrl
                        .newBuilder()
                        .addPathSegment(deviceId)
                        .build())
                .post(RequestBody.create("{}", JSON))
                .build();

        try (Response response = client.newCall(register).execute()) {
            if (!response.isSuccessful()) {
                handleRegistrationFailure(response);
                return;
            }
        }

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

        handleSuccess();

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

    private void handleSuccess() {
        this.stats.success();
    }

    private String credentialJson(long i) {
        final Map<String, Object> result = new HashMap<>(3);
        result.put("type", "hashed-password");
        result.put("auth-id", "device-" + i);

        final Map<String, String> secret = new HashMap<>(1);
        secret.put("pwd-plain", Base64.getEncoder().encodeToString(("longerThanUsualPassword-" + i).getBytes(StandardCharsets.UTF_8)));
        result.put("secrets", Collections.singletonList(secret));

        return Exceptions.wrap(() -> mapper.writeValueAsString(Collections.singletonList(result)));
    }
}
