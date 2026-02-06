package nl.kabisa.dashboarding.widget.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ConfigurationFieldScope {
    @JsonProperty("backend")
    BACKEND,

    @JsonProperty("frontend")
    FRONTEND
}