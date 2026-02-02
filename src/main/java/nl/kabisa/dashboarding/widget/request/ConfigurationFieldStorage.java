package nl.kabisa.dashboarding.widget.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConfigurationFieldStorage {
    PLAIN("plain"),
    ENCRYPTED("encrypted");

    private final String value;

    ConfigurationFieldStorage(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ConfigurationFieldStorage fromValue(String value) {
        for (ConfigurationFieldStorage type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid type: " + value);
    }
}