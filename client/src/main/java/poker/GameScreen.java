package poker;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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
import javafx.util.Duration;

/**
 * Main poker game screen. Handles the UI layout, WebSocket connection, and
 * real-time game updates.
 */
public class GameScreen {

    private static final Logger log = Logger.getLogger(GameScreen.class.getName());

    private final Main app;
    private final ServerConnection serverConnection;
    private final Long playerId;
    private final String username;
    private final Long tournamentId;
    private final String authToken;
    private BorderPane view;
    // Center stack and optional table background image
    private StackPane centerStack;
    private javafx.scene.image.ImageView tableBackgroundImageView;

    // WebSocket helper and JSON parser
    private WebSocketClient wsClient;
    private final Gson gson = new Gson();

    // UI Components
    private Label potLabel;
    private Label currentBetLabel;
    private Label blindsLabel;
    private Label phaseLabel;
    private Label turnIndicatorLabel;
    private Label myChipsLabel;
    private Label myPositionLabel;
    private Label myBetLabel;
    private Label timerLabel;
    private HBox communityCardsBox;
    private HBox playerHandBox;
    private HBox readyButtonBox;
    private Label winnerLabel;
    private Pane tablePane;
    private Rectangle tableOval;
    private final List<PlayerAvatar> playerAvatars = new ArrayList<>();

    // Container for game controls (Action buttons + Raise slider)
    private VBox controlPanel;

    // Action buttons
    private Button foldButton;
    private Button checkButton;
    private Button callButton;
    private Button raiseButton;
    private Button allInButton;
    
    // Raise controls (chip buttons)
    private Label raiseAmountLabel;
    private Label minBetLabel;
    private Button minBetButton;
    private final int[] chipDenominations = new int[] {5, 10, 20, 50, 100};
    private final int[] chipCounts = new int[chipDenominations.length];
    private final List<Button> chipPlusButtons = new ArrayList<>();
    private final List<Button> chipMinusButtons = new ArrayList<>();
    private final List<Label> chipCountLabels = new ArrayList<>();
    private int availableChips = 0; // updated from updateButtonStates
    private int currentBigBlind = 0;
    private int currentLastRaiseSize = 0;

    private Button readyButton;
    private boolean isReady = false;

    // Turn timer
    private Timeline countdownTimeline;
    
    // Current game state for raise calculations
    private int myCurrentBetAmount = 0;

    // UI locks: prevent clicks during dealing/showdown
    private Integer lastHandNumberSeen = null;
    private boolean lockInputsUntilNextHand = false;
    private long lockInputsUntilMs = 0L;
    private PauseTransition lockReleaseTimer;

    private int lastCommunityCardsCountSeen = -1;

    // Bet chip display cache (keep bets visible during a round)
    private final java.util.Map<Long, Integer> lastBetAmounts = new java.util.HashMap<>();
    private String lastPhaseSeen = null;

    // Cached values for re-enabling buttons after a timed lock
    private boolean cachedIsMyTurn = false;
    private int cachedGameCurrentBet = 0;
    private int cachedMyCurrentBet = 0;
    private int cachedMyChips = 0;
    private boolean cachedIsWaitingPhase = true;

    private boolean eliminatedAlertShown = false;
    private boolean finishedAlertShown = false;

    /**
     * Represents a player avatar on the table.
     */
    private static class PlayerAvatar {

        VBox container;
        Label nameLabel;
        Label chipsLabel;
        Label betChipLabel;
        Label readyLabel;
        Label positionBadge;
        Circle avatar;
        double x, y;
        Long playerId;
    }

    private static int countActivePlayers(JsonArray players) {
        int count = 0;
        for (JsonElement p : players) {
            if (!p.isJsonObject()) continue;
            JsonObject pl = p.getAsJsonObject();
            boolean eliminated = pl.has("eliminated") && !pl.get("eliminated").isJsonNull() && pl.get("eliminated").getAsBoolean();
            if (!eliminated) count++;
        }
        return count;
    }

