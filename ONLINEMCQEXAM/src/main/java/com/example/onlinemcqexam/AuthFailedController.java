package com.example.onlinemcqexam;

import javafx.fxml.FXML;

public class AuthFailedController {
    private Runnable onBackToLoginRequested;

    public void setOnBackToLoginRequested(Runnable onBackToLoginRequested) {
        this.onBackToLoginRequested = onBackToLoginRequested;
    }

    @FXML
    private void onBackToLogin() {
        if (onBackToLoginRequested != null) {
            onBackToLoginRequested.run();
        }
    }
}
