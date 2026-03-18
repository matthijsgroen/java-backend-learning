package nl.kabisa.dashboarding.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request")
public record LoginRequest(

        @NotBlank(message = "Username cannot be blank")
        @Schema(description = "Username", example = "johndoe")
        String username,

        @NotBlank(message = "Password cannot be blank")
        @Schema(description = "Password", example = "s3cureP@ss!")
        String password
) {}
