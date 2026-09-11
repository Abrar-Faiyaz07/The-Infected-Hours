package com.infectedhour.fxlauncher.bridge;

import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.SessionConfig;
import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.save.LocalSaveSlots;
import com.infectedhour.fxlauncher.state.SessionState;
import com.infectedhour.fxlauncher.views.ResultsView;
import com.infectedhour.lwjgl3.Lwjgl3Launcher;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.PlayerDto;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FX-side half of the thread boundary (TRD §2). Hides the JavaFX stage,
 * boots the libGDX Lwjgl3Application on a brand-new dedicated thread
 * (never the FX Application Thread), and re-shows the stage via
 * Platform.runLater when core calls back through GameBridge.
 */
public class GameLauncherBridge {

    private final Stage primaryStage;
    private final BackendClient backendClient;
    private Parent launcherRoot;
    private StackPane loadingRoot;

    public GameLauncherBridge(Stage primaryStage, BackendClient backendClient) {
        this.primaryStage = primaryStage;
        this.backendClient = backendClient;
    }

    /**
     * This laptop hosts the session (solo, or waiting for a partner to join).
     * If the player picked a slot in Load Game, that run is resumed instead of
     * starting a fresh one.
     */
    public void startAsHost(Runnable onReturnToLauncher) {
        startAsHost(displayName(), com.infectedhour.shared.network.CharacterType.ELRIC, onReturnToLauncher);
    }

    public void startAsHost(com.infectedhour.shared.network.CharacterType character, Runnable onReturnToLauncher) {
        startAsHost(displayName(), character, onReturnToLauncher);
    }

    public void startAsHost(String customName, Runnable onReturnToLauncher) {
        startAsHost(customName, com.infectedhour.shared.network.CharacterType.ELRIC, onReturnToLauncher);
    }

    public void startAsHost(String customName, com.infectedhour.shared.network.CharacterType character, Runnable onReturnToLauncher) {
        String name = (customName != null && !customName.isBlank()) ? customName.trim() : displayName();
        var slot = SessionState.get().getLoadedSlot();
        com.infectedhour.shared.network.CharacterType preferred = character != null ? character : com.infectedhour.shared.network.CharacterType.ELRIC;
        SessionConfig config = slot != null && slot.occupied()
                ? SessionConfig.hostingFromSave(playerId(), name,
                        SessionState.get().getBackendUrl(), slot, preferred)
                : SessionConfig.hosting(playerId(), name,
                        SessionState.get().getBackendUrl(), preferred);
        // Consumed once — returning to the menu must not silently reload the
        // same save the next time the player presses Play.
        SessionState.get().clearLoadedSlot();
        startMatch(config, onReturnToLauncher);
    }

    /** This laptop joins a host already running on the LAN. */
    public void startAsClient(String hostAddress, Runnable onReturnToLauncher) {
        startAsClient(hostAddress, displayName(), onReturnToLauncher);
    }

    public void startAsClient(String hostAddress, String customName, Runnable onReturnToLauncher) {
        String name = (customName != null && !customName.isBlank()) ? customName.trim() : displayName();
        startMatch(SessionConfig.joining(hostAddress, playerId(), name), onReturnToLauncher);
    }

    public void startMatch(SessionConfig session, Runnable onReturnToLauncher) {
        GameBridge bridge = new GameBridge();
        bridge.setHasLauncher(true);

        final boolean[] matchEnded = new boolean[]{false};
        AtomicBoolean launcherRestored = new AtomicBoolean(false);

        showLoadingScreen(session.host() ? "Preparing your story…" : "Connecting to the host…");

        bridge.setSlotProvider(LocalSaveSlots::load);

        Runnable restoreLauncher = () -> {
            if (!launcherRestored.compareAndSet(false, true)) {
                return;
            }
            removeLoadingScreen();
            if (!matchEnded[0]) {
                onReturnToLauncher.run();
            }
            primaryStage.show();
            if (SessionState.get().isFullscreen()) {
                primaryStage.setFullScreen(true);
            }
            primaryStage.toFront();
            primaryStage.requestFocus();
        };

        bridge.setOnGameReady(() -> Platform.runLater(() -> {
            if (launcherRestored.get()) {
                return;
            }
            removeLoadingScreen();
            primaryStage.hide();
        }));

        // Q/menu exit is a two-phase handoff: prepare and show JavaFX first,
        // wait through a paint pulse, then tell libGDX it is safe to close.
        bridge.setOnReturnToLauncherRequested(closeGameWindow -> Platform.runLater(() -> {
            restoreLauncher.run();
            // The menu is visible now, but the old GameServer still owns its
            // sockets until InfectedHourGame.dispose() completes. Keep Play
            // blocked so a fast second click cannot start a competing host.
            showLoadingScreen("Closing current session…");
            PauseTransition allowLauncherToPaint = new PauseTransition(Duration.millis(150));
            allowLauncherToPaint.setOnFinished(event -> closeGameWindow.run());
            allowLauncherToPaint.play();
        }));

        // Window-X / OS close has no advance handshake, so this remains the
        // fallback. This callback runs after client/server disposal, so it is
        // also the only point that unlocks Play for the next session.
        bridge.setOnGameWindowClosed(() -> Platform.runLater(() -> {
            restoreLauncher.run();
            removeLoadingScreen();
        }));

        bridge.setOnMatchEnded(outcome -> Platform.runLater(() -> {
            matchEnded[0] = true;
            launcherRestored.set(true);
            removeLoadingScreen();
            primaryStage.show();
            if (SessionState.get().isFullscreen()) {
                primaryStage.setFullScreen(true);
            }
            ResultsView view = new ResultsView(primaryStage, backendClient,
                    outcome.result(), outcome.finalLevelReached());
            primaryStage.getScene().setRoot(view.getRoot());
        }));

        bridge.setOnSaveRequested((slotNumber, slotDto) -> {
            // 1. Save to local disk mirror (~/.infectedhour/slots.json) immediately
            LocalSaveSlots.saveSlot(slotDto);
            bridge.notifySaveConfirmed(slotNumber);

            // 2. Sync to Spring Boot backend database if authenticated & online
            if (!SessionState.get().isOfflineMode() && SessionState.get().isAuthenticated()) {
                backendClient.putSaveSlot(slotNumber, slotDto)
                        .thenAccept(saved -> LocalSaveSlots.saveSlot(saved))
                        .exceptionally(ex -> {
                            System.err.println("Backend save sync failed (saved locally): " + ex.getMessage());
                            return null;
                        });
            }
        });

        // Join/connection failures must never leave the player staring at a
        // hidden stage — re-show the launcher and explain what happened
        // (TRD §9: network failures surface as messages, never crashes).
        bridge.setOnJoinFailed(reason -> Platform.runLater(() -> {
            restoreLauncher.run();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection problem");
            alert.setHeaderText(describeJoinFailureHeader(reason));
            alert.setContentText(describeJoinFailure(reason));
            alert.initOwner(primaryStage);
            alert.show();
        }));

        Thread gameThread = new Thread(() -> {
            try {
                Lwjgl3Launcher.boot(session, bridge, SessionState.get().isFullscreen());
            } catch (Throwable error) {
                Platform.runLater(() -> {
                    restoreLauncher.run();
                    showLaunchFailure(error);
                });
            }
        }, "libGDX-window");
        gameThread.setDaemon(false);
        gameThread.start();
    }

