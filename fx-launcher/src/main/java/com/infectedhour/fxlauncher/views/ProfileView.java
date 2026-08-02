package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.state.SessionState;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Screen 6 (UI/UX doc §2): stats, save slot info, logout. */
public class ProfileView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(12);
    private final Label saveInfo = new Label("Loading save…");

    public ProfileView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
        loadSave();
    }

    private void build() {
        root.setPadding(new Insets(32));

        Label username = new Label("Player: " + SessionState.get().getCurrentPlayer());

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> {
            SessionState.get().setJwtToken(null);
            SessionState.get().setCurrentPlayer(null);
            stage.getScene().setRoot(new LoginView(stage, backendClient).getRoot());
        });

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));

        root.getChildren().addAll(username, saveInfo, logoutBtn, backBtn);
    }

    private void loadSave() {
        backendClient.getMySave()
                .thenAccept(save -> Platform.runLater(() ->
                        saveInfo.setText("Highest level unlocked: " + save.highestLevelUnlocked()
                                + " | Playtime: " + save.totalPlaytimeSec() + "s")))
                .exceptionally(ex -> {
                    Platform.runLater(() -> saveInfo.setText("Could not load save: " + ex.getMessage()));
                    return null;
                });
    }

    public VBox getRoot() {
        return root;
    }
}
