package nl.kabisa.dashboarding.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import nl.kabisa.dashboarding.user.orm.Role;

@Schema(description = "Request to change a user's role")
public record UpdateRoleRequest(
        @NotNull(message = "Role cannot be null")
        @Schema(description = "New role for the user", example = "ADMIN", allowableValues = {"USER", "ADMIN"})
        Role role
) {
}
