package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.state.SessionState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/** Controller for MainMenu.fxml (Screen 3, UI/UX doc §2). */
public class MainMenuController {

    private static final String VERSION = "v0.1-alpha";

    @FXML private Label usernameLabel;
    @FXML private Label statusChip;
    @FXML private Label versionLabel;

    private Stage stage;
    private BackendClient backendClient;

    /** Called by MainMenuView after FXMLLoader so the controller has its dependencies. */
    public void init(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;

        versionLabel.setText(VERSION);

        SessionState session = SessionState.get();
        usernameLabel.setText(session.getCurrentPlayer() != null
                ? session.getCurrentPlayer().displayName()
                : "Guest");

        if (session.isOfflineMode() || !session.isAuthenticated()) {
            statusChip.setText("⬤  offline");
            statusChip.getStyleClass().setAll("chip-lost");
        } else {
            statusChip.setText("⬤  connected");
            statusChip.getStyleClass().setAll("chip-connected");
        }
    }

    /** Solo play: this laptop hosts its own session with no second player (MatchMode.SOLO). */
    @FXML
    private void onSinglePlayer() {
        new GameLauncherBridge(stage, backendClient).startAsHost(() -> { });
    }

    /** Co-op: choose whether this laptop hosts the LAN session or joins another one. */
    @FXML
    private void onCoopMode() {
        navigate(new CoopModeView(stage, backendClient).getRoot());
    }

    @FXML
    private void onLoadGame() {
        navigate(new ProfileView(stage, backendClient).getRoot());
    }

    @FXML
    private void onSettings() {
        navigate(new SettingsView(stage, backendClient).getRoot());
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("The Infected Hour — " + VERSION);
        alert.setContentText("""
                2D top-down co-op action game. Contain the epidemic, destroy the Virus Heart.

                CSE 4402 Visual Programming Lab
                Islamic University of Technology

                Asset credits: see ASSETS_CREDITS.md""");
        alert.initOwner(stage);
        alert.showAndWait();
    }

    @FXML
    private void onQuit() {
        Platform.exit();
    }

    private void navigate(Parent root) {
        stage.getScene().setRoot(root);
    }
}
