package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.BorderPane;

public class LiveExamViewController {
    @FXML BorderPane examPane;
    @FXML Label progressLabel;
    @FXML Label timerLabel;
    @FXML Label questionLabel;
    @FXML Label examTopicLabel;
    @FXML RadioButton optionA;
    @FXML RadioButton optionB;
    @FXML RadioButton optionC;
    @FXML RadioButton optionD;
    @FXML Label levelLabel;
    @FXML ProgressBar progressBar;
    @FXML Button prevButton;
    @FXML Button nextButton;
    @FXML Label examAutosaveLabel;
    @FXML Label examCompletionLabel;
    @FXML Button submitButton;

    private Runnable onPreviousRequested;
    private Runnable onNextRequested;
    private Runnable onSubmitRequested;

    public void setOnPreviousRequested(Runnable callback) { this.onPreviousRequested = callback; }
    public void setOnNextRequested(Runnable callback) { this.onNextRequested = callback; }
    public void setOnSubmitRequested(Runnable callback) { this.onSubmitRequested = callback; }

    @FXML private void handlePreviousQuestion() { if (onPreviousRequested != null) onPreviousRequested.run(); }
    @FXML private void handleNextQuestion() { if (onNextRequested != null) onNextRequested.run(); }
    @FXML private void handleSubmitExam() { if (onSubmitRequested != null) onSubmitRequested.run(); }
}
