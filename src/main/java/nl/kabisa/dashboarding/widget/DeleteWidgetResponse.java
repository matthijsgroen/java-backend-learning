package nl.kabisa.dashboarding.widget;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Widget deletion response")
public record DeleteWidgetResponse(
        @Schema(description = "Total number of widgets deleted (including descendants)") int deletedCount,
        @Schema(description = "Response message",
                example = "Widget and 2 descendant(s) deleted") String message) {
}
