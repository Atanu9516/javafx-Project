package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class RegisterViewController {
    @FXML StackPane registerPane;
    @FXML TextField registerUsername;
    @FXML TextField registerFullName;
    @FXML ChoiceBox<String> registerSemesterChoice;
    @FXML PasswordField registerPassword;
    @FXML PasswordField registerConfirm;
    @FXML Label registerMessageLabel;
    @FXML HBox registerErrorBanner;

    private Runnable onRegisterRequested;
    private Runnable onShowLoginRequested;

    public void setOnRegisterRequested(Runnable onRegisterRequested) {
        this.onRegisterRequested = onRegisterRequested;
    }

    public void setOnShowLoginRequested(Runnable onShowLoginRequested) {
        this.onShowLoginRequested = onShowLoginRequested;
    }

    @FXML
    private void handleRegister() {
        if (onRegisterRequested != null) {
            onRegisterRequested.run();
        }
    }

    @FXML
    private void handleShowLogin() {
        if (onShowLoginRequested != null) {
            onShowLoginRequested.run();
        }
    }
}
