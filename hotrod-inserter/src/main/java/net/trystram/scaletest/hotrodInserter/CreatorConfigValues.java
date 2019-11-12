package net.trystram.scaletest.hotrodInserter;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.enmasse.iot.service.base.infinispan.config.InfinispanProperties;
import java.util.ArrayList;
import net.trystram.scaletest.util.BaseConfigValues;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatorConfigValues extends BaseConfigValues {

    private long numberOfDevicesToCreate;
    private String csvLogFile;

    @JsonProperty("saslServerName")
    private String saslServer;
    private String saslRealm;

    public long getNumberOfDevicesToCreate() {
        return numberOfDevicesToCreate;
    }

    public void setNumberOfDevicesToCreate(long numberOfDevicesToCreate) {
        this.numberOfDevicesToCreate = numberOfDevicesToCreate;
    }

    @Override
    public int getPort(){
        if (super.getPort() == 0){
            return 11222;
        }
        else return super.getPort();
    }

    public String getCsvLogFile() {
        return csvLogFile;
    }

    public void setCsvLogFile(String csvLogFile) {
        this.csvLogFile = csvLogFile;
    }

    public String getSaslServer() {
        return saslServer;
    }

    public void setSaslServer(String saslServer) {
        this.saslServer = saslServer;
    }

    public String getSaslRealm() {
        return saslRealm;
    }

    public void setSaslRealm(String saslRealm) {
        this.saslRealm = saslRealm;
    }

    public String verify(){

        ArrayList<String> missingValues = new ArrayList<>();

        if (getTenantId() == null) {
            missingValues.add("iotProject ");
        }
        if (getPort() == 0) {
            missingValues.add("port ");
        }
        if (getHost() == null) {
            missingValues.add("host");
        }
        if (getUsername() == null) {
            missingValues.add("username");
        }
        if (getPassword() == null) {
            missingValues.add("password");
        }
        if (getSaslServer() == null) {
            missingValues.add("saslServer");
        }
        if (getSaslRealm() == null) {
            missingValues.add("saslRealm");
        }

        if (missingValues.size() != 0){
            final String message = "Missing configuration value(s): ";
            return message + String.join(", ", missingValues);
        } else {
            return null;
        }
    }

    public static InfinispanProperties createInfinispanProperties(CreatorConfigValues config) {

        InfinispanProperties infinispanProperties = new InfinispanProperties();

        infinispanProperties.setHost(config.getHost());
        infinispanProperties.setPort(config.getPort());
        infinispanProperties.setUsername(config.getUsername());
        infinispanProperties.setPassword(config.getPassword());
        infinispanProperties.setSaslServerName(config.getSaslServer());
        infinispanProperties.setSaslRealm(config.getSaslRealm());

        return infinispanProperties;
    }
}