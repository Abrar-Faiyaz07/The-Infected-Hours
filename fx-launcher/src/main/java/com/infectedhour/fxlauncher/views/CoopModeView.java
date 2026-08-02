package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Fork between the two co-op roles (TRD §5): this laptop either runs the
 * authoritative {@code GameServer} (Host) or connects to one (Join).
 * Both paths land in a lobby that uses LAN UDP discovery to find the other
 * machine before KryoNet's TCP/UDP session is opened.
 */
public class CoopModeView {

    private final VBox root = new VBox(14);

    public CoopModeView(Stage stage, BackendClient backendClient) {
        root.setPadding(new Insets(48));
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("bg-night");

        Label title = new Label("2 PLAYER CO-OP");
        title.getStyleClass().add("title-gold");

        Label hint = new Label("""
                One laptop hosts the match and runs the simulation; the other joins it.
                Both machines must be on the same LAN (or the same phone hotspot).""");
        hint.getStyleClass().add("brand-subtitle");

        Button hostBtn = new Button("Host a match  (you are Elric)");
        hostBtn.setMaxWidth(460);
        hostBtn.getStyleClass().addAll("menu-btn", "menu-btn-primary");
        hostBtn.setOnAction(e -> stage.getScene().setRoot(new HostLobbyView(stage, backendClient).getRoot()));

        Button joinBtn = new Button("Join a match  (you are Jane)");
        joinBtn.setMaxWidth(460);
        joinBtn.getStyleClass().addAll("menu-btn", "menu-btn-primary");
        joinBtn.setOnAction(e -> stage.getScene().setRoot(new JoinLobbyView(stage, backendClient).getRoot()));

        Button backBtn = new Button("Back");
        backBtn.setMaxWidth(460);
        backBtn.getStyleClass().add("menu-btn");
        backBtn.setOnAction(e -> stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));

        root.getChildren().addAll(title, hint, hostBtn, joinBtn, backBtn);
    }

    public VBox getRoot() {
        return root;
    }
}
