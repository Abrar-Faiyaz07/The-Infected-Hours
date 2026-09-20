package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Full-screen project and developer credits shown from the main menu Intel entry. */
public class IntelView {
    private static final String VERSION = "v0.1-alpha";

    private final Stage stage;
    private final BackendClient backendClient;
    private final StackPane root = new StackPane();
    private final Label copyStatus = new Label("SELECT A CONTACT CHANNEL TO COPY");

    public IntelView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
    }

    private void build() {
        root.getStyleClass().add("intel-root");
        Region overlay = new Region();
        overlay.getStyleClass().add("intel-overlay");

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("intel-shell");
        shell.setPadding(new Insets(28, 54, 34, 54));
        shell.setTop(buildHeader());

        VBox page = new VBox(24);
        page.setAlignment(Pos.TOP_CENTER);
        page.setMaxWidth(1180);
        page.getChildren().addAll(buildHero(), buildDeveloperCards(), buildProjectStrip(), buildFooter());

        ScrollPane scroll = new ScrollPane(page);
        scroll.getStyleClass().add("intel-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPadding(new Insets(30, 0, 0, 0));
        shell.setCenter(scroll);

        root.getChildren().addAll(overlay, shell);
        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) returnToMenu();
        });
    }

    private Node buildHeader() {
        BorderPane header = new BorderPane();
        header.getStyleClass().add("intel-header");

        Button back = new Button("←  COMMAND CENTER");
        back.getStyleClass().add("intel-back-link");
        back.setOnAction(event -> returnToMenu());

        VBox brand = new VBox(-2,
                styledLabel("THE INFECTED HOUR", "intel-brand"),
                styledLabel("CLASSIFIED PROJECT DOSSIER", "intel-brand-sub"));
        brand.setAlignment(Pos.CENTER);

        VBox status = new VBox(2,
                styledLabel("●  INTEL DECRYPTED", "intel-status"),
                styledLabel("BUILD " + VERSION, "intel-version"));
        status.setAlignment(Pos.CENTER_RIGHT);

        header.setLeft(back);
        header.setCenter(brand);
        header.setRight(status);
        return header;
    }

    private Node buildHero() {
        VBox hero = new VBox(6);
        hero.setAlignment(Pos.CENTER);
        hero.getChildren().addAll(
                styledLabel("INTEL ARCHIVE  /  DEVELOPMENT UNIT", "intel-eyebrow"),
                styledLabel("THE TEAM BEHIND THE OUTBREAK", "intel-page-title"),
                styledLabel("Three developers. One containment mission. Built for CSE 4402 Visual Programming Lab.",
                        "intel-page-subtitle"));
        return hero;
    }

    private Node buildDeveloperCards() {
        HBox cards = new HBox(18,
                developerCard("SK", "DEVELOPER 01", "SADNAN KIBRIA", "230041119",
                        "dotan1774@gmail.com", "github.com/SadnanKibria"),
                developerCard("AF", "DEVELOPER 02", "ABRAR FAIYAZ", "230041143",
                        "abrarsamin04@gmail.com", "github.com/Abrar-Faiyaz07"),
                developerCard("AK", "DEVELOPER 03", "ASHIQUE KHAN", "230041153",
                        "ashiquek056@gmail.com", "github.com/Ashique-7"));
        cards.getStyleClass().add("intel-card-row");
        cards.setAlignment(Pos.TOP_CENTER);
        for (Node card : cards.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        return cards;
    }

    private Node developerCard(String initials, String unit, String name, String studentId,
                               String email, String github) {
        VBox card = new VBox(12);
        card.getStyleClass().add("intel-developer-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox identity = new HBox(14);
        identity.setAlignment(Pos.CENTER_LEFT);
        Label avatar = styledLabel(initials, "intel-avatar");
        VBox identityText = new VBox(2,
                styledLabel(unit, "intel-unit-label"),
                styledLabel(name, "intel-developer-name"),
                styledLabel("STUDENT ID  /  " + studentId, "intel-student-id"));
        identity.getChildren().addAll(avatar, identityText);

        Region divider = new Region();
        divider.getStyleClass().add("intel-divider");

        card.getChildren().addAll(identity, divider,
                contactRow("EMAIL", email),
                contactRow("GITHUB", github));
        return card;
    }

    private Node contactRow(String channel, String value) {
        VBox text = new VBox(2,
                styledLabel(channel, "intel-contact-label"),
                styledLabel(value, "intel-contact-value"));
        HBox.setHgrow(text, Priority.ALWAYS);

        Button copy = new Button("COPY");
        copy.getStyleClass().add("intel-copy-btn");
        copy.setOnAction(event -> copyContact(channel, value));

        HBox row = new HBox(10, text, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("intel-contact-row");
        return row;
    }

    private Node buildProjectStrip() {
        HBox strip = new HBox(34);
        strip.setAlignment(Pos.CENTER);
        strip.getStyleClass().add("intel-project-strip");
        strip.getChildren().addAll(
                projectFact("PROJECT", "THE INFECTED HOUR"),
                projectFact("COURSE", "CSE 4402"),
                projectFact("INSTITUTION", "ISLAMIC UNIVERSITY OF TECHNOLOGY"),
                projectFact("STATUS", "ACTIVE BUILD"));
        return strip;
    }

    private Node projectFact(String label, String value) {
        VBox fact = new VBox(3,
                styledLabel(label, "intel-fact-label"),
                styledLabel(value, "intel-fact-value"));
        fact.setAlignment(Pos.CENTER);
        HBox.setHgrow(fact, Priority.ALWAYS);
        return fact;
    }

    private Node buildFooter() {
        HBox footer = new HBox(18);
        footer.setAlignment(Pos.CENTER);
        copyStatus.getStyleClass().add("intel-copy-status");

        Button back = new Button("RETURN TO COMMAND CENTER");
        back.getStyleClass().add("intel-return-btn");
        back.setOnAction(event -> returnToMenu());
        footer.getChildren().addAll(copyStatus, back);
        return footer;
    }

    private void copyContact(String channel, String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
        copyStatus.setText(channel + " COPIED TO CLIPBOARD");
    }

    private Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private void returnToMenu() {
        stage.getScene().setRoot(new MainMenuView(stage, backendClient).getRoot());
    }

    public StackPane getRoot() {
        return root;
    }
}
