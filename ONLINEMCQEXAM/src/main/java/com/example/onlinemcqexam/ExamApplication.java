package com.example.onlinemcqexam;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ExamApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(ExamApplication.class.getResource("exam-view.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(ExamApplication.class.getResource("styles.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("dashboard.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("availableExams.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("scheduledExams.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("progress.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("leaderboard.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("createAccount.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("discussion.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("messaging.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("examStart.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("liveExam.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("examResult.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("teacherLogin.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("teacherDashboard.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("questionBank.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("createExam.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("scheduleExam.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("resultsAnalytics.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("examLibrary.css").toExternalForm());
        scene.getStylesheets().add(ExamApplication.class.getResource("authFailed.css").toExternalForm());
        stage.setTitle("Online MCQ Exam");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
