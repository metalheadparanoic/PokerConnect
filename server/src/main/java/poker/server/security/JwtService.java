package poker.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for JWT token generation and validation.
 */
@Service
public class JwtService {
    
    // FOLOSIM O CHEIE FIXA PENTRU DEVELOPMENT (Minim 32 caractere)
    private static final String SECRET_STRING = "cheie_secreta_super_sigura_pentru_poker_app_dev_2024";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
    
    private static final long EXPIRATION_TIME = 86400000; // 24 hours
    
    /**
     * Generate JWT token for a player.
     */
    public String generateToken(Long playerId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("playerId", playerId);
        claims.put("username", username);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }
    
    /**
     * Extract player ID from token.
     */
    public Long extractPlayerId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("playerId", Long.class);
    }
    
    /**
     * Extract username from token.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }
    
    /**
     * Validate token.
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extract all claims from token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Check if token is expired.
     */
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}