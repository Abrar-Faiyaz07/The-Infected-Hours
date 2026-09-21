package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.state.SessionState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.List;

public class MainMenuController {

    private static final String VERSION = "v0.1-alpha";

    private static double getMenuMusicVolume() {
        return Math.max(0.0, Math.min(1.0, SessionState.get().getMusicVolumePercent() / 100.0));
    }

    public static void updateMenuMusicVolume(double volumePercent) {
        if (mainMenuMusic != null) {
            try {
                double vol = Math.max(0.0, Math.min(1.0, volumePercent / 100.0));
                mainMenuMusic.setVolume(vol);
            } catch (Exception ignored) {
            }
        }
    }

    @FXML private Label usernameLabel;
    @FXML private Label statusChip;
    @FXML private Label versionLabel;
    @FXML private Label menuDescription;
    @FXML private StackPane rootPane;
    @FXML private Button singlePlayerBtn;
    @FXML private Button coopButton;
    @FXML private Button continueButton;
    @FXML private Button settingsButton;
    @FXML private Button intelButton;
    @FXML private Button debugButton;
    @FXML private Button bossButton;
    @FXML private Button exitButton;

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

        configureMenuNavigation();
        playMainMenuMusic();
    }

    private void configureMenuNavigation() {
        List<Button> buttons = List.of(
                singlePlayerBtn, coopButton, continueButton,
                settingsButton, intelButton, debugButton, bossButton, exitButton);
        List<String> descriptions = List.of(
                "Begin the Ashgrove containment operation.",
                "Host or join a two-operative survival session.",
                "Resume the campaign from a saved checkpoint.",
                "Configure display, audio, subtitles, and connection settings.",
                "Open mission intelligence and project information.",
                "Developer split-screen testing tools.",
                "Developer shortcut to the final encounter.",
                "Close The Infected Hour.");

        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            int index = i;

            button.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                if (isFocused) {
                    menuDescription.setText(descriptions.get(index));
                }
            });
            button.setOnMouseEntered(event -> {
                button.requestFocus();
                menuDescription.setText(descriptions.get(index));
            });
            button.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                    int direction = event.getCode() == KeyCode.UP ? -1 : 1;
                    buttons.get(Math.floorMod(index + direction, buttons.size())).requestFocus();
                    event.consume();
                }
            });
        }

        Platform.runLater(singlePlayerBtn::requestFocus);
    }

    private void playMainMenuMusic() {
        // The music is shared across all JavaFX launcher/menu screens.
        // If it is already playing, do not restart it when MainMenuController
        // is recreated or when the user returns to the main menu.
        double currentVolume = getMenuMusicVolume();
        if (mainMenuMusic != null) {
            try {
                mainMenuMusic.setCycleCount(MediaPlayer.INDEFINITE);
                mainMenuMusic.setVolume(currentVolume);

                if (!mainMenuMusic.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                    mainMenuMusic.play();
                }

                System.out.println("[MainMenuMusic] Menu music already initialized at volume " + currentVolume);
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
            mainMenuMusic.setVolume(currentVolume);

            mainMenuMusic.setOnReady(() -> {
                System.out.println(
                        "[MainMenuMusic] READY - starting playback at volume "
                                + currentVolume
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
        navigate(new DebugCoopView(stage, backendClient).getRoot());
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
        navigate(new IntelView(stage, backendClient).getRoot());
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
