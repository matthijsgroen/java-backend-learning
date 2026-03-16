package nl.kabisa.dashboarding.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User registration request")
public record RegisterUserRequest(
        @NotBlank(message = "Username cannot be blank")
        @Schema(description = "Unique username", example = "johndoe")
        String username,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email must be a valid email address")
        @Schema(description = "Unique email address", example = "john@example.com")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Schema(description = "Plain-text password (will be hashed)", example = "s3cureP@ss!")
        String password) {
}
