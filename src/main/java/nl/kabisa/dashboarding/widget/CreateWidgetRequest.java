package nl.kabisa.dashboarding.widget;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Widget creation request following ARCHITECTURE.md format.
 * Accepts nested JSON objects from frontend for flexible schema validation.
 * Uses Object to accept arbitrary JSON structures that can be stored in
 * PostgreSQL JSONB.
 */
@Schema(description = "Widget creation request with configuration format specification")
public record CreateWidgetRequest(
                @NotBlank(message = "Widget class cannot be blank") @Schema(description = "Unique identifier for the widget type", example = "google-calendar-widget") String widgetClass,

                @NotBlank(message = "Widget version cannot be blank") @Schema(description = "Version of the widget", example = "1.0.0") String version,

                @NotNull(message = "Configuration cannot be null") @Schema(description = "Configuration values for this widget instance") Object configuration,

                @NotNull(message = "Configuration model cannot be null") @Schema(description = "Metadata describing configuration structure and constraints") Object configurationModel,

                @Schema(description = "Optional custom backend endpoints for widget data management") Object dataEndpoints) {
}