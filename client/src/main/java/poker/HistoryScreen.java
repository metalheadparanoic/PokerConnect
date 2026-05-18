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

import java.util.List;
import java.util.Map;

/**
 * History screen showing finished tournaments with modern design.
 */
public class HistoryScreen {
    
    private final Main app;
    private final ServerConnection serverConnection;
    private final Long playerId;
    private final String username;
    private BorderPane view;
    private TableView<Map<String, Object>> historyTable;
    
    public HistoryScreen(Main app, ServerConnection serverConnection, Long playerId, String username) {
        this.app = app;
        this.serverConnection = serverConnection;
        this.playerId = playerId;
        this.username = username;
        createView();
        loadHistory();
    }
    
    private void createView() {
        view = new BorderPane();
        view.setStyle("-fx-background: linear-gradient(to bottom, #0f2606, #1a3010, #0d1808);");
        
        // Top bar with gradient
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(25));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle(
            "-fx-background: linear-gradient(to right, #1a3010, #2d5a20, #1a3010);" +
            "-fx-border-color: #ffd700;" +
            "-fx-border-width: 0 0 3 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 2);"
        );
        
        Label titleLabel = new Label("📜 Tournament History");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #ffd700;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button backButton = new Button("← Back to Lobby");
        backButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        backButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #2196F3, #1976D2);" +
            "-fx-text-fill: white;" +
            "-fx-padding: 12 25;" +
            "-fx-background-radius: 20;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(33, 150, 243, 0.4), 8, 0, 0, 0);"
        );
        backButton.setOnMouseEntered(e -> 
            backButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #42A5F5, #2196F3);" +
                "-fx-text-fill: white;" +
                "-fx-padding: 12 25;" +
                "-fx-background-radius: 20;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(33, 150, 243, 0.7), 12, 0, 0, 0);"
            )
        );
        backButton.setOnMouseExited(e -> 
            backButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #2196F3, #1976D2);" +
                "-fx-text-fill: white;" +
                "-fx-padding: 12 25;" +
                "-fx-background-radius: 20;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(33, 150, 243, 0.4), 8, 0, 0, 0);"
            )
        );
        backButton.setOnAction(e -> app.showLobbyScreen(playerId, username, null));
        
        topBar.getChildren().addAll(titleLabel, spacer, backButton);
        
        // Center container
        VBox centerContainer = new VBox(25);
        centerContainer.setPadding(new Insets(30));
        centerContainer.setAlignment(Pos.TOP_CENTER);
        
        // Title section with poker suits
        VBox titleSection = new VBox(10);
        titleSection.setAlignment(Pos.CENTER);
        
        Label suitsLabel = new Label("♠ ♥ ♦ ♣");
        suitsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        suitsLabel.setStyle("-fx-text-fill: #ffd700;");
        
        Label subtitle = new Label("FINISHED TOURNAMENTS");
        subtitle.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        subtitle.setStyle(
            "-fx-text-fill: #ffd700;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.6), 10, 0, 0, 0);"
        );
        
        Label description = new Label("View past tournament results and winners");
        description.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        description.setStyle("-fx-text-fill: #90EE90; -fx-font-style: italic;");
        
        titleSection.getChildren().addAll(suitsLabel, subtitle, description);
        
        // History table container
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
        
        Label tableTitle = new Label("🏆 Tournament Results");
        tableTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        tableTitle.setStyle("-fx-text-fill: #ffd700;");
        
        // History table
        historyTable = new TableView<>();
        historyTable.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.3);" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #4CAF50;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;"
        );
        historyTable.setPrefHeight(400);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        
        TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("🎰 Tournament");
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
        
        TableColumn<Map<String, Object>, String> winnerCol = new TableColumn<>("👑 Winner");
        winnerCol.setCellValueFactory(data -> {
            Object value = data.getValue().get("winnerName");
            return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "N/A");
        });
        winnerCol.setMinWidth(120);
        styleColumn(winnerCol);
        
        TableColumn<Map<String, Object>, String> prizeCol = new TableColumn<>("💰 Prize");
        prizeCol.setCellValueFactory(data -> {
            Object value = data.getValue().get("prizeAmount");
            return new javafx.beans.property.SimpleStringProperty(value != null ? "$" + value.toString() : "$0");
        });
        prizeCol.setMinWidth(100);
        styleColumn(prizeCol);
        
        TableColumn<Map<String, Object>, String> buyInCol = new TableColumn<>("💵 Buy-in");
        buyInCol.setCellValueFactory(data -> {
            Object value = data.getValue().get("buyIn");
            return new javafx.beans.property.SimpleStringProperty(value != null ? "$" + value.toString() : "$0");
        });
        buyInCol.setMinWidth(90);
        styleColumn(buyInCol);
        
        TableColumn<Map<String, Object>, String> playersCol = new TableColumn<>("👥 Players");
        playersCol.setCellValueFactory(data -> {
            Object value = data.getValue().get("maxPlayers");
            return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "0");
        });
        playersCol.setMinWidth(80);
        styleColumn(playersCol);
        
        historyTable.getColumns().add(nameCol);
        historyTable.getColumns().add(statusCol);
        historyTable.getColumns().add(winnerCol);
        historyTable.getColumns().add(prizeCol);
        historyTable.getColumns().add(buyInCol);
        historyTable.getColumns().add(playersCol);
        
        tableContainer.getChildren().addAll(tableTitle, historyTable);
        
        // Button bar
        HBox buttonBar = new HBox(15);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));
        
        Button refreshButton = createStyledButton("🔄 Refresh", "#2196F3", "#1976D2");
        refreshButton.setOnAction(e -> loadHistory());
        
        buttonBar.getChildren().add(refreshButton);
        
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
        button.setPrefWidth(140);
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
    
    private void loadHistory() {
        try {
            List<Map<String, Object>> history = serverConnection.getTournamentHistory();
            historyTable.getItems().clear();
            historyTable.getItems().addAll(history);
        } catch (Exception e) {
            showError("Failed to load tournament history: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public Parent getView() {
        return view;
    }
}
