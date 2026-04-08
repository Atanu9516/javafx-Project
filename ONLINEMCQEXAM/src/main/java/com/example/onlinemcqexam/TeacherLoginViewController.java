package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class TeacherLoginViewController {
    @FXML StackPane teacherLoginPane;
    @FXML ChoiceBox<String> teacherRoleChoice;
    @FXML TextField teacherUsername;
    @FXML PasswordField teacherPassword;
    @FXML Label teacherLoginMessageLabel;

    private Runnable onLoginRequested;
    private Runnable onBackToStudentLoginRequested;

    public void setOnLoginRequested(Runnable onLoginRequested) {
        this.onLoginRequested = onLoginRequested;
    }

    public void setOnBackToStudentLoginRequested(Runnable onBackToStudentLoginRequested) {
        this.onBackToStudentLoginRequested = onBackToStudentLoginRequested;
    }

    @FXML
    private void handleLogin() {
        if (onLoginRequested != null) {
            onLoginRequested.run();
        }
    }

    @FXML
    private void handleBackToStudentLogin() {
        if (onBackToStudentLoginRequested != null) {
            onBackToStudentLoginRequested.run();
        }
    }
}
