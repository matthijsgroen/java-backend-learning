package nl.kabisa.dashboarding.widget.dto;

import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Widget configuration field model")
public record ConfigurationModelItem(
        @NotBlank(message = "Configuration model item ID cannot be blank") @Schema(description = "Unique identifier for the widget type", example = "title") String id,
        @Schema(description = "Type of the configuration field", example = "string") ConfigurationFieldType type,
        @Schema(description = "Scope of the configuration field", example = "string") ConfigurationFieldScope scope,
        @Schema(description = "Type of storage for the field. Default is plain", example = "encrypted") Optional<ConfigurationFieldStorage> storage) {
}