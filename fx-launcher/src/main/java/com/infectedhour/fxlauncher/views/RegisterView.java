package com.infectedhour.fxlauncher.views;

import com.infectedhour.fxlauncher.net.BackendClient;
import com.infectedhour.shared.dto.RegisterRequest;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Screen 2 (UI/UX doc §2): username, display name, password x2. */
public class RegisterView {

    private final Stage stage;
    private final BackendClient backendClient;
    private final VBox root = new VBox(12);

    public RegisterView(Stage stage, BackendClient backendClient) {
        this.stage = stage;
        this.backendClient = backendClient;
        build();
    }

    private void build() {
        root.setPadding(new Insets(32));

        TextField username = new TextField();
        username.setPromptText("Username (3-24 chars, letters/numbers/_)");
        TextField displayName = new TextField();
        displayName.setPromptText("Display name");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm password");

        Label error = new Label();
        error.getStyleClass().add("error-text");

        Button registerBtn = new Button("Create Account");
        registerBtn.setOnAction(e -> {
            if (!password.getText().equals(confirmPassword.getText())) {
                error.setText("Passwords do not match");
                return;
            }
            backendClient.register(new RegisterRequest(username.getText(), displayName.getText(), password.getText()))
                    .thenAccept(v -> Platform.runLater(this::goToLogin))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> error.setText("Registration failed: " + ex.getMessage()));
                        return null;
                    });
        });

        Button backBtn = new Button("Back to Login");
        backBtn.setOnAction(e -> goToLogin());

        root.getChildren().addAll(username, displayName, password, confirmPassword, error, registerBtn, backBtn);
    }

    private void goToLogin() {
        LoginView view = new LoginView(stage, backendClient);
        stage.getScene().setRoot(view.getRoot());
    }

    public VBox getRoot() {
        return root;
    }
}
