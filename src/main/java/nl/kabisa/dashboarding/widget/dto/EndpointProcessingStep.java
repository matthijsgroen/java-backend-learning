package nl.kabisa.dashboarding.widget.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

public record EndpointProcessingStep(
                @Schema(description = "Name of the action to execute", example = "proxyRequest") String action,
                @Schema(description = "Configuration for the action") Map<String, Object> config) {
}
