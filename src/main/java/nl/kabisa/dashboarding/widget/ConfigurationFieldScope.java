package nl.kabisa.dashboarding.widget;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConfigurationFieldScope {
    BACKEND("backend"),
    FRONTEND("frontend");

    private final String value;

    ConfigurationFieldScope(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ConfigurationFieldScope fromValue(String value) {
        for (ConfigurationFieldScope type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid type: " + value);
    }
}