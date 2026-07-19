package com.infectedhour.fxlauncher.bridge;

import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.lwjgl3.Lwjgl3Launcher;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * FX-side half of the thread boundary (TRD §2). Hides the JavaFX stage,
 * boots the libGDX Lwjgl3Application on a brand-new dedicated thread
 * (never the FX Application Thread), and re-shows the stage via
 * Platform.runLater when core calls back through GameBridge.
 */
public class GameLauncherBridge {

    private final Stage primaryStage;

    public GameLauncherBridge(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void startMatch(boolean isHost, String hostAddressIfClient, Runnable onReturnToLauncher) {
        GameBridge bridge = new GameBridge();

        bridge.setOnGameWindowClosed(() -> Platform.runLater(() -> {
            primaryStage.show();
            onReturnToLauncher.run();
        }));

        bridge.setOnMatchEnded(outcome -> Platform.runLater(() -> {
            // ============ TEAMMATE TASK: SHOW RESULTS ============
            // TODO(fx): navigate to the Results screen:
            //   ResultsView view = new ResultsView(primaryStage, backendClient,
            //       outcome.result(), outcome.finalLevelReached());
            //   primaryStage.getScene().setRoot(view.getRoot());
            //  (Pass a BackendClient into this class from LauncherApplication
            //   — add it to the constructor.)
            // =====================================================
        }));

        Platform.runLater(primaryStage::hide);

        Thread gameThread = new Thread(() -> Lwjgl3Launcher.boot(isHost, hostAddressIfClient, bridge), "libGDX-window");
        gameThread.setDaemon(false);
        gameThread.start();
    }
}
