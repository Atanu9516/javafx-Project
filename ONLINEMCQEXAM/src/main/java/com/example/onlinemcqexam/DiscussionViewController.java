package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

import java.util.function.Consumer;

public class DiscussionViewController {
    @FXML BorderPane discussionPane;
    @FXML TextArea discussionAskArea;
    @FXML ListView<String> discussionList;

    private Runnable onShowStudentDashboard;
    private Runnable onShowAvailableExams;
    private Runnable onShowScheduledExams;
    private Runnable onShowExamHistory;
    private Runnable onShowLeaderboard;
    private Runnable onShowMessaging;
    private Runnable onLogoutRequested;
    private Runnable onPostRequested;
    private Consumer<KeyEvent> onSubmitKeyPressed;

    public void setOnShowStudentDashboard(Runnable callback) { this.onShowStudentDashboard = callback; }
    public void setOnShowAvailableExams(Runnable callback) { this.onShowAvailableExams = callback; }
    public void setOnShowScheduledExams(Runnable callback) { this.onShowScheduledExams = callback; }
    public void setOnShowExamHistory(Runnable callback) { this.onShowExamHistory = callback; }
    public void setOnShowLeaderboard(Runnable callback) { this.onShowLeaderboard = callback; }
    public void setOnShowMessaging(Runnable callback) { this.onShowMessaging = callback; }
    public void setOnLogoutRequested(Runnable callback) { this.onLogoutRequested = callback; }
    public void setOnPostRequested(Runnable callback) { this.onPostRequested = callback; }
    public void setOnSubmitKeyPressed(Consumer<KeyEvent> callback) { this.onSubmitKeyPressed = callback; }

    @FXML private void handleShowStudentDashboard() { if (onShowStudentDashboard != null) onShowStudentDashboard.run(); }
    @FXML private void handleShowAvailableExams() { if (onShowAvailableExams != null) onShowAvailableExams.run(); }
    @FXML private void handleShowScheduledExams() { if (onShowScheduledExams != null) onShowScheduledExams.run(); }
    @FXML private void handleShowExamHistory() { if (onShowExamHistory != null) onShowExamHistory.run(); }
    @FXML private void handleShowLeaderboard() { if (onShowLeaderboard != null) onShowLeaderboard.run(); }
    @FXML private void handleShowMessaging() { if (onShowMessaging != null) onShowMessaging.run(); }
    @FXML private void handleLogout() { if (onLogoutRequested != null) onLogoutRequested.run(); }
    @FXML private void handlePostDiscussion() { if (onPostRequested != null) onPostRequested.run(); }
    @FXML private void handleDiscussionSubmitKey(KeyEvent event) { if (onSubmitKeyPressed != null) onSubmitKeyPressed.accept(event); }
}
