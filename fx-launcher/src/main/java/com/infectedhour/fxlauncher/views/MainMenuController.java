package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.state.SessionState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.List;

public class MainMenuController {

    private static final String VERSION = "v0.1-alpha";

    @FXML private Label usernameLabel;
    @FXML private Label statusChip;
    @FXML private Label versionLabel;
    @FXML private Label menuDescription;
    @FXML private StackPane rootPane;
    @FXML private Button singlePlayerBtn;
    @FXML private Button coopButton;
    @FXML private Button continueButton;
    @FXML private Button settingsButton;
    @FXML private Button intelButton;
    @FXML private Button debugButton;
    @FXML private Button bossButton;
    @FXML private Button exitButton;

    private Stage stage;
    private BackendClient backendClient;

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

        configureMenuNavigation();
    }

    private void configureMenuNavigation() {
        List<Button> buttons = List.of(
                singlePlayerBtn, coopButton, continueButton,
                settingsButton, intelButton, debugButton, bossButton, exitButton);
        List<String> descriptions = List.of(
                "Begin the Ashgrove containment operation.",
                "Host or join a two-operative survival session.",
                "Resume the campaign from a saved checkpoint.",
                "Configure display, audio, subtitles, and connection settings.",
                "Open mission intelligence and project information.",
                "Developer split-screen testing tools.",
                "Developer shortcut to the final encounter.",
                "Close The Infected Hour.");

        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            int index = i;

            button.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                if (isFocused) {
                    menuDescription.setText(descriptions.get(index));
                }
            });
            button.setOnMouseEntered(event -> {
                button.requestFocus();
                menuDescription.setText(descriptions.get(index));
            });
            button.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                    int direction = event.getCode() == KeyCode.UP ? -1 : 1;
                    buttons.get(Math.floorMod(index + direction, buttons.size())).requestFocus();
                    event.consume();
                }
            });
        }

        Platform.runLater(singlePlayerBtn::requestFocus);
    }

    @FXML
    private void onSinglePlayer() {
        navigate(new CharacterSelectView(stage, backendClient).getRoot());
    }

    @FXML
    private void onCoopMode() {
        navigate(new CoopModeView(stage, backendClient).getRoot());
    }

    @FXML
    private void onDebugCoop() {
        navigate(new DebugCoopView(stage, backendClient).getRoot());
    }

    @FXML
    private void onFinalBossFight() {
        navigate(new BossCharacterSelectView(stage, backendClient, rootPane).getRoot());
    }

    @FXML
    private void onLoadGame() {
        navigate(new LoadGameView(stage, backendClient).getRoot());
    }

    @FXML
    private void onCollection() {
        navigate(new CollectionView(stage, backendClient).getRoot());
    }

    @FXML
    private void onSettings() {
        navigate(new SettingsView(stage, backendClient).getRoot());
    }

    @FXML
    private void onAbout() {
        navigate(new IntelView(stage, backendClient).getRoot());
    }

    @FXML
    private void onQuit() {
        Platform.exit();
    }

    private void navigate(Parent root) {
        stage.getScene().setRoot(root);
    }
}
