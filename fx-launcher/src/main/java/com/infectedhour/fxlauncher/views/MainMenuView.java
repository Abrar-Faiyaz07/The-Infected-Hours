package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Screen 3 (UI/UX doc §2): Play (Host/Join), Profile & Saves, Leaderboards, Settings, Credits, Quit. */
public class MainMenuView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(12);

    public MainMenuView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
    }

    private void build() {
        root.setPadding(new Insets(32));

        Button hostBtn = new Button("Host Game");
        hostBtn.setOnAction(e -> navigate(new HostLobbyView(stage, backendClient)));

        Button joinBtn = new Button("Join Game");
        joinBtn.setOnAction(e -> navigate(new JoinLobbyView(stage, backendClient)));

        Button profileBtn = new Button("Profile & Saves");
        profileBtn.setOnAction(e -> navigate(new ProfileView(stage, backendClient)));

        Button leaderboardsBtn = new Button("Leaderboards");
        leaderboardsBtn.setOnAction(e -> navigate(new LeaderboardsView(stage, backendClient)));

        Button settingsBtn = new Button("Settings");
        settingsBtn.setOnAction(e -> navigate(new SettingsView(stage, backendClient)));

        Button quitBtn = new Button("Quit");
        quitBtn.setOnAction(e -> stage.close());

        root.getChildren().addAll(hostBtn, joinBtn, profileBtn, leaderboardsBtn, settingsBtn, quitBtn);
    }

    private void navigate(Object view) {
        // TODO(fx, optional cleanup): replace this instanceof chain with a
        // tiny Navigator helper (interface View { Parent getRoot(); } plus a
        // navigate(View) method) — nice-to-have, not demo-critical.
        if (view instanceof HostLobbyView v) stage.getScene().setRoot(v.getRoot());
        else if (view instanceof JoinLobbyView v) stage.getScene().setRoot(v.getRoot());
        else if (view instanceof ProfileView v) stage.getScene().setRoot(v.getRoot());
        else if (view instanceof LeaderboardsView v) stage.getScene().setRoot(v.getRoot());
        else if (view instanceof SettingsView v) stage.getScene().setRoot(v.getRoot());
    }

    public VBox getRoot() {
        return root;
    }
}