    /** Keep the current launcher content visible and place a modal loading layer over it. */
    private void showLoadingScreen(String message) {
        launcherRoot = primaryStage.getScene().getRoot();

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(72, 72);

        Label status = new Label(message);
        status.getStyleClass().add("launch-loading-label");

        VBox card = new VBox(18, spinner, status);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("launch-loading-card");

        StackPane overlay = new StackPane(card);
        overlay.getStyleClass().add("launch-loading-overlay");
        overlay.setPickOnBounds(true);

        loadingRoot = new StackPane(launcherRoot, overlay);
        primaryStage.getScene().setRoot(loadingRoot);
    }

    /** Restore the exact launcher view that was under the loading layer. */
    private void removeLoadingScreen() {
        StackPane wrapper = loadingRoot;
        Parent originalRoot = launcherRoot;
        loadingRoot = null;
        launcherRoot = null;

        if (wrapper != null
                && originalRoot != null
                && primaryStage.getScene().getRoot() == wrapper) {
            // A node cannot simultaneously be a child of the wrapper and the
            // Scene root. Detach it before restoring it, otherwise JavaFX
            // throws and leaves the loading overlay stuck on screen.
            wrapper.getChildren().remove(originalRoot);
            primaryStage.getScene().setRoot(originalRoot);
        }
    }

    private void showLaunchFailure(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Game could not start");
        alert.setHeaderText("The game window failed to open");
        String detail = error.getMessage();
        alert.setContentText(detail == null || detail.isBlank()
                ? "An unexpected error occurred while starting the game."
                : detail);
        alert.initOwner(primaryStage);
        alert.show();
    }

    private static String playerId() {
        PlayerDto player = SessionState.get().getCurrentPlayer();
        return player != null ? player.id().toString() : "offline-" + System.getProperty("user.name", "player");
    }

    private static String displayName() {
        PlayerDto player = SessionState.get().getCurrentPlayer();
        return player != null ? player.displayName() : System.getProperty("user.name", "Player");
    }

    private static String describeJoinFailureHeader(String reason) {
        return GameConstants.REJECT_HOST_LOST.equals(reason) ? "Connection lost" : "Could not join";
    }

    private static String describeJoinFailure(String reason) {
        if (reason == null) {
            return "Connection failed.";
        }
        return switch (reason) {
            case GameConstants.REJECT_LOBBY_FULL ->
                    "That lobby already has two players.";
            case GameConstants.REJECT_VERSION_MISMATCH ->
                    "The two machines are running different versions of the game. Rebuild both from the same commit and retry.";
            case GameConstants.REJECT_TIMEOUT ->
                    "No host answered at that address within "
                            + (GameConstants.CONNECT_TIMEOUT_MS / 1000) + " seconds.\n\n"
                            + "Check that the host has started its lobby, that both laptops are on the same "
                            + "network, and that TCP " + GameConstants.KRYONET_TCP_PORT + " / UDP "
                            + GameConstants.KRYONET_UDP_PORT + " are not blocked by a firewall.";
            case GameConstants.REJECT_HOST_LOST ->
                    "The connection to the host was lost.";
            case "PORTS_BUSY" ->
                    "Ports " + GameConstants.KRYONET_TCP_PORT + "/" + GameConstants.KRYONET_UDP_PORT
                            + " are already in use. Another copy of the game is probably still running.";
            default -> "Connection failed (" + reason + ").";
        };
    }
}
