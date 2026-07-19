package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.fxlauncher.state.SessionState;
import com.infectedhour.shared.dto.LoginRequest;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Screen 1 (UI/UX doc §2): logo, username + password, Login / Register,
 * offline mode link. Success stores JWT in SessionState then navigates
 * to MainMenuView.
 */
public class LoginView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(12);

    public LoginView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
    }

    private void build() {
        root.setPadding(new Insets(32));
        root.getStyleClass().add("bg-night");

        Label title = new Label("THE INFECTED HOUR");
        title.getStyleClass().add("title-gold");

        TextField username = new TextField();
        username.setPromptText("Username");

        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        Label error = new Label();
        error.getStyleClass().add("error-text");

        Button loginBtn = new Button("Login");
        loginBtn.setOnAction(e -> {
            backendClient.login(new LoginRequest(username.getText(), password.getText()))
                    .thenAccept(resp -> Platform.runLater(this::goToMainMenu))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> error.setText("Login failed: " + ex.getMessage()));
                        return null;
                    });
        });

        Button registerBtn = new Button("Register");
        registerBtn.setOnAction(e -> goToRegister());

        Hyperlink offlineLink = new Hyperlink("Continue offline (solo only)");
        offlineLink.setOnAction(e -> {
            SessionState.get().setOfflineMode(true);
            goToMainMenu();
        });

        root.getChildren().addAll(title, username, password, error, loginBtn, registerBtn, offlineLink);
    }

    private void goToMainMenu() {
        MainMenuView view = new MainMenuView(stage, backendClient);
        stage.getScene().setRoot(view.getRoot());
    }

    private void goToRegister() {
        RegisterView view = new RegisterView(stage, backendClient);
        stage.getScene().setRoot(view.getRoot());
    }

    public VBox getRoot() {
        return root;
    }
}
