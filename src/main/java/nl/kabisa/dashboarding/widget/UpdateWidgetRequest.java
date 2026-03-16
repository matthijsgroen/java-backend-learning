package nl.kabisa.dashboarding.widget;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Widget update request for re-parenting")
public record UpdateWidgetRequest(
        @Schema(description = "New parent widget ID, or null to detach from parent",
                example = "550e8400-e29b-41d4-a716-446655440000",
                nullable = true) String parentId) {
}
