package net.trystram.scaletest.httpInserter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.common.base.MoreObjects;

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
    private boolean onlyRegister;
    private boolean plainPasswords;
    private boolean dynamicPasswords;

    public void setDevicesToCreate(long devicesToCreate) {
        this.devicesToCreate = devicesToCreate;
    }

    public long getDevicesToCreate() {
        return devicesToCreate > 0 ? devicesToCreate : Long.MAX_VALUE;
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

    public void setOnlyRegister(boolean onlyRegister) {
        this.onlyRegister = onlyRegister;
    }

    public boolean isOnlyRegister() {
        return onlyRegister;
    }

    public void setPlainPasswords(boolean plainPasswords) {
        this.plainPasswords = plainPasswords;
    }

    public boolean isPlainPasswords() {
        return plainPasswords;
    }

    public void setDynamicPasswords(boolean dynamicPasswords) {
        this.dynamicPasswords = dynamicPasswords;
    }

    public boolean isDynamicPasswords() {
        return dynamicPasswords;
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
        result.setTenantId(Environment.get("TENANT_ID")
                .or(() -> {
                    return Environment.get("NAMESPACE")
                            .flatMap(ns -> Environment.get("IOT_PROJECT")
                                    .map(prj -> ns + "." + prj));
                })
                .orElseThrow(
                        () -> new IllegalStateException("Missing tenant information. Need 'TENANT_ID' or 'IOT_PROJECT' and 'NAMESPACE'")));

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
        Environment.consumeAs("ONLY_REGISTER", Boolean::parseBoolean, result::setOnlyRegister);
        Environment.consumeAs("PLAIN_PASSWORDS", Boolean::parseBoolean, result::setPlainPasswords);
        Environment.consumeAs("DYNAMIC_PASSWORDS", Boolean::parseBoolean, result::setDynamicPasswords);

        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("authToken", this.authToken)
                .add("deviceIdPrefix", this.deviceIdPrefix)
                .add("devicesToCreate", this.devicesToCreate)
                .add("insecureTls", this.insecureTls)
                .add("onlyRegister", this.onlyRegister)
                .add("plainPasswords", this.plainPasswords)
                .add("registryUrl", this.registryUrl)
                .add("tenantId", this.tenantId)
                .toString();
    }

}
