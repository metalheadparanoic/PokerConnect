package poker;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lobby screen showing available tournaments with modern design.
 */
public class LobbyScreen {
    
    private final Main app;
    private final ServerConnection serverConnection;
    private final Long playerId;
    private final String username;
    private final Integer playerMoney; // Player's total money
    private BorderPane view;
    private TableView<Map<String, Object>> tournamentTable;
    
    public LobbyScreen(Main app, ServerConnection serverConnection, Long playerId, String username) {
        this.app = app;
        this.serverConnection = serverConnection;
        this.playerId = playerId;
        this.username = username;
        this.playerMoney = fetchPlayerMoney(); // Fetch money from server
        createView();
        loadTournaments();
    }
    
    private Integer fetchPlayerMoney() {
        try {
            Map<String, Object> playerData = serverConnection.getPlayer(playerId);
            Object money = playerData.get("money");
            if (money instanceof Number) {
                return ((Number) money).intValue();
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch player money: " + e.getMessage());
        }
        return 0;
    }
    
    private void createView() {
        view = new BorderPane();
        view.setStyle("-fx-background: linear-gradient(to bottom, #0f2606, #1a3010, #0d1808);");
        
        // Top bar with gradient and card suits
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(25));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle(
            "-fx-background: linear-gradient(to right, #1a3010, #2d5a20, #1a3010);" +
            "-fx-border-color: #ffd700;" +
            "-fx-border-width: 0 0 3 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 2);"
        );
        
        // Welcome section with icon
        VBox welcomeBox = new VBox(5);
        Label welcomeLabel = new Label("👋 Welcome, " + username + "!");
        welcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        welcomeLabel.setStyle("-fx-text-fill: #ffd700;");
        
        Label playerIdLabel = new Label("Player ID: #" + playerId);
        playerIdLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        playerIdLabel.setStyle("-fx-text-fill: #90EE90;");
        
        welcomeBox.getChildren().addAll(welcomeLabel, playerIdLabel);
        
        // Money display with enhanced styling
        VBox moneyBox = new VBox(5);
        moneyBox.setAlignment(Pos.CENTER);
        moneyBox.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.4);" +
            "-fx-background-radius: 15;" +
            "-fx-padding: 12 20;" +
            "-fx-border-color: #4CAF50;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 15;"
        );
        
        Label balanceTitle = new Label("💰 BALANCE");
        balanceTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        balanceTitle.setStyle("-fx-text-fill: #90EE90;");
        
        Label moneyLabel = new Label("$" + String.format("%,d", playerMoney));
        moneyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        moneyLabel.setStyle("-fx-text-fill: #FFD700;");
        
        moneyBox.getChildren().addAll(balanceTitle, moneyLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Logout button with hover effect
        Button logoutButton = new Button("🚪 Logout");
        logoutButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        logoutButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #f44336, #d32f2f);" +
            "-fx-text-fill: white;" +
            "-fx-padding: 12 25;" +
            "-fx-background-radius: 20;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(244, 67, 54, 0.4), 8, 0, 0, 0);"
        );
        logoutButton.setOnMouseEntered(e -> 
            logoutButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ff5252, #f44336);" +
                "-fx-text-fill: white;" +
                "-fx-padding: 12 25;" +
                "-fx-background-radius: 20;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(244, 67, 54, 0.7), 12, 0, 0, 0);"
            )
        );
        logoutButton.setOnMouseExited(e -> 
            logoutButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #f44336, #d32f2f);" +
                "-fx-text-fill: white;" +
                "-fx-padding: 12 25;" +
                "-fx-background-radius: 20;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(244, 67, 54, 0.4), 8, 0, 0, 0);"
            )
        );
        logoutButton.setOnAction(e -> app.showLoginScreen());
        
        topBar.getChildren().addAll(welcomeBox, spacer, moneyBox, logoutButton);
        
        // Center - Tournament list in a card
        VBox centerContainer = new VBox(25);
        centerContainer.setPadding(new Insets(30));
        centerContainer.setAlignment(Pos.TOP_CENTER);
        
        // Title section with poker suits
        VBox titleSection = new VBox(10);
        titleSection.setAlignment(Pos.CENTER);
        
        Label suitsLabel = new Label("♠ ♥ ♦ ♣");
        suitsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        suitsLabel.setStyle("-fx-text-fill: #ffd700;");
        
        Label title = new Label("TOURNAMENT LOBBY");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setStyle(
            "-fx-text-fill: #ffd700;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.6), 10, 0, 0, 0);"
        );
        
        Label subtitle = new Label("Select a tournament to join or create your own");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        subtitle.setStyle("-fx-text-fill: #90EE90; -fx-font-style: italic;");
        
        titleSection.getChildren().addAll(suitsLabel, title, subtitle);
        
        // Tournament table with enhanced styling
        VBox tableContainer = new VBox(15);
        tableContainer.setStyle(
            "-fx-background-color: rgba(29, 50, 16, 0.9);" +
            "-fx-background-radius: 15;" +
            "-fx-padding: 20;" +
            "-fx-border-color: #ffd700;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 15;"
        );
        
        DropShadow containerShadow = new DropShadow();
        containerShadow.setRadius(20);
        containerShadow.setColor(Color.rgb(0, 0, 0, 0.5));
        tableContainer.setEffect(containerShadow);
        
        Label tableTitle = new Label("🎰 Active Tournaments");
        tableTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        tableTitle.setStyle("-fx-text-fill: #ffd700;");
        
        tournamentTable = new TableView<>();
        tournamentTable.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.3);" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #4CAF50;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;"
        );
        tournamentTable.setPrefHeight(350);
        tournamentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Style table columns
        TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("🏆 Tournament Name");
        nameCol.setCellValueFactory(data -> {
            Object value = data.getValue().get("name");
            return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "");
        });
        nameCol.setMinWidth(180);
        styleColumn(nameCol);
        
        TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("📊 Status");
        statusCol.setCellValueFactory(data -> {
            Object value = data.getValue().get("status");
            return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "");
        });
        statusCol.setMinWidth(100);
        styleColumn(statusCol);
        
        TableColumn<Map<String, Object>, String> playersCol = new TableColumn<>("👥 Players");
        playersCol.setCellValueFactory(data -> {
            Object current = data.getValue().get("currentPlayers");
            Object max = data.getValue().get("maxPlayers");
            return new javafx.beans.property.SimpleStringProperty(
                (current != null ? current.toString() : "0") + " / " + (max != null ? max.toString() : "0")
            );
        });
        playersCol.setMinWidth(90);
        styleColumn(playersCol);
        
        TableColumn<Map<String, Object>, String> buyInCol = new TableColumn<>("💵 Buy-in");
        buyInCol.setCellValueFactory(data -> {
            Object value = data.getValue().get("buyIn");
            return new javafx.beans.property.SimpleStringProperty(value != null ? "$" + value.toString() : "$0");
        });
        buyInCol.setMinWidth(90);
        styleColumn(buyInCol);
        
        tournamentTable.getColumns().add(nameCol);
        tournamentTable.getColumns().add(statusCol);
        tournamentTable.getColumns().add(playersCol);
        tournamentTable.getColumns().add(buyInCol);
        
        tableContainer.getChildren().addAll(tableTitle, tournamentTable);
        
        // Enhanced button bar
        HBox buttonBar = new HBox(15);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));
        
        Button refreshButton = createStyledButton("🔄 Refresh", "#2196F3", "#1976D2");
        refreshButton.setOnAction(e -> loadTournaments());
        
        Button createButton = createStyledButton("✨ Create Tournament", "#4CAF50", "#45a049");
        createButton.setPrefWidth(180);
        createButton.setOnAction(e -> showCreateTournamentDialog());
        
        Button joinButton = createStyledButton("🎮 Join Selected", "#FF9800", "#F57C00");
        joinButton.setOnAction(e -> joinSelectedTournament());
        
        Button historyButton = createStyledButton("📜 History", "#9C27B0", "#7B1FA2");
        historyButton.setOnAction(e -> app.showHistoryScreen());
        
        Button deleteAllButton = createStyledButton("🗑️ Delete All", "#d32f2f", "#b71c1c");
        deleteAllButton.setOnAction(e -> deleteAllTournaments());
        
        buttonBar.getChildren().addAll(refreshButton, createButton, joinButton, historyButton, deleteAllButton);
        
        centerContainer.getChildren().addAll(titleSection, tableContainer, buttonBar);
        
        view.setTop(topBar);
        view.setCenter(centerContainer);
    }
    
    private void styleColumn(TableColumn<Map<String, Object>, String> column) {
        column.setStyle("-fx-alignment: CENTER; -fx-font-size: 13px; -fx-font-weight: bold;");
    }
    
    private Button createStyledButton(String text, String colorFrom, String colorTo) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        button.setPrefWidth(130);
        button.setPrefHeight(40);
        
        String baseStyle = 
            "-fx-background-color: linear-gradient(to bottom, " + colorFrom + ", " + colorTo + ");" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 20;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 2);";
        
        button.setStyle(baseStyle);
        
        button.setOnMouseEntered(e -> 
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " + colorFrom + ", " + colorFrom + ");" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 20;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 12, 0, 0, 3);" +
                "-fx-scale-x: 1.05;" +
                "-fx-scale-y: 1.05;"
            )
        );
        
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
        
        return button;
    }
    
    private void loadTournaments() {
        try {
            List<Map<String, Object>> tournaments = serverConnection.getTournaments();
            tournamentTable.getItems().clear();
            tournamentTable.getItems().addAll(tournaments);
        } catch (Exception e) {
            showError("Failed to load tournaments: " + e.getMessage());
        }
    }
    
    private void showCreateTournamentDialog() {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Create Tournament");
        dialog.setHeaderText("Enter tournament details");
        
        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Tournament Name");
        
        Spinner<Integer> maxPlayersSpinner = new Spinner<>(2, 10, 6);
        Spinner<Integer> buyInSpinner = new Spinner<>(100, 10000, 1000, 100);
        Spinner<Integer> chipsSpinner = new Spinner<>(1000, 100000, 10000, 1000);
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Max Players:"), 0, 1);
        grid.add(maxPlayersSpinner, 1, 1);
        grid.add(new Label("Buy-in:"), 0, 2);
        grid.add(buyInSpinner, 1, 2);
        grid.add(new Label("Starting Chips:"), 0, 3);
        grid.add(chipsSpinner, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Map<String, Object> result = new HashMap<>();
                result.put("name", nameField.getText());
                result.put("maxPlayers", maxPlayersSpinner.getValue());
                result.put("buyIn", buyInSpinner.getValue());
                result.put("startingChips", chipsSpinner.getValue());
                return result;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            try {
                serverConnection.createTournament(
                    (String) result.get("name"),
                    (Integer) result.get("maxPlayers"),
                    (Integer) result.get("buyIn"),
                    (Integer) result.get("startingChips")
                );
                loadTournaments();
                showInfo("Tournament created successfully!");
            } catch (Exception e) {
                showError("Failed to create tournament: " + e.getMessage());
            }
        });
    }
    
    private void joinSelectedTournament() {
        Map<String, Object> selected = tournamentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a tournament to join");
            return;
        }
        
        Number tournamentIdNum = (Number) selected.get("id");
        Long tournamentId = tournamentIdNum.longValue();
        
        try {
            serverConnection.joinTournament(tournamentId, playerId);
            showInfo("Joined tournament successfully!");
            
            // Navigate to game screen
            app.showGameScreen(tournamentId);
            
        } catch (Exception e) {
            showError("Failed to join tournament: " + e.getMessage());
        }
    }
    
    private void deleteAllTournaments() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText("Delete All Tournaments");
        confirmation.setContentText("Are you sure you want to delete all tournaments? This cannot be undone.");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serverConnection.deleteAllTournaments();
                    loadTournaments();
                    showInfo("All tournaments deleted successfully!");
                } catch (Exception e) {
                    showError("Failed to delete tournaments: " + e.getMessage());
                }
            }
        });
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public Parent getView() {
        return view;
    }
}
