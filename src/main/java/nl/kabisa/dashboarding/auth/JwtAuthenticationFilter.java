package nl.kabisa.dashboarding.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Parse the token once; skip authentication if token is invalid/expired.
        Optional<String> userId = jwtService.extractSubjectIfValid(token);
        if (userId.isPresent()) {
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(userId.get());
            } catch (UsernameNotFoundException e) {
                // Valid JWT but user no longer exists in DB (e.g. account was deleted).
                // We intentionally create a stub principal so the request passes the security
                // layer and reaches the controller, which will return 404 via UserNotFoundException.
                //
                // NOTE: Any new endpoint that does NOT perform its own user lookup must explicitly
                // check that the authenticated user still exists, to avoid serving data to
                // deleted accounts.
                userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(userId.get())
                        .password("")
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                        .build();
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
