package poker.server.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import poker.server.security.JwtService;

import java.util.List;
import java.util.Map;

/**
 * WebSocket configuration for game communication with JWT authentication.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private GameWebSocketHandler gameWebSocketHandler;
    
    @Autowired
    private JwtService jwtService;
    
    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/game")
            .setAllowedOrigins("*")
            .addInterceptors(new JwtHandshakeInterceptor());
    }
    
    /**
     * Interceptor to validate JWT token during WebSocket handshake.
     */
    private class JwtHandshakeInterceptor implements HandshakeInterceptor {
        
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                     WebSocketHandler wsHandler, Map<String, Object> attributes) {
            // Extract Authorization header
            List<String> authHeaders = request.getHeaders().get("Authorization");
            
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                
                if (authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    
                    try {
                        if (jwtService.validateToken(token)) {
                            Long playerId = jwtService.extractPlayerId(token);
                            String username = jwtService.extractUsername(token);
                            
                            // Store in attributes for use in handler
                            attributes.put("playerId", playerId);
                            attributes.put("username", username);
                            attributes.put("authenticated", true);
                            
                            return true; // Allow connection
                        }
                    } catch (Exception e) {
                        System.err.println("JWT validation failed: " + e.getMessage());
                        return false; // Reject connection
                    }
                }
            }
            
            // For now, allow unauthenticated connections for testing
            // TODO: Change to 'return false;' in production
            System.out.println("Warning: Unauthenticated WebSocket connection allowed");
            return true;
        }
        
        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Exception exception) {
            // No action needed after handshake
        }
    }
}
