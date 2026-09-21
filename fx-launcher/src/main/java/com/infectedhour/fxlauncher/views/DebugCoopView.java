package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated Debug Split-Screen Co-Op & Map Testing Screen:
 * Exclusively loaded when clicking "DEBUG CO-OP SPLIT SCREEN" from the main menu.
 * Allows choosing any of the 6 maps, toggling collision block overlay, and testing interactions.
 */
public class DebugCoopView {

    public record LevelOption(int number, String name, String mapFile) {
        @Override
        public String toString() {
            return "Level " + number + ": " + name + "  [" + mapFile + "]";
        }
    }

    private final VBox root = new VBox(12);
    private int selectedLevel = 1;
    private final List<Button> mapButtons = new ArrayList<>();
    private final Label selectedSummaryLabel = new Label();
    private final ComboBox<LevelOption> levelCombo = new ComboBox<>();

    private static final String CARD_DEFAULT_STYLE =
            "-fx-background-color: #1C2536; -fx-border-color: #2E3A50; -fx-border-width: 1px; " +
            "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-text-fill: #F2F0E9; " +
            "-fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-padding: 8 14 8 14; -fx-font-size: 12px;";

    private static final String CARD_SELECTED_STYLE =
            "-fx-background-color: linear-gradient(to bottom right, rgba(108, 77, 19, 0.96), rgba(36, 40, 22, 0.98)); " +
            "-fx-border-color: #E8B02A; -fx-border-width: 2.5px; " +
            "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-text-fill: #FFFFFF; " +
            "-fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-padding: 8 14 8 14; -fx-font-size: 12px; -fx-font-weight: bold;";

    public DebugCoopView(Stage stage, BackendClient backendClient) {
        root.setPadding(new Insets(26, 44, 26, 44));
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("bg-night");

        Label title = new Label("DEBUG CO-OP & MAP SELECT (MAPS 1 - 6)");
        title.getStyleClass().add("title-gold");

        Label hint = new Label(
                "Select any map to test its opening cinematic, subtitles, gameplay, co-op interactions, and collision blocks in split-screen.");
        hint.getStyleClass().add("brand-subtitle");

        List<LevelOption> levels = List.of(
                new LevelOption(1, "Hospital Ground Floor", "map1.png"),
                new LevelOption(2, "Hospital Second Floor", "map1_part2.png"),
                new LevelOption(3, "Roadside Outskirts", "map2.png"),
                new LevelOption(4, "Subterranean Corridor", "map2_part2.png"),
                new LevelOption(5, "Research Facility", "map3.png"),
                new LevelOption(6, "Secret Laboratory & Final Boss", "map_final.png")
        );

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setMaxWidth(820);

        for (int i = 0; i < levels.size(); i++) {
            final LevelOption opt = levels.get(i);
            Button btn = new Button("LEVEL " + opt.number() + "\n" + opt.name() + "\n[" + opt.mapFile() + "]");
            btn.setPrefWidth(260);
            btn.setPrefHeight(64);
            btn.setStyle(CARD_DEFAULT_STYLE);

            final int lvlNum = opt.number();
            btn.setOnAction(e -> selectLevel(lvlNum, levels));
            mapButtons.add(btn);

            int col = i % 3;
            int row = i / 3;
            grid.add(btn, col, row);
        }

        levelCombo.setMaxWidth(400);
        levelCombo.getItems().addAll(levels);
        levelCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(LevelOption option) {
                return option != null ? option.toString() : "";
            }

            @Override
            public LevelOption fromString(String string) {
                return null;
            }
        });
        levelCombo.setOnAction(e -> {
            LevelOption sel = levelCombo.getValue();
            if (sel != null && sel.number() != selectedLevel) {
                selectLevel(sel.number(), levels);
            }
        });

        HBox dropdownRow = new HBox(12, new Label("Quick Select:"), levelCombo);
        dropdownRow.setAlignment(Pos.CENTER_LEFT);
        dropdownRow.setStyle("-fx-text-fill: #94a3b8;");

        selectedSummaryLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 14px; -fx-font-weight: bold;");

        CheckBox collisionBox = new CheckBox("Show Collision Blocks Overlay during play [Hotkey: C / F1]");
        collisionBox.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 13px; -fx-font-weight: bold;");

        Button splitScreenBtn = new Button("▶  Play Cinematic + Split-Screen Debug Co-Op");
        splitScreenBtn.setMaxWidth(520);
        splitScreenBtn.getStyleClass().addAll("menu-btn", "menu-btn-primary");
        splitScreenBtn.setOnAction(e -> {
            boolean showColl = collisionBox.isSelected();
            new GameLauncherBridge(stage, backendClient).startDebugLocalCoop(selectedLevel, showColl, () ->
                    stage.getScene().setRoot(root));
        });

        Button backBtn = new Button("←  Back to Main Menu");
        backBtn.setMaxWidth(520);
        backBtn.getStyleClass().add("menu-btn");
        backBtn.setOnAction(e -> stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));

        Label controlsHint = new Label(
                "P1 (Elric): WASD | Space (Attack) | E (Rescue) | X (Command) | H/[3] (Heal)\n" +
                "P2 (Jane): Arrow Keys | Num 0 (Attack) | Num 3/[.] (Rescue) | Num 7/[,] (Command) | Num 9 (Heal)\n" +
                "Cinematic: E/Space/Enter/Click (Reveal or Continue) | S (Skip Sequence)\n" +
                "In-Game Collision Toggle: Press [C] or [F1], or click [COLLISION: ON/OFF] HUD button.");
        controlsHint.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        root.getChildren().addAll(
                title,
                hint,
                grid,
                dropdownRow,
                selectedSummaryLabel,
                collisionBox,
                splitScreenBtn,
                backBtn,
                controlsHint
        );

        selectLevel(1, levels);
    }

    private void selectLevel(int levelNumber, List<LevelOption> levels) {
        this.selectedLevel = Math.max(1, Math.min(6, levelNumber));
        LevelOption currentOption = null;
        for (LevelOption opt : levels) {
            if (opt.number() == selectedLevel) {
                currentOption = opt;
                break;
            }
        }

        for (int i = 0; i < mapButtons.size(); i++) {
            if (i == selectedLevel - 1) {
                mapButtons.get(i).setStyle(CARD_SELECTED_STYLE);
            } else {
                mapButtons.get(i).setStyle(CARD_DEFAULT_STYLE);
            }
        }

        if (currentOption != null) {
            levelCombo.getSelectionModel().select(currentOption);
            selectedSummaryLabel.setText("Selected Map: Level " + currentOption.number() + " — "
                    + currentOption.name() + " (" + currentOption.mapFile() + ")");
        }
    }

    public VBox getRoot() {
        return root;
    }
}
