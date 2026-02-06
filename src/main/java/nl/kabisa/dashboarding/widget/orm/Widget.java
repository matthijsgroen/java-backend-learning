package nl.kabisa.dashboarding.widget.orm;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import nl.kabisa.dashboarding.widget.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.security.GeneralSecurityException;
import nl.kabisa.dashboarding.widget.dto.DataEndpointModelItem;

@Entity
@Table(name = "widgets")
public class Widget {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String widgetType;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime modifiedAt;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> frontendConfiguration;

    @Column
    private String secretsConfiguration;

    @Transient
    private Map<String, Object> decryptedSecretsConfiguration;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Object configurationModel;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<DataEndpointModelItem> endpoints;

    public Widget() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        encryptSecretsConfiguration();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
        encryptSecretsConfiguration();
    }

    @PostLoad
    protected void onLoad() {
        decryptSecretsConfiguration();
    }

    private void encryptSecretsConfiguration() {
        if (decryptedSecretsConfiguration != null && widgetType != null) {
            try {
                String jsonString = OBJECT_MAPPER.writeValueAsString(decryptedSecretsConfiguration);
                secretsConfiguration = EncryptionUtil.encrypt(jsonString, widgetType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(
                        "Failed to serialize secrets configuration to JSON before encryption: " + e.getMessage(), e);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException("Encryption of secrets configuration failed: " + e.getMessage(), e);
            }
        }
    }

    private void decryptSecretsConfiguration() {
        if (secretsConfiguration != null && widgetType != null && !secretsConfiguration.isEmpty()) {
            try {
                String decryptedJson = EncryptionUtil.decrypt(secretsConfiguration, widgetType);
                decryptedSecretsConfiguration = OBJECT_MAPPER.readValue(decryptedJson,
                        new TypeReference<Map<String, Object>>() {
                        });
            } catch (GeneralSecurityException e) {
                throw new RuntimeException("Decryption of secrets configuration failed: " + e.getMessage(), e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(
                        "Failed to deserialize decrypted secrets configuration from JSON: " + e.getMessage(), e);
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getWidgetType() {
        return widgetType;
    }

    public void setWidgetType(String widgetType) {
        this.widgetType = widgetType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Map<String, Object> getFrontendConfiguration() {
        return frontendConfiguration;
    }

    public void setFrontendConfiguration(Map<String, Object> frontendConfiguration) {
        this.frontendConfiguration = frontendConfiguration;
    }

    public Map<String, Object> getSecretsConfiguration() {
        return decryptedSecretsConfiguration;
    }

    public void setSecretsConfiguration(Map<String, Object> secretsConfiguration) {
        this.decryptedSecretsConfiguration = secretsConfiguration;
    }

    public Object getConfigurationModel() {
        return configurationModel;
    }

    public void setConfigurationModel(Object configurationModel) {
        this.configurationModel = configurationModel;
    }

    public List<DataEndpointModelItem> getEndpoints() {
        return this.endpoints;
    }

    public void setEndpoints(List<DataEndpointModelItem> endpoints) {
        this.endpoints = endpoints;
    }

}
