package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class ExamStartViewController {
    @FXML BorderPane startPane;
    @FXML Label subtitleLabel;
    @FXML Label passLabel;
    @FXML ChoiceBox<Integer> examQuestionCountChoice;

    private Runnable onShowStudentDashboard;
    private Runnable onShowScheduledExams;
    private Runnable onShowExamHistory;
    private Runnable onShowLeaderboard;
    private Runnable onShowDiscussion;
    private Runnable onShowMessaging;
    private Runnable onLogoutRequested;
    private Runnable onStartExamRequested;

    public void setOnShowStudentDashboard(Runnable callback) { this.onShowStudentDashboard = callback; }
    public void setOnShowScheduledExams(Runnable callback) { this.onShowScheduledExams = callback; }
    public void setOnShowExamHistory(Runnable callback) { this.onShowExamHistory = callback; }
    public void setOnShowLeaderboard(Runnable callback) { this.onShowLeaderboard = callback; }
    public void setOnShowDiscussion(Runnable callback) { this.onShowDiscussion = callback; }
    public void setOnShowMessaging(Runnable callback) { this.onShowMessaging = callback; }
    public void setOnLogoutRequested(Runnable callback) { this.onLogoutRequested = callback; }
    public void setOnStartExamRequested(Runnable callback) { this.onStartExamRequested = callback; }

    @FXML private void handleShowStudentDashboard() { if (onShowStudentDashboard != null) onShowStudentDashboard.run(); }
    @FXML private void handleShowScheduledExams() { if (onShowScheduledExams != null) onShowScheduledExams.run(); }
    @FXML private void handleShowExamHistory() { if (onShowExamHistory != null) onShowExamHistory.run(); }
    @FXML private void handleShowLeaderboard() { if (onShowLeaderboard != null) onShowLeaderboard.run(); }
    @FXML private void handleShowDiscussion() { if (onShowDiscussion != null) onShowDiscussion.run(); }
    @FXML private void handleShowMessaging() { if (onShowMessaging != null) onShowMessaging.run(); }
    @FXML private void handleLogout() { if (onLogoutRequested != null) onLogoutRequested.run(); }
    @FXML private void handleStartExam() { if (onStartExamRequested != null) onStartExamRequested.run(); }
}
