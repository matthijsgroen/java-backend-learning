package nl.kabisa.dashboarding.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login response containing the JWT access token")
public record LoginResponse(

        @Schema(description = "Signed JWT bearer token",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        String token
) {}
