package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Screen 4 (UI/UX doc §2): shows host IP, level select (unlocked only),
 * waiting slot for Player 2, character lock (Elric fixed to host), Start
 * button (enabled solo or when P2 joins). Discovery/session setup here is
 * TCP/UDP per TRD §5 — NOT the in-match KryoNet session, which core owns
 * once GameLauncherBridge boots the libGDX window.
 */
public class HostLobbyView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(12);

    private final Label connectionStatusChip = new Label("Searching…"); // gray/green/red per UI/UX doc §7
    private boolean player2Connected = false;

    public HostLobbyView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
    }

    private void build() {
        root.setPadding(new Insets(32));

        Label hostIpLabel = new Label("Host IP: " + resolveLocalIp());
        // ============ TEAMMATE TASK: LEVEL SELECT ============
        // TODO(fx): ComboBox<Integer> of levels 1..highestLevelUnlocked —
        // call backendClient.getMySave() and populate on the FX thread
        // (Platform.runLater). Pass the chosen level into startMatch so the
        // game boots into that LevelBriefingScreen.
        // =====================================================

        Button startBtn = new Button("Start (Solo)");
        startBtn.setOnAction(e -> startMatch());

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> new MainMenuView(stage, backendClient).getRoot());

        root.getChildren().addAll(hostIpLabel, connectionStatusChip, startBtn, backBtn);

        // ============ TEAMMATE TASK: DISCOVERY RESPONDER ============
        // TODO(fx): make this host discoverable on the LAN (TRD par.5):
        //  1. Daemon thread with a DatagramSocket bound to
        //     GameConstants.DISCOVERY_UDP_PORT.
        //  2. On any packet starting with "IH_DISCOVER" -> reply
        //     "IH_HOST:<displayName>" to the sender's address.
        //  3. When P2's KryoNet connection arrives, flip connectionStatusChip
        //     to green "Connected" and relabel Start to "Start (Co-op)".
        //  4. Stop the responder when the lobby closes or the match starts.
        // ============================================================
    }

    private void startMatch() {
        GameLauncherBridge bridge = new GameLauncherBridge(stage);
        bridge.startMatch(true, null, () -> {
            // returned from match -> show ResultsView (wired via GameBridge.onMatchEnded upstream)
        });
    }

    private String resolveLocalIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public VBox getRoot() {
        return root;
    }
}
