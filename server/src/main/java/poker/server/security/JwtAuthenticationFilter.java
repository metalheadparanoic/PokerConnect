package poker.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter that validates tokens on every request.
 * Extracts JWT from Authorization header and validates it before allowing access.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtService jwtService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Skip authentication for public endpoints
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract token from Authorization header
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                // Validate token
                if (jwtService.validateToken(token)) {
                    // Extract user details
                    Long playerId = jwtService.extractPlayerId(token);
                    String username = jwtService.extractUsername(token);
                    
                    // Create authentication object
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            playerId, // Principal is player ID
                            null, // No credentials needed
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                    
                    // Store username in details for easy access
                    authentication.setDetails(username);
                    
                    // Set authentication in context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // Invalid token - will be handled by Spring Security
                logger.warn("Invalid JWT token: " + e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Check if the endpoint is public (doesn't require authentication).
     */
    private boolean isPublicEndpoint(String path) {
        return path.equals("/api/login") || 
               path.equals("/api/register") ||
               path.startsWith("/ws"); // WebSocket handled separately
    }
}
