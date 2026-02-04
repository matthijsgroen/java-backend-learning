package nl.kabisa.dashboarding.widget.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ConfigurationFieldStorage {
    @JsonProperty("plain")
    PLAIN,

    @JsonProperty("encrypted")
    ENCRYPTED
}