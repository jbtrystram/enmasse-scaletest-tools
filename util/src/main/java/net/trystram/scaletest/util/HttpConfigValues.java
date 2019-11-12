package net.trystram.scaletest.util;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpConfigValues extends BaseConfigValues {


    private long numberOfDevicesToCreate = 1000;
    private String deviceIdPrefix = "";

    private String csvLogFile;
    private String createdIdsFile;
    private int logInterval = 10;
    private int durationLimit;

    @JsonProperty("authToken")
    private String password;

    public long getNumberOfDevicesToCreate() {
        return numberOfDevicesToCreate;
    }

    public void setNumberOfDevicesToCreate(long numberOfDevicesToCreate) {
        this.numberOfDevicesToCreate = numberOfDevicesToCreate;
    }

    public String getDeviceIdPrefix() {
        return deviceIdPrefix;
    }

    public void setDeviceIdPrefix(String deviceIdPrefix) {
        this.deviceIdPrefix = deviceIdPrefix;
    }

    public String getCsvLogFile() {
        return csvLogFile;
    }

    public void setCsvLogFile(String csvLogFile) {
        this.csvLogFile = csvLogFile;
    }

    public String getCreatedIdsFile() {
        return createdIdsFile;
    }

    public void setCreatedIdsFile(String createdIdsFile) {
        this.createdIdsFile = createdIdsFile;
    }

    public int getLogInterval() {
        return logInterval;
    }

    public void setLogInterval(int logInterval) {
        this.logInterval = logInterval;
    }

    public int getDurationLimit() {
        return durationLimit;
    }

    public void setDurationLimit(int durationLimit) {
        this.durationLimit = durationLimit;
    }

    @Override
    public int getPort() {
        return (super.getPort() == 0)? 443 : super.getPort();
    }

    public String verify(){

        ArrayList<String> missingValues = new ArrayList<>();

        if (this.getTenantId() == null) {
            missingValues.add("iotProject ");
        }
        if (this.getHost() == null) {
            missingValues.add("host");
        }
        if (this.getPassword() == null) {
            missingValues.add("authToken");
        }

        if (missingValues.size() != 0){
            final String message = "Missing configuration value(s): ";
            return message + String.join(", ", missingValues);
        } else {
            return null;
        }
    }
}