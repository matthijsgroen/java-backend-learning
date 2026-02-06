package nl.kabisa.dashboarding.widget.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data endpoint model item for widget configuration")
public record DataEndpointModelItem(
        @Schema(description = "Unique identifier for the data endpoint", example = "get-events") String path,
        @Schema(description = "Cache duration for the data endpoint in milliseconds", example = "600000") int cache,
        @Schema(description = "HTTP method for the data endpoint", example = "GET") List<EndpointProcessingStep> steps) {
}
