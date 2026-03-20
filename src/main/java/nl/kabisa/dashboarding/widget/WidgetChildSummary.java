package nl.kabisa.dashboarding.widget;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Child widget summary")
public record WidgetChildSummary(
        @Schema(description = "Unique identifier of the widget") String id,
        @Schema(description = "Widget type identifier") String widgetType,
        @Schema(description = "Widget version") String version,
        @Schema(description = "Parent widget ID", nullable = true) String parentId,
        @Schema(description = "Owner user ID") String ownerId,
        @Schema(description = "Owner username") String ownerName) {
}
