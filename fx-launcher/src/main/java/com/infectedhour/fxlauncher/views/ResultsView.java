package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Screen 9 (UI/UX doc §2): cinematic victory/defeat report. The background
 * and copy react to the match outcome while the level value remains real data
 * supplied by the core game.
 */
public class ResultsView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final StackPane root = new StackPane();

    public ResultsView(Stage stage, BackendClient backendClient, String result, int finalLevelReached) {
        this.stage = stage;
        this.backendClient = backendClient;
        build(result, finalLevelReached);
    }

    private void build(String result, int finalLevelReached) {
        boolean victory = "VICTORY".equalsIgnoreCase(result);
        root.setMinSize(900, 560);
        root.getStyleClass().setAll("results-root", victory ? "results-victory" : "results-defeat");

        Region overlay = new Region();
        overlay.getStyleClass().add("results-overlay");

        Label section = new Label(victory ? "FINAL OPERATION REPORT" : "EMERGENCY FIELD REPORT");
        section.getStyleClass().add("results-eyebrow");

        Label banner = new Label(victory ? "ASHGROVE\nSECURED" : "CONTAINMENT\nFAILED");
        banner.getStyleClass().addAll("results-title", victory ? "results-title-victory" : "results-title-defeat");

        Label narrative = new Label(victory
                ? "The Virus Heart is silent. The district survived the night."
                : "The infection breached the perimeter. Regroup, rearm, and return before Ashgrove is lost.");
        narrative.setWrapText(true);
        narrative.setMaxWidth(520);
        narrative.getStyleClass().add("results-description");

        VBox levelStat = resultStat("DISTRICT REACHED", String.format("LEVEL %02d", finalLevelReached));
        VBox outcomeStat = resultStat("OPERATION STATUS", victory ? "COMPLETE" : "OVERRUN");
        outcomeStat.getStyleClass().add(victory ? "result-stat-success" : "result-stat-danger");

        HBox stats = new HBox(42, levelStat, outcomeStat);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getStyleClass().add("results-stats-card");

        Button playAgainBtn = new Button(victory ? "PLAY AGAIN" : "RETRY MISSION");
        playAgainBtn.getStyleClass().add("results-primary-btn");
        playAgainBtn.setOnAction(e -> returnToMenu());

        Button menuBtn = new Button("HOME MENU");
        menuBtn.getStyleClass().add("results-secondary-btn");
        menuBtn.setOnAction(e -> returnToMenu());

        HBox actions = new HBox(12, playAgainBtn, menuBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox report = new VBox(12, section, banner, narrative, stats, actions);
        report.setMaxWidth(560);

        AnchorPane content = new AnchorPane(report);
        AnchorPane.setLeftAnchor(report, 64.0);
        AnchorPane.setBottomAnchor(report, 64.0);

        Label cornerMark = new Label("THE INFECTED HOUR  /  AFTER ACTION");
        cornerMark.getStyleClass().add("results-corner-mark");
        content.getChildren().add(cornerMark);
        AnchorPane.setRightAnchor(cornerMark, 48.0);
        AnchorPane.setTopAnchor(cornerMark, 32.0);

        root.getChildren().addAll(overlay, content);
    }

    private VBox resultStat(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("result-stat-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("result-stat-value");
        return new VBox(3, label, value);
    }

    private void returnToMenu() {
        stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot());
    }

    public StackPane getRoot() {
        return root;
    }
}
