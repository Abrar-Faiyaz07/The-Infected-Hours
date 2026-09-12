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
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * FX-side half of the thread boundary (TRD §2). Hides the JavaFX stage,
 * boots the libGDX Lwjgl3Application on a brand-new dedicated thread
 * (never the FX Application Thread), and re-shows the stage via
 * Platform.runLater when core calls back through GameBridge.
 */
public class GameLauncherBridge {

    private final Stage primaryStage;
    private final BackendClient backendClient;

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
        var slot = SessionState.get().getLoadedSlot();
        SessionConfig config = slot != null && slot.occupied()
                ? SessionConfig.hostingFromSave(playerId(), displayName(),
                        SessionState.get().getBackendUrl(), slot)
                : SessionConfig.hosting(playerId(), displayName(),
                        SessionState.get().getBackendUrl());
        // Consumed once — returning to the menu must not silently reload the
        // same save the next time the player presses Play.
        SessionState.get().clearLoadedSlot();
        startMatch(config, onReturnToLauncher);
    }

    /** This laptop joins a host already running on the LAN. */
    public void startAsClient(String hostAddress, Runnable onReturnToLauncher) {
        startMatch(SessionConfig.joining(hostAddress, playerId(), displayName()), onReturnToLauncher);
    }

    public void startMatch(SessionConfig session, Runnable onReturnToLauncher) {
        GameBridge bridge = new GameBridge();

        final boolean[] matchEnded = new boolean[]{false};

        bridge.setSlotProvider(LocalSaveSlots::load);

        bridge.setOnGameWindowClosed(() -> Platform.runLater(() -> {
            primaryStage.show();
            if (!matchEnded[0]) {
                onReturnToLauncher.run();
            }
        }));

        bridge.setOnMatchEnded(outcome -> Platform.runLater(() -> {
            matchEnded[0] = true;
            primaryStage.show();
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
            primaryStage.show();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection problem");
            alert.setHeaderText(describeJoinFailureHeader(reason));
            alert.setContentText(describeJoinFailure(reason));
            alert.initOwner(primaryStage);
            alert.show();
            onReturnToLauncher.run();
        }));

        Platform.runLater(primaryStage::hide);

        Thread gameThread = new Thread(() -> Lwjgl3Launcher.boot(session, bridge), "libGDX-window");
        gameThread.setDaemon(false);
        gameThread.start();
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
