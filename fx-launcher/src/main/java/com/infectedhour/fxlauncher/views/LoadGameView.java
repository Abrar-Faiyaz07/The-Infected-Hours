package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.save.LocalSaveSlots;
import com.infectedhour.fxlauncher.state.SessionState;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.SaveSlotDto;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Load Game — nine manual save slots in a 3x3 grid, Resident Evil style.
 *
 * <p>Each card shows where the player was (level + checkpoint) and what they
 * were carrying (HP, contamination, playtime), so a slot can be identified at a
 * glance without loading it.
 *
 * <p><b>Data source order.</b> The backend copy is authoritative and, when
 * fetched successfully, is written through to the local cache. If the call
 * fails — offline mode, no server running, host laptop asleep — the cached copy
 * is shown instead. The screen therefore always renders something and never
 * blocks on the network, which matters because the backend is optional by
 * design.
 */
public class LoadGameView {

    private static final DateTimeFormatter SAVED_AT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(16);
    private final GridPane grid = new GridPane();
    private final Label status = new Label();

    private List<SaveSlotDto> slots = LocalSaveSlots.load();

    public LoadGameView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
        renderSlots();
        refreshFromBackend();
    }

    private void build() {
        root.setPadding(new Insets(40));
        root.getStyleClass().add("bg-night");

        Label title = new Label("LOAD GAME");
        title.getStyleClass().add("title-gold");

        Label hint = new Label("Select a save slot. Nine slots are available.");
        hint.getStyleClass().add("brand-subtitle");

        status.getStyleClass().add("version-label");

        grid.setHgap(14);
        grid.setVgap(14);

        Button backBtn = new Button("Back");
        backBtn.setMaxWidth(240);
        backBtn.getStyleClass().add("menu-btn");
        backBtn.setOnAction(e -> stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));

        root.getChildren().addAll(title, hint, grid, status, backBtn);
    }

    private void renderSlots() {
        grid.getChildren().clear();
        for (int i = 0; i < GameConstants.SAVE_SLOT_COUNT; i++) {
            grid.add(slotCard(slots.get(i)), i % 3, i / 3);
        }
    }

    private Region slotCard(SaveSlotDto slot) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        // 175 not 150: an occupied card stacks a heading, level, checkpoint,
        // stats, timestamp and a button row. At 150 a long checkpoint name
        // pushes the Load/Delete buttons past the bottom edge.
        card.setPrefSize(290, 175);
        card.setMinSize(290, 175);
        card.getStyleClass().add(slot.occupied() ? "slot-card" : "slot-card-empty");

        Label heading = new Label("SLOT " + slot.slotNumber());
        heading.getStyleClass().add("slot-number");

        if (!slot.occupied()) {
            Label empty = new Label("— Empty —");
            empty.getStyleClass().add("slot-empty-text");
            StackPane centre = new StackPane(empty);
            VBox.setVgrow(centre, Priority.ALWAYS);
            card.getChildren().addAll(heading, centre);
            return card;
        }

        Label where = new Label("Level " + slot.levelNumber()
                + (slot.levelName() == null ? "" : " — " + slot.levelName()));
        where.getStyleClass().add("slot-title");

        Label checkpoint = new Label(slot.checkpointName() == null
                ? "Checkpoint: —"
                : slot.checkpointName());
        checkpoint.getStyleClass().add("slot-detail");

        Label stats = new Label(String.format("%s   HP %.0f",
                slot.formattedPlaytime(), slot.playerHp()));
        stats.getStyleClass().add("slot-detail");

        Label when = new Label(slot.savedAt() == null ? "" : SAVED_AT.format(Instant.parse(slot.savedAt())));
        when.getStyleClass().add("slot-detail-muted");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button loadBtn = new Button("Load");
        loadBtn.getStyleClass().addAll("menu-btn", "menu-btn-primary");
        loadBtn.setOnAction(e -> loadSlot(slot));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().addAll("menu-btn", "menu-btn-danger");
        deleteBtn.setOnAction(e -> confirmDelete(slot));

        HBox actions = new HBox(8, loadBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(heading, where, checkpoint, stats, when, spacer, actions);
        return card;
    }

    /** Backend wins when reachable; otherwise keep showing the cached copy. */
    private void refreshFromBackend() {
        if (SessionState.get().isOfflineMode() || !SessionState.get().isAuthenticated()) {
            status.setText("Offline — showing saves stored on this machine ("
                    + LocalSaveSlots.file() + ")");
            return;
        }
        status.setText("Syncing saves…");
        backendClient.getSaveSlots()
                .thenAccept(fetched -> Platform.runLater(() -> {
                    slots = fetched;
                    LocalSaveSlots.save(fetched); // write through so offline still works
                    renderSlots();
                    status.setText("Synced with the server.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> status.setText(
                            "Server unreachable — showing saves stored on this machine."));
                    return null;
                });
    }

    private void loadSlot(SaveSlotDto slot) {
        SessionState.get().setLoadedSlot(slot);
        new GameLauncherBridge(stage, backendClient).startAsHost(() ->
                stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));
    }

    private void confirmDelete(SaveSlotDto slot) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete save");
        confirm.setHeaderText("Delete slot " + slot.slotNumber() + "?");
        confirm.setContentText("This cannot be undone.");
        confirm.initOwner(stage);
        confirm.showAndWait().ifPresent(button -> {
            if (button != ButtonType.OK) {
                return;
            }
            LocalSaveSlots.deleteSlot(slot.slotNumber());
            slots = LocalSaveSlots.load();
            renderSlots();

            if (!SessionState.get().isOfflineMode() && SessionState.get().isAuthenticated()) {
                backendClient.deleteSaveSlot(slot.slotNumber())
                        .exceptionally(ex -> {
                            Platform.runLater(() -> status.setText(
                                    "Deleted locally, but the server could not be reached."));
                            return null;
                        });
            }
        });
    }

    public VBox getRoot() {
        return root;
    }
}
