package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class TeacherDashboardViewController {
    @FXML BorderPane teacherDashboardPane;
    @FXML Label teacherWelcomeLabel;
    @FXML Label teacherActiveExamsBadgeLabel;

    private Runnable onShowQuestionBank;
    private Runnable onShowCreateExam;
    private Runnable onShowScheduleExam;
    private Runnable onShowTeacherAnalytics;
    private Runnable onShowViewExams;
    private Runnable onLogoutRequested;

    public void setOnShowQuestionBank(Runnable callback) { this.onShowQuestionBank = callback; }
    public void setOnShowCreateExam(Runnable callback) { this.onShowCreateExam = callback; }
    public void setOnShowScheduleExam(Runnable callback) { this.onShowScheduleExam = callback; }
    public void setOnShowTeacherAnalytics(Runnable callback) { this.onShowTeacherAnalytics = callback; }
    public void setOnShowViewExams(Runnable callback) { this.onShowViewExams = callback; }
    public void setOnLogoutRequested(Runnable callback) { this.onLogoutRequested = callback; }

    @FXML private void handleShowQuestionBank() { if (onShowQuestionBank != null) onShowQuestionBank.run(); }
    @FXML private void handleShowCreateExam() { if (onShowCreateExam != null) onShowCreateExam.run(); }
    @FXML private void handleShowScheduleExam() { if (onShowScheduleExam != null) onShowScheduleExam.run(); }
    @FXML private void handleShowTeacherAnalytics() { if (onShowTeacherAnalytics != null) onShowTeacherAnalytics.run(); }
    @FXML private void handleShowViewExams() { if (onShowViewExams != null) onShowViewExams.run(); }
    @FXML private void handleLogout() { if (onLogoutRequested != null) onLogoutRequested.run(); }
}
