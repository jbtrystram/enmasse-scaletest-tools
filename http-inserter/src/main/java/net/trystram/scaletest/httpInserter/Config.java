package net.trystram.scaletest.httpInserter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.glutamate.lang.Environment;
import io.glutamate.lang.Exceptions;
import okhttp3.HttpUrl;

public class Config {

    private long devicesToCreate = Long.MAX_VALUE;
    private String deviceIdPrefix;

    private String tenantId;
    private String authToken;
    private HttpUrl registryUrl;
    private boolean insecureTls;

    public void setDevicesToCreate(long devicesToCreate) {
        this.devicesToCreate = devicesToCreate;
    }

    public long getDevicesToCreate() {
        return devicesToCreate;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setDeviceIdPrefix(String deviceIdPrefix) {
        this.deviceIdPrefix = deviceIdPrefix;
    }

    public String getDeviceIdPrefix() {
        return deviceIdPrefix;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setRegistryUrl(HttpUrl registryUrl) {
        this.registryUrl = registryUrl;
    }

    public HttpUrl getRegistryUrl() {
        return registryUrl;
    }

    public void setInsecureTls(boolean disableTls) {
        this.insecureTls = disableTls;
    }

    public boolean isInsecureTls() {
        return insecureTls;
    }

    public static Config fromEnv() throws IOException {
        final Config result = new Config();

        result.setAuthToken(Environment.get("AUTH_TOKEN")
                .orElseGet(() -> Exceptions.wrap(
                        () -> Files.readString(
                                Paths.get("/run/secrets/kubernetes.io/serviceaccount/token"),
                                StandardCharsets.UTF_8))));

        System.out.format("Using authToken: '%s'%n", result.getAuthToken());

        result.setDeviceIdPrefix(Environment.get("DEVICE_ID_PREFIX").orElse(""));
        result.setTenantId(Environment.getRequired("TENANT_ID"));
        Environment.consumeAs("MAX_DEVICES", Long::parseLong, result::setDevicesToCreate);

        result.setRegistryUrl(Environment.get("REGISTRY_URL")
                .map(HttpUrl::parse)
                .orElseGet(() -> {
                    final HttpUrl.Builder builder = new HttpUrl.Builder()
                            .scheme(Environment.get("REGISTRY_SCHEME").orElse("https"))
                            .host(Environment.getRequired("REGISTRY_HOST"));

                    Environment.consumeAs("REGISTRY_PORT", Integer::parseInt, builder::port);
                    Environment.get("REGISTRY_BASE").ifPresent(builder::addPathSegments);

                    return builder.build();
                }));

        Environment.consumeAs("INSECURE_TLS", Boolean::parseBoolean, result::setInsecureTls);

        return result;
    }

}
