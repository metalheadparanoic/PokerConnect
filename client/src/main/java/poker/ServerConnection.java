package poker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Handles HTTP and WebSocket communication with the server.
 */
public class ServerConnection {
    
    // Match server port configured in server/src/main/resources/application.properties
    private static final String DEFAULT_BASE_URL = "http://localhost:8081";
    private final HttpClient httpClient;
    private final Gson gson;
    private String authToken; // Store JWT token after login

    public static String getBaseUrl() {
        String override = System.getProperty("poker.serverUrl");
        if (override == null || override.isBlank()) {
            override = System.getenv("POKER_SERVER_URL");
        }
        if (override == null || override.isBlank()) {
            override = System.getenv("POKER_SERVER_BASE_URL");
        }

        String base = (override == null || override.isBlank()) ? DEFAULT_BASE_URL : override.trim();
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "http://" + base;
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    public static String getWebSocketUrl(String wsPath) {
        String base = getBaseUrl();
        URI uri = URI.create(base);

        String scheme = uri.getScheme();
        String wsScheme = "https".equalsIgnoreCase(scheme) ? "wss" : "ws";
        String host = uri.getHost();
        int port = uri.getPort();

        if (host == null || host.isBlank()) {
            // Fallback for unusual URIs
            String authority = uri.getAuthority();
            host = authority != null ? authority : "localhost";
            port = -1;
        }

        String authority = (port == -1) ? host : (host + ":" + port);
        String path = (wsPath == null || wsPath.isBlank()) ? "/" : (wsPath.startsWith("/") ? wsPath : "/" + wsPath);
        return wsScheme + "://" + authority + path;
    }

    // Instance wrappers (avoid static lookups from other classes)
    public String getBaseUrlValue() {
        return getBaseUrl();
    }

    public String getWebSocketUrlValue(String wsPath) {
        return getWebSocketUrl(wsPath);
    }
    
    public ServerConnection() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Set the JWT token for authenticated requests.
     */
    public void setAuthToken(String token) {
        this.authToken = token;
    }
    
    /**
     * Register a new player.
     */
    public Map<String, Object> register(String username, String email, String password) throws Exception {
        Map<String, String> requestBody = Map.of(
            "username", username,
            "email", email,
            "password", password
        );
        
        String json = gson.toJson(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/api/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.ConnectException ce) {
            throw new Exception("Cannot connect to server (is the server running on " + getBaseUrl() + "?)", ce);
        }

        if (response.statusCode() >= 400) {
            String body = response.body();
            System.err.println("[ServerConnection] register() received error status: " + response.statusCode());
            System.err.println("[ServerConnection] response body: " + body);
            if (body == null || body.isBlank()) {
                throw new Exception("Registration failed (Status: " + response.statusCode() + ")");
            }
            try {
                Map<String, Object> error = gson.fromJson(body,
                    new TypeToken<Map<String, Object>>(){}.getType());
                if (error == null) {
                    throw new Exception("Registration failed (Status: " + response.statusCode() + ")");
                }
                Object errObj = error.get("error");
                String errorMsg = errObj != null ? String.valueOf(errObj) : null;
                throw new Exception(errorMsg != null ? errorMsg : "Registration failed (Status: " + response.statusCode() + ")");
            } catch (com.google.gson.JsonSyntaxException | ClassCastException ex) {
                throw new Exception("Registration failed (Status: " + response.statusCode() + ")");
            }
        }
        
        return gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
    }
    
    /**
     * Login a player.
     */
    public Map<String, Object> login(String username, String password) throws Exception {
        Map<String, String> requestBody = Map.of(
            "username", username,
            "password", password
        );
        
        String json = gson.toJson(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/api/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.ConnectException ce) {
            throw new Exception("Cannot connect to server (is the server running on " + getBaseUrl() + "?)", ce);
        }

        if (response.statusCode() >= 400) {
            try {
                Map<String, Object> error = gson.fromJson(response.body(), 
                    new TypeToken<Map<String, Object>>(){}.getType());
                String errorMsg = (String) error.get("error");
                throw new Exception(errorMsg != null ? errorMsg : "Login failed");
            } catch (com.google.gson.JsonSyntaxException e) {
                throw new Exception("Login failed (Status: " + response.statusCode() + ")");
            }
        }
        
        return gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
    }
    
    /**
     * Get all tournaments.
     */
    public List<Map<String, Object>> getTournaments() throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/api/tournaments"))
            .GET();
        
        // Add JWT token if available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new Exception("Failed to get tournaments");
        }
        
