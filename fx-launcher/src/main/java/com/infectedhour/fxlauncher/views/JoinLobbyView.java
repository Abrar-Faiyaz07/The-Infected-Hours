package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Screen 5 (UI/UX doc §2): auto-discovered hosts list (refresh), manual IP
 * field, character preview (Jane), Ready toggle.
 */
public class JoinLobbyView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(12);

    public JoinLobbyView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
    }

    private void build() {
        root.setPadding(new Insets(32));

        ListView<String> discoveredHosts = new ListView<>();
        // ============ TEAMMATE TASK: HOST DISCOVERY ============
        // TODO(fx): fill this list by broadcasting on the LAN (TRD par.5):
        //  1. On Refresh: DatagramSocket -> send "IH_DISCOVER" to
        //     255.255.255.255 : GameConstants.DISCOVERY_UDP_PORT.
        //  2. Listen ~2s for "IH_HOST:<name>" replies; add
        //     "<name> (<senderIp>)" rows on the FX thread (Platform.runLater).
        //  3. Joining uses the sender IP; keep the manual-IP field as the
        //     fallback for networks that block broadcast (TRD risk table:
        //     campus Wi-Fi client isolation -> phone hotspot).
        // =======================================================
        Button refreshBtn = new Button("Refresh");

        TextField manualIp = new TextField();
        manualIp.setPromptText("Manual host IP (fallback)");

        Button joinBtn = new Button("Join");
        joinBtn.setOnAction(e -> {
            String targetIp = discoveredHosts.getSelectionModel().getSelectedItem() != null
                    ? discoveredHosts.getSelectionModel().getSelectedItem()
                    : manualIp.getText();
            joinMatch(targetIp);
        });

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> new MainMenuView(stage, backendClient).getRoot());

        root.getChildren().addAll(discoveredHosts, refreshBtn, manualIp, joinBtn, backBtn);
    }

    private void joinMatch(String hostIp) {
        GameLauncherBridge bridge = new GameLauncherBridge(stage);
        bridge.startMatch(false, hostIp, () -> {
            // returned -> ResultsView
        });
    }

    public VBox getRoot() {
        return root;
    }
}
