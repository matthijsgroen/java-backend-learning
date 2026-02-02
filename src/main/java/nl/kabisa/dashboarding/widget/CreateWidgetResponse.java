package nl.kabisa.dashboarding.widget;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Widget creation response")
public record CreateWidgetResponse(
        @Schema(description = "Unique identifier of the created widget", example = "550e8400-e29b-41d4-a716-446655440000") String id,

        @Schema(description = "Response message", example = "Widget created successfully") String message) {

    public CreateWidgetResponse {
        // Canonical constructor - validates arguments
    }
}
