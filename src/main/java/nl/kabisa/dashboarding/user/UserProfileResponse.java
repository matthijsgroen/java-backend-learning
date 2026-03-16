package nl.kabisa.dashboarding.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "User profile response")
public record UserProfileResponse(
        @Schema(description = "Unique identifier of the user",
                example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Username", example = "johndoe")
        String username,

        @Schema(description = "Email address", example = "john@example.com")
        String email,

        @Schema(description = "Account creation timestamp", example = "2026-03-16T10:30:00")
        LocalDateTime createdAt) {
}
