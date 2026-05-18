package poker.server.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poker.server.model.PlayerEntity;
import poker.server.repository.PlayerRepository;
import poker.server.security.JwtService;

/**
 * REST controller for authentication endpoints (register and login).
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private PlayerRepository playerRepository;
    
    @Autowired
    private JwtService jwtService;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 6;
    
    /**
     * Validate username format.
     */
    private String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "Username is required";
        }
        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            return "Username must be between " + MIN_USERNAME_LENGTH + " and " + MAX_USERNAME_LENGTH + " characters";
        }
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            return "Username can only contain letters, numbers, underscores, and hyphens";
        }
        return null;
    }
    
    /**
     * Validate email format.
     */
    private String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Invalid email format";
        }
        return null;
    }
    
    /**
     * Validate password strength.
     */
    private String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        }
        return null;
    }
    
    /**
     * Register a new player.
     * POST /api/register
     * Body: { "username": "user", "email": "user@email.com", "password": "pass" }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            
            // Validate username
            String usernameError = validateUsername(username);
            if (usernameError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", usernameError));
            }
            
            // Validate email
            String emailError = validateEmail(email);
            if (emailError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", emailError));
            }
            
            // Validate password
            String passwordError = validatePassword(password);
            if (passwordError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", passwordError));
            }
            
            // Check if user already exists
            if (playerRepository.existsByUsername(username)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            }
            
            if (playerRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }
            
            // Hash password using BCrypt
            String passwordHash = passwordEncoder.encode(password);
            
            // Create new player
            PlayerEntity player = new PlayerEntity(username, email, passwordHash);
            player = playerRepository.save(player);
            
            // Generate JWT token
            String token = jwtService.generateToken(player.getId(), player.getUsername());
            
            // Return success with player info (excluding password)
            Map<String, Object> response = new HashMap<>();
            response.put("id", player.getId());
            response.put("username", player.getUsername());
            response.put("email", player.getEmail());
            response.put("token", token);
            response.put("message", "Registration successful");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
    
    /**
     * Login a player.
     * POST /api/login
     * Body: { "username": "user", "password": "pass" }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            // Validate input
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }
            
            // Find player
            PlayerEntity player = playerRepository.findByUsername(username).orElse(null);
            
            if (player == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
            }
            
            // Verify password using BCrypt
            if (!passwordEncoder.matches(password, player.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
            }
            // PROTECȚIE LA FALIMENT
            // Dacă jucătorul a pierdut tot, îi resetăm chipurile la 1000 la logare
            if (player.getChips() < 1000) {
                player.setChips(1000);
                playerRepository.save(player);
            }
            
            // Generate JWT token
            String token = jwtService.generateToken(player.getId(), player.getUsername());
            
            // Return success with player info and token
            Map<String, Object> response = new HashMap<>();
            response.put("id", player.getId());
            response.put("username", player.getUsername());
            response.put("email", player.getEmail());
            response.put("totalScore", player.getTotalScore());
            response.put("tournamentsWon", player.getTournamentsWon());
            response.put("token", token);
            response.put("message", "Login successful");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Login failed: " + e.getMessage()));
        }
    }
    
    /**
     * Get player by ID.
     * GET /api/players/{id}
     */
    @GetMapping("/players/{id}")
    public ResponseEntity<?> getPlayer(@PathVariable Long id) {
        try {
            PlayerEntity player = playerRepository.findById(id).orElse(null);
            
            if (player == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", player.getId());
            response.put("username", player.getUsername());
            response.put("email", player.getEmail());
            response.put("totalScore", player.getTotalScore());
            response.put("tournamentsWon", player.getTournamentsWon());
            response.put("chips", player.getChips());
            response.put("createdAt", player.getCreatedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get player: " + e.getMessage()));
        }
    }
    
}
