package poker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * A simple WebSocket client wrapper using Java's native HttpClient.
 * Handles the asynchronous connection, message sending, and receiving with JWT authentication.
 */
public class WebSocketClient {

    private WebSocket webSocket;
    private final Queue<String> pendingMessages = new ConcurrentLinkedQueue<>();
    private volatile boolean connected = false;
    @SuppressWarnings("unused")
    private final Consumer<String> onMessageReceived;

    /**
     * Initializes the client and connects to the WebSocket server asynchronously with JWT token.
     *
     * @param url               The WebSocket URL (e.g., "ws://localhost:8080/ws")
     * @param authToken         JWT authentication token
     * @param onMessageReceived A callback function that triggers whenever a message is received from the server.
     */
    public WebSocketClient(String url, String authToken, Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
        
        HttpClient client = HttpClient.newHttpClient();
        
        // Build the WebSocket connection with Authorization header
        CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
                .header("Authorization", "Bearer " + authToken)
                .buildAsync(URI.create(url), new WebSocket.Listener() {
                    
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("CONNECTED to WebSocket at " + url);
                        connected = true;
                        String queued;
                        while ((queued = pendingMessages.poll()) != null) {
                            webSocket.sendText(queued, true);
                        }
                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        System.out.println("RECEIVED: " + data);
                        // Trigger the callback function provided by GameScreen
                        onMessageReceived.accept(data.toString());
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }
                    
                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        System.err.println("WebSocket Error: " + error.getMessage());
                    }
                });
        
        // Save the active WebSocket instance when connection completes
        wsFuture.thenAccept(ws -> this.webSocket = ws);
    }

    /**
     * Sends a text message (JSON) to the server.
     *
     * @param message The JSON string to send.
     */
    public void sendMessage(String message) {
        if (webSocket != null && connected) {
            System.out.println("SENDING: " + message);
            webSocket.sendText(message, true);
        } else {
            pendingMessages.offer(message);
            System.err.println("WebSocket is not connected yet! Message queued.");
        }
    }

    /**
     * Close the WebSocket connection.
     */
    public void close() {
        try {
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "client leaving");
            }
        } catch (Exception e) {
            System.err.println("WebSocket close error: " + e.getMessage());
        }
    }
}