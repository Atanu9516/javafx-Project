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
        scene.getStylesheets().add(ExamApplication.class.getResource("exam.css").toExternalForm());
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
