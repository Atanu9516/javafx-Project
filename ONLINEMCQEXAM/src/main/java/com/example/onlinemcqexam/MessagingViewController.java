package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

public class MessagingViewController {
    @FXML BorderPane messagingPane;
    @FXML Label messagingFeedbackLabel;
    @FXML Label messagingRequestBadgeLabel;
    @FXML ListView<String> pendingRequestsList;
    @FXML ListView<String> friendsList;
    @FXML ListView<String> chatHistoryList;
    @FXML TextField friendRequestField;
    @FXML TextField chatMessageField;
    @FXML Label messagingActiveFriendLabel;
    @FXML Label messagingActiveStatusLabel;

    private Runnable onShowStudentDashboard;
    private Runnable onShowAvailableExams;
    private Runnable onShowScheduledExams;
    private Runnable onShowExamHistory;
    private Runnable onShowLeaderboard;
    private Runnable onLogoutRequested;
    private Runnable onSendFriendRequest;
    private Runnable onSendChatMessage;

    public void setOnShowStudentDashboard(Runnable callback) { this.onShowStudentDashboard = callback; }
    public void setOnShowAvailableExams(Runnable callback) { this.onShowAvailableExams = callback; }
    public void setOnShowScheduledExams(Runnable callback) { this.onShowScheduledExams = callback; }
    public void setOnShowExamHistory(Runnable callback) { this.onShowExamHistory = callback; }
    public void setOnShowLeaderboard(Runnable callback) { this.onShowLeaderboard = callback; }
    public void setOnLogoutRequested(Runnable callback) { this.onLogoutRequested = callback; }
    public void setOnSendFriendRequest(Runnable callback) { this.onSendFriendRequest = callback; }
    public void setOnSendChatMessage(Runnable callback) { this.onSendChatMessage = callback; }

    @FXML private void handleShowStudentDashboard() { if (onShowStudentDashboard != null) onShowStudentDashboard.run(); }
    @FXML private void handleShowAvailableExams() { if (onShowAvailableExams != null) onShowAvailableExams.run(); }
    @FXML private void handleShowScheduledExams() { if (onShowScheduledExams != null) onShowScheduledExams.run(); }
    @FXML private void handleShowExamHistory() { if (onShowExamHistory != null) onShowExamHistory.run(); }
    @FXML private void handleShowLeaderboard() { if (onShowLeaderboard != null) onShowLeaderboard.run(); }
    @FXML private void handleLogout() { if (onLogoutRequested != null) onLogoutRequested.run(); }
    @FXML private void handleSendFriendRequest() { if (onSendFriendRequest != null) onSendFriendRequest.run(); }
    @FXML private void handleSendChatMessage() { if (onSendChatMessage != null) onSendChatMessage.run(); }
}
