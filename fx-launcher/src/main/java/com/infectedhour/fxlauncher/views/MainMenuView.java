package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

/** Screen 3 (UI/UX doc §2): Play (Host/Join), Profile & Saves, Leaderboards, Settings, Credits, Quit. */
public class MainMenuView {

    private final StackPane root;

    public MainMenuView(Stage stage, BackendClient backendClient) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/MainMenu.fxml"));
            root = loader.load();
            MainMenuController controller = loader.getController();
            controller.init(stage, backendClient);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MainMenu.fxml", e);
        }
    }

    public StackPane getRoot() {
        return root;
    }
}
