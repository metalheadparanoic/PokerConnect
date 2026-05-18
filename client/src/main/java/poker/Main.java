package poker;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the Poker Client application.
 * Manages scene transitions between login, lobby, and game screens.
 */
public class Main extends Application {
    
    private Stage primaryStage;
    private ServerConnection serverConnection;
    private Long playerId;
    private String username;
    private String authToken;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.serverConnection = new ServerConnection();
        
        primaryStage.setTitle("Poker Tournament");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        
        // Start with login screen
        showLoginScreen();
        
        primaryStage.show();
    }
    
    /**
     * Show the login screen.
     */
    public void showLoginScreen() {
        LoginScreen loginScreen = new LoginScreen(this, serverConnection);
        Scene scene = new Scene(loginScreen.getView(), 1200, 800);
        primaryStage.setScene(scene);
    }
    
    /**
     * Show the lobby screen after successful login.
     */
    public void showLobbyScreen(Long playerId, String username, String token) {
        this.playerId = playerId;
        this.username = username;
        this.authToken = token;
        
        LobbyScreen lobbyScreen = new LobbyScreen(this, serverConnection, playerId, username);
        Scene scene = new Scene(lobbyScreen.getView(), 1200, 800);
        primaryStage.setScene(scene);
    }
    
    /**
     * Show the game screen when joining a tournament.
     */
    public void showGameScreen(Long tournamentId) {
        GameScreen gameScreen = new GameScreen(this, serverConnection, playerId, username, tournamentId, authToken);
        Scene scene = new Scene(gameScreen.getView(), 1200, 800);
        primaryStage.setScene(scene);
    }
    
    /**
     * Show the tournament history screen.
     */
    public void showHistoryScreen() {
        HistoryScreen historyScreen = new HistoryScreen(this, serverConnection, playerId, username);
        Scene scene = new Scene(historyScreen.getView(), 1200, 800);
        primaryStage.setScene(scene);
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
