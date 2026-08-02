package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.net.LanDiscovery;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Screen 5 (UI/UX doc §2): auto-discovered hosts list (refresh), manual IP
 * field, character preview (Jane), Ready toggle.
 *
 * <p>Discovery is a UDP broadcast to port {@value GameConstants#DISCOVERY_UDP_PORT}
 * (TRD §5). It runs on a background thread because
 * {@link LanDiscovery#probe()} blocks for its full listen window — doing that
 * on the FX Application Thread would freeze the UI for two seconds.
 *
 * <p>The manual IP field is not a debug leftover: campus Wi-Fi with client
 * isolation drops broadcast traffic entirely (TRD risk table), and on those
 * networks typing the host's address is the only way in.
 */
public class JoinLobbyView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(12);

    private final ListView<LanDiscovery.DiscoveredHost> discoveredHosts = new ListView<>();
    private final TextField manualIp = new TextField();
    private final Label status = new Label();
    private final Button refreshBtn = new Button("Search the LAN");

    public JoinLobbyView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
        refresh();
    }

    private void build() {
        root.setPadding(new Insets(40));
        root.getStyleClass().add("bg-night");

        Label title = new Label("JOIN A MATCH");
        title.getStyleClass().add("title-gold");

        discoveredHosts.setPrefHeight(180);
        discoveredHosts.setPlaceholder(new Label("No hosts found yet."));

        status.getStyleClass().add("version-label");

        refreshBtn.setMaxWidth(460);
        refreshBtn.getStyleClass().add("menu-btn");
        refreshBtn.setOnAction(e -> refresh());

        manualIp.setPromptText("Manual host IP (e.g. 192.168.0.14)");
        manualIp.setMaxWidth(460);

        Button joinBtn = new Button("Join");
        joinBtn.setMaxWidth(460);
        joinBtn.getStyleClass().addAll("menu-btn", "menu-btn-primary");
        joinBtn.setOnAction(e -> joinMatch());

        Button backBtn = new Button("Back");
        backBtn.setMaxWidth(460);
        backBtn.getStyleClass().add("menu-btn");
        backBtn.setOnAction(e -> stage.getScene().setRoot(new CoopModeView(stage, backendClient).getRoot()));

        root.getChildren().addAll(title, discoveredHosts, refreshBtn, status,
                new Label("…or connect directly:"), manualIp, joinBtn, backBtn);
    }

    /** Broadcast + listen on a daemon thread, then publish results on the FX thread. */
    private void refresh() {
        refreshBtn.setDisable(true);
        status.setText("Searching for hosts on UDP " + GameConstants.DISCOVERY_UDP_PORT + "…");
        discoveredHosts.getItems().clear();

        Thread probe = new Thread(() -> {
            List<LanDiscovery.DiscoveredHost> hosts = LanDiscovery.probe();
            Platform.runLater(() -> {
                discoveredHosts.getItems().setAll(hosts);
                if (!hosts.isEmpty()) {
                    discoveredHosts.getSelectionModel().selectFirst();
                }
                status.setText(hosts.isEmpty()
                        ? "No hosts answered. If this network blocks broadcasts, enter the IP manually."
                        : hosts.size() + " host(s) found.");
                refreshBtn.setDisable(false);
            });
        }, "lan-discovery-probe");
        probe.setDaemon(true);
        probe.start();
    }

    private void joinMatch() {
        LanDiscovery.DiscoveredHost selected = discoveredHosts.getSelectionModel().getSelectedItem();
        String typed = manualIp.getText() == null ? "" : manualIp.getText().trim();

        // A typed address always wins: the player entered it deliberately.
        String targetIp = !typed.isEmpty() ? typed : (selected != null ? selected.address() : null);
        if (targetIp == null || targetIp.isBlank()) {
            warn("Pick a host from the list, or type the host's IP address.");
            return;
        }
        if (selected != null && typed.isEmpty()
                && selected.protocolVersion() != GameConstants.PROTOCOL_VERSION) {
            warn("That host runs protocol v" + selected.protocolVersion()
                    + " but this machine speaks v" + GameConstants.PROTOCOL_VERSION
                    + ". Rebuild both laptops from the same commit.");
            return;
        }

        new GameLauncherBridge(stage, backendClient).startAsClient(targetIp, () ->
                stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));
    }

    private void warn(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Join");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.show();
    }

    public VBox getRoot() {
        return root;
    }
}
