package nl.kabisa.dashboarding.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private static final int MIN_HMAC_KEY_BYTES = 32; // 256-bit key for HS256

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(JwtProperties jwtProperties) {
        String secret = jwtProperties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured. Please set a Base64-encoded secret with at least "
                    + MIN_HMAC_KEY_BYTES + " bytes (256 bits) for HS256.");
        }

        final byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT secret is not valid Base64. Please provide a valid Base64-encoded secret.", e);
        }

        if (keyBytes.length < MIN_HMAC_KEY_BYTES) {
            throw new IllegalStateException("JWT secret is too short: decoded length is " + keyBytes.length
                    + " bytes, but at least " + MIN_HMAC_KEY_BYTES + " bytes (256 bits) are required for HS256.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = jwtProperties.expirationMs();
    }

    public String generateToken(UUID userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses the token once and returns the subject (userId) if the token is valid,
     * or empty if the token is invalid or expired.
     */
    public Optional<String> extractSubjectIfValid(String token) {
        try {
            return Optional.of(parseClaims(token).getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
