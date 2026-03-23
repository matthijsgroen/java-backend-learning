package nl.kabisa.dashboarding.widget;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Widget read response including hierarchy information")
public record GetWidgetResponse(
        @Schema(description = "Unique identifier of the widget") String id,
        @Schema(description = "Widget type identifier") String widgetType,
        @Schema(description = "Widget version") String version,
        @Schema(description = "Frontend configuration values") Map<String, Object> configuration,
        @Schema(description = "Parent widget ID, null if this is a root widget", nullable = true) String parentId,
        @Schema(description = "Direct child widget IDs") List<String> childIds,
        @Schema(description = "Owner user ID") String ownerId,
        @Schema(description = "Owner username") String ownerName) {
}
