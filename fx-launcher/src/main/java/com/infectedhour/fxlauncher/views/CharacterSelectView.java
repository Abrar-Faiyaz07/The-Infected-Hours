package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.bridge.GameLauncherBridge;
import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.shared.network.CharacterType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
 * Screen for selecting between Elric (Field Medic) and Jane (Local Scout)
 * prior to starting a Single Player campaign session.
 */
public class CharacterSelectView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(20);

    public CharacterSelectView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
    }

    private void build() {
        root.setPadding(new Insets(28, 48, 28, 48));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bg-night");

        Label title = new Label("CHOOSE YOUR SURVIVOR");
        title.getStyleClass().add("title-gold");

        Label subtitle = new Label("Select your operative for the containment mission.");
        subtitle.setStyle("-fx-text-fill: #A0AAB8; -fx-font-size: 14px;");

        HBox cardsBox = new HBox(36);
        cardsBox.setAlignment(Pos.CENTER);
        cardsBox.setPadding(new Insets(10, 0, 10, 0));

        // ── Card 1: ELRIC ──
        VBox elricCard = createCharacterCard(
                "ELRIC",
                "ROLE: FIELD MEDIC",
                "#38BDF8",
                "Specialized in biological containment, sample analysis, and immune recovery. Holds high infection resistance.",
                "male_character_select.jpg",
                CharacterType.ELRIC
        );

        // ── Card 2: JANE ──
        VBox janeCard = createCharacterCard(
                "JANE",
                "ROLE: LOCAL SCOUT",
                "#34D399",
                "Veteran local survivor with high combat instincts, fast evasion, and deadly machete proficiency.",
                "female_character_select.jpg",
                CharacterType.JANE
        );

        cardsBox.getChildren().addAll(elricCard, janeCard);

        Button backBtn = new Button("Back to Main Menu");
        backBtn.setPrefWidth(260);
        backBtn.getStyleClass().add("menu-btn");
        backBtn.setOnAction(e -> stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));

        Label streamTip = new Label("Stream Tip: In Discord screen share, select 'Screens -> Screen 1' for seamless streaming.");
        streamTip.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        root.getChildren().addAll(title, subtitle, cardsBox, backBtn, streamTip);
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

        // Hover styling
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

        // Character Portrait
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

        Button selectBtn = new Button("SELECT " + name);
        selectBtn.setPrefWidth(240);
        selectBtn.getStyleClass().addAll("menu-btn", "menu-btn-primary");
        selectBtn.setOnAction(e -> {
            new GameLauncherBridge(stage, backendClient).startAsHost(characterType, () -> { });
        });

        card.getChildren().addAll(imageView, nameLabel, roleLabel, descLabel, selectBtn);
        return card;
    }

    private Image loadImage(String name) {
        var stream = getClass().getResourceAsStream("/assets/" + name);
        if (stream != null) {
            return new Image(stream);
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