        return gson.fromJson(response.body(), new TypeToken<List<Map<String, Object>>>(){}.getType());
    }
    
    /**
     * Get tournament history (finished tournaments).
     */
    public List<Map<String, Object>> getTournamentHistory() throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/api/tournaments/history"))
            .GET();
        
        // Add JWT token if available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new Exception("Failed to get tournament history");
        }
        
        return gson.fromJson(response.body(), new TypeToken<List<Map<String, Object>>>(){}.getType());
    }
    
    /**
     * Delete all tournaments.
     */
    public void deleteAllTournaments() throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/api/tournaments/deleteAll"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody());
        
        // Add JWT token if available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new Exception("Failed to delete tournaments");
        }
    }
    
    /**
     * Get player by ID.
     */
    public Map<String, Object> getPlayer(Long playerId) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/api/players/" + playerId))
            .GET();
        
        // Add JWT token if available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new Exception("Failed to get player data");
        }
        
        return gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
    }
    
    /**
     * Create a new tournament.
     */
    public Map<String, Object> createTournament(String name, int maxPlayers, int buyIn, int startingChips) throws Exception {
        Map<String, Object> requestBody = Map.of(
            "name", name,
            "maxPlayers", maxPlayers,
            "buyIn", buyIn,
            "startingChips", startingChips
        );
        
        String json = gson.toJson(requestBody);
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/api/tournaments"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json));
        
        // Add JWT token if available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            Map<String, Object> error = gson.fromJson(response.body(), 
                new TypeToken<Map<String, Object>>(){}.getType());
            throw new Exception((String) error.get("error"));
        }
        
        return gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
    }
    
    /**
     * Join a tournament.
     */
    public Map<String, Object> joinTournament(Long tournamentId, Long playerId) throws Exception {
        Map<String, Long> requestBody = Map.of("playerId", playerId);
        
        String json = gson.toJson(requestBody);
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/api/tournaments/" + tournamentId + "/join"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json));
        
        // Add JWT token if available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            Map<String, Object> error = gson.fromJson(response.body(), 
                new TypeToken<Map<String, Object>>(){}.getType());
            throw new Exception((String) error.get("error"));
        }
        
        return gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
    }

    /**
     * Leave a tournament.
     */
    public Map<String, Object> leaveTournament(Long tournamentId, Long playerId) throws Exception {
        Map<String, Object> requestBody = Map.of("playerId", playerId);
        String json = gson.toJson(requestBody);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/api/tournaments/" + tournamentId + "/leave"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json));
        
        // Add JWT token if available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            try {
                 Map<String, Object> error = gson.fromJson(response.body(), 
                    new TypeToken<Map<String, Object>>(){}.getType());
                 throw new Exception((String) error.get("error"));
            } catch (Exception e) {
                // Fallback daca serverul nu trimite JSON la eroare
                throw new Exception("Failed to leave tournament (Code: " + response.statusCode() + ")");
            }
        }
        
        // Returnam raspunsul serverului (mesajul de succes)
        return gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
    }
    
    /**
     * Generic POST request.
     */
    public String post(String url, String jsonBody) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        
        // Add JWT token if available
        if (authToken != null) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new Exception("POST request failed: " + response.statusCode());
        }
        
        return response.body();
    }
    
    /**
     * Generic GET request.
     */
    public String get(String url) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET();
        
        // Add JWT token if available
        if (authToken != null) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new Exception("GET request failed: " + response.statusCode());
        }
        
        return response.body();
    }
}
