package nl.kabisa.dashboarding.auth;

import nl.kabisa.dashboarding.user.orm.User;
import nl.kabisa.dashboarding.user.orm.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Called by JwtAuthenticationFilter with the userId (UUID string) as the "username".
     * Called by DaoAuthenticationProvider with the literal username string during login.
     * Tries UUID pattern match first (filter path), falls back to username lookup (login path).
     */
    @Override
    public UserDetails loadUserByUsername(String usernameOrId) throws UsernameNotFoundException {
        User user = tryLoadById(usernameOrId)
                .or(() -> userRepository.findByUsername(usernameOrId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrId));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .disabled(!user.isEnabled())
                .build();
    }

    private Optional<User> tryLoadById(String value) {
        if (!UUID_PATTERN.matcher(value).matches()) {
            return Optional.empty();
        }
        return userRepository.findById(UUID.fromString(value));
    }
}
