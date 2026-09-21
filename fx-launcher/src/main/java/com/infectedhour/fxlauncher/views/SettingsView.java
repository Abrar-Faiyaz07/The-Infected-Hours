package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.state.SessionState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/** Screen 8 (UI/UX doc §2): audio, display and advanced connection settings. */
public class SettingsView {
    private final Stage stage;
    private final BackendClient backendClient;
    private final StackPane root = new StackPane();

    public SettingsView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
    }

    private void build() {
        root.getStyleClass().add("settings-root");
        Region overlay = new Region();
        overlay.getStyleClass().add("settings-overlay");

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("settings-shell");
        shell.setPadding(new Insets(28, 54, 34, 54));
        shell.setTop(buildHeader());

        VBox page = new VBox(24);
        page.setAlignment(Pos.TOP_CENTER);
        page.setMaxWidth(1120);
        page.getChildren().addAll(buildHero(), buildSettingsGrid(), buildControlsCard(), buildFooter());

        ScrollPane scroll = new ScrollPane(page);
        scroll.getStyleClass().add("settings-scroll");
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
        header.getStyleClass().add("settings-header");

        Button back = new Button("←  COMMAND CENTER");
        back.getStyleClass().add("settings-back-link");
        back.setOnAction(event -> returnToMenu());

        VBox brand = new VBox(-2);
        brand.setAlignment(Pos.CENTER);
        Label brandName = styledLabel("THE INFECTED HOUR", "settings-brand");
        Label brandSub = styledLabel("SURVIVAL OPERATIONS NETWORK", "settings-brand-sub");
        brand.getChildren().addAll(brandName, brandSub);

        VBox status = new VBox(2);
        status.setAlignment(Pos.CENTER_RIGHT);
        status.getChildren().addAll(
                styledLabel("●  LOCAL SYSTEM", "settings-local-status"),
                styledLabel("BUILD v0.1-alpha", "settings-version"));

        header.setLeft(back);
        header.setCenter(brand);
        header.setRight(status);
        return header;
    }

    private Node buildHero() {
        VBox hero = new VBox(6);
        hero.setAlignment(Pos.CENTER);
        hero.getChildren().addAll(
                styledLabel("OPERATIONS CONSOLE  /  CONFIGURATION", "settings-eyebrow"),
                styledLabel("SYSTEM SETTINGS", "settings-page-title"),
                styledLabel("Tune your field equipment and connection before entering the containment zone.",
                        "settings-page-subtitle"));
        return hero;
    }

    private Node buildSettingsGrid() {
        Slider musicVolume = createSlider(SessionState.get().getMusicVolumePercent());
        Slider sfxVolume = createSlider(SessionState.get().getSfxVolumePercent());
        Slider subtitleVoiceVolume = createSlider(SessionState.get().getSubtitleVoiceVolumePercent());

        musicVolume.valueProperty().addListener((observable, oldValue, newValue) ->
                SessionState.get().setMusicVolumePercent(newValue.doubleValue()));
        sfxVolume.valueProperty().addListener((observable, oldValue, newValue) ->
                SessionState.get().setSfxVolumePercent(newValue.doubleValue()));
        subtitleVoiceVolume.valueProperty().addListener((observable, oldValue, newValue) ->
                SessionState.get().setSubtitleVoiceVolumePercent(newValue.doubleValue()));

        VBox audioCard = createCard("AUDIO", "FIELD MIXER",
                "Balance the soundtrack, cinematic dialogue, and combat feedback for your current operation.");
        audioCard.getStyleClass().add("settings-card-featured");
        audioCard.getChildren().addAll(
                buildSliderControl("♫", "MUSIC VOLUME", "Atmosphere and cinematic score", musicVolume),
                buildSliderControl("◉", "SFX VOLUME", "Weapons, infected and interface", sfxVolume),
                buildSliderControl("▣", "SUBTITLE VOICE VOLUME",
                        "Spoken dialogue played with cinematic subtitles", subtitleVoiceVolume));

        CheckBox fullscreen = new CheckBox("FULLSCREEN MODE");
        fullscreen.getStyleClass().add("settings-toggle");
        fullscreen.setSelected(SessionState.get().isFullscreen());
        fullscreen.setOnAction(event -> {
            boolean isFullscreen = fullscreen.isSelected();
            SessionState.get().setFullscreen(isFullscreen);
            stage.setFullScreen(isFullscreen);
        });

        VBox displayCard = createCard("DISPLAY", "VISUAL OUTPUT",
                "Use the complete display area for a more immersive view of the outbreak.");
        HBox displayControl = new HBox(16);
        displayControl.setAlignment(Pos.CENTER_LEFT);
        displayControl.getStyleClass().add("settings-option-row");
        VBox displayText = new VBox(3,
                styledLabel("BORDERLESS FIELD VIEW", "settings-control-label"),
                styledLabel("Switch between windowed and fullscreen play", "settings-control-copy"));
        HBox.setHgrow(displayText, Priority.ALWAYS);
        displayControl.getChildren().addAll(displayText, fullscreen);

        ToggleButton centeredSubtitles = new ToggleButton("CENTER");
        ToggleButton leftSubtitles = new ToggleButton("LEFT");
        centeredSubtitles.getStyleClass().add("settings-choice-button");
        leftSubtitles.getStyleClass().add("settings-choice-button");

        ToggleGroup subtitleAlignment = new ToggleGroup();
        centeredSubtitles.setToggleGroup(subtitleAlignment);
        leftSubtitles.setToggleGroup(subtitleAlignment);
        centeredSubtitles.setUserData(SessionState.SubtitleAlignment.CENTER);
        leftSubtitles.setUserData(SessionState.SubtitleAlignment.LEFT);

        if (SessionState.get().getSubtitleAlignment() == SessionState.SubtitleAlignment.LEFT) {
            leftSubtitles.setSelected(true);
        } else {
            centeredSubtitles.setSelected(true);
        }

        subtitleAlignment.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                if (previous != null) previous.setSelected(true);
                return;
            }
            SessionState.get().setSubtitleAlignment((SessionState.SubtitleAlignment) selected.getUserData());
        });

        VBox subtitleText = new VBox(3,
                styledLabel("CINEMATIC DIALOGUE ALIGNMENT", "settings-control-label"),
                styledLabel("Choose centered or left-aligned cinematic subtitles", "settings-control-copy"));
        HBox.setHgrow(subtitleText, Priority.ALWAYS);
        HBox subtitleButtons = new HBox(8, centeredSubtitles, leftSubtitles);
        subtitleButtons.setAlignment(Pos.CENTER_RIGHT);

        HBox subtitleControl = new HBox(16, subtitleText, subtitleButtons);
        subtitleControl.setAlignment(Pos.CENTER_LEFT);
        subtitleControl.getStyleClass().add("settings-option-row");

        displayCard.getChildren().addAll(displayControl, subtitleControl);

        TextField backendUrl = new TextField(SessionState.get().getBackendUrl());
        backendUrl.getStyleClass().add("settings-input");
        backendUrl.setPromptText("http://localhost:8080");
        Label connectionStatus = styledLabel("LOCAL ROUTE • READY", "settings-save-status");
        connectionStatus.getStyleClass().add("settings-save-status-ready");

        Button saveBackendUrl = new Button("SAVE CONNECTION");
        saveBackendUrl.getStyleClass().add("settings-save-btn");
        saveBackendUrl.setOnAction(event -> {
            String value = backendUrl.getText().trim();
            connectionStatus.getStyleClass().removeAll("settings-save-status-ready", "settings-save-status-error");
            if (value.isEmpty()) {
                connectionStatus.setText("ROUTE REQUIRED • NOT SAVED");
                connectionStatus.getStyleClass().add("settings-save-status-error");
                return;
            }
            backendUrl.setText(value);
            SessionState.get().setBackendUrl(value);
            connectionStatus.setText("ROUTE SAVED • READY");
            connectionStatus.getStyleClass().add("settings-save-status-ready");
        });

        VBox networkCard = createCard("ADVANCED", "BACKEND CONNECTION",
                "Change this only when connecting to a different game service.");
        Label inputLabel = styledLabel("SERVICE ENDPOINT", "settings-field-label");
        Region spacer = new Region();
        HBox networkActions = new HBox(12, connectionStatus, spacer, saveBackendUrl);
        networkActions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        networkCard.getChildren().addAll(inputLabel, backendUrl, networkActions);

        VBox rightColumn = new VBox(18, displayCard, networkCard);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        HBox.setHgrow(audioCard, Priority.ALWAYS);
        audioCard.setMaxWidth(Double.MAX_VALUE);
        rightColumn.setMaxWidth(Double.MAX_VALUE);

        HBox grid = new HBox(18, audioCard, rightColumn);
        grid.getStyleClass().add("settings-grid");
        grid.setAlignment(Pos.TOP_CENTER);
        return grid;
    }

    private VBox createCard(String kickerText, String titleText, String descriptionText) {
        VBox card = new VBox(10);
        card.getStyleClass().add("settings-card");
        Label description = styledLabel(descriptionText, "settings-card-description");
        description.setWrapText(true);
        card.getChildren().addAll(
                styledLabel(kickerText, "settings-card-kicker"),
                styledLabel(titleText, "settings-card-title"), description);
        return card;
    }

    private Node buildControlsCard() {
        VBox card = createCard("CONTROLS", "KEY REFERENCE",
                "Current keyboard and mouse controls. Key rebinding will be added later.");
        card.setMaxWidth(Double.MAX_VALUE);

        GridPane controls = new GridPane();
        controls.getStyleClass().add("settings-controls-grid");
        controls.setHgap(18);
        controls.setVgap(9);

        String[][] entries = {
                {"W / A / S / D", "Move"},
                {"SHIFT", "Sprint / evade"},
                {"SPACE / LEFT CLICK", "Melee attack"},
                {"E", "Interact / loot"},
                {"H", "Heal"},
                {"X", "Order survivor to stay / follow"},
                {"M", "Show / hide minimap"},
                {"O", "Show / hide objectives"},
                {"I", "Open / close inventory"},
                {"ESC", "Pause / back"},
                {"K", "Open controls from pause menu"},
                {"CTRL + S", "Open checkpoint save slots"},
                {"F11", "Toggle fullscreen"},
                {"F12", "Take screenshot"},
                {"J", "Show coordinate diagnostics"},
                {"C / F1", "Show collision overlay"},
                {"F3", "Toggle split-screen debug view"},
                {"Q", "Exit to main menu while paused"},
                {"ARROWS / NUMPAD", "Player 2 movement in debug co-op"},
                {"NUMPAD 0", "Player 2 attack in debug co-op"},
                {"NUMPAD 3", "Player 2 interact in debug co-op"},
                {"NUMPAD 7", "Player 2 stay / follow command"},
                {"NUMPAD 9", "Player 2 heal"}
        };

        int rowsPerColumn = (entries.length + 1) / 2;
        for (int i = 0; i < entries.length; i++) {
            int group = i / rowsPerColumn;
            int row = i % rowsPerColumn;
            int column = group * 2;
            Label key = styledLabel(entries[i][0], "settings-key-chip");
            Label action = styledLabel(entries[i][1], "settings-control-copy");
            controls.add(key, column, row);
            controls.add(action, column + 1, row);
        }

        ColumnConstraints keyColumn = new ColumnConstraints();
        keyColumn.setMinWidth(145);
        ColumnConstraints actionColumn = new ColumnConstraints();
        actionColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints keyColumnTwo = new ColumnConstraints();
        keyColumnTwo.setMinWidth(145);
        ColumnConstraints actionColumnTwo = new ColumnConstraints();
        actionColumnTwo.setHgrow(Priority.ALWAYS);
        controls.getColumnConstraints().addAll(keyColumn, actionColumn, keyColumnTwo, actionColumnTwo);

        card.getChildren().add(controls);
        return card;
    }

    private Slider createSlider(double initialValue) {
        Slider slider = new Slider(0, 100, initialValue);
        slider.getStyleClass().add("settings-slider");
        slider.setMaxWidth(Double.MAX_VALUE);
        return slider;
    }

    private Node buildSliderControl(String iconText, String titleText, String helpText, Slider slider) {
        VBox control = new VBox(10);
        control.getStyleClass().add("settings-slider-control");
        Label icon = styledLabel(iconText, "settings-control-icon");
        VBox copy = new VBox(2,
                styledLabel(titleText, "settings-control-label"),
                styledLabel(helpText, "settings-control-copy"));
        Label value = styledLabel(Math.round(slider.getValue()) + "%", "settings-value-chip");
        slider.valueProperty().addListener((observable, oldValue, newValue) ->
                value.setText(Math.round(newValue.doubleValue()) + "%"));
        Region spacer = new Region();
        HBox heading = new HBox(12, icon, copy, spacer, value);
        heading.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        control.getChildren().addAll(heading, slider);
        return control;
    }

    private Node buildFooter() {
        HBox footer = new HBox(18);
        footer.setAlignment(Pos.CENTER);
        Button back = new Button("RETURN TO COMMAND CENTER");
        back.getStyleClass().add("settings-return-btn");
        back.setOnAction(event -> returnToMenu());
        footer.getChildren().addAll(
                styledLabel("ESC  BACK    •    CHANGES APPLY TO THIS DEVICE", "settings-footer-hint"), back);
        return footer;
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
