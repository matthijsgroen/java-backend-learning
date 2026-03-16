package nl.kabisa.dashboarding.widget;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import nl.kabisa.dashboarding.widget.dto.ConfigurationModelItem;
import nl.kabisa.dashboarding.widget.dto.DataEndpointModelItem;

/**
 * Widget creation request following ARCHITECTURE.md format.
 * Accepts nested JSON objects from frontend for flexible schema validation.
 * ConfigurationModel is validated against the ConfigurationModelItem schema.
 */
@Schema(description = "Widget creation request with configuration format specification")
public record CreateWidgetRequest(
                @NotBlank(message = "Widget type cannot be blank") @Schema(description = "Unique identifier for the widget type", example = "google-calendar-widget") String widgetType,

                @NotBlank(message = "Widget version cannot be blank") @Schema(description = "Version of the widget", example = "1.0.0") String version,

                @NotNull(message = "Configuration cannot be null") @Schema(description = "Configuration values for this widget instance") Map<String, Object> configuration,

                @Valid @Schema(description = "Metadata describing configuration structure and constraints") List<ConfigurationModelItem> configurationModel,

                @Schema(description = "Optional custom backend endpoints for widget data management") List<DataEndpointModelItem> dataEndpoints,

                @Schema(description = "Optional parent widget ID", example = "550e8400-e29b-41d4-a716-446655440000", nullable = true) String parentId) {
}