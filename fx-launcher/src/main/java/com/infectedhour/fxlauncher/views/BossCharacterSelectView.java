package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.shared.network.CharacterType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.File;

/**
 * Character selection screen specifically for the Final Boss encounter,
 * allowing the player to lead with either Elric or Jane while the other
 * operative fights alongside as an ally.
 */
public class BossCharacterSelectView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final Parent returnRoot;
    private final VBox root = new VBox(20);

    public BossCharacterSelectView(Stage stage, BackendClient backendClient, Parent returnRoot) {
        this.stage = stage;
        this.backendClient = backendClient;
        this.returnRoot = returnRoot;
        build();
    }

    public BossCharacterSelectView(Stage stage, BackendClient backendClient) {
        this(stage, backendClient, null);
    }

    private void build() {
        root.setPadding(new Insets(28, 48, 28, 48));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bg-night");

        Label title = new Label("FINAL BOSS FIGHT: CHOOSE YOUR OPERATIVE");
        title.getStyleClass().add("title-gold");

        Label subtitle = new Label("Select your lead agent against the Final Mutation. The other agent will fight by your side!");
        subtitle.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 14px; -fx-font-weight: bold;");

        HBox cardsBox = new HBox(36);
        cardsBox.setAlignment(Pos.CENTER);
        cardsBox.setPadding(new Insets(10, 0, 10, 0));

        // ── Card 1: ELRIC ──
        VBox elricCard = createCharacterCard(
                "ELRIC",
                "LEAD: FIELD STRIKER",
                "#38BDF8",
                "High-impact melee & containment specialist.\n+20% Attack speed | Heavy machete cleaves\n(Jane provides tactical scout support)",
                "male_character_select.jpg",
                CharacterType.ELRIC
        );

        // ── Card 2: JANE ──
        VBox janeCard = createCharacterCard(
                "JANE",
                "LEAD: TACTICAL SCOUT",
                "#34D399",
                "High agility & objective runner.\n+15% Movement speed | Rapid katana strikes\n(Elric provides field combat support)",
                "female_character_select_v3.jpg",
                CharacterType.JANE
        );

        cardsBox.getChildren().addAll(elricCard, janeCard);

        Button backBtn = new Button("Back to Main Menu");
        backBtn.setPrefWidth(260);
        backBtn.getStyleClass().add("menu-btn");
        backBtn.setOnAction(e -> {
            if (returnRoot != null) {
                stage.getScene().setRoot(returnRoot);
            } else {
                stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot());
            }
        });

        Label bossTip = new Label("Arena Intel: Destroy the shield injection nodes, then coordinate your strikes against the Final Mutation.");
        bossTip.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");

        root.getChildren().addAll(title, subtitle, cardsBox, backBtn, bossTip);
    }

    private VBox createCharacterCard(String name, String role, String roleColor,
                                     String description, String imageName,
                                     CharacterType characterType) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setMaxWidth(300);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: #151D2A; " +
                "-fx-border-color: #2D3A4F; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
        );

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #1A2434; " +
                "-fx-border-color: #E8B02A; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #151D2A; " +
                "-fx-border-color: #2D3A4F; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
        ));

        Image img = loadImage(imageName);
        ImageView imageView = new ImageView();
        if (img != null) {
            imageView.setImage(img);
        }
        imageView.setFitWidth(230);
        imageView.setFitHeight(310);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Rectangle clip = new Rectangle(230, 310);
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        imageView.setClip(clip);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: #E8B02A; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label roleLabel = new Label(role);
        roleLabel.setStyle("-fx-text-fill: " + roleColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(240);
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 11px; -fx-text-alignment: center;");

        Button selectBtn = new Button("FIGHT AS " + name);
        selectBtn.setPrefWidth(240);
        selectBtn.getStyleClass().addAll("menu-btn", "menu-btn-primary");
        selectBtn.setOnAction(e -> {
            new GameLauncherBridge(stage, backendClient).startBossFight(characterType, () -> {
                if (returnRoot != null) {
                    stage.getScene().setRoot(returnRoot);
                } else {
                    stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot());
                }
            });
        });

        card.getChildren().addAll(imageView, nameLabel, roleLabel, descLabel, selectBtn);
        return card;
    }

    private Image loadImage(String name) {
        var p2Stream = getClass().getResourceAsStream("/assets/player 2/" + name);
        if (p2Stream != null) {
            return new Image(p2Stream);
        }
        var femaleStream = getClass().getResourceAsStream("/assets/female/" + name);
        if (femaleStream != null) {
            return new Image(femaleStream);
        }
        var stream = getClass().getResourceAsStream("/assets/" + name);
        if (stream != null) {
            return new Image(stream);
        }
        File fP2 = new File("assets/player 2/" + name);
        if (fP2.exists()) {
            return new Image(fP2.toURI().toString());
        }
        File fP2b = new File("../assets/player 2/" + name);
        if (fP2b.exists()) {
            return new Image(fP2b.toURI().toString());
        }
        File f0 = new File("assets/female/" + name);
        if (f0.exists()) {
            return new Image(f0.toURI().toString());
        }
        File f0b = new File("../assets/female/" + name);
        if (f0b.exists()) {
            return new Image(f0b.toURI().toString());
        }
        File f1 = new File("assets/" + name);
        if (f1.exists()) {
            return new Image(f1.toURI().toString());
        }
        File f2 = new File("Images/character_select/" + name);
        if (f2.exists()) {
            return new Image(f2.toURI().toString());
        }
        File f3 = new File("../Images/character_select/" + name);
        if (f3.exists()) {
            return new Image(f3.toURI().toString());
        }
        return null;
    }

    public VBox getRoot() {
        return root;
    }
}
