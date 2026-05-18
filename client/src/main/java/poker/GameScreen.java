package poker;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Main poker game screen. Handles the UI layout, WebSocket connection, and
 * real-time game updates.
 */
public class GameScreen {

    private final Main app;
    private final ServerConnection serverConnection;
    private final Long playerId;
    private final String username;
    private final Long tournamentId;
    private final String authToken;
    private BorderPane view;

    // WebSocket helper and JSON parser
    private WebSocketClient wsClient;
    private final Gson gson = new Gson();

    // UI Components
    private Label potLabel;
    private Label currentBetLabel;
    private Label phaseLabel;
    private Label turnIndicatorLabel;
    private Label myChipsLabel;
    private Label myBetLabel;
    private Label timerLabel;
    private HBox communityCardsBox;
    private HBox playerHandBox;
    private HBox readyButtonBox;
    private Label winnerLabel;
    private Pane tablePane;
    private List<PlayerAvatar> playerAvatars = new ArrayList<>();

    // Container for game controls (Action buttons + Raise slider)
    private VBox controlPanel;

    // Action buttons
    private Button foldButton;
    private Button checkButton;
    private Button callButton;
    private Button raiseButton;
    private Button allInButton;
    
    // Raise slider
    private Slider raiseSlider;
    private Label raiseAmountLabel;

    private Button readyButton;
    private boolean isReady = false;

    // Turn timer
    private Thread timerThread;
    private volatile boolean timerRunning = false;
    
    // Current game state for raise calculations
    private int currentGameBet = 0;
    private int myChipsAmount = 0;

    /**
     * Represents a player avatar on the table.
     */
    private static class PlayerAvatar {

        VBox container;
        Label nameLabel;
        Label chipsLabel;
        Label betLabel;
        Label readyLabel;
        Circle avatar;
        double x, y;
        Long playerId;
        boolean ready;
    }

    /**
     * Constructor: Initializes the game screen and establishes connection.
     */
    public GameScreen(Main app, ServerConnection serverConnection, Long playerId, String username, Long tournamentId, String authToken) {
        this.app = app;
        this.serverConnection = serverConnection;
        this.playerId = playerId;
        this.username = username;
        this.tournamentId = tournamentId;
        this.authToken = authToken;

        createView();
        connectWebSocket();
    }

    /**
     * Establishes the WebSocket connection to the server. Sends the initial
     * CONNECT/JOIN message once connected.
     */
    private void connectWebSocket() {
        // Connect to the server endpoint with JWT authentication
        wsClient = new WebSocketClient("ws://localhost:8081/game", authToken, this::handleMessage);

        // Wait briefly for connection, then send JOIN message
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }

