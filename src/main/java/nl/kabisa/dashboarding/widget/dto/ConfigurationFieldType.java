package nl.kabisa.dashboarding.widget.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConfigurationFieldType {
    STRING("string"),
    INTEGER("integer"),
    BOOLEAN("boolean");

    private final String value;

    ConfigurationFieldType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ConfigurationFieldType fromValue(String value) {
        for (ConfigurationFieldType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid type: " + value);
    }
}