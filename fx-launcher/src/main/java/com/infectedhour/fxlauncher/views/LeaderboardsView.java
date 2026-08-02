package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Screen 7 (UI/UX doc §2): tabs Fastest Clear / Lowest Contamination / Boss Time, top 20 rows. */
public class LeaderboardsView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(12);
    private final ListView<String> rows = new ListView<>();

    public LeaderboardsView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
    }

    private void build() {
        root.setPadding(new Insets(32));

        TabPane tabs = new TabPane();
        // Board names per Backend Schema §3: FASTEST_L1..L3, LOWEST_CONTAM_L1..L3, BOSS_TIME
        for (String board : new String[]{"FASTEST_L1", "FASTEST_L2", "FASTEST_L3",
                "LOWEST_CONTAM_L1", "LOWEST_CONTAM_L2", "LOWEST_CONTAM_L3", "BOSS_TIME"}) {
            Tab tab = new Tab(board);
            tab.setOnSelectionChanged(e -> {
                if (tab.isSelected()) loadBoard(board);
            });
            tabs.getTabs().add(tab);
        }

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));

        root.getChildren().addAll(tabs, rows, backBtn);
    }

    private void loadBoard(String board) {
        backendClient.getLeaderboard(board, 20)
                .thenAccept(entries -> Platform.runLater(() -> {
                    rows.getItems().clear();
                    entries.forEach(entry -> rows.getItems().add(
                            "#" + entry.rank() + "  " + entry.displayName() + "  " + entry.value()));
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> rows.getItems().setAll("Could not load leaderboard: " + ex.getMessage()));
                    return null;
                });
    }

    public VBox getRoot() {
        return root;
    }
}
