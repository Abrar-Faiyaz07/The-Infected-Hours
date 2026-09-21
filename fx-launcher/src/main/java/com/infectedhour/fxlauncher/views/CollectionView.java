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
 * Operative Collection & Lore Archive screen.
 * Displays declassified field dossiers, stats, playstyle perks, and starting equipment
 * for Elric (Field Striker) and Jane (Tactical Scout).
 */
public class CollectionView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(16);

    public CollectionView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
    }

    private void build() {
        root.setPadding(new Insets(24, 40, 24, 40));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bg-night");

        Label eyebrow = new Label("DECLASSIFIED PERSONNEL ARCHIVE  •  ASHGROVE CORPORATION");
        eyebrow.setStyle("-fx-text-fill: #E8B02A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label title = new Label("OPERATIVE COLLECTION");
        title.getStyleClass().add("title-gold");

        Label subtitle = new Label("Review operative backgrounds, tactical perks, and mission combat profiles.");
        subtitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");

        HBox dossiersBox = new HBox(28);
        dossiersBox.setAlignment(Pos.CENTER);
        dossiersBox.setPadding(new Insets(6, 0, 8, 0));

        // ── Dossier 1: ELRIC — Field Striker ──
        VBox elricDossier = createDossierCard(
                "ELRIC — FIELD STRIKER",
                "ROLE: MELEE COMBAT & CONTAINMENT",
                "#38BDF8",
                "Ashgrove Corporation Field Agent",
                "A resilient survivor who thrives under pressure. Built for close-quarters clearing and holding choke points against infected swarms.",
                new String[]{
                        "• Combat Focus: +20% Melee attack speed & knockback",
                        "• Hazard Resistance: 30% slower contamination buildup",
                        "• Melee Power: 50 Damage per hit",
                        "• Healing Surge: 35 HP restore capacity"
                },
                "male_character_select.jpg",
                CharacterType.ELRIC
        );

        // ── Dossier 2: JANE — Tactical Scout ──
        VBox janeDossier = createDossierCard(
                "JANE — TACTICAL SCOUT",
                "ROLE: HIGH AGILITY & OBJECTIVE RUNNER",
                "#34D399",
                "Missing Ashgrove Field Agent",
                "Analytical and razor-sharp. Uses superior agility and tactical blade strikes to secure quarantine zones and rescue survivors before time runs out.",
                new String[]{
                        "• Agility Focus: +15% base speed & fast dash recovery",
                        "• Field Expertise: 35% faster objective interactions",
                        "• Katana Armament: 30 Damage per strike + knockback",
                        "• Field Chemist: Produces 1 Medkit every 60s (50 HP heal)"
                },
                "female_character_select_v3.jpg",
                CharacterType.JANE
        );

        dossiersBox.getChildren().addAll(elricDossier, janeDossier);

        Button backBtn = new Button("Back to Main Menu");
        backBtn.setPrefWidth(240);
        backBtn.getStyleClass().add("menu-btn");
        backBtn.setOnAction(e -> stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot()));

        root.getChildren().addAll(eyebrow, title, subtitle, dossiersBox, backBtn);
    }

    private VBox createDossierCard(String nameTitle, String role, String roleColor,
                                   String affiliation, String bio, String[] perks,
                                   String imageName, CharacterType characterType) {
        VBox card = new VBox(8);
        card.setPrefWidth(420);
        card.setMaxWidth(440);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
                "-fx-background-color: #121A26; " +
                "-fx-border-color: #2D3A4F; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
        );

        // Header with portrait & basic info
        HBox headerBox = new HBox(14);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Image img = loadImage(imageName);
        ImageView imageView = new ImageView();
        if (img != null) {
            imageView.setImage(img);
        }
        imageView.setFitWidth(95);
        imageView.setFitHeight(130);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Rectangle clip = new Rectangle(95, 130);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        imageView.setClip(clip);

        VBox metaBox = new VBox(4);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(nameTitle);
        nameLabel.setStyle("-fx-text-fill: #E8B02A; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label roleLabel = new Label(role);
        roleLabel.setStyle("-fx-text-fill: " + roleColor + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label affLabel = new Label("Affiliation: " + affiliation);
        affLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-style: italic;");

        metaBox.getChildren().addAll(nameLabel, roleLabel, affLabel);
        headerBox.getChildren().addAll(imageView, metaBox);

        // Bio section
        Label bioHeader = new Label("OPERATIVE BACKGROUND");
        bioHeader.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label bioLabel = new Label("\"" + bio + "\"");
        bioLabel.setWrapText(true);
        bioLabel.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 11px; -fx-font-style: italic; -fx-line-spacing: 2px;");

        // Perks section
        Label perksHeader = new Label("TACTICAL PERKS & COMBAT ABILITIES");
        perksHeader.setStyle("-fx-text-fill: #E8B02A; -fx-font-size: 11px; -fx-font-weight: bold;");

        VBox perksList = new VBox(3);
        for (String perk : perks) {
            Label pLabel = new Label(perk);
            pLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 11px;");
            perksList.getChildren().add(pLabel);
        }

        // Deploy button
        Button deployBtn = new Button("DEPLOY OPERATIVE (" + (characterType == CharacterType.ELRIC ? "ELRIC" : "JANE") + ")");
        deployBtn.setMaxWidth(Double.MAX_VALUE);
        deployBtn.getStyleClass().addAll("menu-btn", "menu-btn-primary");
        deployBtn.setOnAction(e -> {
            new GameLauncherBridge(stage, backendClient).startAsHost(characterType, () -> { });
        });

        card.getChildren().addAll(headerBox, bioHeader, bioLabel, perksHeader, perksList, deployBtn);
        return card;
    }

    private Image loadImage(String name) {
        var p2Stream = getClass().getResourceAsStream("/assets/player 2/" + name);
        if (p2Stream != null) return new Image(p2Stream);
        var femaleStream = getClass().getResourceAsStream("/assets/female/" + name);
        if (femaleStream != null) return new Image(femaleStream);
        var stream = getClass().getResourceAsStream("/assets/" + name);
        if (stream != null) return new Image(stream);

        File fP2 = new File("assets/player 2/" + name);
        if (fP2.exists()) return new Image(fP2.toURI().toString());
        File fP2b = new File("../assets/player 2/" + name);
        if (fP2b.exists()) return new Image(fP2b.toURI().toString());
        File f0 = new File("assets/female/" + name);
        if (f0.exists()) return new Image(f0.toURI().toString());
        File f0b = new File("../assets/female/" + name);
        if (f0b.exists()) return new Image(f0b.toURI().toString());
        File f1 = new File("assets/" + name);
        if (f1.exists()) return new Image(f1.toURI().toString());
        File f2 = new File("Images/character_select/" + name);
        if (f2.exists()) return new Image(f2.toURI().toString());
        File f3 = new File("../Images/character_select/" + name);
        if (f3.exists()) return new Image(f3.toURI().toString());
        return null;
    }

    public VBox getRoot() {
        return root;
    }
}
