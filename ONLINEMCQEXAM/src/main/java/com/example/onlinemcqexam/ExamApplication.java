package com.example.onlinemcqexam;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class ExamApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        try {
            AppPaths.initialize();
            FXMLLoader loader = new FXMLLoader(ExamApplication.class.getResource("exam-view.fxml"));
            Scene scene = new Scene(loader.load());
            scene.setFill(Color.web("#08130d"));
            addStylesheet(scene, "styles.css");
            addStylesheet(scene, "dashboard.css");
            addStylesheet(scene, "availableExams.css");
            addStylesheet(scene, "scheduledExams.css");
            addStylesheet(scene, "progress.css");
            addStylesheet(scene, "leaderboard.css");
            addStylesheet(scene, "createAccount.css");
            addStylesheet(scene, "discussion.css");
            addStylesheet(scene, "messaging.css");
            addStylesheet(scene, "examStart.css");
            addStylesheet(scene, "liveExam.css");
            addStylesheet(scene, "examResult.css");
            addStylesheet(scene, "teacherLogin.css");
            addStylesheet(scene, "teacherDashboard.css");
            addStylesheet(scene, "questionBank.css");
            addStylesheet(scene, "createExam.css");
            addStylesheet(scene, "scheduleExam.css");
            addStylesheet(scene, "resultsAnalytics.css");
            addStylesheet(scene, "examLibrary.css");
            addStylesheet(scene, "authFailed.css");
            stage.setTitle("Online MCQ Exam");
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Online MCQ Exam");
            alert.setHeaderText("The application failed to start.");
            alert.setContentText(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            alert.showAndWait();
            Platform.exit();
        }
    }

    private void addStylesheet(Scene scene, String resourceName) {
        var url = ExamApplication.class.getResource(resourceName);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
