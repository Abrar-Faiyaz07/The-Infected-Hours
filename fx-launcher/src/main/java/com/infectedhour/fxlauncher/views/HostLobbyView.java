package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.state.SessionState;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.PlayerDto;
import com.infectedhour.shared.net.LanDiscovery;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.SocketException;

/**
 * Screen 4 (UI/UX doc §2): shows host IP, level select (unlocked only),
 * waiting slot for Player 2, character lock (Elric fixed to host), Start
 * button (enabled solo or when P2 joins).
 *
 * <p>This screen owns only the <b>discovery</b> half of TRD §5 — a plain UDP
 * responder on port {@value GameConstants#DISCOVERY_UDP_PORT} that answers
 * "who is hosting?" broadcasts so the Join Lobby can list this machine. The
 * KryoNet session itself (TCP 54555 / UDP 54777) does not exist until Start is
 * pressed and {@code core} boots the game window; the two never bind the same
 * port, which is why discovery moved off 54777 (see GameConstants).
 */
public class HostLobbyView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(12);

    private final TextField hostUsername = new TextField();
    private final Label connectionStatusChip = new Label("⬤  waiting for player…");
    private final Label discoveryStatus = new Label();
    private final ComboBox<Integer> levelSelect = new ComboBox<>();

    private LanDiscovery.Responder responder;

    public HostLobbyView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
        startDiscoveryResponder();
        loadUnlockedLevels();
    }

    private void build() {
        root.setPadding(new Insets(40));
        root.getStyleClass().add("bg-night");

        Label title = new Label("HOST A MATCH");
        title.getStyleClass().add("title-gold");

        String lanIp = LanDiscovery.resolveLanIpv4();
        Label hostIpLabel = new Label("Your LAN address:  " + lanIp);
        hostIpLabel.getStyleClass().add("player-name");

        Label manualHint = new Label(
                "If the other laptop cannot find you automatically, have them type this address\n"
                        + "into the Join screen's manual IP field.");
        manualHint.getStyleClass().add("brand-subtitle");

        hostUsername.setPromptText("Your Host Username");
        hostUsername.setText(displayName());
        hostUsername.setMaxWidth(460);

        connectionStatusChip.getStyleClass().setAll("chip-searching");
        discoveryStatus.getStyleClass().add("version-label");

        levelSelect.setPromptText("Level");
        levelSelect.getItems().add(1);
        levelSelect.getSelectionModel().selectFirst();

        Button startBtn = new Button("Start match");
        startBtn.setMaxWidth(460);
        startBtn.getStyleClass().addAll("menu-btn", "menu-btn-primary");
        startBtn.setOnAction(e -> startMatch());

        Button backBtn = new Button("Back");
        backBtn.setMaxWidth(460);
        backBtn.getStyleClass().add("menu-btn");
        backBtn.setOnAction(e -> {
            stopDiscoveryResponder();
            stage.getScene().setRoot(new CoopModeView(stage, backendClient).getRoot());
        });

        root.getChildren().addAll(
                title,
                hostIpLabel,
                manualHint,
                new Label("Host Username:"),
                hostUsername,
                new HBox(10, new Label("Start at level:"), levelSelect),
                connectionStatusChip,
                discoveryStatus,
                startBtn,
                backBtn);
    }

    /**
     * Makes this machine answer "IH_DISCOVER" broadcasts for as long as the
     * lobby is open. A bind failure is shown, not thrown — manual IP entry
     * still works without discovery.
     */
    private void startDiscoveryResponder() {
        String name = displayName();
        responder = new LanDiscovery.Responder(name);
        try {
            responder.start();
            discoveryStatus.setText("Discoverable as \"" + name + "\" on UDP "
                    + GameConstants.DISCOVERY_UDP_PORT);
        } catch (SocketException e) {
            responder = null;
            discoveryStatus.setText("Auto-discovery unavailable (UDP "
                    + GameConstants.DISCOVERY_UDP_PORT + " is in use) — share your IP manually.");
        }
    }

    private void stopDiscoveryResponder() {
        if (responder != null) {
            responder.close();
            responder = null;
        }
    }

    private void loadUnlockedLevels() {
        if (SessionState.get().isOfflineMode() || !SessionState.get().isAuthenticated()) {
            return; // offline: level 1 only, already in the list
        }
        backendClient.getMySave()
                .thenAccept(save -> Platform.runLater(() -> {
                    int highest = Math.max(1, Math.min(GameConstants.LEVEL_COUNT, save.highestLevelUnlocked()));
                    levelSelect.getItems().setAll();
                    for (int level = 1; level <= highest; level++) {
                        levelSelect.getItems().add(level);
                    }
                    levelSelect.getSelectionModel().selectFirst();
                }))
                .exceptionally(ex -> null); // offline-tolerant: keep level 1
    }

    private void startMatch() {
        // The KryoNet server binds inside the game window; free the discovery
        // socket first so nothing races over UDP on this machine.
        stopDiscoveryResponder();
        String name = hostUsername.getText() != null && !hostUsername.getText().isBlank()
                ? hostUsername.getText().trim()
                : displayName();
        new GameLauncherBridge(stage, backendClient).startAsHost(name, () ->
                stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));
    }

    private static String displayName() {
        PlayerDto player = SessionState.get().getCurrentPlayer();
        return player != null ? player.displayName() : System.getProperty("user.name", "Host");
    }

    public VBox getRoot() {
        return root;
    }
}
