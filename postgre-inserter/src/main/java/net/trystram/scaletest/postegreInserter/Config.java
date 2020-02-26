package net.trystram.scaletest.postegreInserter;

import com.google.common.base.MoreObjects;
import io.glutamate.lang.Environment;
import java.io.IOException;

public class Config {

    private long devicesToCreate = Long.MAX_VALUE;
    private String deviceIdPrefix;
    private String tenantId;
    private boolean plainPasswords;
    private boolean dynamicPasswords;

    private boolean alternateCredentialFormat;

    private String dbHost;
    private String dbPort;
    private String dbUser;
    private String dbPassword;
    private String dbName;

    public boolean isAlternateCredentialFormat() {
        return alternateCredentialFormat;
    }

    public void setAlternateCredentialFormat(boolean alternateCredentialFormat) {
        this.alternateCredentialFormat = alternateCredentialFormat;
    }

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

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public String getDbPort() {
        return dbPort;
    }

    public void setDbPort(String dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
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

    public String getDbUrl(){
        return String.format("jdbc:postgresql://{}:{}/{}",
                this.getDbHost(),
                this.getDbPort(),
                this.getDbName());
    }

    public static Config fromEnv() throws IOException {
        final Config result = new Config();

        result.setDbHost(Environment.get("DB_HOST").orElse(""));
        result.setDbUser(Environment.get("DB_USER").orElse(""));
        result.setDbPassword(Environment.get("DB_PASSWORD").orElse(""));
        result.setDbName(Environment.get("DB_NAME").orElse(""));

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

        Environment.consumeAs("PLAIN_PASSWORDS", Boolean::parseBoolean, result::setPlainPasswords);
        Environment.consumeAs("DYNAMIC_PASSWORDS", Boolean::parseBoolean, result::setDynamicPasswords);

        Environment.consumeAs("ALTERNATE_CREDENTIAL_FORMAT", Boolean::parseBoolean, result::setAlternateCredentialFormat);

        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("dbHost", this.dbHost)
                .add("dbPort", this.dbPort)
                .add("dbUser", this.dbUser)
                .add("dbPassword", this.dbPassword)
                .add("dbName", this.dbName)
                .add("alternateCredentialFormat", this.alternateCredentialFormat)
                .add("deviceIdPrefix", this.deviceIdPrefix)
                .add("devicesToCreate", this.devicesToCreate)
                .add("plainPasswords", this.plainPasswords)
                .add("dynamicPasswords", this.dynamicPasswords)
                .add("tenantId", this.tenantId)
                .toString();
    }

}
