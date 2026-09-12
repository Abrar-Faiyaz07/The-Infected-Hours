package com.infectedhour.fxlauncher;

import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.views.MainMenuView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX entry point (TRD §2 step 1). Owns the single Stage that every view
 * shares; views swap themselves in with {@code stage.getScene().setRoot(...)}.
 */
public class LauncherApplication extends Application {

    /**
     * The scene is created at 1280x720, not 1920x1080. A scene larger than the
     * display forces the root's layout pass to run against a size the window
     * can never show, so anything below the fold (About / Quit / the player
     * footer) was simply clipped. The menu is fully responsive, so starting at
     * a size that fits and then maximising gives the intended layout on any
     * screen.
     */
    private static final int INITIAL_WIDTH = 1280;
    private static final int INITIAL_HEIGHT = 720;

    private BackendClient backendClient;

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        backendClient = new BackendClient();

        primaryStage.setTitle("The Infected Hour");

        // DEV SHORTCUT: skip login and open the main menu directly.
        // Swap to `new LoginView(primaryStage, backendClient).getRoot()` for the real flow.
        MainMenuView mainMenu = new MainMenuView(primaryStage, backendClient);
        Scene scene = new Scene(mainMenu.getRoot(), INITIAL_WIDTH, INITIAL_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/launcher.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
