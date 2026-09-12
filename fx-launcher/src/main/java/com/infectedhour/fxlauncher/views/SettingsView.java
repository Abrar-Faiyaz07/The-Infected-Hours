package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.state.SessionState;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Screen 8 (UI/UX doc §2): music/SFX volume sliders, fullscreen toggle, keybind display, backend URL (advanced). */
public class SettingsView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(12);

    public SettingsView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
    }

    private void build() {
        root.setPadding(new Insets(32));

        Slider musicVolume = new Slider(0, 100, 80);
        Slider sfxVolume = new Slider(0, 100, 80);
        CheckBox fullscreen = new CheckBox("Fullscreen");
        fullscreen.setSelected(SessionState.get().isFullscreen());
        fullscreen.setOnAction(e -> {
            boolean isFull = fullscreen.isSelected();
            SessionState.get().setFullscreen(isFull);
            stage.setFullScreen(isFull);
        });

        TextField backendUrl = new TextField(SessionState.get().getBackendUrl());
        backendUrl.setPromptText("Backend URL (advanced)");
        Button saveBackendUrl = new Button("Save backend URL");
        saveBackendUrl.setOnAction(e -> SessionState.get().setBackendUrl(backendUrl.getText()));

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));

        root.getChildren().addAll(
                new Label("Music Volume"), musicVolume,
                new Label("SFX Volume"), sfxVolume,
                fullscreen,
                new Label("Backend URL"), backendUrl, saveBackendUrl,
                backBtn);
    }

    public VBox getRoot() {
        return root;
    }
}
