package poker;

import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Login and registration screen with modern poker-themed design.
 */
public class LoginScreen {
    
    private final Main app;
    private final ServerConnection serverConnection;
    private StackPane view;
    
    public LoginScreen(Main app, ServerConnection serverConnection) {
        this.app = app;
        this.serverConnection = serverConnection;
        createView();
    }
    
    private void createView() {
        view = new StackPane();

        java.net.URL cssUrl = getClass().getResource("/styles/login.css");
        if (cssUrl != null) {
            view.getStylesheets().add(cssUrl.toExternalForm());
        }
        
        // Background with gradient
        view.setStyle("-fx-background: linear-gradient(to bottom, #0f2606, #1a3010, #0d1808);");
        
        // Main container with glass effect
        VBox mainContainer = new VBox(30);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(50));
        mainContainer.setMaxWidth(500);
        
        // Card-style container with shadow
        VBox card = new VBox(25);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setStyle(
            "-fx-background-color: rgba(29, 50, 16, 0.95);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: #ffd700;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 20;"
        );
        
        DropShadow cardShadow = new DropShadow();
        cardShadow.setRadius(30);
        cardShadow.setColor(Color.rgb(0, 0, 0, 0.7));
        card.setEffect(cardShadow);
        
        // Poker-themed title with card suits
        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);
        
        Label suitsTop = new Label("♠ ♥ ♦ ♣");
        suitsTop.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        suitsTop.setStyle("-fx-text-fill: #ffd700;");
        
        Label title = new Label("Poker Connect");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setStyle(
            "-fx-text-fill: linear-gradient(to bottom, #ffd700, #ffed4e);" +
            "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.6), 10, 0, 0, 0);"
        );
        title.setWrapText(true);
        title.setMaxWidth(420);
        title.setAlignment(Pos.CENTER);
        
        Label subtitle = new Label("High Stakes • Big Wins");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        subtitle.setStyle("-fx-text-fill: #90EE90; -fx-font-style: italic;");
        
        Label suitsBottom = new Label("♣ ♦ ♥ ♠");
        suitsBottom.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        suitsBottom.setStyle("-fx-text-fill: #ffd700;");
        
        titleBox.getChildren().addAll(suitsTop, title, subtitle, suitsBottom);
        
        // Custom styled tab pane
        TabPane tabPane = new TabPane();
        tabPane.setPrefWidth(260);
        tabPane.setMaxWidth(260);
        tabPane.setMinWidth(260);
        tabPane.setTabMinWidth(120);
        tabPane.setTabMaxWidth(120);
        tabPane.setTabMinHeight(30);
        tabPane.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-tab-min-width: 120;" +
            "-fx-tab-max-width: 120;" +
            "-fx-tab-min-height: 30;"
        );
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Login tab
        Tab loginTab = new Tab("🎰 LOGIN");
        loginTab.setClosable(false);
        loginTab.setContent(createLoginForm());
        loginTab.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 12 12 0 0;" +
            "-fx-focus-color: transparent;" +
            "-fx-faint-focus-color: transparent;"
        );
        
        // Register tab
        Tab registerTab = new Tab("✨ REGISTER");
        registerTab.setClosable(false);
        registerTab.setContent(createRegisterForm());
        registerTab.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 12 12 0 0;" +
            "-fx-focus-color: transparent;" +
            "-fx-faint-focus-color: transparent;"
        );
        
        tabPane.getTabs().addAll(loginTab, registerTab);

        HBox tabContainer = new HBox(tabPane);
        tabContainer.setAlignment(Pos.CENTER);
        
        // Footer info
        Label footerLabel = new Label("Welcome to the Ultimate Poker Experience");
        footerLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        footerLabel.setStyle("-fx-text-fill: #888888;");
        
        card.getChildren().addAll(titleBox, tabContainer, footerLabel);
        mainContainer.getChildren().add(card);
        
        view.getChildren().add(mainContainer);
    }
    
    private Parent createLoginForm() {
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30, 20, 20, 20));
        form.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 10;");
        
        // Username field with icon
        VBox usernameBox = new VBox(8);
        Label usernameLabel = new Label("👤 Username");
        usernameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        usernameLabel.setStyle("-fx-text-fill: #ffd700;");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setMaxWidth(350);
        usernameField.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.9);" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 12;" +
            "-fx-font-size: 14px;" +
            "-fx-border-color: #4CAF50;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;"
        );
        usernameBox.getChildren().addAll(usernameLabel, usernameField);
        
        // Password field with icon
        VBox passwordBox = new VBox(8);
        Label passwordLabel = new Label("🔒 Password");
        passwordLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        passwordLabel.setStyle("-fx-text-fill: #ffd700;");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setMaxWidth(350);
        passwordField.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.9);" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 12;" +
            "-fx-font-size: 14px;" +
            "-fx-border-color: #4CAF50;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;"
        );
        passwordBox.getChildren().addAll(passwordLabel, passwordField);
        
        // Message label
        Label messageLabel = new Label();
        messageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(350);
        messageLabel.setAlignment(Pos.CENTER);
        
        // Login button with glow effect
        Button loginButton = new Button("🎲 LOGIN & PLAY");
        loginButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        loginButton.setPrefWidth(250);
        loginButton.setPrefHeight(45);
        loginButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #4CAF50, #45a049);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 25;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(76, 175, 80, 0.6), 15, 0, 0, 0);"
        );
        
        loginButton.setOnMouseEntered(e -> 
            loginButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #5CBF60, #4CAF50);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 25;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(76, 175, 80, 0.9), 20, 0, 0, 0);" +
                "-fx-scale-x: 1.05;" +
                "-fx-scale-y: 1.05;"
            )
        );
        
        loginButton.setOnMouseExited(e -> 
            loginButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #4CAF50, #45a049);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 25;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(76, 175, 80, 0.6), 15, 0, 0, 0);"
            )
        );
        
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            
            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("⚠ Please fill all fields");
                messageLabel.setStyle("-fx-text-fill: #ff6666;");
                return;
            }
            
            try {
                Map<String, Object> response = serverConnection.login(username, password);
                Number id = (Number) response.get("id");
                String user = (String) response.get("username");
                String token = (String) response.get("token");
                
                serverConnection.setAuthToken(token);
                
                messageLabel.setText("✓ Login successful! Welcome back, " + user + "!");
                messageLabel.setStyle("-fx-text-fill: #66ff66;");
                
                app.showLobbyScreen(id.longValue(), user, token);
                
            } catch (Exception ex) {
                ex.printStackTrace();
                String err = ex.getMessage();
                if (err == null || err.isBlank()) err = ex.toString();
                messageLabel.setText("✗ " + err);
                messageLabel.setStyle("-fx-text-fill: #ff6666;");
            }
        });
        
        // Add enter key support
        passwordField.setOnAction(e -> loginButton.fire());
        
        form.getChildren().addAll(
            usernameBox,
            passwordBox,
            loginButton,
            messageLabel
        );
        
        return form;
    }
    
    private Parent createRegisterForm() {
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30, 20, 20, 20));
        form.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 10;");
        
        // Username field
        VBox usernameBox = new VBox(8);
        Label usernameLabel = new Label("👤 Username");
        usernameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        usernameLabel.setStyle("-fx-text-fill: #ffd700;");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a unique username");
        usernameField.setMaxWidth(350);
        usernameField.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.9);" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 12;" +
            "-fx-font-size: 14px;" +
            "-fx-border-color: #2196F3;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;"
        );
        usernameBox.getChildren().addAll(usernameLabel, usernameField);
        
        // Email field
        VBox emailBox = new VBox(8);
        Label emailLabel = new Label("📧 Email");
        emailLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        emailLabel.setStyle("-fx-text-fill: #ffd700;");
        
        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email address");
        emailField.setMaxWidth(350);
        emailField.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.9);" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 12;" +
            "-fx-font-size: 14px;" +
            "-fx-border-color: #2196F3;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;"
        );
        emailBox.getChildren().addAll(emailLabel, emailField);
        
        // Password field
        VBox passwordBox = new VBox(8);
        Label passwordLabel = new Label("🔒 Password");
        passwordLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        passwordLabel.setStyle("-fx-text-fill: #ffd700;");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Create a secure password");
        passwordField.setMaxWidth(350);
        passwordField.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.9);" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 12;" +
            "-fx-font-size: 14px;" +
            "-fx-border-color: #2196F3;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;"
        );
        passwordBox.getChildren().addAll(passwordLabel, passwordField);
        
        // Message label
        Label messageLabel = new Label();
        messageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(350);
        messageLabel.setAlignment(Pos.CENTER);
        
        // Register button with glow effect
        Button registerButton = new Button("✨ CREATE ACCOUNT");
        registerButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        registerButton.setPrefWidth(250);
        registerButton.setPrefHeight(45);
        registerButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #2196F3, #1976D2);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 25;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(33, 150, 243, 0.6), 15, 0, 0, 0);"
        );
        
        registerButton.setOnMouseEntered(e -> 
            registerButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #42A5F5, #2196F3);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 25;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(33, 150, 243, 0.9), 20, 0, 0, 0);" +
                "-fx-scale-x: 1.05;" +
                "-fx-scale-y: 1.05;"
            )
        );
        
        registerButton.setOnMouseExited(e -> 
            registerButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #2196F3, #1976D2);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 25;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(33, 150, 243, 0.6), 15, 0, 0, 0);"
            )
        );
        
        registerButton.setOnAction(e -> {
            String username = usernameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                messageLabel.setText("⚠ Please fill all fields");
                messageLabel.setStyle("-fx-text-fill: #ff6666;");
                return;
            }
            
            if (!email.contains("@")) {
                messageLabel.setText("⚠ Please enter a valid email address");
                messageLabel.setStyle("-fx-text-fill: #ff6666;");
                return;
            }
            
            if (password.length() < 4) {
                messageLabel.setText("⚠ Password must be at least 4 characters");
                messageLabel.setStyle("-fx-text-fill: #ff6666;");
                return;
            }
            
            try {
                serverConnection.register(username, email, password);
                
                messageLabel.setText("✓ Registration successful! Please login to continue.");
                messageLabel.setStyle("-fx-text-fill: #66ff66;");
                
                usernameField.clear();
                emailField.clear();
                passwordField.clear();
                
            } catch (Exception ex) {
                ex.printStackTrace();
                String err = ex.getMessage();
                if (err == null || err.isBlank()) err = ex.toString();
                messageLabel.setText("✗ " + err);
                messageLabel.setStyle("-fx-text-fill: #ff6666;");
            }
        });
        
        // Add enter key support
        passwordField.setOnAction(e -> registerButton.fire());
        
        form.getChildren().addAll(
            usernameBox,
            emailBox,
            passwordBox,
            registerButton,
            messageLabel
        );
        
        return form;
    }
    
    public Parent getView() {
        return view;
    }
}
