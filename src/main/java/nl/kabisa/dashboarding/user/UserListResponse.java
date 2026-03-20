package nl.kabisa.dashboarding.user;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.kabisa.dashboarding.user.orm.Role;

import java.time.LocalDateTime;

@Schema(description = "User summary in admin user list")
public record UserListResponse(
        @Schema(description = "Unique identifier of the user",
                example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Username", example = "johndoe")
        String username,

        @Schema(description = "Email address", example = "john@example.com")
        String email,

        @Schema(description = "User role", example = "USER")
        Role role,

        @Schema(description = "Whether the account is enabled (approved by admin)", example = "false")
        boolean enabled,

        @Schema(description = "Account creation timestamp", example = "2026-03-16T10:30:00")
        LocalDateTime createdAt) {
}
