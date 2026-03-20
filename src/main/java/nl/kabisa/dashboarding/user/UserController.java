package nl.kabisa.dashboarding.user;

import jakarta.validation.Valid;
import nl.kabisa.dashboarding.user.orm.Role;
import nl.kabisa.dashboarding.user.orm.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @GetMapping
    public ResponseEntity<List<UserListResponse>> listUsers() {
        List<UserListResponse> users = userService.getAllUsers().stream()
                .map(u -> new UserListResponse(
                        u.getId().toString(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getRole(),
                        u.isEnabled(),
                        u.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<UserProfileResponse> approveUser(@PathVariable UUID id) {
        User user = userService.approveUser(id);
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserProfileResponse> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request,
            Authentication authentication) {
        UUID actingAdminId = UUID.fromString(authentication.getName());
        User user = userService.changeRole(id, request.role(), actingAdminId);
        return ResponseEntity.ok(toProfileResponse(user));
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt());
    }
}