    private static int nextNonEliminatedIndexAfter(JsonArray players, int startIndex) {
        int n = players.size();
        if (n <= 0) return -1;
        for (int off = 1; off <= n; off++) {
            int idx = (startIndex + off) % n;
            JsonObject pl = players.get(idx).getAsJsonObject();
            boolean eliminated = pl.has("eliminated") && !pl.get("eliminated").isJsonNull() && pl.get("eliminated").getAsBoolean();
            if (!eliminated) return idx;
        }
        return startIndex;
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
        wsClient = new WebSocketClient(serverConnection.getWebSocketUrlValue("/game"), authToken, this::handleMessage);

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
                    log.log(Level.WARNING, "Server Error: {0}", json.get("message").getAsString());
                }
            } catch (com.google.gson.JsonSyntaxException | IllegalStateException e) {
                log.log(Level.WARNING, "Failed to process websocket message", e);
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

        blindsLabel = new Label("Blinds: ?/?");
        blindsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        blindsLabel.setStyle(
            "-fx-text-fill: #ffffff;" +
            "-fx-background-color: rgba(0,0,0,0.35);" +
            "-fx-padding: 6 10;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.2);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;"
        );

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

        myPositionLabel = new Label("");
        myPositionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        myPositionLabel.setStyle(
            "-fx-text-fill: white;" +
            "-fx-background-color: rgba(0,0,0,0.4);" +
            "-fx-padding: 8 12;" +
            "-fx-background-radius: 15;" +
            "-fx-border-color: rgba(255,255,255,0.25);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 15;"
        );
        myPositionLabel.setVisible(false);
        myPositionLabel.setManaged(false);

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
                serverConnection.leaveTournament(tournamentId, playerId);
            } catch (Exception ex) {
                log.log(Level.WARNING, "Failed to leave tournament", ex);
            } finally {
                if (wsClient != null) {
                    wsClient.close();
                }
            }
            app.showLobbyScreen(playerId, username, authToken);
        });

        topBar.getChildren().addAll(titleLabel, turnIndicatorLabel, timerLabel, blindsLabel, spacer, readyButtonBox, myChipsLabel, leaveButton);
    }
    
    private StackPane createCenterArea() {
        this.centerStack = new StackPane();
        centerStack.setPadding(new Insets(20));

        // Prepare optional background image view (loads /images/table.jpg if present)
        tableBackgroundImageView = new ImageView();
        tableBackgroundImageView.setPreserveRatio(false);
        tableBackgroundImageView.setSmooth(true);
        tableBackgroundImageView.setVisible(false);
        tableBackgroundImageView.setManaged(false);
        // fit size will be bound to tablePane once created below

        // Table pane
        tablePane = new Pane();
        // Give extra vertical room so we can render the player's hand BELOW the bottom seat
        tablePane.setPrefSize(860, 880);
        tablePane.setMaxSize(860, 880);

        // Poker table oval
        tableOval = new Rectangle(740, 500);
        tableOval.setArcWidth(120);
        tableOval.setArcHeight(120);
        tableOval.setFill(Color.web("#2d5a20"));
        tableOval.setStroke(Color.web("#ffd700"));
        tableOval.setStrokeWidth(4);
        tableOval.setLayoutX(60);
        tableOval.setLayoutY(95);

        DropShadow tableShadow = new DropShadow();
        tableShadow.setColor(Color.BLACK);
        tableShadow.setRadius(25);
        tableShadow.setOffsetY(8);
        tableOval.setEffect(tableShadow);

        // Pot display
        VBox potDisplay = new VBox(5);
        potDisplay.setAlignment(Pos.CENTER);
        potDisplay.setLayoutX(360);
        potDisplay.setLayoutY(210);
        potDisplay.setStyle(
            "-fx-background-color: rgba(10, 10, 10, 0.78);" +
            "-fx-padding: 14 18;" +
            "-fx-background-radius: 24;" +
            "-fx-border-color: linear-gradient(to right, #ffd700, #ffec8b, #ffd700);" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 24;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 10, 0, 0, 3);"
        );

        Label potTitle = new Label("POT");
        potTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        potTitle.setStyle("-fx-text-fill: #ffd700;");

        potLabel = new Label("$0");
        potLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 26));
        potLabel.setStyle("-fx-text-fill: #ffd700;");

        potDisplay.getChildren().addAll(potTitle, potLabel);

        // Community cards
        communityCardsBox = new HBox(8);
        communityCardsBox.setAlignment(Pos.CENTER);
        // Initial position is overridden dynamically in updateUI to keep it centered.
        communityCardsBox.setLayoutX(310);
        communityCardsBox.setLayoutY(365);
        communityCardsBox.setStyle(
            "-fx-background-color: rgba(0,0,0,0.5);" +
            "-fx-padding: 8;" +
            "-fx-background-radius: 8;"
        );

        // Phase label
        phaseLabel = new Label("WAITING");
        phaseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        phaseLabel.setStyle(
            "-fx-text-fill: #ffffff;" +
            "-fx-background-color: rgba(0,0,0,0.55);" +
            "-fx-padding: 6 14;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(255,255,255,0.2);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0, 0, 2);"
        );
        phaseLabel.setLayoutX(400);
        phaseLabel.setLayoutY(170);

        // Current bet (not shown on table; kept for compatibility)
        currentBetLabel = new Label("Bet: $0");
        currentBetLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        currentBetLabel.setStyle(
            "-fx-text-fill: white;" +
            "-fx-background-color: rgba(0,0,0,0.6);" +
            "-fx-padding: 4 10;" +
            "-fx-background-radius: 10;"
        );
        currentBetLabel.setVisible(false);
        currentBetLabel.setManaged(false);

        // Player cards (displayed in control panel under the All-In button)
        myBetLabel = new Label("Your Bet: $0");
        myBetLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        myBetLabel.setStyle(
            "-fx-text-fill: #90EE90;" +
            "-fx-background-color: rgba(0,0,0,0.7);" +
            "-fx-padding: 5 12;" +
            "-fx-background-radius: 10;"
        );
        myBetLabel.setLayoutX(365);
        myBetLabel.setLayoutY(685);

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
        playerHandBox.setLayoutX(330);
        playerHandBox.setLayoutY(710);

        // Winner label
        winnerLabel = new Label("");
        winnerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        winnerLabel.setStyle(
            "-fx-text-fill: #FFD700;" +
            "-fx-background-color: rgba(0,0,0,0.9);" +
            "-fx-padding: 18;" +
            "-fx-background-radius: 12;"
        );
        winnerLabel.setLayoutX(300);
        winnerLabel.setLayoutY(290);
        winnerLabel.setVisible(false);

        tablePane.getChildren().addAll(tableOval, potDisplay, communityCardsBox, phaseLabel,
                   myBetLabel, winnerLabel);

        // Stack everything
        StackPane.setAlignment(tablePane, Pos.CENTER);

        // Add background image behind the table pane
        centerStack.getChildren().addAll(tableBackgroundImageView, tablePane);
        return centerStack;
    }

    private void positionCommunityCardsInTableCenter() {
        if (communityCardsBox == null || tableOval == null || tablePane == null) {
            return;
        }
        if (!communityCardsBox.isVisible()) {
            return;
        }

        try {
            tablePane.applyCss();
            tablePane.layout();
            communityCardsBox.applyCss();
            communityCardsBox.autosize();
        } catch (Exception ex) {
            // ignore
        }

        double boxW = communityCardsBox.getWidth();
        if (boxW <= 0) {
            boxW = communityCardsBox.prefWidth(-1);
        }
        double boxH = communityCardsBox.getHeight();
        if (boxH <= 0) {
            boxH = communityCardsBox.prefHeight(-1);
        }

        double ovalLeft = tableOval.getLayoutX();
        double ovalTop = tableOval.getLayoutY();
        double ovalW = tableOval.getWidth();
        double ovalH = tableOval.getHeight();

        double centerX = ovalLeft + (ovalW / 2.0);
        double centerY = ovalTop + (ovalH / 2.0);

        // Slightly below true center so it doesn't crowd the pot/phase labels.
        double desiredCenterY = centerY + 35;
        double x = centerX - (boxW / 2.0);
        double y = desiredCenterY - (boxH / 2.0);

        // Keep the community cards well inside the table and away from the bottom seat UI.
        double minY = ovalTop + 190;
        double maxY = ovalTop + ovalH - boxH - 110;
        if (maxY < minY) {
            maxY = minY;
        }
        if (y < minY) y = minY;
        if (y > maxY) y = maxY;

        communityCardsBox.setLayoutX(x);
        communityCardsBox.setLayoutY(y);
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

        // Ensure we have a layout pass so bounds are reliable (prevents 0-height measurements)
        try {
            if (tablePane != null) {
                tablePane.applyCss();
                tablePane.layout();
            }
        } catch (Exception ex) {
            // ignore
        }

        // Ensure the avatar container has been laid out so we can read accurate bounds
        try {
            bottomAvatar.container.applyCss();
            bottomAvatar.container.autosize();
            bottomAvatar.container.layout();
        } catch (Exception ex) {
            // layout may not be available in some headless test contexts; ignore
        }

        // Prefer layout bounds when available; boundsInParent can be 0 before a pulse.
        double avatarWidth = Math.max(
            bottomAvatar.container.getBoundsInParent().getWidth(),
            bottomAvatar.container.getLayoutBounds().getWidth()
        );
        if (avatarWidth <= 0) {
            double prefW = bottomAvatar.container.prefWidth(-1);
            avatarWidth = prefW > 0 ? prefW : bottomAvatar.container.getPrefWidth();
        }

        double avatarHeight = Math.max(
            bottomAvatar.container.getBoundsInParent().getHeight(),
            bottomAvatar.container.getLayoutBounds().getHeight()
        );
        if (avatarHeight <= 0) {
            avatarHeight = bottomAvatar.container.prefHeight(-1);
        }

        double bottomCenterX = bottomAvatar.container.getLayoutX() + (avatarWidth / 2.0);
        double yCursor = bottomAvatar.container.getLayoutY() + avatarHeight + 12; // safe margin

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
            yCursor += betH + 8; // slightly larger spacing to avoid overlap
        }

        if (playerHandBox.isVisible() && playerHandBox.getParent() == tablePane) {
            playerHandBox.applyCss();
            playerHandBox.autosize();

            double handW = playerHandBox.getWidth();
            if (handW <= 0) {
                double maxW = playerHandBox.getMaxWidth();
                handW = maxW > 0 ? maxW : playerHandBox.prefWidth(-1);
            }

            // Ensure the hand box is placed below the bet label and avatar with enough margin
            double targetY = yCursor;
            double minY = bottomAvatar.container.getLayoutY() + avatarHeight + 8;
            if (targetY < minY) targetY = minY + 6;

            playerHandBox.setLayoutX(bottomCenterX - (handW / 2.0));
            playerHandBox.setLayoutY(targetY);
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

        raiseAmountLabel = new Label("Chips: 0");
        raiseAmountLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        raiseAmountLabel.setStyle("-fx-text-fill: #FFD700;");

        minBetLabel = new Label("Min Bet: $0");
        minBetLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        minBetLabel.setStyle("-fx-text-fill: #b0bec5;");

        minBetButton = new Button("Set Min");
        minBetButton.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        minBetButton.setStyle(
            "-fx-background-color: #263238;" +
            "-fx-text-fill: #cfd8dc;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 4 10;" +
            "-fx-cursor: hand;"
        );
        minBetButton.setOnAction(e -> applyMinBetSelection());

        // Chip denomination controls (allow multiple same-chip selection)
        HBox chipsBox = new HBox(8);
        chipsBox.setAlignment(Pos.CENTER);
        String[] colors = new String[] {"#FFFFFF", "#E53935", "#4CAF50", "#3F51B5", "#212121"};
        for (int i = 0; i < chipDenominations.length; i++) {
            int val = chipDenominations[i];

            VBox chipUnit = new VBox(4);
            chipUnit.setAlignment(Pos.CENTER);

            Button chipBtn = new Button(String.valueOf(val));
            chipBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            chipBtn.setStyle(
                "-fx-background-color: " + colors[i] + ";" +
                "-fx-text-fill: " + (colors[i].equals("#FFFFFF") ? "#000000" : "#FFFFFF") + ";" +
                "-fx-background-radius: 20;" +
                "-fx-min-width: 48; -fx-min-height: 36;"
            );
            chipBtn.setDisable(true); // main chip button is decorative

            HBox counterBox = new HBox(4);
            counterBox.setAlignment(Pos.CENTER);

            Button minus = new Button("-");
            minus.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            minus.setPrefSize(28, 28);
            minus.setDisable(true);
            int idx = i;
            minus.setOnAction(e -> {
                if (chipCounts[idx] > 0) chipCounts[idx]--;
                updateSelectedAmountDisplay();
            });

            Label countLabel = new Label("0");
            countLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            countLabel.setStyle("-fx-text-fill: #FFFFFF;");

            Button plus = new Button("+");
            plus.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            plus.setPrefSize(28, 28);
            plus.setDisable(true);
            plus.setOnAction(e -> {
                // Allow increment only if total stays within availableChips
                int totalSelected = computeSelectedAmount();
                if (totalSelected + val <= availableChips) {
                    chipCounts[idx]++;
                    updateSelectedAmountDisplay();
                }
            });

            chipPlusButtons.add(plus);
            chipMinusButtons.add(minus);
            chipCountLabels.add(countLabel);

            counterBox.getChildren().addAll(minus, countLabel, plus);
            chipUnit.getChildren().addAll(chipBtn, counterBox);
            chipsBox.getChildren().add(chipUnit);
        }

        raiseButton = createActionButton("↑ Bet / Raise", "#FFA000");
        raiseButton.setOnAction(e -> {
            int additional = computeSelectedAmount();
            if (additional > 0) {
                int targetTotalBet = myCurrentBetAmount + additional;
                performAction("RAISE", targetTotalBet);
                // clear selections
                for (int j = 0; j < chipCounts.length; j++) chipCounts[j] = 0;
                updateSelectedAmountDisplay();
            }
        });

        allInButton = createActionButton("ALL-IN", "#D32F2F");
        allInButton.setOnAction(e -> performAction("ALL_IN", 999999));

        raiseSection.getChildren().addAll(
            raiseTitle,
            raiseAmountLabel,
            minBetLabel,
            minBetButton,
            chipsBox,
            raiseButton,
            allInButton,
            playerHandBox,
            myPositionLabel
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
        if (chipPlusButtons != null) {
            for (Button t : chipPlusButtons) t.setDisable(true);
        }
        if (chipMinusButtons != null) {
            for (Button t : chipMinusButtons) t.setDisable(true);
        }
    }

    /**
     * Updates button availability based on game state.
     */
    private void updateButtonStates(boolean isMyTurn, int currentBet, int myCurrentBet, int myChips, boolean lockAllInputs) {
        if (lockAllInputs) {
            disableAllButtons();
            return;
        }
        // Always update chip +/- controls so the player can prepare a raise
        // even before it's their turn. Action buttons (fold/call/raise/all-in)
        // remain enabled only when it's the player's turn.
        availableChips = myChips;
        int currentSelected = computeSelectedAmount();
        for (int i = 0; i < chipDenominations.length; i++) {
            int v = chipDenominations[i];
            boolean plusDisabled = (currentSelected + v) > availableChips;
            chipPlusButtons.get(i).setDisable(plusDisabled);
            chipMinusButtons.get(i).setDisable(chipCounts[i] <= 0);
            chipCountLabels.get(i).setText(String.valueOf(chipCounts[i]));
        }

        updateMinBetLabel(currentBet, myCurrentBet);

        // Action buttons enabled only when it's my turn
        if (isMyTurn) {
            foldButton.setDisable(false);
            allInButton.setDisable(false);

            // Check vs Call
            if (currentBet == myCurrentBet) {
                checkButton.setDisable(false);
                callButton.setDisable(true);
            } else {
                checkButton.setDisable(true);
                callButton.setDisable(false);
            }

            // Poker rules (no-limit style): you can bet/raise on your turn.
            // - If you select enough to reach/exceed current bet => valid (call/raise).
            // - If you select less than current bet => only valid if you're all-in (short call).
            int additional = computeSelectedAmount();
            int targetTotalBet = myCurrentBetAmount + additional;
            boolean isAllIn = additional > 0 && additional >= availableChips;
            boolean putsMoreChips = additional > 0 && targetTotalBet > myCurrentBetAmount;
            int minTotalBet = computeMinTotalBet(currentBet, myCurrentBet);
            boolean meetsMin = targetTotalBet >= minTotalBet;
            boolean canBetRaise = putsMoreChips && (meetsMin || isAllIn);
            raiseButton.setDisable(!canBetRaise);
        } else {
            // Not my turn: disable action buttons but keep chip selection available
            foldButton.setDisable(true);
            checkButton.setDisable(true);
            callButton.setDisable(true);
            raiseButton.setDisable(true);
            allInButton.setDisable(true);
        }
    }

    private int computeMinTotalBet(int currentBet, int myCurrentBet) {
        int bb = Math.max(1, currentBigBlind);
        if (currentBet <= 0) {
            return bb;
        }
        int minRaiseSize = Math.max(bb, Math.max(0, currentLastRaiseSize));
        return currentBet + minRaiseSize;
    }

    private void updateMinBetLabel(int currentBet, int myCurrentBet) {
        if (minBetLabel == null) return;
        int minTotalBet = computeMinTotalBet(currentBet, myCurrentBet);
        if (currentBet <= 0) {
            minBetLabel.setText("Min Bet: $" + minTotalBet);
        } else {
            minBetLabel.setText("Min Raise To: $" + minTotalBet);
        }
        if (minBetButton != null) {
            minBetButton.setDisable(minTotalBet <= myCurrentBetAmount || availableChips <= 0);
        }
    }

    /**
     * Sends a player action to the server via WebSocket.
     */
    private void performAction(String action, int amount) {
        if (isInputLockedNow()) {
            return;
        }
        Map<String, Object> msg = Map.of(
                "type", "ACTION",
                "action", action,
                "amount", amount
        );
        wsClient.sendMessage(gson.toJson(msg));
    }

    private boolean isInputLockedNow() {
        if (cachedIsWaitingPhase) return true;
        if (lockInputsUntilNextHand) return true;
        return System.currentTimeMillis() < lockInputsUntilMs;
    }

    private void lockInputsTemporarily(long millis) {
        long until = System.currentTimeMillis() + Math.max(0, millis);
        lockInputsUntilMs = Math.max(lockInputsUntilMs, until);

        // Disable now
        disableAllButtons();

        // Re-enable after delay using cached state (so we don't depend on a new server update)
        if (lockReleaseTimer != null) {
            lockReleaseTimer.stop();
        }
        lockReleaseTimer = new PauseTransition(Duration.millis(Math.max(10, millis)));
        lockReleaseTimer.setOnFinished(e -> {
            boolean locked = isInputLockedNow();
            updateButtonStates(cachedIsMyTurn, cachedGameCurrentBet, cachedMyCurrentBet, cachedMyChips, locked);
        });
        lockReleaseTimer.playFromStart();
    }

    private int computeSelectedAmount() {
        int sum = 0;
        for (int i = 0; i < chipDenominations.length; i++) {
            sum += chipDenominations[i] * chipCounts[i];
        }
        return sum;
    }

    private void applyMinBetSelection() {
        int minTotalBet = computeMinTotalBet(cachedGameCurrentBet, cachedMyCurrentBet);
        int needed = Math.max(0, minTotalBet - myCurrentBetAmount);
        if (needed <= 0) {
            return;
        }
        int remaining = Math.min(needed, availableChips);
        // Clear current selection
        for (int i = 0; i < chipCounts.length; i++) {
            chipCounts[i] = 0;
        }

        // Greedy fill from largest to smallest denomination
        for (int i = chipDenominations.length - 1; i >= 0; i--) {
            int denom = chipDenominations[i];
            if (denom <= 0) continue;
            int count = remaining / denom;
            if (count > 0) {
                chipCounts[i] = count;
                remaining -= count * denom;
            }
        }

        updateSelectedAmountDisplay();
    }

    private void updateSelectedAmountDisplay() {
        int additional = computeSelectedAmount();
        int targetTotalBet = myCurrentBetAmount + additional;
        if (additional <= 0) {
            raiseAmountLabel.setText("Bet/Raise: +0");
        } else {
            raiseAmountLabel.setText("Bet/Raise: +" + additional + " (To: " + targetTotalBet + ")");
        }
        updateMinBetLabel(cachedGameCurrentBet, cachedMyCurrentBet);
        // update labels for counts
        for (int i = 0; i < chipCountLabels.size(); i++) {
            chipCountLabels.get(i).setText(String.valueOf(chipCounts[i]));
        }
        // refresh plus/minus enablement according to availableChips
        int currentSelected = additional;
        for (int i = 0; i < chipDenominations.length; i++) {
            int v = chipDenominations[i];
            if (i < chipPlusButtons.size()) {
                chipPlusButtons.get(i).setDisable((currentSelected + v) > availableChips);
            }
            if (i < chipMinusButtons.size()) {
                chipMinusButtons.get(i).setDisable(chipCounts[i] <= 0);
            }
        }

        // Refresh action button enablement after chip changes
        boolean lockAllInputs = isInputLockedNow();
        updateButtonStates(cachedIsMyTurn, cachedGameCurrentBet, cachedMyCurrentBet, cachedMyChips, lockAllInputs);
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
            String url = serverConnection.getBaseUrlValue() + "/api/tournaments/" + tournamentId + "/ready";
            String json = "{\"playerId\":" + playerId + ",\"ready\":" + isReady + "}";

            new Thread(() -> {
                try {
                    serverConnection.post(url, json);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to send ready status", e);
                }
            }).start();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to toggle ready", e);
        }
    }

    /**
     * Fetch ready status for all players from server.
     */
    private void fetchReadyStatus() {
        new Thread(() -> {
            try {
                String url = serverConnection.getBaseUrlValue() + "/api/tournaments/" + tournamentId + "/ready";
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
                    } catch (com.google.gson.JsonSyntaxException | IllegalStateException e) {
                        log.log(Level.FINE, "Failed to parse/update ready status", e);
                    }
                });
            } catch (Exception e) {
                log.log(Level.FINE, "Failed to fetch ready status", e);
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

        int handNumber = -1;
        if (state.has("handNumber") && !state.get("handNumber").isJsonNull()) {
            try {
                handNumber = state.get("handNumber").getAsInt();
            } catch (Exception ignored) {
            }
        }

        int smallBlindAmount = state.has("smallBlind") ? state.get("smallBlind").getAsInt() : -1;
        int bigBlindAmount = state.has("bigBlind") ? state.get("bigBlind").getAsInt() : -1;
        currentLastRaiseSize = state.has("lastRaiseSize") ? state.get("lastRaiseSize").getAsInt() : 0;
        if (smallBlindAmount > 0 && bigBlindAmount > 0) {
            blindsLabel.setText("Blinds: $" + smallBlindAmount + "/$" + bigBlindAmount);
            currentBigBlind = bigBlindAmount;
        } else {
            blindsLabel.setText("Blinds: ?/?");
            currentBigBlind = 0;
        }

        // Show/hide ready button based on phase
        boolean isWaitingPhase = "WAITING".equals(phase);
        cachedIsWaitingPhase = isWaitingPhase;
        readyButtonBox.setVisible(isWaitingPhase);
        readyButtonBox.setManaged(isWaitingPhase);
        readyButton.setVisible(isWaitingPhase);

        // In WAITING, don't show empty hand/bet UI (prevents opaque bars over the player box)
        boolean showHandUi = !isWaitingPhase;
        myBetLabel.setVisible(showHandUi);
        myBetLabel.setManaged(showHandUi);
        playerHandBox.setVisible(showHandUi);
        playerHandBox.setManaged(showHandUi);

        // Blinds label lives in the top bar
        blindsLabel.setVisible(!isWaitingPhase);
        blindsLabel.setManaged(!isWaitingPhase);

        myPositionLabel.setVisible(!isWaitingPhase);
        myPositionLabel.setManaged(!isWaitingPhase);

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
        if (showCommunity) {
            Platform.runLater(this::positionCommunityCardsInTableCenter);
        }

        // Get current player index
        int currentPlayerIndex = state.has("currentPlayerIndex") ? state.get("currentPlayerIndex").getAsInt() : -1;

        // --- Texas Hold'em input locks ---
        // Short lock while new community cards are dealt; after showdown, lock until next hand.

        // New hand clears the long lock.
        if (lastHandNumberSeen != null && handNumber != -1 && handNumber != lastHandNumberSeen) {
            lockInputsUntilNextHand = false;
            lockInputsUntilMs = 0L;
        }

        int communityCount = communityCards.size();
        if (lastCommunityCardsCountSeen >= 0 && communityCount > lastCommunityCardsCountSeen
                && ("FLOP".equals(phase) || "TURN".equals(phase) || "RIVER".equals(phase))) {
            lockInputsTemporarily(700);
        }
        lastCommunityCardsCountSeen = communityCount;

        if (lastWinner != null && !lastWinner.isEmpty() && ("SHOWDOWN".equals(phase) || "FINISHED".equals(phase))) {
            lockInputsUntilNextHand = true;
        }

        // Between betting rounds the server may briefly send -1; keep inputs locked.
        if (!isWaitingPhase && currentPlayerIndex < 0) {
            lockInputsTemporarily(500);
        }

        if (handNumber != -1) {
            lastHandNumberSeen = handNumber;
        }

        // Update Players around table
        JsonArray players = state.get("players").getAsJsonArray();

        // Clear bet cache when phase changes (new betting round)
        boolean phaseChanged = lastPhaseSeen != null && !lastPhaseSeen.equals(phase);
        if (phaseChanged) {
            lastBetAmounts.clear();
        }
        int myChips = 0;
        int myCurrentBet = 0;
        boolean isMyTurn = false;

        // Dealer / blinds positions (computed client-side from dealerButtonIndex + active players)
        int dealerIndex = state.has("dealerButtonIndex") ? state.get("dealerButtonIndex").getAsInt() : -1;
        int smallBlindIndex = -1;
        int bigBlindIndex = -1;
        if (dealerIndex >= 0 && dealerIndex < players.size()) {
            int activeCount = countActivePlayers(players);
            if (activeCount >= 2) {
                if (activeCount == 2) {
                    // Heads-up rule: dealer posts small blind
                    smallBlindIndex = dealerIndex;
                    bigBlindIndex = nextNonEliminatedIndexAfter(players, dealerIndex);
                } else {
                    smallBlindIndex = nextNonEliminatedIndexAfter(players, dealerIndex);
                    bigBlindIndex = nextNonEliminatedIndexAfter(players, smallBlindIndex);
                }
            }
        }

        // Clear old avatars and their bet chips
        playerAvatars.forEach(avatar -> {
            if (avatar.container != null) {
                tablePane.getChildren().remove(avatar.container);
            }
            if (avatar.betChipLabel != null) {
                tablePane.getChildren().remove(avatar.betChipLabel);
            }
        });
        playerAvatars.clear();

        // Position players around oval table
        int numPlayers = players.size();
        double ovalLeftForSeats = (tableOval != null) ? tableOval.getLayoutX() : 60;
        double ovalTopForSeats = (tableOval != null) ? tableOval.getLayoutY() : 95;
        double ovalWForSeats = (tableOval != null) ? tableOval.getWidth() : 740;
        double ovalHForSeats = (tableOval != null) ? tableOval.getHeight() : 500;

        double centerX = ovalLeftForSeats + (ovalWForSeats / 2.0);
        double centerY = ovalTopForSeats + (ovalHForSeats / 2.0);
        // Put seats slightly outside the felt.
        double radiusX = (ovalWForSeats / 2.0) + 40;
        double radiusY = (ovalHForSeats / 2.0) + 30;

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

        // If we have multiple players connected, try to show the table background image
        try {
            if (players.size() > 1 && tableBackgroundImageView != null && !tableBackgroundImageView.isVisible()) {
                // attempt to load resource if not already loaded
                if (tableBackgroundImageView.getImage() == null) {
                    java.net.URL url = getClass().getResource("/images/table.jpg");
                    if (url != null) {
                        javafx.scene.image.Image img = new javafx.scene.image.Image(url.toString(), false);
                        tableBackgroundImageView.setImage(img);
                        // Bind sizing to the table pane area
                        tableBackgroundImageView.fitWidthProperty().bind(tablePane.widthProperty().add(100));
                        tableBackgroundImageView.fitHeightProperty().bind(tablePane.heightProperty().add(160));
                        // apply image as table fill
                        try {
                            tableOval.setFill(new javafx.scene.paint.ImagePattern(img));
                        } catch (Exception ex) {
                        }
                    } else {
                        // Fallback to a remote Commons image if no local resource is available
                        String remote = "https://upload.wikimedia.org/wikipedia/commons/9/96/World_Poker_Tour_table_1.jpg";
                        javafx.scene.image.Image img = new javafx.scene.image.Image(remote, true);
                        tableBackgroundImageView.setImage(img);
                        tableBackgroundImageView.fitWidthProperty().bind(tablePane.widthProperty().add(100));
                        tableBackgroundImageView.fitHeightProperty().bind(tablePane.heightProperty().add(160));
                        try {
                            tableOval.setFill(new javafx.scene.paint.ImagePattern(img));
                        } catch (Exception ex) {
                        }
                    }
                }

                if (tableBackgroundImageView.getImage() != null) {
                    tableBackgroundImageView.setVisible(true);
                }
            } else if (players.size() <= 1 && tableBackgroundImageView != null) {
                tableBackgroundImageView.setVisible(false);
            }
        } catch (Exception ex) {
            // If anything fails, silently ignore and keep existing gradient background
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

            String positionTag = "";
            if (i == dealerIndex) {
                positionTag = "D";
            }
            if (i == smallBlindIndex) {
                positionTag = positionTag.isEmpty() ? "SB" : (positionTag + "/SB");
            }
            if (i == bigBlindIndex) {
                positionTag = positionTag.isEmpty() ? "BB" : (positionTag + "/BB");
            }

            StringBuilder positionTooltip = new StringBuilder();
            if (i == dealerIndex) {
                positionTooltip.append("Dealer button");
            }
            if (i == smallBlindIndex) {
                if (!positionTooltip.isEmpty()) positionTooltip.append("\n");
                positionTooltip.append("Small Blind");
                if (smallBlindAmount > 0) positionTooltip.append(" ($").append(smallBlindAmount).append(")");
            }
            if (i == bigBlindIndex) {
                if (!positionTooltip.isEmpty()) positionTooltip.append("\n");
                positionTooltip.append("Big Blind");
                if (bigBlindAmount > 0) positionTooltip.append(" ($").append(bigBlindAmount).append(")");
            }

            // Track my info for button states
            if (pId == playerId) {
                myChips = chips;
                myCurrentBet = currentBet;
                myCurrentBetAmount = currentBet;
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
                    i == currentPlayerIndex, lastWinner != null && lastWinner.contains(pName), pId == playerId, positionTag);

            if (avatar.positionBadge != null && avatar.positionBadge.isVisible() && !positionTooltip.isEmpty()) {
                avatar.positionBadge.setStyle(avatar.positionBadge.getStyle() + "-fx-cursor: hand;");
                Tooltip tooltip = new Tooltip(positionTooltip.toString());
                tooltip.setShowDelay(javafx.util.Duration.millis(150));
                tooltip.setHideDelay(javafx.util.Duration.millis(50));
                Tooltip.install(avatar.positionBadge, tooltip);
            }
            avatar.x = x - 50;
            avatar.y = y - 60;
            avatar.playerId = pId;
            avatar.container.setLayoutX(avatar.x);
            avatar.container.setLayoutY(avatar.y);

            // Bet chip display (placed toward the table center from the player)
            if (avatar.betChipLabel != null) {
                int displayBet = currentBet;
                if (displayBet <= 0 && lastBetAmounts.containsKey(pId)) {
                    displayBet = lastBetAmounts.getOrDefault(pId, 0);
                }

                if (displayBet > 0) {
                    avatar.betChipLabel.setText("$" + displayBet);
                    avatar.betChipLabel.setVisible(true);
                    avatar.betChipLabel.setManaged(true);

                    double avatarCenterX = avatar.container.getLayoutX() + (avatar.container.getPrefWidth() / 2.0);
                    double avatarCenterY = avatar.container.getLayoutY() + 25; // near avatar center
                    double dx = centerX - avatarCenterX;
                    double dy = centerY - avatarCenterY;
                    double len = Math.sqrt(dx * dx + dy * dy);
                    double offset = 30;
                    double chipX = avatarCenterX;
                    double chipY = avatarCenterY;
                    if (len > 0.1) {
                        chipX = avatarCenterX + (dx / len) * offset;
                        chipY = avatarCenterY + (dy / len) * offset;
                    }
                    avatar.betChipLabel.relocate(chipX - 15, chipY - 10);
                } else {
                    avatar.betChipLabel.setVisible(false);
                    avatar.betChipLabel.setManaged(false);
                }
            }

            playerAvatars.add(avatar);
            tablePane.getChildren().add(avatar.container);
            if (avatar.betChipLabel != null) {
                tablePane.getChildren().add(avatar.betChipLabel);
            }
        }

        // Update cached bet amounts when there are active bets
        for (JsonElement p : players) {
            if (!p.isJsonObject()) continue;
            JsonObject pl = p.getAsJsonObject();
            long pId = pl.has("playerId") ? pl.get("playerId").getAsLong() : pl.get("id").getAsLong();
            int cb = pl.has("currentBet") ? pl.get("currentBet").getAsInt() : 0;
            if (cb > 0) {
                lastBetAmounts.put(pId, cb);
            }
        }

        lastPhaseSeen = phase;

        // Update my position badge (top bar)
        if (!isWaitingPhase) {
            String myPos = "";
            if (myIndex == dealerIndex) myPos = "Dealer";
            if (myIndex == smallBlindIndex) myPos = myPos.isEmpty() ? "Small Blind" : (myPos + " / Small Blind");
            if (myIndex == bigBlindIndex) myPos = myPos.isEmpty() ? "Big Blind" : (myPos + " / Big Blind");

            if (myPos.isEmpty()) {
                myPositionLabel.setText("Position: —");
                myPositionLabel.setStyle(
                    "-fx-text-fill: white;" +
                    "-fx-background-color: rgba(0,0,0,0.4);" +
                    "-fx-padding: 8 12;" +
                    "-fx-background-radius: 15;" +
                    "-fx-border-color: rgba(255,255,255,0.25);" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 15;"
                );
            } else {
                String extra = "";
                if (myIndex == smallBlindIndex && smallBlindAmount > 0) extra = " ($" + smallBlindAmount + ")";
                if (myIndex == bigBlindIndex && bigBlindAmount > 0) extra = " ($" + bigBlindAmount + ")";

                String accent = (myIndex == bigBlindIndex) ? "#ff5252" : (myIndex == smallBlindIndex) ? "#4fc3f7" : "#ffffff";
                String textColor = (myIndex == bigBlindIndex) ? "#ffffff" : "#000000";
                myPositionLabel.setText("Position: " + myPos + extra);
                myPositionLabel.setStyle(
                    "-fx-text-fill: " + textColor + ";" +
                    "-fx-background-color: " + accent + ";" +
                    "-fx-padding: 8 12;" +
                    "-fx-background-radius: 15;" +
                    "-fx-border-color: rgba(0,0,0,0.25);" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 15;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0, 0, 2);"
                );
            }
        } else {
            myPositionLabel.setText("");
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

            if (!finishedAlertShown) {
                finishedAlertShown = true;
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
        }
        // If I'm eliminated (but tournament not finished yet)
        else if (amIEliminated && !"WAITING".equals(phase)) {
            if (!eliminatedAlertShown) {
                eliminatedAlertShown = true;
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Eliminated");
                    alert.setHeaderText("You have been eliminated!");
                    alert.setContentText("You ran out of chips. You can leave or watch the rest of the tournament.");
                    alert.showAndWait();
                });
            }
        } else {
            eliminatedAlertShown = false;
            finishedAlertShown = false;
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
        cachedIsMyTurn = isMyTurn;
        cachedGameCurrentBet = gameCurrentBet;
        cachedMyCurrentBet = myCurrentBet;
        cachedMyChips = myChips;

        boolean lockAllInputs = isInputLockedNow();
        updateButtonStates(isMyTurn, gameCurrentBet, myCurrentBet, myChips, lockAllInputs);

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
    private PlayerAvatar createPlayerAvatar(String name, int chips, int bet, boolean folded, boolean eliminated, boolean isTurn, boolean isWinner, boolean isMe, String positionTag) {
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

        // Dealer / Blinds badge (D / SB / BB)
        avatar.positionBadge = new Label(positionTag != null ? positionTag : "");
        avatar.positionBadge.setVisible(positionTag != null && !positionTag.isBlank());
        avatar.positionBadge.setManaged(positionTag != null && !positionTag.isBlank());
        avatar.positionBadge.setFont(Font.font("Arial", FontWeight.BOLD, 9));

        String badgeBg = "#ffffff";
        String badgeText = "#000000";
        if (positionTag != null) {
            if (positionTag.contains("BB")) {
                badgeBg = "#ff5252";
                badgeText = "#ffffff";
            } else if (positionTag.contains("SB")) {
                badgeBg = "#4fc3f7";
                badgeText = "#000000";
            } else if (positionTag.contains("D")) {
                badgeBg = "#ffffff";
                badgeText = "#000000";
            }
        }
        avatar.positionBadge.setStyle(
            "-fx-background-color: " + badgeBg + ";" +
            "-fx-text-fill: " + badgeText + ";" +
            "-fx-padding: 2 6;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: rgba(0,0,0,0.35);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;"
        );

        StackPane avatarStack = new StackPane();
        avatarStack.getChildren().add(avatar.avatar);

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

        // Bet chip (positioned on table near the player, not inside the avatar box)
        avatar.betChipLabel = new Label();
        avatar.betChipLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        avatar.betChipLabel.setStyle(
            "-fx-text-fill: #1a1a1a;" +
            "-fx-background-color: #FFD54F;" +
            "-fx-padding: 3 8;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #5D4037;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 6, 0, 0, 2);"
        );
        avatar.betChipLabel.setVisible(false);
        avatar.betChipLabel.setManaged(false);

        if (avatar.positionBadge != null && avatar.positionBadge.isVisible()) {
            avatar.container.getChildren().addAll(avatarStack, avatar.positionBadge, avatar.nameLabel, avatar.chipsLabel, avatar.readyLabel);
        } else {
            avatar.container.getChildren().addAll(avatarStack, avatar.nameLabel, avatar.chipsLabel, avatar.readyLabel);
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
            log.log(Level.FINE, "Failed to load card image: " + filename, e);
        }
    }

    /**
     * Converts Card Rank and Suit into the correct filename. Example: ACE +
     * HEARTS -> "AH.png"
     */
    private String getCardFilename(String rank, String suit) {
        String r = switch (rank) {
            case "TWO" -> "2";
            case "THREE" -> "3";
            case "FOUR" -> "4";
            case "FIVE" -> "5";
            case "SIX" -> "6";
            case "SEVEN" -> "7";
            case "EIGHT" -> "8";
            case "NINE" -> "9";
            case "TEN" -> "10";
            case "JACK" -> "J";
            case "QUEEN" -> "Q";
            case "KING" -> "K";
            case "ACE" -> "A";
            default -> "";
        };

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
        stopTimer();
        if (seconds < 0) {
            timerLabel.setText("");
            return;
        }

        updateTimerLabel(seconds);
        final int[] remaining = new int[] { seconds };

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), evt -> {
            remaining[0]--;
            if (remaining[0] >= 0) {
                updateTimerLabel(remaining[0]);
            }
        }));
        countdownTimeline.setCycleCount(seconds + 1);
        countdownTimeline.setOnFinished(evt -> timerLabel.setText(""));
        countdownTimeline.playFromStart();
    }

    private void updateTimerLabel(int timeLeft) {
        if (timeLeft <= 5) {
            timerLabel.setStyle("-fx-text-fill: #FF0000; -fx-font-weight: bold; -fx-font-size: 20px;");
        } else if (timeLeft <= 10) {
            timerLabel.setStyle("-fx-text-fill: #FFA500; -fx-font-weight: bold; -fx-font-size: 18px;");
        } else {
            timerLabel.setStyle("-fx-text-fill: #FF6347; -fx-font-weight: bold; -fx-font-size: 16px;");
        }
        timerLabel.setText("⏱ " + timeLeft + "s");
    }

    /**
     * Stop the countdown timer.
     */
    private void stopTimer() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
        timerLabel.setText("");
    }
}
