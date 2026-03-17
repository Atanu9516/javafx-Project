package com.example.onlinemcqexam;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ExamController {
    private static final int EXAM_DURATION_SECONDS = 900;
    private static final double PASS_THRESHOLD = 0.6;

    @FXML
    private AnchorPane loginPane;
    @FXML
    private AnchorPane registerPane;
    @FXML
    private BorderPane studentDashboardPane;
    @FXML
    private AnchorPane availableExamsPane;
    @FXML
    private AnchorPane discussionPane;
    @FXML
    private AnchorPane messagingPane;
    @FXML
    private AnchorPane progressPane;
    @FXML
    private AnchorPane leaderboardPane;
    @FXML
    private AnchorPane levelPane;
    @FXML
    private AnchorPane startPane;
    @FXML
    private BorderPane examPane;
    @FXML
    private AnchorPane resultPane;
    @FXML
    private AnchorPane teacherLoginPane;
    @FXML
    private AnchorPane teacherAuthFailPane;
    @FXML
    private BorderPane teacherDashboardPane;
    @FXML
    private AnchorPane questionBankPane;
    @FXML
    private AnchorPane createExamPane;
    @FXML
    private AnchorPane scheduleExamPane;
    @FXML
    private AnchorPane viewExamsPane;
    @FXML
    private AnchorPane teacherAnalyticsPane;

    @FXML
    private TextField loginUsername;
    @FXML
    private PasswordField loginPassword;
    @FXML
    private Label loginMessageLabel;
    @FXML
    private Label studentWelcomeLabel;

    @FXML
    private TextField registerUsername;
    @FXML
    private PasswordField registerPassword;
    @FXML
    private PasswordField registerConfirm;
    @FXML
    private Label registerMessageLabel;

    @FXML
    private Label levelMessageLabel;
    @FXML
    private Label levelSubtitleLabel;

    @FXML
    private Label subtitleLabel;
    @FXML
    private Label levelLabel;
    @FXML
    private Label questionLabel;
    @FXML
    private Label progressLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private RadioButton optionA;
    @FXML
    private RadioButton optionB;
    @FXML
    private RadioButton optionC;
    @FXML
    private RadioButton optionD;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button submitButton;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label correctLabel;
    @FXML
    private Label wrongLabel;
    @FXML
    private Label percentLabel;
    @FXML
    private Label passLabel;
    @FXML
    private TextArea resultArea;
    @FXML
    private ListView<String> availableExamList;
    @FXML
    private ListView<String> discussionList;
    @FXML
    private TextArea discussionAskArea;
    @FXML
    private TextField friendRequestField;
    @FXML
    private ListView<String> pendingRequestsList;
    @FXML
    private ListView<String> friendsList;
    @FXML
    private ListView<String> chatHistoryList;
    @FXML
    private TextField chatMessageField;
    @FXML
    private ListView<String> historyList;
    @FXML
    private BarChart<String, Number> progressChart;
    @FXML
    private ListView<String> leaderboardList;
    @FXML
    private Label highestScorerLabel;
    @FXML
    private ChoiceBox<String> teacherRoleChoice;
    @FXML
    private TextField teacherUsername;
    @FXML
    private PasswordField teacherPassword;
    @FXML
    private Label teacherLoginMessageLabel;
    @FXML
    private Label teacherWelcomeLabel;
    @FXML
    private ListView<String> questionBankList;
    @FXML
    private ChoiceBox<String> qbCategoryChoice;
    @FXML
    private ChoiceBox<String> qbDifficultyChoice;
    @FXML
    private ChoiceBox<String> qbCorrectChoice;
    @FXML
    private ChoiceBox<String> createCategoryChoice;
    @FXML
    private ChoiceBox<String> createDifficultyChoice;
    @FXML
    private Spinner<Integer> createNumQuestions;
    @FXML
    private Spinner<Integer> createTime;
    @FXML
    private Spinner<Integer> createMarks;
    @FXML
    private ChoiceBox<String> scheduleExamChoice;
    @FXML
    private ListView<String> viewExamsList;

    private final ToggleGroup optionsGroup = new ToggleGroup();
    private final ExamClient examClient = new ExamClient();
    private final UserService userService = new NetworkUserService(examClient);
    private final DiscussionService discussionService = new NetworkDiscussionService(examClient);
    private final MessagingService messagingService = new NetworkMessagingService(examClient);
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "exam-network-worker");
        thread.setDaemon(true);
        return thread;
    });
    private List<Question> questions = new ArrayList<>();
    private int currentIndex;
    private int[] answers;
    private Timeline timer;
    private int secondsRemaining;
    private Level selectedLevel;
    private String activeUser;
    private String activeTeacher;
    private final List<QuestionDraft> qbDrafts = new ArrayList<>();

    @FXML
    private void initialize() {
        optionA.setToggleGroup(optionsGroup);
        optionB.setToggleGroup(optionsGroup);
        optionC.setToggleGroup(optionsGroup);
        optionD.setToggleGroup(optionsGroup);
        optionA.setUserData(0);
        optionB.setUserData(1);
        optionC.setUserData(2);
        optionD.setUserData(3);
        updateTimerLabel(EXAM_DURATION_SECONDS);
        configureSpinners();
        configureMessaging();
        populateSampleData();
        showPane(loginPane);
    }

    private void configureSpinners() {
        if (createNumQuestions != null) {
            createNumQuestions.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 100, 20, 5));
            createNumQuestions.setEditable(true);
        }
        if (createTime != null) {
            createTime.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 180, 30, 5));
            createTime.setEditable(true);
        }
        if (createMarks != null) {
            createMarks.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 200, 100, 5));
            createMarks.setEditable(true);
        }
    }

    private void configureMessaging() {
        if (friendsList != null) {
            friendsList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null || newValue.isBlank()) {
                    if (chatHistoryList != null) {
                        chatHistoryList.getItems().clear();
                    }
                    return;
                }
                loadChatHistory(newValue);
            });
        }
    }

    private void populateSampleData() {
        if (availableExamList != null) {
            availableExamList.getItems().setAll(
                    "Java Fundamentals | Feb 15, 2026 | 10:00 AM",
                    "SQL Basics | Feb 17, 2026 | 02:30 PM",
                    "Data Structures | Feb 19, 2026 | 09:00 AM"
            );
        }
        if (historyList != null) {
            historyList.getItems().setAll(
                    "Feb 10, 2026 - Java Basics - 78%",
                    "Feb 07, 2026 - SQL Basics - 84%",
                    "Feb 03, 2026 - OOP Concepts - 72%"
            );
        }
        if (leaderboardList != null) {
            leaderboardList.getItems().setAll(
                    "1. Amina K - 96 (09:41)",
                    "2. Rahul S - 94 (10:05)",
                    "3. Jing L - 92 (09:58)",
                    "4. You - 88 (11:12)",
                    "5. Sam T - 86 (10:43)"
            );
        }
        if (highestScorerLabel != null) {
            highestScorerLabel.setText("Amina K - 96");
        }
        if (progressChart != null) {
            progressChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Score");
            series.getData().add(new XYChart.Data<>("E1", 62));
            series.getData().add(new XYChart.Data<>("E2", 74));
            series.getData().add(new XYChart.Data<>("E3", 78));
            series.getData().add(new XYChart.Data<>("E4", 84));
            series.getData().add(new XYChart.Data<>("E5", 88));
            progressChart.getData().add(series);
        }
        if (teacherRoleChoice != null) {
            teacherRoleChoice.getItems().setAll("Teacher", "Admin");
            teacherRoleChoice.getSelectionModel().selectFirst();
        }
        if (qbCategoryChoice != null) {
            qbCategoryChoice.getItems().setAll("Java", "SQL", "DSA", "OOP");
            qbCategoryChoice.getSelectionModel().selectFirst();
        }
        if (qbDifficultyChoice != null) {
            qbDifficultyChoice.getItems().setAll("Beginner", "Intermediate", "Advanced");
            qbDifficultyChoice.getSelectionModel().selectFirst();
        }
        if (qbCorrectChoice != null) {
            qbCorrectChoice.getItems().setAll("A", "B", "C", "D");
            qbCorrectChoice.getSelectionModel().selectFirst();
        }
        if (createCategoryChoice != null) {
            createCategoryChoice.getItems().setAll("Java", "SQL", "DSA", "OOP");
            createCategoryChoice.getSelectionModel().selectFirst();
        }
        if (createDifficultyChoice != null) {
            createDifficultyChoice.getItems().setAll("Beginner", "Intermediate", "Advanced");
            createDifficultyChoice.getSelectionModel().selectFirst();
        }
        if (scheduleExamChoice != null) {
            scheduleExamChoice.getItems().setAll("Java Fundamentals", "SQL Basics", "Data Structures");
            scheduleExamChoice.getSelectionModel().selectFirst();
        }
        if (viewExamsList != null) {
            viewExamsList.getItems().setAll(
                    "Java Fundamentals - Upcoming",
                    "SQL Basics - Ongoing",
                    "Data Structures - Completed"
            );
        }
        if (questionBankList != null) {
            qbDrafts.clear();
            qbDrafts.add(new QuestionDraft("What is JVM?",
                    "Java Virtual Machine", "Java Visual Model", "Joint Vector Method", "Just Virtual Memory", "A"));
            qbDrafts.add(new QuestionDraft("Which SQL clause is used for grouping?",
                    "ORDER BY", "GROUP BY", "WHERE", "HAVING", "B"));
            qbDrafts.add(new QuestionDraft("Time complexity of merge sort?",
                    "O(n)", "O(n log n)", "O(n^2)", "O(log n)", "B"));
            refreshQuestionBankList();
        }
    }

    private void refreshQuestionBankList() {
        if (questionBankList == null) {
            return;
        }
        questionBankList.getItems().clear();
        for (int i = 0; i < qbDrafts.size(); i++) {
            QuestionDraft draft = qbDrafts.get(i);
            questionBankList.getItems().add("Q" + (i + 1) + ": " + draft.text);
        }
    }

    @FXML
    private void showRegister() {
        clearLoginMessages();
        showPane(registerPane);
    }

    @FXML
    private void showLogin() {
        clearRegisterMessages();
        showPane(loginPane);
    }

    @FXML
    private void showStudentDashboard() {
        showPane(studentDashboardPane);
    }

    @FXML
    private void showAvailableExams() {
        showPane(availableExamsPane);
    }

    @FXML
    private void showDiscussion() {
        showPane(discussionPane);
        refreshDiscussion();
    }

    @FXML
    private void showMessaging() {
        showPane(messagingPane);
        refreshMessaging();
    }

    @FXML
    private void showProgress() {
        showPane(progressPane);
    }

    @FXML
    private void showLeaderboard() {
        showPane(leaderboardPane);
    }

    @FXML
    private void showResultPage() {
        showPane(resultPane);
    }

    @FXML
    private void showTeacherLogin() {
        if (teacherLoginMessageLabel != null) {
            teacherLoginMessageLabel.setText("");
        }
        showPane(teacherLoginPane);
    }

    @FXML
    private void loginTeacher() {
        String username = teacherUsername != null ? teacherUsername.getText() : "";
        String password = teacherPassword != null ? teacherPassword.getText() : "";
        if (teacherLoginMessageLabel != null) {
            teacherLoginMessageLabel.setText("");
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            if (teacherLoginMessageLabel != null) {
                teacherLoginMessageLabel.setText("Enter username and password.");
            }
            return;
        }
        if (!"teacher".equalsIgnoreCase(username.trim()) || !"teacher123".equals(password)) {
            showPane(teacherAuthFailPane);
            return;
        }
        activeTeacher = username.trim();
        if (teacherWelcomeLabel != null) {
            teacherWelcomeLabel.setText("Welcome " + activeTeacher);
        }
        showPane(teacherDashboardPane);
    }

    @FXML
    private void showTeacherDashboard() {
        showPane(teacherDashboardPane);
    }

    @FXML
    private void showQuestionBank() {
        showPane(questionBankPane);
    }

    @FXML
    private void showCreateExam() {
        showPane(createExamPane);
    }

    @FXML
    private void showScheduleExam() {
        showPane(scheduleExamPane);
    }

    @FXML
    private void showViewExams() {
        showPane(viewExamsPane);
    }

    @FXML
    private void showTeacherAnalytics() {
        showPane(teacherAnalyticsPane);
    }

    @FXML
    private void registerUser() {
        String username = registerUsername.getText();
        String password = registerPassword.getText();
        String confirm = registerConfirm.getText();
        registerMessageLabel.setText("");

        String validation = validateRegistration(username, password, confirm);
        if (validation != null) {
            registerMessageLabel.setText(validation);
            return;
        }
        registerMessageLabel.setText("Registering...");
        runNetwork(() -> userService.register(username, password), created -> {
            if (!created) {
                registerMessageLabel.setText("Username already exists.");
                return;
            }
            registerUsername.clear();
            registerPassword.clear();
            registerConfirm.clear();
            registerMessageLabel.setText("Account created. Please log in.");
            showPane(loginPane);
        }, error -> registerMessageLabel.setText("Server error: " + error));
    }

    @FXML
    private void loginUser() {
        String username = loginUsername.getText();
        String password = loginPassword.getText();
        loginMessageLabel.setText("");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            loginMessageLabel.setText("Enter username and password.");
            return;
        }
        loginMessageLabel.setText("Signing in...");
        runNetwork(() -> userService.authenticate(username, password), ok -> {
            if (!ok) {
                loginMessageLabel.setText("Invalid username or password.");
                return;
            }
            activeUser = userService.normalizeUsername(username);
            if (studentWelcomeLabel != null) {
                studentWelcomeLabel.setText("Welcome " + activeUser);
            }
            showPane(studentDashboardPane);
        }, error -> loginMessageLabel.setText("Server error: " + error));
    }

    @FXML
    private void logout() {
        stopTimer();
        activeUser = null;
        selectedLevel = null;
        loginPassword.clear();
        clearMessagingLists();
        if (discussionList != null) {
            discussionList.getItems().clear();
        }
        showPane(loginPane);
    }

    @FXML
    private void selectBeginner() {
        selectLevel(Level.BEGINNER);
    }

    @FXML
    private void selectIntermediate() {
        selectLevel(Level.INTERMEDIATE);
    }

    @FXML
    private void selectAdvanced() {
        selectLevel(Level.ADVANCED);
    }

    private void selectLevel(Level level) {
        selectedLevel = level;
        levelMessageLabel.setText("");
        levelLabel.setText("Level: " + level.getDisplayName());
        subtitleLabel.setText("Answer all questions before time ends.");
        showPane(startPane);
    }

    @FXML
    private void startExam() {
        if (selectedLevel == null) {
            levelMessageLabel.setText("Select a level to continue.");
            showPane(levelPane);
            return;
        }
        try {
            questions = QuestionBank.loadQuestions(selectedLevel);
        } catch (IOException ex) {
            showError("Unable to load questions.", ex.getMessage());
            return;
        }
        if (questions.size() < 20) {
            showError("Not enough questions.", "Need at least 20 questions for " + selectedLevel.getDisplayName() + ".");
            return;
        }
        answers = new int[questions.size()];
        for (int i = 0; i < answers.length; i++) {
            answers[i] = -1;
        }
        currentIndex = 0;
        secondsRemaining = EXAM_DURATION_SECONDS;
        showPane(examPane);
        startTimer();
        updateQuestionView();
    }

    @FXML
    private void nextQuestion() {
        saveAnswer();
        if (currentIndex < questions.size() - 1) {
            currentIndex++;
            updateQuestionView();
        }
    }

    @FXML
    private void previousQuestion() {
        saveAnswer();
        if (currentIndex > 0) {
            currentIndex--;
            updateQuestionView();
        }
    }

    @FXML
    private void submitExam() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Submit Exam");
        alert.setContentText("Are you sure you want to submit your answers?");
        alert.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                finishExam(false);
            }
        });
    }

    @FXML
    private void restartExam() {
        stopTimer();
        showPane(levelPane);
    }

    private void finishExam(boolean timeExpired) {
        saveAnswer();
        stopTimer();
        int correctCount = 0;
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            int answer = answers[i];
            boolean correct = answer == question.getCorrectIndex();
            if (correct) {
                correctCount++;
            }
            summary.append("Q").append(i + 1).append(": ");
            summary.append(correct ? "Correct" : "Incorrect");
            summary.append(" | Your: ");
            summary.append(answer >= 0 ? optionLetter(answer) : "None");
            summary.append(" | Correct: ").append(optionLetter(question.getCorrectIndex()));
            summary.append(System.lineSeparator());
        }
        if (timeExpired) {
            summary.append(System.lineSeparator()).append("Time expired. Exam auto-submitted.");
        }
        scoreLabel.setText(correctCount + "/" + questions.size());
        if (correctLabel != null) {
            correctLabel.setText(String.valueOf(correctCount));
        }
        int wrongCount = questions.size() - correctCount;
        if (wrongLabel != null) {
            wrongLabel.setText(String.valueOf(wrongCount));
        }
        double percent = questions.isEmpty() ? 0 : (correctCount / (double) questions.size());
        if (percentLabel != null) {
            percentLabel.setText(String.format("%.0f%%", percent * 100));
        }
        if (passLabel != null) {
            passLabel.setText(percent >= PASS_THRESHOLD ? "Pass" : "Fail");
        }
        resultArea.setText(summary.toString());
        showPane(resultPane);
    }

    private void updateQuestionView() {
        Question question = questions.get(currentIndex);
        questionLabel.setText(question.getText());
        optionA.setText(question.getOptions().get(0));
        optionB.setText(question.getOptions().get(1));
        optionC.setText(question.getOptions().get(2));
        optionD.setText(question.getOptions().get(3));

        optionsGroup.selectToggle(null);
        int savedAnswer = answers[currentIndex];
        if (savedAnswer >= 0) {
            for (Toggle toggle : optionsGroup.getToggles()) {
                if (toggle.getUserData().equals(savedAnswer)) {
                    optionsGroup.selectToggle(toggle);
                    break;
                }
            }
        }

        progressLabel.setText("Question " + (currentIndex + 1) + " of " + questions.size());
        progressBar.setProgress((currentIndex + 1) / (double) questions.size());
        prevButton.setDisable(currentIndex == 0);
        nextButton.setDisable(currentIndex == questions.size() - 1);
    }

    private void saveAnswer() {
        Toggle toggle = optionsGroup.getSelectedToggle();
        if (toggle != null) {
            answers[currentIndex] = (int) toggle.getUserData();
        }
    }

    private void startTimer() {
        stopTimer();
        updateTimerLabel(secondsRemaining);
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            secondsRemaining--;
            updateTimerLabel(secondsRemaining);
            if (secondsRemaining <= 0) {
                finishExam(true);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void updateTimerLabel(int totalSeconds) {
        int safe = Math.max(totalSeconds, 0);
        int minutes = safe / 60;
        int seconds = safe % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void showPane(Node pane) {
        setPaneVisible(loginPane, false);
        setPaneVisible(registerPane, false);
        setPaneVisible(studentDashboardPane, false);
        setPaneVisible(availableExamsPane, false);
        setPaneVisible(discussionPane, false);
        setPaneVisible(messagingPane, false);
        setPaneVisible(progressPane, false);
        setPaneVisible(leaderboardPane, false);
        setPaneVisible(levelPane, false);
        setPaneVisible(startPane, false);
        setPaneVisible(examPane, false);
        setPaneVisible(resultPane, false);
        setPaneVisible(teacherLoginPane, false);
        setPaneVisible(teacherAuthFailPane, false);
        setPaneVisible(teacherDashboardPane, false);
        setPaneVisible(questionBankPane, false);
        setPaneVisible(createExamPane, false);
        setPaneVisible(scheduleExamPane, false);
        setPaneVisible(viewExamsPane, false);
        setPaneVisible(teacherAnalyticsPane, false);
        setPaneVisible(pane, true);
    }

    private void setPaneVisible(Node pane, boolean visible) {
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    private boolean ensureLoggedIn() {
        if (activeUser == null || activeUser.isBlank()) {
            showError("Login required", "Please log in to use messaging.");
            return false;
        }
        return true;
    }

    private void clearMessagingLists() {
        if (pendingRequestsList != null) {
            pendingRequestsList.getItems().clear();
        }
        if (friendsList != null) {
            friendsList.getItems().clear();
        }
        if (chatHistoryList != null) {
            chatHistoryList.getItems().clear();
        }
    }

    private void refreshMessaging() {
        if (!ensureLoggedIn()) {
            clearMessagingLists();
            return;
        }
        if (pendingRequestsList != null) {
            runNetwork(() -> messagingService.fetchFriendRequests(activeUser), requests -> {
                pendingRequestsList.getItems().setAll(requests);
            }, error -> showError("Unable to load requests", error));
        }
        if (friendsList != null) {
            runNetwork(() -> messagingService.fetchFriends(activeUser), friends -> {
                friendsList.getItems().setAll(friends);
            }, error -> showError("Unable to load friends", error));
        }
    }

    private void loadChatHistory(String friend) {
        if (!ensureLoggedIn() || chatHistoryList == null) {
            return;
        }
        runNetwork(() -> messagingService.fetchChat(activeUser, friend), messages -> {
            chatHistoryList.getItems().setAll(messages);
        }, error -> showError("Unable to load chat", error));
    }

    private void refreshDiscussion() {
        if (discussionList == null) {
            return;
        }
        runNetwork(() -> discussionService.fetchMessages(), messages -> {
            discussionList.getItems().setAll(messages);
        }, error -> showError("Unable to load discussion", error));
    }

    private <T> void runNetwork(Callable<T> action, Consumer<T> onSuccess, Consumer<String> onError) {
        networkExecutor.submit(() -> {
            try {
                T result = action.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception ex) {
                String message = formatNetworkError(ex);
                Platform.runLater(() -> onError.accept(message));
            }
        });
    }

    private String formatNetworkError(Exception ex) {
        if (ex instanceof ConnectException || ex instanceof UnknownHostException) {
            return "Cannot reach server. Start ExamServer first.";
        }
        if (ex instanceof SocketTimeoutException) {
            return "Server timed out. Try again.";
        }
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Server error.";
        }
        return message;
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String optionLetter(int index) {
        return switch (index) {
            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            case 3 -> "D";
            default -> "?";
        };
    }

    private String validateRegistration(String username, String password, String confirm) {
        if (username == null || username.isBlank()) {
            return "Username is required.";
        }
        if (username.contains(",")) {
            return "Username cannot contain commas.";
        }
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters.";
        }
        if (!password.equals(confirm)) {
            return "Passwords do not match.";
        }
        return null;
    }

    private void clearLoginMessages() {
        loginMessageLabel.setText("");
    }

    private void clearRegisterMessages() {
        registerMessageLabel.setText("");
    }

    @FXML
    private void goToLevelSelect() {
        if (activeUser != null) {
            levelSubtitleLabel.setText("Welcome " + activeUser + ". Choose your exam level.");
        } else {
            levelSubtitleLabel.setText("Choose your exam level.");
        }
        showPane(levelPane);
    }

    @FXML
    private void postDiscussion() {
        if (discussionAskArea == null || discussionList == null) {
            return;
        }
        String text = discussionAskArea.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        String author = activeUser != null ? activeUser : "Student";
        runNetwork(() -> {
            discussionService.postMessage(author, text.trim());
            return true;
        }, ignored -> {
            discussionAskArea.clear();
            refreshDiscussion();
        }, error -> showError("Unable to post discussion", error));
    }

    @FXML
    private void commentDiscussion() {
        if (discussionList == null) {
            return;
        }
        String selected = discussionList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        int index = discussionList.getSelectionModel().getSelectedIndex();
        runNetwork(() -> discussionService.commentMessage(index), ok -> {
            if (!ok) {
                showError("Unable to comment", "Selected message was not found.");
                return;
            }
            refreshDiscussion();
        }, error -> showError("Unable to comment", error));
    }

    @FXML
    private void sendFriendRequest() {
        if (!ensureLoggedIn() || friendRequestField == null) {
            return;
        }
        String target = friendRequestField.getText();
        if (target == null || target.isBlank()) {
            showError("Friend request", "Enter a username.");
            return;
        }
        String from = activeUser;
        runNetwork(() -> messagingService.sendFriendRequest(from, target.trim()), status -> {
            switch (status) {
                case SENT, ACCEPTED -> {
                    friendRequestField.clear();
                    refreshMessaging();
                }
                case ALREADY -> showError("Friend request", "Request already exists or you are already friends.");
                case INVALID -> showError("Friend request", "Invalid user.");
            }
        }, error -> showError("Friend request failed", error));
    }

    @FXML
    private void acceptFriendRequest() {
        if (!ensureLoggedIn() || pendingRequestsList == null) {
            return;
        }
        String from = pendingRequestsList.getSelectionModel().getSelectedItem();
        if (from == null || from.isBlank()) {
            showError("Friend request", "Select a request to accept.");
            return;
        }
        runNetwork(() -> messagingService.acceptFriendRequest(activeUser, from), ok -> {
            if (!ok) {
                showError("Friend request", "Request not found.");
                return;
            }
            refreshMessaging();
        }, error -> showError("Unable to accept request", error));
    }

    @FXML
    private void declineFriendRequest() {
        if (!ensureLoggedIn() || pendingRequestsList == null) {
            return;
        }
        String from = pendingRequestsList.getSelectionModel().getSelectedItem();
        if (from == null || from.isBlank()) {
            showError("Friend request", "Select a request to decline.");
            return;
        }
        runNetwork(() -> messagingService.declineFriendRequest(activeUser, from), ok -> {
            if (!ok) {
                showError("Friend request", "Request not found.");
                return;
            }
            refreshMessaging();
        }, error -> showError("Unable to decline request", error));
    }

    @FXML
    private void sendChatMessage() {
        if (!ensureLoggedIn() || friendsList == null || chatMessageField == null) {
            return;
        }
        String friend = friendsList.getSelectionModel().getSelectedItem();
        if (friend == null || friend.isBlank()) {
            showError("Chat", "Select a friend to chat with.");
            return;
        }
        String text = chatMessageField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        runNetwork(() -> {
            messagingService.sendChat(activeUser, friend, text.trim());
            return true;
        }, ignored -> {
            chatMessageField.clear();
            loadChatHistory(friend);
        }, error -> showError("Unable to send message", error));
    }

    @FXML
    private void addQuestion() {
        if (questionBankList == null) {
            return;
        }
        QuestionDraft draft = showQuestionDialog("Add Question", null);
        if (draft != null) {
            qbDrafts.add(draft);
            refreshQuestionBankList();
        }
    }

    @FXML
    private void editQuestion() {
        if (questionBankList == null) {
            return;
        }
        int index = questionBankList.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            showError("Select a question", "Choose a question from the list to edit.");
            return;
        }
        QuestionDraft current = qbDrafts.get(index);
        QuestionDraft updated = showQuestionDialog("Edit Question", current);
        if (updated != null) {
            qbDrafts.set(index, updated);
            refreshQuestionBankList();
        }
    }

    @FXML
    private void deleteQuestion() {
        if (questionBankList == null) {
            return;
        }
        int index = questionBankList.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            showError("Select a question", "Choose a question from the list to delete.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Delete Question");
        alert.setContentText("Are you sure you want to delete the selected question?");
        alert.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                qbDrafts.remove(index);
                refreshQuestionBankList();
            }
        });
    }

    private QuestionDraft showQuestionDialog(String title, QuestionDraft existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField questionField = new TextField(existing != null ? existing.text : "");
        TextField optionAField = new TextField(existing != null ? existing.optionA : "");
        TextField optionBField = new TextField(existing != null ? existing.optionB : "");
        TextField optionCField = new TextField(existing != null ? existing.optionC : "");
        TextField optionDField = new TextField(existing != null ? existing.optionD : "");
        ChoiceBox<String> correctChoice = new ChoiceBox<>();
        correctChoice.getItems().setAll("A", "B", "C", "D");
        if (existing != null) {
            correctChoice.getSelectionModel().select(existing.correctOption);
        } else {
            correctChoice.getSelectionModel().selectFirst();
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Question"), questionField);
        grid.addRow(1, new Label("Option A"), optionAField);
        grid.addRow(2, new Label("Option B"), optionBField);
        grid.addRow(3, new Label("Option C"), optionCField);
        grid.addRow(4, new Label("Option D"), optionDField);
        grid.addRow(5, new Label("Correct"), correctChoice);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait();
        if (dialog.getResult() != ButtonType.OK) {
            return null;
        }

        String questionText = questionField.getText() != null ? questionField.getText().trim() : "";
        String a = optionAField.getText() != null ? optionAField.getText().trim() : "";
        String b = optionBField.getText() != null ? optionBField.getText().trim() : "";
        String c = optionCField.getText() != null ? optionCField.getText().trim() : "";
        String d = optionDField.getText() != null ? optionDField.getText().trim() : "";
        String correct = correctChoice.getSelectionModel().getSelectedItem();

        if (questionText.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
            showError("Incomplete question", "Fill in the question text and all four options.");
            return null;
        }
        return new QuestionDraft(questionText, a, b, c, d, correct);
    }

    private static final class QuestionDraft {
        private final String text;
        private final String optionA;
        private final String optionB;
        private final String optionC;
        private final String optionD;
        private final String correctOption;

        private QuestionDraft(String text, String optionA, String optionB, String optionC, String optionD, String correctOption) {
            this.text = text;
            this.optionA = optionA;
            this.optionB = optionB;
            this.optionC = optionC;
            this.optionD = optionD;
            this.correctOption = correctOption;
        }
    }
}
