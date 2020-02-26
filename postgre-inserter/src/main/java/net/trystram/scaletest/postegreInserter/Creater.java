package net.trystram.scaletest.postegreInserter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.glutamate.lang.Exceptions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Creater implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Creater.class);

    /**
     * The RNG for the salt. As this is just for testing, we don't need to use {@link SecureRandom}.
     */
    private static final Random R = new Random();

    private final ObjectMapper mapper = new ObjectMapper();
    private final Config config;
    private final boolean plain;
    private final boolean dynamic;

    private final Connection sqlConnection;
    private final String DEVICE_TABLE = "devices";



    public Creater(final Config config ) throws SQLException {
        System.out.format("Running with config: %s%n", config);

        this.config = config;
        this.plain = config.isPlainPasswords();
        this.dynamic = config.isDynamicPasswords();

        sqlConnection = DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword());


        // create the tables
        Statement statement = sqlConnection.createStatement();
        statement.execute(CreateTables.getFullCreateQuery());

    }

    @Override
    public void close() {
        try {
            sqlConnection.close();
        } catch (SQLException e){
            log.warn("Error while closing the connection to the database", e);
        }
    }


    public void run() {

        //TODO : deal with async
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

    private void createDevice(final long i) throws SQLException {

        final String deviceId = this.config.getDeviceIdPrefix() + Long.toString(i);

        final String version = UUID.randomUUID().toString();
        final String deviceInfo = deviceJson(i);
        final String creds = credentialJson(i);

        final List<String> params = new ArrayList<>();
        params.add(config.getTenantId());
        params.add(deviceId);
        params.add(UUID.randomUUID().toString());
        params.add(deviceInfo);
        params.add(creds);

        // forge insert request
        final String sql = String.format("INSERT INTO %s (tenant_id, device_id, version, data, credentials) VALUES (?, ?, ?, to_jsonb(?::jsonb), to_jsonb(?::jsonb))", DEVICE_TABLE);
        PreparedStatement pst = sqlConnection.prepareStatement(sql);

        pst.setString(1, config.getTenantId());
        pst.setString(2, deviceId);
        pst.setString(3, version);
        pst.setString(4, deviceInfo);
        pst.setString(5, creds);

        log.debug("createDevice - sql: {}, params: {}", sql, params);

        pst.executeUpdate();
    }

    private void handleError(final Exception e) {
        log.warn("Failed to process", e);
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

    private String deviceJson(long i) {
        final Map<String, Object> result = new HashMap<>(3);
        result.put("enabled", true);

        final Map<String, String> ext = new HashMap<>(2);
        ext.put("externalField", "This is device "+ String.valueOf(i));
        ext.put("someComment", "about device"+ String.valueOf(i));

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
