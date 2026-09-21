package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.state.SessionState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

public class MainMenuController {

    private static final String VERSION = "v0.1-alpha";

    // JavaFX MediaPlayer volume range is 0.0 -> 1.0.
    private static final double MAIN_MENU_MUSIC_VOLUME = 1.0;

    @FXML private Label usernameLabel;
    @FXML private Label statusChip;
    @FXML private Label versionLabel;
    @FXML private StackPane rootPane;

    private Stage stage;
    private BackendClient backendClient;

    private static MediaPlayer mainMenuMusic;

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

        playMainMenuMusic();
    }

    private void playMainMenuMusic() {
        // The music is shared across all JavaFX launcher/menu screens.
        // If it is already playing, do not restart it when MainMenuController
        // is recreated or when the user returns to the main menu.
        if (mainMenuMusic != null) {
            try {
                mainMenuMusic.setCycleCount(MediaPlayer.INDEFINITE);
                mainMenuMusic.setVolume(MAIN_MENU_MUSIC_VOLUME);

                if (!mainMenuMusic.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                    mainMenuMusic.play();
                }

                System.out.println("[MainMenuMusic] Menu music already initialized.");
                return;
            } catch (Exception e) {
                System.err.println("[MainMenuMusic] Existing player was invalid; recreating it.");
                try {
                    mainMenuMusic.dispose();
                } catch (Exception ignored) {
                }
                mainMenuMusic = null;
            }
        }

        try {
            String mediaUrl = null;

            File musicFile = new File("assets/music/main_menu.mp3");

            System.out.println("[MainMenuMusic] Working directory: "
                    + new File(".").getAbsolutePath());
            System.out.println("[MainMenuMusic] Checking file: "
                    + musicFile.getAbsolutePath());
            System.out.println("[MainMenuMusic] File exists: "
                    + musicFile.exists());

            if (musicFile.exists()) {
                mediaUrl = musicFile.toURI().toString();
            } else {
                URL resource = getClass().getResource("/music/main_menu.mp3");

                if (resource != null) {
                    mediaUrl = resource.toExternalForm();
                    System.out.println("[MainMenuMusic] Using classpath resource: "
                            + mediaUrl);
                }
            }

            if (mediaUrl == null) {
                System.err.println("[MainMenuMusic] main_menu.mp3 was not found.");
                return;
            }

            Media media = new Media(mediaUrl);
            mainMenuMusic = new MediaPlayer(media);

            mainMenuMusic.setCycleCount(MediaPlayer.INDEFINITE);
            mainMenuMusic.setVolume(MAIN_MENU_MUSIC_VOLUME);

            mainMenuMusic.setOnReady(() -> {
                System.out.println(
                        "[MainMenuMusic] READY - starting playback at volume "
                                + MAIN_MENU_MUSIC_VOLUME
                );
                mainMenuMusic.play();
            });

            mainMenuMusic.setOnPlaying(() ->
                    System.out.println("[MainMenuMusic] PLAYING"));

            mainMenuMusic.setOnError(() ->
                    System.err.println(
                            "[MainMenuMusic] PLAYER ERROR: "
                                    + mainMenuMusic.getError()
                    )
            );

            media.setOnError(() ->
                    System.err.println(
                            "[MainMenuMusic] MEDIA ERROR: "
                                    + media.getError()
                    )
            );

        } catch (Exception e) {
            System.err.println("[MainMenuMusic] Failed to initialize menu music.");
            e.printStackTrace();
        }
    }

    /**
     * Call this exactly when the real libGDX game is about to launch.
     * Menu-to-menu JavaFX navigation should NOT call this.
     */
    public static void stopMenuMusicForGame() {
        if (mainMenuMusic != null) {
            try {
                mainMenuMusic.stop();
                mainMenuMusic.dispose();
            } catch (Exception ignored) {
            }

            mainMenuMusic = null;
            System.out.println("[MainMenuMusic] STOPPED - gameplay is starting.");
        }
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
        new GameLauncherBridge(stage, backendClient)
                .startDebugLocalCoop(() -> stage.getScene().setRoot(rootPane));
    }

    @FXML
    private void onFinalBossFight() {
        navigate(new BossCharacterSelectView(
                stage,
                backendClient,
                rootPane
        ).getRoot());
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("The Infected Hour — " + VERSION);
        alert.setContentText("""
                2D top-down co-op action game. Contain the epidemic, destroy the Virus Heart.

                CSE 4402 Visual Programming Lab
                Islamic University of Technology

                Asset credits: see ASSETS_CREDITS.md""");
        alert.initOwner(stage);
        alert.showAndWait();
    }

    @FXML
    private void onQuit() {
        stopMenuMusicForGame();
        Platform.exit();
    }

    private void navigate(Parent root) {
        stage.getScene().setRoot(root);
    }
}