            Map<String, Object> joinMsg = Map.of(
                    "type", "CONNECT",
                    "playerId", playerId,
                    "tournamentId", tournamentId
            );
            wsClient.sendMessage(gson.toJson(joinMsg));
        }).start();
    }

    /**
     * Handles incoming WebSocket messages from the server. Updates the UI on
     * the JavaFX Application Thread.
     */
    private void handleMessage(String message) {
        // UI updates must happen on the JavaFX thread
        Platform.runLater(() -> {
            try {
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                String type = json.get("type").getAsString();

                if ("GAME_STATE".equals(type)) {
                    updateUI(json.get("state").getAsJsonObject());
                } else if ("ERROR".equals(type)) {
                    System.err.println("Server Error: " + json.get("message").getAsString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Creates the main visual layout of the game screen with circular table.
     */
    private void createView() {
        view = new BorderPane();
        view.setStyle("-fx-background: linear-gradient(to bottom, #0a1f08, #1a3010);");

        // Top bar
        createTopBar();
        
        // Center - Table and cards
        StackPane center = createCenterArea();
        
        // Right side - Control panel
        VBox rightPanel = createControlPanel();
        
        view.setTop(topBar);
        view.setCenter(center);
        view.setRight(rightPanel);
    }
    
    private HBox topBar;
    
    private void createTopBar() {
        topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle(
            "-fx-background: linear-gradient(to right, #1a3010, #2d5a20, #1a3010);" +
            "-fx-border-color: #ffd700;" +
            "-fx-border-width: 0 0 2 0;"
        );

        Label titleLabel = new Label("🎰 Tournament #" + tournamentId);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setStyle("-fx-text-fill: #ffd700;");

        turnIndicatorLabel = new Label("");
        turnIndicatorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        turnIndicatorLabel.setStyle("-fx-text-fill: #FFD700;");

        timerLabel = new Label("");
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        timerLabel.setStyle("-fx-text-fill: #FF6347;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Ready button (WAITING phase) - placed in the top bar, left of the balance label
        readyButton = new Button("✓ Ready");
        readyButton.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        readyButton.setStyle(
            "-fx-background-color: #4CAF50;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 8 18;" +
            "-fx-background-radius: 18;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 0, 2);"
        );
        readyButton.setOnAction(e -> toggleReady());

        readyButtonBox = new HBox();
        readyButtonBox.setAlignment(Pos.CENTER);
        readyButtonBox.getChildren().add(readyButton);
        readyButtonBox.setVisible(false);
        readyButtonBox.setManaged(false);

        myChipsLabel = new Label("💰 $0");
        myChipsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        myChipsLabel.setStyle(
            "-fx-text-fill: #90EE90;" +
            "-fx-background-color: rgba(0,0,0,0.4);" +
            "-fx-padding: 8 15;" +
            "-fx-background-radius: 15;"
        );

        Button leaveButton = new Button("✕ Leave");
        leaveButton.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        leaveButton.setStyle(
            "-fx-background-color: #d32f2f;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 8 18;" +
            "-fx-background-radius: 18;" +
            "-fx-cursor: hand;"
        );
        leaveButton.setOnMouseEntered(e -> leaveButton.setStyle(
            "-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 8 18; -fx-background-radius: 18; -fx-cursor: hand;"
        ));
        leaveButton.setOnMouseExited(e -> leaveButton.setStyle(
            "-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-padding: 8 18; -fx-background-radius: 18; -fx-cursor: hand;"
        ));
        leaveButton.setOnAction(e -> {
            try {
                serverConnection.leaveTournament(tournamentId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            app.showLobbyScreen(playerId, username, "");
        });

        topBar.getChildren().addAll(titleLabel, turnIndicatorLabel, timerLabel, spacer, readyButtonBox, myChipsLabel, leaveButton);
    }
    
    private StackPane createCenterArea() {
        StackPane centerStack = new StackPane();
        centerStack.setPadding(new Insets(20));

        // Table pane
        tablePane = new Pane();
        // Give extra vertical room so we can render the player's hand BELOW the bottom seat
        tablePane.setPrefSize(700, 750);
        tablePane.setMaxSize(700, 750);

        // Poker table oval
        Rectangle tableOval = new Rectangle(600, 400);
        tableOval.setArcWidth(120);
        tableOval.setArcHeight(120);
        tableOval.setFill(Color.web("#2d5a20"));
        tableOval.setStroke(Color.web("#ffd700"));
        tableOval.setStrokeWidth(4);
        tableOval.setLayoutX(50);
        tableOval.setLayoutY(75);

        DropShadow tableShadow = new DropShadow();
        tableShadow.setColor(Color.BLACK);
        tableShadow.setRadius(25);
        tableShadow.setOffsetY(8);
        tableOval.setEffect(tableShadow);

        // Pot display
        VBox potDisplay = new VBox(5);
        potDisplay.setAlignment(Pos.CENTER);
        potDisplay.setLayoutX(285);
        potDisplay.setLayoutY(200);
        potDisplay.setStyle(
            "-fx-background-color: rgba(0,0,0,0.7);" +
            "-fx-padding: 12;" +
            "-fx-background-radius: 40;" +
            "-fx-border-color: #ffd700;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 40;"
        );

        Label potTitle = new Label("POT");
        potTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        potTitle.setStyle("-fx-text-fill: #ffd700;");

        potLabel = new Label("$0");
        potLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        potLabel.setStyle("-fx-text-fill: #ffd700;");

        potDisplay.getChildren().addAll(potTitle, potLabel);

        // Community cards
        communityCardsBox = new HBox(8);
        communityCardsBox.setAlignment(Pos.CENTER);
        communityCardsBox.setLayoutX(235);
        communityCardsBox.setLayoutY(300);
        communityCardsBox.setStyle(
            "-fx-background-color: rgba(0,0,0,0.5);" +
            "-fx-padding: 8;" +
            "-fx-background-radius: 8;"
        );

        // Phase label
        phaseLabel = new Label("WAITING");
        phaseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        phaseLabel.setStyle(
            "-fx-text-fill: white;" +
            "-fx-background-color: rgba(0,0,0,0.6);" +
            "-fx-padding: 5 12;" +
            "-fx-background-radius: 12;"
        );
        phaseLabel.setLayoutX(310);
        phaseLabel.setLayoutY(150);

        // Current bet
        currentBetLabel = new Label("Bet: $0");
        currentBetLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        currentBetLabel.setStyle(
            "-fx-text-fill: white;" +
            "-fx-background-color: rgba(0,0,0,0.6);" +
            "-fx-padding: 4 10;" +
            "-fx-background-radius: 10;"
        );
        currentBetLabel.setLayoutX(315);
        currentBetLabel.setLayoutY(390);

        // Player cards (positioned dynamically under the bottom seat in updateUI)
        myBetLabel = new Label("Your Bet: $0");
        myBetLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        myBetLabel.setStyle(
            "-fx-text-fill: #90EE90;" +
            "-fx-background-color: rgba(0,0,0,0.7);" +
            "-fx-padding: 5 12;" +
            "-fx-background-radius: 10;"
        );
        myBetLabel.setLayoutX(285);
        myBetLabel.setLayoutY(560);

        playerHandBox = new HBox(10);
        playerHandBox.setAlignment(Pos.CENTER);
        playerHandBox.setPrefHeight(95);
        playerHandBox.setMaxWidth(200); // Max width for 2 cards
        playerHandBox.setStyle(
            "-fx-background-color: rgba(0,0,0,0.8);" +
            "-fx-padding: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #4CAF50;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 10;"
        );
        playerHandBox.setLayoutX(250);
        playerHandBox.setLayoutY(585);

        // Winner label
        winnerLabel = new Label("");
        winnerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        winnerLabel.setStyle(
            "-fx-text-fill: #FFD700;" +
            "-fx-background-color: rgba(0,0,0,0.9);" +
            "-fx-padding: 18;" +
            "-fx-background-radius: 12;"
        );
        winnerLabel.setLayoutX(250);
        winnerLabel.setLayoutY(250);
        winnerLabel.setVisible(false);

        tablePane.getChildren().addAll(tableOval, potDisplay, communityCardsBox, phaseLabel, currentBetLabel, 
                                       myBetLabel, playerHandBox, winnerLabel);

        // Stack everything
        StackPane.setAlignment(tablePane, Pos.CENTER);

        centerStack.getChildren().addAll(tablePane);
        return centerStack;
    }

    private void positionHandUnderBottomSeat() {
        if (playerAvatars == null || playerAvatars.isEmpty()) {
            return;
        }

        PlayerAvatar bottomAvatar = playerAvatars.stream()
            .filter(a -> a.playerId != null && a.playerId.equals(playerId))
            .findFirst()
            .orElseGet(() -> playerAvatars.stream()
                .max(Comparator.comparingDouble(a -> a.y))
                .orElse(null));

        if (bottomAvatar == null || bottomAvatar.container == null) {
            return;
        }

        double avatarWidth = bottomAvatar.container.getWidth();
        if (avatarWidth <= 0) {
            avatarWidth = bottomAvatar.container.getPrefWidth();
        }

        double avatarHeight = bottomAvatar.container.getHeight();
        if (avatarHeight <= 0) {
            avatarHeight = bottomAvatar.container.prefHeight(-1);
        }

        double bottomCenterX = bottomAvatar.x + (avatarWidth / 2.0);
        double yCursor = bottomAvatar.y + avatarHeight + 10;

        // Ensure we have usable sizes before centering
        if (myBetLabel.isVisible()) {
            myBetLabel.applyCss();
            myBetLabel.autosize();

            double betW = myBetLabel.getWidth();
            if (betW <= 0) {
                betW = myBetLabel.prefWidth(-1);
            }
            double betH = myBetLabel.getHeight();
            if (betH <= 0) {
                betH = myBetLabel.prefHeight(-1);
            }

            myBetLabel.setLayoutX(bottomCenterX - (betW / 2.0));
            myBetLabel.setLayoutY(yCursor);
            yCursor += betH + 6;
        }

        if (playerHandBox.isVisible()) {
            playerHandBox.applyCss();
            playerHandBox.autosize();

            double handW = playerHandBox.getWidth();
            if (handW <= 0) {
                double maxW = playerHandBox.getMaxWidth();
                handW = maxW > 0 ? maxW : playerHandBox.prefWidth(-1);
            }
            double handH = playerHandBox.getHeight();
            if (handH <= 0) {
                handH = playerHandBox.prefHeight(-1);
            }

            playerHandBox.setLayoutX(bottomCenterX - (handW / 2.0));
            playerHandBox.setLayoutY(yCursor);
            yCursor += handH + 10;
        }

        // Ready button is placed in the top bar, not in the table area.
    }
    
    private VBox createControlPanel() {
        controlPanel = new VBox(15);
        controlPanel.setPrefWidth(220);
        controlPanel.setPadding(new Insets(20));
        controlPanel.setAlignment(Pos.TOP_CENTER);
        controlPanel.setStyle(
            "-fx-background-color: rgba(0,0,0,0.7);" +
            "-fx-border-color: #333;" +
            "-fx-border-width: 0 0 0 2;"
        );
        controlPanel.setVisible(false);

        // Title
        Label title = new Label("ACTIONS");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setStyle("-fx-text-fill: #ffd700;");

        // Basic actions
        VBox basicActions = new VBox(10);
        basicActions.setAlignment(Pos.CENTER);

        foldButton = createActionButton("✖ Fold", "#424242");
        foldButton.setOnAction(e -> performAction("FOLD", 0));

        checkButton = createActionButton("✓ Check", "#1976D2");
        checkButton.setOnAction(e -> performAction("CHECK", 0));

        callButton = createActionButton("📞 Call", "#388E3C");
        callButton.setOnAction(e -> performAction("CALL", 0));

        basicActions.getChildren().addAll(foldButton, checkButton, callButton);

        // Separator line
        Region separator = new Region();
        separator.setPrefHeight(2);
        separator.setStyle("-fx-background-color: #555;");

        // Raise section
        VBox raiseSection = new VBox(10);
        raiseSection.setAlignment(Pos.CENTER);

        Label raiseTitle = new Label("RAISE AMOUNT");
        raiseTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        raiseTitle.setStyle("-fx-text-fill: #ffd700;");

        raiseAmountLabel = new Label("$0");
        raiseAmountLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        raiseAmountLabel.setStyle("-fx-text-fill: #FFD700;");

        raiseSlider = new Slider();
        raiseSlider.setMin(0);
        raiseSlider.setMax(1000);
        raiseSlider.setValue(0);
        raiseSlider.setShowTickMarks(false);
        raiseSlider.setShowTickLabels(false);
        raiseSlider.setPrefWidth(180);
        raiseSlider.setStyle(
            "-fx-control-inner-background: #2d5a20;" +
            "-fx-accent: #FFD700;"
        );
        raiseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int amount = newVal.intValue();
            raiseAmountLabel.setText("$" + amount);
        });

        raiseButton = createActionButton("↑ Raise", "#FFA000");
        raiseButton.setOnAction(e -> {
            int amount = (int) raiseSlider.getValue();
            if (amount > 0) {
                performAction("RAISE", amount);
                raiseSlider.setValue(0);
            }
        });

        allInButton = createActionButton("ALL-IN", "#D32F2F");
        allInButton.setOnAction(e -> performAction("ALL_IN", 999999));

        raiseSection.getChildren().addAll(
            raiseTitle,
            raiseAmountLabel,
            raiseSlider,
            raiseButton,
            allInButton
        );

        controlPanel.getChildren().addAll(title, basicActions, separator, raiseSection);
        
        disableAllButtons();
        return controlPanel;
    }
    
    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btn.setPrefWidth(180);
        btn.setPrefHeight(40);
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 0, 2);"
        );
        
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: derive(" + color + ", 20%);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 6, 0, 0, 3);"
        ));
        
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 0, 2);"
        ));
        
        return btn;
    }

    /**
     * Disables all action buttons.
     */
    private void disableAllButtons() {
        if (foldButton != null) foldButton.setDisable(true);
        if (checkButton != null) checkButton.setDisable(true);
        if (callButton != null) callButton.setDisable(true);
        if (raiseButton != null) raiseButton.setDisable(true);
        if (allInButton != null) allInButton.setDisable(true);
        if (raiseSlider != null) raiseSlider.setDisable(true);
    }

    /**
     * Updates button availability based on game state.
     */
    private void updateButtonStates(boolean isMyTurn, int currentBet, int myCurrentBet, int myChips) {
        if (!isMyTurn) {
            disableAllButtons();
            return;
        }

        // Enable all buttons
        foldButton.setDisable(false);
        raiseButton.setDisable(false);
        allInButton.setDisable(false);
        raiseSlider.setDisable(false);

        // Update slider range
        int minRaise = currentBet - myCurrentBet + currentBet;
        raiseSlider.setMin(Math.max(minRaise, 1));
        raiseSlider.setMax(myChips);
        raiseSlider.setValue(minRaise);

        // Check vs Call
        if (currentBet == myCurrentBet) {
            checkButton.setDisable(false);
            callButton.setDisable(true);
        } else {
            checkButton.setDisable(true);
            callButton.setDisable(false);
        }
    }

    /**
     * Sends a player action to the server via WebSocket.
     */
    private void performAction(String action, int amount) {
        Map<String, Object> msg = Map.of(
                "type", "ACTION",
                "action", action,
                "amount", amount
        );
        wsClient.sendMessage(gson.toJson(msg));
    }

    /**
     * Toggle ready status for tournament start.
     */
    private void toggleReady() {
        isReady = !isReady;

        // Update button appearance
        String buttonStyle = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 25; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);";
        if (isReady) {
            readyButton.setText("✓ Ready!");
            readyButton.setStyle(buttonStyle + "-fx-background-color: #FFD700; -fx-text-fill: black;");
        } else {
            readyButton.setText("✓ Ready");
            readyButton.setStyle(buttonStyle + "-fx-background-color: #4CAF50; -fx-text-fill: white;");
        }

        // Send ready status to server
        try {
            String url = "http://localhost:8081/api/tournaments/" + tournamentId + "/ready";
            String json = "{\"playerId\":" + playerId + ",\"ready\":" + isReady + "}";

            new Thread(() -> {
                try {
                    serverConnection.post(url, json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetch ready status for all players from server.
     */
    private void fetchReadyStatus() {
        new Thread(() -> {
            try {
                String url = "http://localhost:8081/api/tournaments/" + tournamentId + "/ready";
                String response = serverConnection.get(url);

                Platform.runLater(() -> {
                    try {
                        JsonArray readyPlayers = JsonParser.parseString(response).getAsJsonArray();

                        // Update each player avatar with ready status
                        for (JsonElement elem : readyPlayers) {
                            JsonObject readyPlayer = elem.getAsJsonObject();
                            long pId = readyPlayer.get("playerId").getAsLong();
                            boolean ready = readyPlayer.get("ready").getAsBoolean();

                            // Find matching avatar and update
                            for (PlayerAvatar avatar : playerAvatars) {
                                if (avatar.playerId == pId) {
                                    avatar.ready = ready;
                                    if (ready) {
                                        avatar.readyLabel.setText("✓ READY");
                                        avatar.readyLabel.setVisible(true);
                                    } else {
                                        avatar.readyLabel.setText("");
                                        avatar.readyLabel.setVisible(false);
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Updates the UI elements based on the received game state JSON. Positions
     * players around circular table and updates all game info.
     */
    private void updateUI(JsonObject state) {
        // Update labels
        String phase = state.get("phase").getAsString();
        phaseLabel.setText(phase);
        potLabel.setText("$" + state.get("pot").getAsInt());
        currentBetLabel.setText("Bet: $" + state.get("currentBet").getAsInt());

        // Show/hide ready button based on phase
        boolean isWaitingPhase = "WAITING".equals(phase);
        readyButtonBox.setVisible(isWaitingPhase);
        readyButtonBox.setManaged(isWaitingPhase);
        readyButton.setVisible(isWaitingPhase);

        // In WAITING, don't show empty hand/bet UI (prevents opaque bars over the player box)
        boolean showHandUi = !isWaitingPhase;
        myBetLabel.setVisible(showHandUi);
        myBetLabel.setManaged(showHandUi);
        playerHandBox.setVisible(showHandUi);
        playerHandBox.setManaged(showHandUi);

        // Current bet label looks like an opaque overlay when waiting
        currentBetLabel.setVisible(!isWaitingPhase);
        currentBetLabel.setManaged(!isWaitingPhase);

        // Show/hide controls
        if (controlPanel != null) {
            controlPanel.setVisible(!isWaitingPhase);
        }

        if (!isWaitingPhase) {
            isReady = false; // Reset ready status when game starts
        }

        // Check if hand just finished and show winner
        // --- FIX DEFINITIV PENTRU JSON NULL ---
        String lastWinner = null;
        try {
            if (state.has("lastWinner") && !state.get("lastWinner").isJsonNull()) {
                lastWinner = state.get("lastWinner").getAsString();
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not read lastWinner: " + e.getMessage());
        }

        if (winnerLabel != null) {
            if (lastWinner != null && !lastWinner.isEmpty()) {
                winnerLabel.setText(lastWinner);
                winnerLabel.setVisible(true);
            } else {
                winnerLabel.setVisible(false);
            }
        }
        // --------------------------------------

        // Update Community Cards
        communityCardsBox.getChildren().clear();
        JsonArray communityCards = state.get("communityCards").getAsJsonArray();
        for (JsonElement card : communityCards) {
            addCardToBox(communityCardsBox, card.getAsJsonObject());
        }

        // Hide the community cards container when it's empty (otherwise it shows as an opaque box)
        boolean showCommunity = !isWaitingPhase && communityCards.size() > 0;
        communityCardsBox.setVisible(showCommunity);
        communityCardsBox.setManaged(showCommunity);

        // Get current player index
        int currentPlayerIndex = state.has("currentPlayerIndex") ? state.get("currentPlayerIndex").getAsInt() : -1;

        // Update Players around table
        JsonArray players = state.get("players").getAsJsonArray();
        int myChips = 0;
        int myCurrentBet = 0;
        boolean isMyTurn = false;

        // Clear old avatars
        playerAvatars.forEach(avatar -> tablePane.getChildren().remove(avatar.container));
        playerAvatars.clear();

        // Position players around oval table
        int numPlayers = players.size();
        double centerX = 350;
        double centerY = 275;
        double radiusX = 320;
        double radiusY = 210;

        // Rotate seating so the local player is always displayed at the bottom.
        int myIndex = -1;
        for (int i = 0; i < numPlayers; i++) {
            JsonObject player = players.get(i).getAsJsonObject();
            long pIdTmp = player.has("playerId") ? player.get("playerId").getAsLong() : player.get("id").getAsLong();
            if (pIdTmp == playerId) {
                myIndex = i;
                break;
            }
        }

        for (int i = 0; i < numPlayers; i++) {
            JsonObject player = players.get(i).getAsJsonObject();

            // --- COD NOU: Verifică dacă e "id" sau "playerId" ---
            long pId;
            if (player.has("playerId")) {
                pId = player.get("playerId").getAsLong();
            } else {
                pId = player.get("id").getAsLong();
            }

            String pName = player.get("username").getAsString();
            int chips = player.get("chips").getAsInt();
            boolean folded = player.get("folded").getAsBoolean();
            boolean eliminated = player.has("eliminated") && player.get("eliminated").getAsBoolean();
            int currentBet = player.get("currentBet").getAsInt();

            // Track my info for button states
            if (pId == playerId) {
                myChips = chips;
                myCurrentBet = currentBet;
                myChipsAmount = chips;
                isMyTurn = (myIndex == currentPlayerIndex) && !eliminated;
                myChipsLabel.setText("💰 $" + chips);
                myBetLabel.setText("Your Bet: $" + currentBet);
                
                // If I'm eliminated, show it
                if (eliminated) {
                    myChipsLabel.setStyle("-fx-text-fill: #ff5252; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 8 15; -fx-background-radius: 20;");
                }
            }

            // Calculate position around oval (local player at bottom, go clockwise)
            int displayIndex = myIndex >= 0 ? (i - myIndex + numPlayers) % numPlayers : i;
            double angle = -Math.PI / 2 + (2 * Math.PI * displayIndex / numPlayers);
            double x = centerX + radiusX * Math.cos(angle);
            double y = centerY - radiusY * Math.sin(angle);

            // Create player avatar
            PlayerAvatar avatar = createPlayerAvatar(pName, chips, currentBet, folded, eliminated,
                    i == currentPlayerIndex, lastWinner != null && lastWinner.contains(pName), pId == playerId);
            avatar.x = x - 50;
            avatar.y = y - 60;
            avatar.playerId = pId;
            avatar.container.setLayoutX(avatar.x);
            avatar.container.setLayoutY(avatar.y);

            playerAvatars.add(avatar);
            tablePane.getChildren().add(avatar.container);
        }

        // --- DETECȚIE ELIMINARE ---
        boolean amIStillInGame = false;
        boolean amIEliminated = false;
        for (JsonElement p : players) {
            JsonObject pl = p.getAsJsonObject();
            // Folosim din nou verificarea sigură de ID
            long pIdCheck = pl.has("playerId") ? pl.get("playerId").getAsLong() : pl.get("id").getAsLong();

            if (pIdCheck == playerId) {
                amIStillInGame = true;
                amIEliminated = pl.has("eliminated") && pl.get("eliminated").getAsBoolean();
                break;
            }
        }

        // Make final copy for use in lambda
        final String finalLastWinner = lastWinner;
        
        // Tournament finished - show winner screen
        if ("FINISHED".equals(phase)) {
            // Check if I'm the winner by looking for my eliminated status
            boolean iAmWinner = !amIEliminated && amIStillInGame;
            
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Tournament Finished");
                
                if (iAmWinner) {
                    alert.setHeaderText("🏆 Congratulations! You Won! 🏆");
                    alert.setContentText(finalLastWinner + "\n\nYou can now leave and collect your winnings.");
                } else {
                    alert.setHeaderText("Tournament Complete");
                    alert.setContentText(finalLastWinner != null ? finalLastWinner : "The tournament has ended.");
                }
                
                alert.showAndWait();
            });
        }
        // If I'm eliminated (but tournament not finished yet)
        else if (amIEliminated && !"WAITING".equals(phase)) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Eliminated");
                alert.setHeaderText("You have been eliminated!");
                alert.setContentText("You ran out of chips. You can leave or watch the rest of the tournament.");
                alert.showAndWait();
            });
        }

        // Fetch and update ready status if in waiting phase
        if (isWaitingPhase) {
            fetchReadyStatus();
        }

        // Update turn indicator
        if (currentPlayerIndex >= 0 && currentPlayerIndex < players.size()) {
            JsonObject currentPlayer = players.get(currentPlayerIndex).getAsJsonObject();
            long currentPId = currentPlayer.get("playerId").getAsLong();
            if (currentPId == playerId) {
                turnIndicatorLabel.setText("🎯 YOUR TURN");
                turnIndicatorLabel.setStyle("-fx-text-fill: #00FF00; -fx-font-weight: bold; -fx-font-size: 18px;");
                startTimer(30);
            } else {
                turnIndicatorLabel.setText("⏳ " + currentPlayer.get("username").getAsString());
                turnIndicatorLabel.setStyle("-fx-text-fill: #FFA500; -fx-font-weight: bold; -fx-font-size: 18px;");
                stopTimer();
            }
        } else {
            turnIndicatorLabel.setText("");
            stopTimer();
        }

        // Update button states
        int gameCurrentBet = state.get("currentBet").getAsInt();
        currentGameBet = gameCurrentBet;
        updateButtonStates(isMyTurn, gameCurrentBet, myCurrentBet, myChips);

        // Update My Hand
        playerHandBox.getChildren().clear();
        for (JsonElement p : players) {
            JsonObject player = p.getAsJsonObject();

            // Verificăm ID-ul (sigur, ca să nu avem erori la parse)
            long pId;
            if (player.has("playerId")) {
                pId = player.get("playerId").getAsLong();
            } else {
                pId = player.get("id").getAsLong();
            }

            // Dacă sunt eu, îmi afișez cărțile
            if (pId == playerId) {
                // LOGICĂ NOUĂ ȘI ROBUSTĂ:

                // Varianta 1: Cărțile sunt direct în rădăcina jucătorului (ex: faza WAITING sau SHOWDOWN)
                if (player.has("cards") && !player.get("cards").isJsonNull() && player.get("cards").isJsonArray() && player.get("cards").getAsJsonArray().size() > 0) {
                    for (JsonElement card : player.get("cards").getAsJsonArray()) {
                        if (card.isJsonObject()) {
                            addCardToBox(playerHandBox, card.getAsJsonObject());
                        }
                    }
                } // Varianta 2: Cărțile sunt în obiectul "hand" (faza de joc activ)
                else if (player.has("hand") && !player.get("hand").isJsonNull()) {
                    JsonObject hand = player.get("hand").getAsJsonObject();

                    // Verificăm dacă avem "cards" (cărțile brute) - Asta e ce trimite serverul acum
                    if (hand.has("cards") && !hand.get("cards").isJsonNull() && hand.get("cards").isJsonArray()) {
                        for (JsonElement card : hand.get("cards").getAsJsonArray()) {
                            addCardToBox(playerHandBox, card.getAsJsonObject());
                        }
                    } // Fallback (pentru siguranță): Verificăm "sortedCards" doar dacă există explicit
                    else if (hand.has("sortedCards") && !hand.get("sortedCards").isJsonNull() && hand.get("sortedCards").isJsonArray()) {
                        for (JsonElement card : hand.get("sortedCards").getAsJsonArray()) {
                            addCardToBox(playerHandBox, card.getAsJsonObject());
                        }
                    }
                }
            }
        }

        // Keep my bet/cards/ready aligned under the bottom seat
        Platform.runLater(this::positionHandUnderBottomSeat);
    }

    /**
     * Creates a player avatar with styling.
     */
    private PlayerAvatar createPlayerAvatar(String name, int chips, int bet, boolean folded, boolean eliminated, boolean isTurn, boolean isWinner, boolean isMe) {
        PlayerAvatar avatar = new PlayerAvatar();

        // Container
        avatar.container = new VBox(5);
        avatar.container.setAlignment(Pos.CENTER);
        avatar.container.setPrefWidth(100);
        
        // Eliminated players get special styling
        String bgColor = eliminated ? "rgba(50,0,0,0.7)" : (isTurn ? "rgba(255,215,0,0.3)" : "rgba(0,0,0,0.7)");
        String borderColor = eliminated ? "#800000" : (isWinner ? "#FFD700" : (isTurn ? "#FFD700" : (isMe ? "#4CAF50" : "#666")));
        
        avatar.container.setStyle("-fx-background-color: " + bgColor
                + "; -fx-padding: 10; -fx-background-radius: 15; -fx-border-color: "
                + borderColor
                + "; -fx-border-width: " + (isWinner || isTurn ? "3" : "2") + "; -fx-border-radius: 15;");

        // Avatar circle
        Color avatarColor = eliminated ? Color.web("#4a0000") : (isMe ? Color.web("#4CAF50") : (folded ? Color.web("#757575") : Color.web("#2196F3")));
        avatar.avatar = new Circle(25);
        avatar.avatar.setFill(avatarColor);
        avatar.avatar.setStroke(Color.WHITE);
        avatar.avatar.setStrokeWidth(2);

        // Name
        avatar.nameLabel = new Label(name + (isMe ? " (You)" : ""));
        avatar.nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        avatar.nameLabel.setStyle("-fx-text-fill: " + (isWinner ? "#FFD700" : "white") + ";");

        // Chips
        avatar.chipsLabel = new Label("💰 $" + chips);
        avatar.chipsLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        avatar.chipsLabel.setStyle("-fx-text-fill: #90EE90;");

        // Ready status label
        avatar.readyLabel = new Label("");
        avatar.readyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        avatar.readyLabel.setStyle("-fx-text-fill: #FFD700;");
        avatar.readyLabel.setVisible(false);

        // Bet
        if (bet > 0) {
            avatar.betLabel = new Label("Bet: $" + bet);
            avatar.betLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
            avatar.betLabel.setStyle("-fx-text-fill: #FFD700;");
            avatar.container.getChildren().addAll(avatar.avatar, avatar.nameLabel, avatar.chipsLabel, avatar.readyLabel, avatar.betLabel);
        } else {
            avatar.container.getChildren().addAll(avatar.avatar, avatar.nameLabel, avatar.chipsLabel, avatar.readyLabel);
        }

        // Folded indicator
        if (folded) {
            Label foldedLabel = new Label("FOLDED");
            foldedLabel.setFont(Font.font("Arial", FontWeight.BOLD, 9));
            foldedLabel.setStyle("-fx-text-fill: #f44336;");
            avatar.container.getChildren().add(foldedLabel);
        }
        
        // Eliminated indicator
        if (eliminated) {
            Label eliminatedLabel = new Label("ELIMINATED");
            eliminatedLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            eliminatedLabel.setStyle("-fx-text-fill: #ff1744;");
            avatar.container.getChildren().add(eliminatedLabel);
        }

        return avatar;
    }

    /**
     * Shows the winner announcement.
     */
    private void showWinner(String winnerInfo) {
        winnerLabel.setText("🏆 " + winnerInfo + " 🏆");
        winnerLabel.setVisible(true);

        // Auto-hide after 5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            Platform.runLater(() -> winnerLabel.setVisible(false));
        }).start();
    }

    /**
     * Helper method to load a card image and add it to the specified container.
     */
    private void addCardToBox(HBox box, JsonObject cardJson) {
        String rank = cardJson.get("rank").getAsString(); // e.g., "ACE"
        String suit = cardJson.get("suit").getAsString(); // e.g., "HEARTS"

        String filename = getCardFilename(rank, suit);

        try {
            // Try to load image resource
            InputStream is = getClass().getResourceAsStream("/cards/" + filename);
            if (is != null) {
                Image img = new Image(is);
                ImageView imgView = new ImageView(img);
                imgView.setFitWidth(70);
                imgView.setPreserveRatio(true);
                box.getChildren().add(imgView);
            } else {
                // Fallback text if image missing
                Label cardLabel = new Label(filename);
                cardLabel.setStyle("-fx-background-color: white; -fx-padding: 5;");
                box.getChildren().add(cardLabel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts Card Rank and Suit into the correct filename. Example: ACE +
     * HEARTS -> "AH.png"
     */
    private String getCardFilename(String rank, String suit) {
        String r = "";
        switch (rank) {
            case "TWO":
                r = "2";
                break;
            case "THREE":
                r = "3";
                break;
            case "FOUR":
                r = "4";
                break;
            case "FIVE":
                r = "5";
                break;
            case "SIX":
                r = "6";
                break;
            case "SEVEN":
                r = "7";
                break;
            case "EIGHT":
                r = "8";
                break;
            case "NINE":
                r = "9";
                break;
            case "TEN":
                r = "10";
                break;
            case "JACK":
                r = "J";
                break;
            case "QUEEN":
                r = "Q";
                break;
            case "KING":
                r = "K";
                break;
            case "ACE":
                r = "A";
                break;
        }

        String s = suit.substring(0, 1); // "H", "D", "C", "S"
        return r + s + ".png";
    }

    /**
     * Returns the root view of this screen.
     */
    public Parent getView() {
        return view;
    }

    /**
     * Start countdown timer.
     */
    private void startTimer(int seconds) {
        stopTimer(); // Stop any existing timer

        timerRunning = true;
        timerThread = new Thread(() -> {
            for (int i = seconds; i >= 0 && timerRunning; i--) {
                final int timeLeft = i;
                Platform.runLater(() -> {
                    if (timeLeft <= 5) {
                        timerLabel.setStyle("-fx-text-fill: #FF0000; -fx-font-weight: bold; -fx-font-size: 20px;");
                    } else if (timeLeft <= 10) {
                        timerLabel.setStyle("-fx-text-fill: #FFA500; -fx-font-weight: bold; -fx-font-size: 18px;");
                    } else {
                        timerLabel.setStyle("-fx-text-fill: #FF6347; -fx-font-weight: bold; -fx-font-size: 16px;");
                    }
                    timerLabel.setText("⏱ " + timeLeft + "s");
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }

            Platform.runLater(() -> timerLabel.setText(""));
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    /**
     * Stop the countdown timer.
     */
    private void stopTimer() {
        timerRunning = false;
        if (timerThread != null) {
            timerThread.interrupt();
        }
        timerLabel.setText("");
    }
}
