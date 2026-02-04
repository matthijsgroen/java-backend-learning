package nl.kabisa.dashboarding.widget.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ConfigurationFieldType {
    @JsonProperty("string")
    STRING,

    @JsonProperty("integer")
    INTEGER,

    @JsonProperty("boolean")
    BOOLEAN
}