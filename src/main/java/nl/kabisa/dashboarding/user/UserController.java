package nl.kabisa.dashboarding.user;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nl.kabisa.dashboarding.user.orm.User;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<RegisterUserResponse> register(
            @Valid @RequestBody RegisterUserRequest request) {
        User user = userService.register(
                request.username(),
                request.email(),
                request.password());

        RegisterUserResponse response = new RegisterUserResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        User user = userService.getUserById(userId);
        UserProfileResponse response = new UserProfileResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt());
        return ResponseEntity.ok(response);
    }
}
