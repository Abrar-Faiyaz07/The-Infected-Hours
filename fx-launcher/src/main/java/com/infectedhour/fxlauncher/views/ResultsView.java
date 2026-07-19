package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Screen 9 (UI/UX doc §2): victory/defeat banner, per-player stats table,
 * "New leaderboard entry!" badge, Play Again / Menu. Populated from the
 * GameBridge.MatchOutcome delivered when core calls notifyMatchEnded.
 */
public class ResultsView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(12);

    public ResultsView(Stage stage, BackendClient backendClient, String result, int finalLevelReached) {
        this.stage = stage;
        this.backendClient = backendClient;
        build(result, finalLevelReached);
    }

    private void build(String result, int finalLevelReached) {
        root.setPadding(new Insets(32));

        Label banner = new Label(result.equals("VICTORY") ? "VICTORY" : "DEFEAT");
        Label levelLabel = new Label("Reached level " + finalLevelReached);
        // ============ TEAMMATE TASK: RESULTS CONTENT ============
        // TODO(fx): UI/UX doc par.2 screen 9:
        //  1. TableView with one row per player: character, damage, rescues,
        //     samples, medicine, barricades, sanitations, downs, revives.
        //     Easiest source: pass the ParticipantStats the game POSTed
        //     through the bridge alongside MatchOutcome.
        //  2. "New leaderboard entry!" badge: extend /matches/{id}/complete
        //     to RETURN which boards improved, or simply re-fetch the boards
        //     and compare — either works at this scale.
        // ========================================================

        Button playAgainBtn = new Button("Play Again");
        playAgainBtn.setOnAction(e -> stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot())); // roles kept per App Flow §1

        Button menuBtn = new Button("Main Menu");
        menuBtn.setOnAction(e -> stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));

        root.getChildren().addAll(banner, levelLabel, playAgainBtn, menuBtn);
    }

    public VBox getRoot() {
        return root;
    }
}
