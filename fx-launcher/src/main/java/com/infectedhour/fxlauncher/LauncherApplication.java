package com.infectedhour.fxlauncher;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.views.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX entry point (TRD §2 step 1): boots straight into LoginView.
 * Owns the single Stage that every view shares and the GameLauncherBridge
 * used to hand off to libGDX when a match starts.
 */
public class LauncherApplication extends Application {

    private BackendClient backendClient;
    private GameLauncherBridge gameLauncherBridge;

    @Override
    public void start(Stage primaryStage) {
        backendClient = new BackendClient();
        gameLauncherBridge = new GameLauncherBridge(primaryStage);

        primaryStage.setTitle("The Infected Hour");

        LoginView loginView = new LoginView(primaryStage, backendClient);
        Scene scene = new Scene(loginView.getRoot(), 960, 600);
        scene.getStylesheets().add(getClass().getResource("/launcher.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
