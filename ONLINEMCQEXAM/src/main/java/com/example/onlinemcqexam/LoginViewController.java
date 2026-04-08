package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class LoginViewController {
    @FXML StackPane loginPane;
    @FXML TextField loginUsername;
    @FXML PasswordField loginPassword;
    @FXML Label loginMessageLabel;

    private Runnable onLoginRequested;
    private Runnable onRegisterRequested;
    private Runnable onTeacherLoginRequested;

    public void setOnLoginRequested(Runnable onLoginRequested) {
        this.onLoginRequested = onLoginRequested;
    }

    public void setOnRegisterRequested(Runnable onRegisterRequested) {
        this.onRegisterRequested = onRegisterRequested;
    }

    public void setOnTeacherLoginRequested(Runnable onTeacherLoginRequested) {
        this.onTeacherLoginRequested = onTeacherLoginRequested;
    }

    @FXML
    private void handleLogin() {
        if (onLoginRequested != null) {
            onLoginRequested.run();
        }
    }

    @FXML
    private void handleShowRegister() {
        if (onRegisterRequested != null) {
            onRegisterRequested.run();
        }
    }

    @FXML
    private void handleShowTeacherLogin() {
        if (onTeacherLoginRequested != null) {
            onTeacherLoginRequested.run();
        }
    }
}
