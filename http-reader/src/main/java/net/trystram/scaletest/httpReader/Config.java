package net.trystram.scaletest.httpReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import io.glutamate.lang.Environment;
import io.glutamate.lang.Exceptions;
import java.util.Arrays;
import java.util.List;
import okhttp3.HttpUrl;

public class Config {

    private long maxDevicesCreated = Long.MAX_VALUE;
    private long devicesToRead = Long.MAX_VALUE;
    private List<String> deviceIdPrefixes;

    private String tenantId;
    private String authToken;
    private HttpUrl registryUrl;
    private boolean insecureTls;
    private boolean onlyRegister;

    public void setMaxDevicesCreated(long maxDevicesCreated) {
        this.maxDevicesCreated = maxDevicesCreated;
    }

    public long getMaxDevicesCreated() {
        return maxDevicesCreated;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setDeviceIdPrefixes(List deviceIdPrefixes) {
        this.deviceIdPrefixes = deviceIdPrefixes;
    }

    public List<String> getDeviceIdPrefixes() {
        return deviceIdPrefixes;
    }

    public long getDevicesToRead() {
        return devicesToRead;
    }

    public void setDevicesToRead(long devicesToRead) {
        this.devicesToRead = devicesToRead;
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

    public void setOnlyRegister(boolean onlyRegister) {
        this.onlyRegister = onlyRegister;
    }

    public boolean isOnlyRegister() {
        return onlyRegister;
    }

    public static Config fromEnv() throws IOException {
        final Config result = new Config();

        result.setAuthToken(Environment.get("AUTH_TOKEN")
                .orElseGet(() -> Exceptions.wrap(
                        () -> Files.readString(
                                Paths.get("/run/secrets/kubernetes.io/serviceaccount/token"),
                                StandardCharsets.UTF_8))));

        System.out.format("Using authToken: '%s'%n", result.getAuthToken());

        final String commaSeparatedStr = Environment.get("DEVICE_ID_PREFIXES").orElseThrow(
                () -> new IllegalStateException("Missing DEVICE_ID_PREFIXES parameters. "));
        result.setDeviceIdPrefixes(Arrays.asList(commaSeparatedStr.split("\\s*,\\s*")));

        result.setTenantId(Environment.get("TENANT_ID")
                .or(() -> {
                    return Environment.get("NAMESPACE")
                            .flatMap(ns -> Environment.get("IOT_PROJECT")
                                    .map(prj -> ns + "." + prj));
                })
                .orElseThrow(
                        () -> new IllegalStateException("Missing tenant information. Need 'TENANT_ID' or 'IOT_PROJECT' and 'NAMESPACE'")));

        Environment.consumeAs("MAX_DEVICES_CREATED", Long::parseLong, result::setMaxDevicesCreated);

        Environment.consumeAs("DEVICES_TO_READ", Long::parseLong, result::setDevicesToRead);

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
        Environment.consumeAs("ONLY_REGISTER", Boolean::parseBoolean, result::setOnlyRegister);

        return result;
    }

}
