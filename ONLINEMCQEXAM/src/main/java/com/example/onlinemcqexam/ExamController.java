package com.example.onlinemcqexam;
import java.util.Comparator;
import javafx.event.ActionEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ExamController {
    private static final int EXAM_DURATION_SECONDS = 900;
    private static final double PASS_THRESHOLD = 0.6;
    private static final double REGISTER_COMPACT_BREAKPOINT = 1000;
    private static final String CHAT_METADATA_SEPARATOR = "\u001F";
    private static final DateTimeFormatter CHAT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int DEFAULT_EXAM_QUESTION_COUNT = 20;
    private static final int SECONDS_PER_QUESTION = 30;
    private static final String EXAM_HISTORY_FILE = AppPaths.resourceFile("exam_history.csv").toString();
    private static final String QUESTIONS_FILE = AppPaths.packageResourceFile("questions.csv").toString();
    private static final String EXAMS_FILE = AppPaths.resourceFile("exams.csv").toString();
    private static final String SCHEDULE_FILE = AppPaths.appFile("schedule.csv").toString();
    private static final String LEGACY_SCHEDULE_FILE = AppPaths.resourceFile("schedule.csv").toString();
    private static final String RESULTS_FILE = AppPaths.resourceFile("results.csv").toString();
    private static final String RESULT_DETAILS_FILE = AppPaths.resourceFile("result_details.csv").toString();
    private static final double ANALYTICS_PASS_THRESHOLD = 40.0;
    private static final String FILTER_ALL_TERMS = "All Terms";
    private static final String FILTER_ALL_COURSES = "All Courses";
    private static final String NO_DATA_TEXT = "No data available";
    private static final String STRONGEST_SUBJECT_MESSAGE = "Consistent accuracy and strong performance";
    private static final String IMPROVEMENT_AREA_MESSAGE = "Needs improvement and more practice";
    private static final DateTimeFormatter HISTORY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter SCHEDULE_TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    private static final List<String> SUPPORTED_SEMESTERS = List.of("1-1", "1-2", "2-1", "2-2", "3-1", "3-2", "4-1", "4-2");

    @FXML
    private Node loginPane;
    @FXML
    private LoginViewController loginPaneController;
    @FXML
    private Node registerPane;
    @FXML
    private RegisterViewController registerPaneController;
    @FXML
    private Node studentDashboardPane;
    @FXML
    private Node availableExamsPane;
    @FXML
    private Node scheduledExamsPane;
    @FXML
    private Node discussionPane;
    @FXML
    private DiscussionViewController discussionPaneController;
    @FXML
    private Node messagingPane;
    @FXML
    private MessagingViewController messagingPaneController;
    @FXML
    private Node progressPane;
    @FXML
    private Node leaderboardPane;
    @FXML
    private Node levelPane;
    @FXML
    private Node startPane;
    @FXML
    private ExamStartViewController startPaneController;
    @FXML
    private Node examPane;
    @FXML
    private LiveExamViewController examPaneController;
    @FXML
    private Node resultPane;
    @FXML
    private Node teacherLoginPane;
    @FXML
    private TeacherLoginViewController teacherLoginPaneController;
    @FXML
    private Node teacherAuthFailPane;
    @FXML
    private AuthFailedController authFailedViewController;
    @FXML
    private Node teacherDashboardPane;
    @FXML
    private TeacherDashboardViewController teacherDashboardPaneController;
    @FXML
    private Node questionBankPane;
    @FXML
    private QuestionBankController questionBankViewController;
    @FXML
    private Node createExamPane;
    @FXML
    private CreateExamController createExamViewController;
    @FXML
    private Node scheduleExamPane;
    @FXML
    private ScheduleExamController scheduleExamViewController;
    @FXML
    private Node viewExamsPane;
    @FXML
    private ExamLibraryController examLibraryViewController;
    @FXML
    private Node teacherAnalyticsPane;
    @FXML
    private ResultsAnalyticsController resultsAnalyticsViewController;
    @FXML
    private ExamResultController examResultViewController;

    @FXML
    private TextField loginUsername;
    @FXML
    private PasswordField loginPassword;
    @FXML
    private Label loginMessageLabel;
    @FXML
    private Label studentWelcomeLabel;
    @FXML
    private Label dashboardProfileInitialsLabel;
    @FXML
    private Label dashboardProfileNameLabel;
    @FXML
    private Label dashboardProfileSubtitleLabel;
    @FXML
    private Label dashboardAlertsLabel;
    @FXML
    private Label dashboardUpcomingCountLabel;
    @FXML
    private Label dashboardCompletedCountLabel;
    @FXML
    private Label dashboardRankLabel;
    @FXML
    private Label dashboardNextExamTitleLabel;
    @FXML
    private Label dashboardNextExamStatusLabel;
    @FXML
    private Label dashboardNextExamDateValueLabel;
    @FXML
    private Label dashboardNextExamTimeValueLabel;
    @FXML
    private ListView<String> dashboardUpcomingExamList;

    @FXML
    private TextField registerUsername;
    @FXML
    private ChoiceBox<String> registerSemesterChoice;
    @FXML
    private PasswordField registerPassword;
    @FXML
    private PasswordField registerConfirm;
    @FXML
    private Label registerMessageLabel;
    @FXML
    private HBox registerErrorBanner;

    @FXML
    private Label levelMessageLabel;
    @FXML
    private Label levelSubtitleLabel;
    @FXML
    private ChoiceBox<String> termChoice;
    @FXML
    private ChoiceBox<String> courseChoice;
    @FXML
    private ChoiceBox<Integer> examQuestionCountChoice;

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
    private Label examTopicLabel;
    @FXML
    private Label examAutosaveLabel;
    @FXML
    private Label examCompletionLabel;
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
    private VBox availableExamCardContainer;
    @FXML
    private ListView<String> discussionList;
    @FXML
    private TextArea discussionAskArea;
    @FXML
    private TextField friendRequestField;
    @FXML
    private Label messagingFeedbackLabel;
    @FXML
    private Label messagingRequestBadgeLabel;
    @FXML
    private ListView<String> pendingRequestsList;
    @FXML
    private ListView<String> friendsList;
    @FXML
    private ListView<String> chatHistoryList;
    @FXML
    private TextField chatMessageField;
    @FXML
    private Label messagingActiveFriendLabel;
    @FXML
    private Label messagingActiveStatusLabel;
    @FXML
    private ListView<String> historyList;
    @FXML
    private TableView<CourseSummaryRow> historyTable;
    @FXML
    private TableColumn<CourseSummaryRow, String> historyCourseColumn;
    @FXML
    private TableColumn<CourseSummaryRow, Integer> historyAttemptsColumn;
    @FXML
    private TableColumn<CourseSummaryRow, String> historyBestColumn;
    @FXML
    private TableColumn<CourseSummaryRow, String> historyAvgColumn;
    @FXML
    private ChoiceBox<String> progressTermChoice;
    @FXML
    private ChoiceBox<String> progressCourseChoice;
    @FXML
    private BarChart<String, Number> progressChart;
    @FXML
    private LineChart<String, Number> progressLineChart;
    @FXML
    private Label progressEmptyStateLabel;
    @FXML
    private Label avgScoreLabel;
    @FXML
    private Label avgTimeLabel;
    @FXML
    private Label strongestSubjectLabel;
    @FXML
    private Label strongestSubjectMessageLabel;
    @FXML
    private Label improvementAreaLabel;
    @FXML
    private Label improvementAreaMessageLabel;
    @FXML
    private ListView<LeaderboardRowCard> leaderboardList;
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
    private Label teacherActiveExamsBadgeLabel;
    @FXML
    private Label teacherActiveStudentsLabel;
    @FXML
    private ProgressBar teacherActiveStudentsProgressBar;
    @FXML
    private Label teacherAverageGradeLabel;
    @FXML
    private ListView<String> questionBankList;
    @FXML
    private ChoiceBox<String> qbTermChoice;
    @FXML
    private ChoiceBox<String> qbCourseChoice;
    @FXML
    private Label qbStatusLabel;
    @FXML
    private ChoiceBox<String> createTermChoice;
    @FXML
    private ChoiceBox<String> createCourseChoice;
    @FXML
    private RadioButton createManualModeRadio;
    @FXML
    private RadioButton createAutomaticModeRadio;
    @FXML
    private ListView<String> createQuestionList;
    @FXML
    private Spinner<Integer> createNumQuestions;
    @FXML
    private Label createComputedTimeLabel;
    @FXML
    private Label createComputedMarksLabel;
    @FXML
    private Label createExamStatusLabel;
    @FXML
    private ChoiceBox<String> scheduleTermChoice;
    @FXML
    private ChoiceBox<String> scheduleCourseChoice;
    @FXML
    private ChoiceBox<String> scheduleExamChoice;
    @FXML
    private javafx.scene.control.DatePicker scheduleDate;
    @FXML
    private Spinner<Integer> scheduleHourSpinner;
    @FXML
    private Spinner<Integer> scheduleMinuteSpinner;
    @FXML
    private Button schedulePublishButton;
    @FXML
    private Label scheduleStatusLabel;
    @FXML
    private ListView<String> viewExamsList;
    @FXML
    private ChoiceBox<String> studentScheduledTermChoice;
    @FXML
    private ChoiceBox<String> studentScheduledCourseChoice;
    @FXML
    private ListView<String> studentScheduledExamList;
    @FXML
    private Button startScheduledExamButton;
    @FXML
    private Label studentScheduledSubtitleLabel;
    @FXML
    private Label studentScheduledStatusLabel;
    @FXML
    private Button viewExamEditButton;
    @FXML
    private Button viewExamDeleteButton;
    @FXML
    private Label viewExamStatusLabel;
    @FXML
    private ChoiceBox<String> analyticsTermChoice;
    @FXML
    private ChoiceBox<String> analyticsCourseChoice;
    @FXML
    private ChoiceBox<String> leaderboardTermChoice;
    @FXML
    private ChoiceBox<String> leaderboardCourseChoice;
    @FXML
    private Label leaderboardSummaryLabel;
    @FXML
    private Label leaderboardClassAverageValueLabel;
    @FXML
    private Label leaderboardParticipationValueLabel;
    @FXML
    private Label leaderboardMasteryValueLabel;
    @FXML
    private Label teacherAnalyticsAverageLabel;
    @FXML
    private Label teacherAnalyticsHighestLabel;
    @FXML
    private Label teacherAnalyticsLowestLabel;
    @FXML
    private Label teacherAnalyticsPassLabel;

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
    private Timeline messagingRefreshTimeline;
    private Timeline dashboardAlertRefreshTimeline;
    private int secondsRemaining;
    private String selectedTerm;
    private String selectedCourseCode;
    private String selectedCourseLabel;
    private String activeUser;
    private String activeUserSemester;
    private String activeTeacher;
    private String activeAttemptType = "practice";
    private String activeAttemptExamId = "NA";
    private int allocatedExamSeconds;
    private final Map<String, String> courseCodeByLabel = new LinkedHashMap<>();
    private final Map<String, String> qbCourseCodeByLabel = new LinkedHashMap<>();
    private final Map<String, String> createCourseCodeByLabel = new LinkedHashMap<>();
    private final Map<String, String> scheduleCourseCodeByLabel = new LinkedHashMap<>();
    private final Map<String, String> studentScheduledCourseCodeByLabel = new LinkedHashMap<>();
    private final Map<String, String> analyticsCourseCodeByLabel = new LinkedHashMap<>();
    private final Map<String, String> leaderboardCourseCodeByLabel = new LinkedHashMap<>();
    private final Map<String, Long> latestSeenIncomingMessageByFriend = new LinkedHashMap<>();
    private final Map<String, TeacherExamSummary> scheduleExamByLabel = new LinkedHashMap<>();
    private final Map<String, TeacherExamSummary> viewExamByLabel = new LinkedHashMap<>();
    private final Map<String, ScheduleRow> studentScheduledRowByLabel = new LinkedHashMap<>();
    private final Map<String, List<ExamRecord>> courseMap = new LinkedHashMap<>();
    private boolean progressFilterRefreshInProgress;
    private boolean chatAlertSnapshotInitialized;
    private boolean dashboardAlertRefreshInFlight;
    private final ToggleGroup createModeGroup = new ToggleGroup();
    private final List<TeacherQuestionRow> qbCurrentRows = new ArrayList<>();
    private final List<Question> createCourseQuestions = new ArrayList<>();

    @FXML
    private void initialize() {
        bindExtractedViews();
        optionA.setToggleGroup(optionsGroup);
        optionB.setToggleGroup(optionsGroup);
        optionC.setToggleGroup(optionsGroup);
        optionD.setToggleGroup(optionsGroup);
        optionA.setUserData(0);
        optionB.setUserData(1);
        optionC.setUserData(2);
        optionD.setUserData(3);
        if (timerLabel != null) {
            timerLabel.setText("No data available");
        }
        configureSpinners();
        configureExamQuestionCount();
        configureHistoryTable();
        configureProgressFilters();
        configureProgressCharts();
        configureLeaderboardList();
        configureMessaging();
        configureDiscussionFeed();
        configureTeacherAuthoring();
        configureAuthFailedNavigation();
        configureQuestionBankNavigation();
        configureCreateExamNavigation();
        configureScheduleExamNavigation();
        configureExamLibraryNavigation();
        configureResultsAnalyticsNavigation();
        configureExamResultNavigation();
        populateRegistrationSemesters();
        populateSampleData();
        configureExamSelection();
        configureRegisterResponsiveMode();
        showPane(loginPane);
    }

    private void bindExtractedViews() {
        if (loginPaneController != null) {
            loginUsername = loginPaneController.loginUsername;
            loginPassword = loginPaneController.loginPassword;
            loginMessageLabel = loginPaneController.loginMessageLabel;
            loginPaneController.setOnLoginRequested(this::loginUser);
            loginPaneController.setOnRegisterRequested(this::showRegister);
            loginPaneController.setOnTeacherLoginRequested(this::showTeacherLogin);
        }

        if (registerPaneController != null) {
            registerUsername = registerPaneController.registerUsername;
            registerSemesterChoice = registerPaneController.registerSemesterChoice;
            registerPassword = registerPaneController.registerPassword;
            registerConfirm = registerPaneController.registerConfirm;
            registerMessageLabel = registerPaneController.registerMessageLabel;
            registerErrorBanner = registerPaneController.registerErrorBanner;
            registerPaneController.setOnRegisterRequested(this::registerUser);
            registerPaneController.setOnShowLoginRequested(this::showLogin);
        }

        if (discussionPaneController != null) {
            discussionAskArea = discussionPaneController.discussionAskArea;
            discussionList = discussionPaneController.discussionList;
            discussionPaneController.setOnShowStudentDashboard(this::showStudentDashboard);
            discussionPaneController.setOnShowAvailableExams(this::showAvailableExams);
            discussionPaneController.setOnShowScheduledExams(this::showScheduledExams);
            discussionPaneController.setOnShowExamHistory(this::showExamHistory);
            discussionPaneController.setOnShowLeaderboard(this::showLeaderboard);
            discussionPaneController.setOnShowMessaging(this::showMessaging);
            discussionPaneController.setOnLogoutRequested(this::handleLogout);
            discussionPaneController.setOnPostRequested(this::postDiscussion);
            discussionPaneController.setOnSubmitKeyPressed(this::handleDiscussionSubmitKey);
        }

        if (messagingPaneController != null) {
            friendRequestField = messagingPaneController.friendRequestField;
            messagingFeedbackLabel = messagingPaneController.messagingFeedbackLabel;
            messagingRequestBadgeLabel = messagingPaneController.messagingRequestBadgeLabel;
            pendingRequestsList = messagingPaneController.pendingRequestsList;
            friendsList = messagingPaneController.friendsList;
            chatHistoryList = messagingPaneController.chatHistoryList;
            chatMessageField = messagingPaneController.chatMessageField;
            messagingActiveFriendLabel = messagingPaneController.messagingActiveFriendLabel;
            messagingActiveStatusLabel = messagingPaneController.messagingActiveStatusLabel;
            messagingPaneController.setOnShowStudentDashboard(this::showStudentDashboard);
            messagingPaneController.setOnShowAvailableExams(this::showAvailableExams);
            messagingPaneController.setOnShowScheduledExams(this::showScheduledExams);
            messagingPaneController.setOnShowExamHistory(this::showExamHistory);
            messagingPaneController.setOnShowLeaderboard(this::showLeaderboard);
            messagingPaneController.setOnLogoutRequested(this::handleLogout);
            messagingPaneController.setOnSendFriendRequest(this::sendFriendRequest);
            messagingPaneController.setOnSendChatMessage(this::sendChatMessage);
        }

        if (startPaneController != null) {
            subtitleLabel = startPaneController.subtitleLabel;
            passLabel = startPaneController.passLabel;
            examQuestionCountChoice = startPaneController.examQuestionCountChoice;
            startPaneController.setOnShowStudentDashboard(this::showStudentDashboard);
            startPaneController.setOnShowScheduledExams(this::showScheduledExams);
            startPaneController.setOnShowExamHistory(this::showExamHistory);
            startPaneController.setOnShowLeaderboard(this::showLeaderboard);
            startPaneController.setOnShowDiscussion(this::showDiscussion);
            startPaneController.setOnShowMessaging(this::showMessaging);
            startPaneController.setOnLogoutRequested(this::handleLogout);
            startPaneController.setOnStartExamRequested(this::startExam);
        }

        if (examPaneController != null) {
            progressLabel = examPaneController.progressLabel;
            timerLabel = examPaneController.timerLabel;
            questionLabel = examPaneController.questionLabel;
            examTopicLabel = examPaneController.examTopicLabel;
            optionA = examPaneController.optionA;
            optionB = examPaneController.optionB;
            optionC = examPaneController.optionC;
            optionD = examPaneController.optionD;
            levelLabel = examPaneController.levelLabel;
            progressBar = examPaneController.progressBar;
            prevButton = examPaneController.prevButton;
            nextButton = examPaneController.nextButton;
            examAutosaveLabel = examPaneController.examAutosaveLabel;
            examCompletionLabel = examPaneController.examCompletionLabel;
            submitButton = examPaneController.submitButton;
            examPaneController.setOnPreviousRequested(this::previousQuestion);
            examPaneController.setOnNextRequested(this::nextQuestion);
            examPaneController.setOnSubmitRequested(this::submitExam);
        }

        if (teacherLoginPaneController != null) {
            teacherRoleChoice = teacherLoginPaneController.teacherRoleChoice;
            teacherUsername = teacherLoginPaneController.teacherUsername;
            teacherPassword = teacherLoginPaneController.teacherPassword;
            teacherLoginMessageLabel = teacherLoginPaneController.teacherLoginMessageLabel;
            teacherLoginPaneController.setOnLoginRequested(this::loginTeacher);
            teacherLoginPaneController.setOnBackToStudentLoginRequested(this::showLogin);
        }

        if (teacherDashboardPaneController != null) {
            teacherWelcomeLabel = teacherDashboardPaneController.teacherWelcomeLabel;
            teacherActiveExamsBadgeLabel = teacherDashboardPaneController.teacherActiveExamsBadgeLabel;
            teacherActiveStudentsLabel = teacherDashboardPaneController.teacherActiveStudentsLabel;
            teacherActiveStudentsProgressBar = teacherDashboardPaneController.teacherActiveStudentsProgressBar;
            teacherAverageGradeLabel = teacherDashboardPaneController.teacherAverageGradeLabel;
            teacherDashboardPaneController.setOnShowQuestionBank(this::showQuestionBank);
            teacherDashboardPaneController.setOnShowCreateExam(this::showCreateExam);
            teacherDashboardPaneController.setOnShowScheduleExam(this::showScheduleExam);
            teacherDashboardPaneController.setOnShowTeacherAnalytics(this::showTeacherAnalytics);
            teacherDashboardPaneController.setOnShowViewExams(this::showViewExams);
            teacherDashboardPaneController.setOnLogoutRequested(this::logout);
        }
    }

    private void populateRegistrationSemesters() {
        if (registerSemesterChoice == null) {
            return;
        }
        registerSemesterChoice.getItems().setAll(SUPPORTED_SEMESTERS);
        registerSemesterChoice.getSelectionModel().clearSelection();
    }

    private void configureExamResultNavigation() {
        if (examResultViewController != null) {
            examResultViewController.setNavigationActions(
                    this::showLeaderboard,
                    this::showProgress,
                    this::showDiscussion,
                    this::showStudentDashboard
            );
        }
    }

    private void configureQuestionBankNavigation() {
        if (questionBankViewController != null) {
            questionBankViewController.setOnBackRequested(this::showTeacherDashboard);
        }
    }

    private void configureAuthFailedNavigation() {
        if (authFailedViewController != null) {
            authFailedViewController.setOnBackToLoginRequested(this::showTeacherLogin);
        }
    }

    private void configureCreateExamNavigation() {
        if (createExamViewController != null) {
            createExamViewController.setOnBackRequested(this::showTeacherDashboard);
            createExamViewController.setOnManageBankRequested(this::showQuestionBank);
            createExamViewController.setOnExamSaved(() -> {
                refreshViewExamsList();
                loadExamCatalog();
            });
        }
    }

    private void configureScheduleExamNavigation() {
        if (scheduleExamViewController != null) {
            scheduleExamViewController.setOnBackRequested(this::showTeacherDashboard);
            scheduleExamViewController.setOnPublished(() -> {
                loadExamCatalog();
                refreshViewExamsList();
                refreshStudentScheduledExamList();
                refreshDashboardUpcomingCard();
            });
        }
    }

    private void configureResultsAnalyticsNavigation() {
        if (resultsAnalyticsViewController != null) {
            resultsAnalyticsViewController.setOnBackRequested(this::showTeacherDashboard);
            resultsAnalyticsViewController.refreshFromData();
        }
    }

    private void configureExamLibraryNavigation() {
        if (examLibraryViewController != null) {
            examLibraryViewController.setOnDashboardRequested(this::showTeacherDashboard);
            examLibraryViewController.setOnScheduleRequested(this::showScheduleExam);
            examLibraryViewController.setOnAnalyticsRequested(this::showTeacherAnalytics);
            examLibraryViewController.setOnNewExamRequested(this::showCreateExam);
            examLibraryViewController.refreshFromData();
        }
    }

    private void configureRegisterResponsiveMode() {
        if (registerPane == null) {
            return;
        }
        registerPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            updateRegisterResponsiveStyles(newScene.getWidth());
            newScene.widthProperty().addListener((widthObs, oldWidth, newWidth) ->
                    updateRegisterResponsiveStyles(newWidth == null ? 0 : newWidth.doubleValue()));
        });
        Platform.runLater(() -> {
            if (registerPane.getScene() != null) {
                updateRegisterResponsiveStyles(registerPane.getScene().getWidth());
            }
        });
    }

    private void updateRegisterResponsiveStyles(double sceneWidth) {
        if (registerPane == null) {
            return;
        }
        boolean compact = sceneWidth > 0 && sceneWidth < REGISTER_COMPACT_BREAKPOINT;
        if (compact) {
            if (!registerPane.getStyleClass().contains("register-compact")) {
                registerPane.getStyleClass().add("register-compact");
            }
        } else {
            registerPane.getStyleClass().remove("register-compact");
        }
    }

    private void configureHistoryTable() {
        if (historyTable == null) {
            return;
        }
        historyCourseColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        historyAttemptsColumn.setCellValueFactory(new PropertyValueFactory<>("attempts"));
        historyBestColumn.setCellValueFactory(new PropertyValueFactory<>("bestPercentage"));
        historyAvgColumn.setCellValueFactory(new PropertyValueFactory<>("averagePercentage"));
        Label placeholder = new Label("No attempts yet for this course filter.");
        placeholder.getStyleClass().add("progress-empty-state");
        historyTable.setPlaceholder(placeholder);
        historyTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(CourseSummaryRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                    return;
                }
                setStyle(item.bestPercentageValue >= 80.0
                        ? "-fx-font-weight: 700;"
                        : "");
            }
        });

        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                return;
            }
            List<ExamRecord> attempts = courseMap.get(selected.getCourseCode());
            loadChartForCourse(attempts);
        });
    }

    private void configureProgressFilters() {
        if (progressTermChoice != null) {
            progressTermChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (progressFilterRefreshInProgress) {
                    return;
                }
                refreshProgressCourseFilterOptions();
                loadExamHistory();
            });
        }
        if (progressCourseChoice != null) {
            progressCourseChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (progressFilterRefreshInProgress) {
                    return;
                }
                loadExamHistory();
            });
        }
    }

    private void configureProgressCharts() {
        if (progressLineChart != null) {
            progressLineChart.setAnimated(false);
            progressLineChart.setLegendVisible(false);
        }
        if (progressChart != null) {
            progressChart.setAnimated(false);
            progressChart.setLegendVisible(false);
            progressChart.setCategoryGap(18);
            progressChart.setBarGap(8);
        }
    }

    private void configureExamQuestionCount() {
        if (examQuestionCountChoice == null) {
            return;
        }
        examQuestionCountChoice.getItems().setAll(10, 20, 30, 40, 50);
        examQuestionCountChoice.getSelectionModel().select(Integer.valueOf(DEFAULT_EXAM_QUESTION_COUNT));
    }

    private void configureExamSelection() {
        if (termChoice != null) {
            termChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> onTermChanged(newValue));
        }
        loadExamCatalog();
    }

    private void loadExamCatalog() {
        try {
            List<String> terms = filterStudentTerms(QuestionBank.loadTerms());
            if (termChoice != null) {
                termChoice.getItems().setAll(terms);
                termChoice.setDisable(!normalizeSemester(activeUserSemester).isBlank());
                if (!terms.isEmpty()) {
                    termChoice.getSelectionModel().selectFirst();
                    onTermChanged(terms.get(0));
                }
            }
            updateAvailableExamList(terms);
            refreshDashboardUpcomingCard();
        } catch (IOException ex) {
            if (levelMessageLabel != null) {
                levelMessageLabel.setText("Unable to load exam catalog.");
            }
        }
    }

    private void updateAvailableExamList(List<String> terms) {
        if (availableExamList == null) {
            return;
        }
        List<String> rows = new ArrayList<>();
        for (String term : terms) {
            rows.add(termLabel(term));
            try {
                List<QuestionBank.CourseInfo> courses = QuestionBank.loadCoursesForTerm(term);
                for (QuestionBank.CourseInfo course : courses) {
                    rows.add("  - " + course.getDisplayLabel());
                }
            } catch (IOException ex) {
                rows.add("  - Unable to load courses");
            }
        }
        availableExamList.getItems().setAll(rows);
        renderAvailableExamCards(terms);
    }

    private void renderAvailableExamCards(List<String> terms) {
        if (availableExamCardContainer == null) {
            return;
        }
        availableExamCardContainer.getChildren().clear();
        if (terms == null || terms.isEmpty()) {
            Label empty = new Label("No courses available for your semester.");
            empty.getStyleClass().add("available-course-desc");
            availableExamCardContainer.getChildren().add(empty);
            return;
        }

        for (String term : terms) {
            List<QuestionBank.CourseInfo> courses;
            try {
                courses = QuestionBank.loadCoursesForTerm(term);
            } catch (IOException ex) {
                continue;
            }
            if (courses.isEmpty()) {
                continue;
            }

            Label termTitle = new Label(term);
            termTitle.getStyleClass().add("available-term-title");
            Label termMeta = new Label(termLabel(term));
            termMeta.getStyleClass().add("available-group-meta");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(termTitle, spacer, termMeta);
            header.setAlignment(Pos.CENTER_LEFT);

            FlowPane grid = new FlowPane();
            grid.setHgap(20);
            grid.setVgap(20);
            grid.setPrefWrapLength(860);
            grid.getStyleClass().add("available-card-grid");

            for (QuestionBank.CourseInfo course : courses) {
                String courseCode = canonicalCourseCode(course.getCourseCode());
                String courseName = course.getCourseName() == null || course.getCourseName().isBlank()
                        ? courseCode
                        : course.getCourseName().trim();

                Label badge = new Label(courseCode);
                badge.getStyleClass().add("available-badge");
                Region badgeSpacer = new Region();
                HBox.setHgrow(badgeSpacer, Priority.ALWAYS);
                Label iconOne = new Label("i");
                iconOne.getStyleClass().add("available-icon-placeholder");
                Label iconTwo = new Label("...");
                iconTwo.getStyleClass().add("available-icon-placeholder");
                HBox topRow = new HBox(8, badge, badgeSpacer, iconOne, iconTwo);
                topRow.setAlignment(Pos.CENTER_LEFT);

                Label title = new Label(courseName);
                title.setWrapText(true);
                title.getStyleClass().add("available-course-title");

                Label desc = new Label("Questions are available only for semester " + term + " in this course.");
                desc.setWrapText(true);
                desc.getStyleClass().add("available-course-desc");

                Button continueButton = new Button("Continue");
                continueButton.setUserData(term + "|" + courseCode);
                continueButton.getStyleClass().add("available-continue-button");
                continueButton.setOnAction(this::selectCourseExam);

                VBox card = new VBox(12, topRow, title, desc, continueButton);
                card.setPrefWidth(255);
                card.getStyleClass().add("available-course-card");
                grid.getChildren().add(card);
            }

            VBox termGroup = new VBox(16, header, grid);
            termGroup.getStyleClass().add("available-term-group");
            availableExamCardContainer.getChildren().add(termGroup);
        }
    }

    private void onTermChanged(String term) {
        if (!isActiveStudentSemester(term)) {
            if (courseChoice != null) {
                courseChoice.getItems().clear();
            }
            levelMessageLabel.setText("You can only view courses from semester " + activeUserSemester + ".");
            return;
        }
        selectedTerm = term;
        courseCodeByLabel.clear();
        selectedCourseCode = null;
        selectedCourseLabel = null;
        if (courseChoice == null || term == null || term.isBlank()) {
            return;
        }
        try {
            List<QuestionBank.CourseInfo> courses = QuestionBank.loadCoursesForTerm(term);
            List<String> labels = new ArrayList<>();
            for (QuestionBank.CourseInfo course : courses) {
                String label = course.getDisplayLabel();
                labels.add(label);
                courseCodeByLabel.put(label, course.getCourseCode());
            }
            courseChoice.getItems().setAll(labels);
            if (!labels.isEmpty()) {
                courseChoice.getSelectionModel().selectFirst();
            }
        } catch (IOException ex) {
            courseChoice.getItems().clear();
            levelMessageLabel.setText("Unable to load courses for " + term + ".");
        }
    }

    private String termLabel(String term) {
        return switch (term) {
            case "1-1" -> "Level-1 Term-1 (1-1)";
            case "1-2" -> "Level-1 Term-2 (1-2)";
            case "2-1" -> "Level-2 Term-1 (2-1)";
            case "2-2" -> "Level-2 Term-2 (2-2)";
            case "3-1" -> "Level-3 Term-1 (3-1)";
            case "3-2" -> "Level-3 Term-2 (3-2)";
            case "4-1" -> "Level-4 Term-1 (4-1)";
            case "4-2" -> "Level-4 Term-2 (4-2)";
            default -> term;
        };
    }

    private List<String> filterStudentTerms(List<String> terms) {
        if (terms == null) {
            return List.of();
        }
        String semester = normalizeSemester(activeUserSemester);
        List<String> filtered = new ArrayList<>();
        if (semester.isBlank()) {
            filtered.addAll(terms);
            return filtered;
        }
        for (String term : terms) {
            if (semester.equals(normalizeSemester(term))) {
                filtered.add(term);
            }
        }
        if (filtered.isEmpty()) {
            filtered.add(semester);
        }
        return filtered;
    }

    private String normalizeSemester(String semester) {
        if (semester == null) {
            return "";
        }
        String normalized = semester.trim();
        return SUPPORTED_SEMESTERS.contains(normalized) ? normalized : "";
    }

    private boolean isActiveStudentSemester(String semester) {
        String activeSemester = normalizeSemester(activeUserSemester);
        return activeSemester.isBlank() || activeSemester.equals(normalizeSemester(semester));
    }

    private void setStudentSemesterChoice(ChoiceBox<String> choiceBox, String semester) {
        if (choiceBox == null) {
            return;
        }
        String normalized = normalizeSemester(semester);
        if (normalized.isBlank()) {
            choiceBox.getItems().clear();
            choiceBox.setDisable(false);
            return;
        }
        choiceBox.getItems().setAll(normalized);
        choiceBox.getSelectionModel().select(normalized);
        choiceBox.setDisable(true);
    }

    private void configureSpinners() {
        if (createNumQuestions != null) {
            createNumQuestions.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 20, 1));
            createNumQuestions.setEditable(true);
        }
        if (scheduleHourSpinner != null) {
            scheduleHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9, 1));
            scheduleHourSpinner.setEditable(false);
        }
        if (scheduleMinuteSpinner != null) {
            scheduleMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
            scheduleMinuteSpinner.setEditable(false);
        }
    }

    private void configureTeacherAuthoring() {
        if (createManualModeRadio != null) {
            createManualModeRadio.setToggleGroup(createModeGroup);
        }
        if (createAutomaticModeRadio != null) {
            createAutomaticModeRadio.setToggleGroup(createModeGroup);
        }
        if (createManualModeRadio != null) {
            createManualModeRadio.setSelected(true);
        }

        if (questionBankList != null) {
            questionBankList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        }
        if (createQuestionList != null) {
            createQuestionList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            createQuestionList.getSelectionModel().getSelectedItems().addListener((ListChangeListener<String>) change -> {
                int count = createQuestionList.getSelectionModel().getSelectedItems().size();
                if (isManualModeSelected() && createNumQuestions != null && createNumQuestions.getValueFactory() != null) {
                    createNumQuestions.getValueFactory().setValue(count);
                }
                updateTimeAndMarks(count);
            });
        }

        if (qbTermChoice != null) {
            qbTermChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                loadQbCoursesForTerm(newValue);
                refreshQuestionBankList();
            });
        }
        if (qbCourseChoice != null) {
            qbCourseChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refreshQuestionBankList());
        }

        if (createTermChoice != null) {
            createTermChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                loadCreateExamCoursesForTerm(newValue);
                loadCreateExamQuestionPool();
            });
        }
        if (createCourseChoice != null) {
            createCourseChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadCreateExamQuestionPool());
        }

        if (createModeGroup != null) {
            createModeGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
                updateCreateExamModeView();
                updateCreateExamComputedFields();
            });
        }

        if (createNumQuestions != null && createNumQuestions.getValueFactory() != null) {
            createNumQuestions.getValueFactory().valueProperty().addListener((obs, oldValue, newValue) -> {
                if (!isManualModeSelected()) {
                    int count = newValue == null ? 0 : newValue;
                    updateTimeAndMarks(count);
                }
            });
        }

        if (scheduleTermChoice != null) {
            scheduleTermChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                loadScheduleCoursesForTerm(newValue);
                refreshScheduleExamChoices();
            });
        }
        if (scheduleCourseChoice != null) {
            scheduleCourseChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refreshScheduleExamChoices());
        }
        if (scheduleExamChoice != null) {
            scheduleExamChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateScheduleControls());
        }
        if (studentScheduledTermChoice != null) {
            studentScheduledTermChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                loadStudentScheduledCoursesForTerm(newValue);
                refreshStudentScheduledExamList();
            });
        }
        if (studentScheduledCourseChoice != null) {
            studentScheduledCourseChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refreshStudentScheduledExamList());
        }
        if (studentScheduledExamList != null) {
            studentScheduledExamList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateStudentScheduledControls());
        }
        if (analyticsTermChoice != null) {
            analyticsTermChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                loadAnalyticsCoursesForTerm(newValue);
                updateTeacherAnalyticsMetrics();
            });
        }
        if (analyticsCourseChoice != null) {
            analyticsCourseChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateTeacherAnalyticsMetrics());
        }
        if (leaderboardTermChoice != null) {
            leaderboardTermChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                    loadLeaderboardCoursesForTerm(newValue));
        }
        if (leaderboardCourseChoice != null) {
            leaderboardCourseChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                refreshLeaderboardView());
        }
        if (viewExamsList != null) {
            viewExamsList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateViewExamControls());
        }

        loadTeacherTerms();
        updateCreateExamModeView();
        updateCreateExamComputedFields();
        refreshScheduleExamChoices();
        refreshStudentScheduledExamList();
        updateStudentScheduledControls();
        loadLeaderboardCoursesForTerm(leaderboardTermChoice != null ? leaderboardTermChoice.getValue() : null);
        refreshLeaderboardView();
        updateViewExamControls();
    }

    private void configureMessaging() {
        if (pendingRequestsList != null) {
            pendingRequestsList.setCellFactory(listView -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.isBlank()) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    Label avatarLabel = new Label(item.substring(0, 1).toUpperCase());
                    avatarLabel.getStyleClass().add("messaging-avatar-label");
                    StackPane avatar = new StackPane(avatarLabel);
                    avatar.getStyleClass().add("messaging-avatar");

                    Label nameLabel = new Label(item);
                    nameLabel.getStyleClass().add("messaging-request-name");
                    Label majorLabel = new Label("Computer Science");
                    majorLabel.getStyleClass().add("messaging-request-meta");
                    VBox info = new VBox(2, nameLabel, majorLabel);

                    Button acceptButton = new Button("Accept");
                    acceptButton.getStyleClass().add("messaging-accept-button");
                    acceptButton.setOnAction(event -> handleFriendRequestDecision(item, true));

                    Button declineButton = new Button("Decline");
                    declineButton.getStyleClass().add("messaging-decline-button");
                    declineButton.setOnAction(event -> handleFriendRequestDecision(item, false));

                    HBox actions = new HBox(6, acceptButton, declineButton);
                    actions.setAlignment(Pos.CENTER_RIGHT);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    HBox row = new HBox(10, avatar, info, spacer, actions);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("messaging-request-card");

                    setText(null);
                    setGraphic(row);
                }
            });
        }

        if (friendsList != null) {
            friendsList.setCellFactory(listView -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.isBlank()) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    Label avatarLabel = new Label(item.substring(0, 1).toUpperCase());
                    avatarLabel.getStyleClass().add("messaging-avatar-label");
                    StackPane avatar = new StackPane(avatarLabel);
                    avatar.getStyleClass().add("messaging-avatar");

                    Label nameLabel = new Label(item);
                    nameLabel.getStyleClass().add("messaging-friend-name");
                    Label statusLabel = new Label(friendStatusText(item));
                    statusLabel.getStyleClass().add("messaging-friend-status");
                    VBox info = new VBox(2, nameLabel, statusLabel);

                    HBox row = new HBox(10, avatar, info);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("messaging-friend-card");

                    setText(null);
                    setGraphic(row);
                }
            });

            friendsList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null || newValue.isBlank()) {
                    if (chatHistoryList != null) {
                        chatHistoryList.getItems().clear();
                    }
                    updateMessagingHeader("Select a friend", "Active");
                    return;
                }
                updateMessagingHeader(newValue, "Typing...");
                loadChatHistory(newValue);
            });
        }

        if (chatHistoryList != null) {
            chatHistoryList.setCellFactory(listView -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.isBlank()) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    ChatBubbleData bubble = parseChatBubble(item, getIndex());

                    Label messageLabel = new Label(bubble.message());
                    messageLabel.setWrapText(true);
                    messageLabel.getStyleClass().add("messaging-bubble-text");

                    Label timeLabel = new Label(bubble.timeLabel());
                    timeLabel.getStyleClass().add("messaging-bubble-time");

                    VBox bubbleBox = new VBox(4, messageLabel, timeLabel);
                    bubbleBox.getStyleClass().add(bubble.outgoing() ? "messaging-bubble-right" : "messaging-bubble-left");

                    HBox row = new HBox(bubbleBox);
                    row.getStyleClass().add("messaging-chat-bubble-row");
                    row.setAlignment(bubble.outgoing() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                    setText(null);
                    setGraphic(row);
                }
            });
        }
    }

    private String friendStatusText(String friend) {
        int code = Math.abs(friend.hashCode() % 3);
        return switch (code) {
            case 0 -> "Typing...";
            case 1 -> "Active";
            default -> "Sent you a message";
        };
    }

    private void updateMessagingHeader(String friendName, String status) {
        if (messagingActiveFriendLabel != null) {
            messagingActiveFriendLabel.setText(friendName == null || friendName.isBlank() ? "Select a friend" : friendName);
        }
        if (messagingActiveStatusLabel != null) {
            messagingActiveStatusLabel.setText(status == null || status.isBlank() ? "Active" : status);
        }
    }

    private ChatBubbleData parseChatBubble(String raw, int index) {
        String value = raw == null ? "" : raw;
        String sender;
        String message;
        String timeLabel;

        String[] metadataParts = value.split(CHAT_METADATA_SEPARATOR, 3);
        if (metadataParts.length == 3) {
            sender = metadataParts[0].trim();
            message = metadataParts[2].trim();
            timeLabel = formatChatTimestamp(metadataParts[1]);
        } else {
            int split = value.indexOf(": ");
            sender = split >= 0 ? value.substring(0, split).trim() : "Friend";
            message = split >= 0 ? value.substring(split + 2).trim() : value.trim();
            int minute = (index * 3) + 1;
            timeLabel = minute + " min";
        }
        boolean outgoing = activeUser != null && activeUser.equalsIgnoreCase(sender);
        return new ChatBubbleData(sender, message, outgoing, timeLabel);
    }

    private String formatChatTimestamp(String rawTimestamp) {
        try {
            long timestamp = Long.parseLong(rawTimestamp == null ? "" : rawTimestamp.trim());
            return Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .format(CHAT_TIME_FORMATTER);
        } catch (RuntimeException ex) {
            return "Now";
        }
    }

    private void handleFriendRequestDecision(String from, boolean accept) {
        if (!ensureLoggedIn() || from == null || from.isBlank()) {
            return;
        }
        if (accept) {
            runNetwork(() -> messagingService.acceptFriendRequest(activeUser, from), ok -> {
                if (!ok) {
                    showError("Friend request", "Request not found.");
                    return;
                }
                refreshMessaging();
            }, error -> showError("Unable to accept request", error));
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

    private record ChatBubbleData(String sender, String message, boolean outgoing, String timeLabel) {
    }

    private void configureDiscussionFeed() {
        if (discussionList == null) {
            return;
        }
        discussionList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                DiscussionCardData data = parseDiscussionMessage(item, getIndex());

                Label avatarText = new Label(data.avatarInitials());
                avatarText.getStyleClass().add("discussion-feed-avatar-label");
                StackPane avatar = new StackPane(avatarText);
                avatar.getStyleClass().add("discussion-feed-avatar");

                Label userLabel = new Label(data.author());
                userLabel.getStyleClass().add("discussion-feed-user");
                Label timeLabel = new Label(data.ageText());
                timeLabel.getStyleClass().add("discussion-feed-time");
                VBox metaBox = new VBox(2, userLabel, timeLabel);

                Label menu = new Label("...");
                menu.getStyleClass().add("discussion-feed-menu");

                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                HBox header = new HBox(10, avatar, metaBox, spacer, menu);
                header.setAlignment(Pos.CENTER_LEFT);

                Label content = new Label(data.content());
                content.setWrapText(true);
                content.getStyleClass().add("discussion-feed-content");

                Button commentButton = new Button("Comment");
                commentButton.getStyleClass().add("discussion-feed-action-button");
                commentButton.setOnAction(event -> {
                    int current = getIndex();
                    if (current >= 0 && discussionList != null) {
                        discussionList.getSelectionModel().select(current);
                        commentDiscussion();
                    }
                });

                Label likes = new Label("Likes " + data.likes());
                likes.getStyleClass().add("discussion-feed-like");

                HBox actionRow = new HBox(8, commentButton, likes);
                actionRow.getStyleClass().add("discussion-feed-actions");
                actionRow.setAlignment(Pos.CENTER_LEFT);

                VBox card = new VBox(10, header, content, actionRow);
                card.getStyleClass().add("discussion-feed-card");

                setText(null);
                setGraphic(card);
            }
        });
    }

    private void configureLeaderboardList() {
        if (leaderboardList == null) {
            return;
        }
        Label placeholder = new Label("No rankings yet for the selected semester and course.");
        placeholder.getStyleClass().add("leaderboard-empty-label");
        leaderboardList.setPlaceholder(placeholder);
        leaderboardList.setFocusTraversable(false);
        leaderboardList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(LeaderboardRowCard item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label rankNumber = new Label(String.format("%02d", item.rank()));
                rankNumber.getStyleClass().add("leaderboard-rank-number");
                Label tierLabel = new Label(item.tierLabel());
                tierLabel.getStyleClass().add("leaderboard-rank-icon");
                VBox rankBox = new VBox(2, rankNumber, tierLabel);
                rankBox.setAlignment(Pos.CENTER_LEFT);

                Label avatarText = new Label(item.avatarText());
                avatarText.getStyleClass().add("leaderboard-avatar-text");
                StackPane avatar = new StackPane(avatarText);
                avatar.getStyleClass().add("leaderboard-avatar-circle");

                Label nameLabel = new Label(item.username());
                nameLabel.getStyleClass().add("leaderboard-name");
                Label subjectLabel = new Label(item.subtitle());
                subjectLabel.getStyleClass().add("leaderboard-subject");
                HBox attemptBadge = new HBox();
                Label attemptsLabel = new Label(item.attempts() + " attempt(s)");
                attemptsLabel.getStyleClass().add("leaderboard-attempt-badge");
                attemptBadge.getChildren().add(attemptsLabel);

                ProgressBar scoreBar = new ProgressBar(item.progressValue());
                scoreBar.setMaxWidth(Double.MAX_VALUE);
                scoreBar.getStyleClass().add("leaderboard-progress-bar");

                VBox identityBox = new VBox(6, nameLabel, subjectLabel, attemptBadge, scoreBar);
                HBox.setHgrow(identityBox, Priority.ALWAYS);

                Label avgLabel = new Label(String.format("AVG %.1f%%", item.averageScore()));
                avgLabel.getStyleClass().add("leaderboard-score-main");
                Label bestLabel = new Label(String.format("BEST %.1f%%", item.bestScore()));
                bestLabel.getStyleClass().add("leaderboard-score-sub");
                VBox scoreBox = new VBox(6, avgLabel, bestLabel);
                scoreBox.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(14, rankBox, avatar, identityBox, scoreBox);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("leaderboard-row-card");
                if (item.currentUser()) {
                    row.getStyleClass().add("leaderboard-row-current");
                }
                if (item.rank() == 1) {
                    row.getStyleClass().add("leaderboard-row-top1");
                } else if (item.rank() == 2) {
                    row.getStyleClass().add("leaderboard-row-top2");
                } else if (item.rank() == 3) {
                    row.getStyleClass().add("leaderboard-row-top3");
                }

                setText(null);
                setGraphic(row);
                if (!getStyleClass().contains("leaderboard-list-cell")) {
                    getStyleClass().add("leaderboard-list-cell");
                }
            }
        });
    }

    private DiscussionCardData parseDiscussionMessage(String raw, int index) {
        String text = raw == null ? "" : raw.trim();
        boolean commented = text.endsWith("[Commented]");
        if (commented) {
            text = text.substring(0, text.length() - "[Commented]".length()).trim();
        }

        if (text.startsWith("Q:")) {
            text = text.substring(2).trim();
        }

        String author = "Student";
        int open = text.lastIndexOf("(");
        int close = text.lastIndexOf(")");
        if (open >= 0 && close > open) {
            author = text.substring(open + 1, close).trim();
            text = text.substring(0, open).trim();
        }

        if (commented) {
            text = text + "\n\nComment status: Commented";
        }

        int minutes = Math.max(1, (index + 1) * 2);
        String ageText = minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        int likes = 6 + Math.abs(raw.hashCode() % 33);
        String initials = author.isBlank() ? "S" : author.substring(0, 1).toUpperCase();
        return new DiscussionCardData(author, text, ageText, likes, initials);
    }

    private record DiscussionCardData(String author, String content, String ageText, int likes, String avatarInitials) {
    }

    private void populateSampleData() {
        if (historyList != null) {
            historyList.getItems().clear();
        }
        if (leaderboardList != null) {
            leaderboardList.getItems().clear();
        }
        if (highestScorerLabel != null) {
            highestScorerLabel.setText("No data available");
        }
        if (progressChart != null) {
            progressChart.getData().clear();
        }
        if (progressLineChart != null) {
            progressLineChart.getData().clear();
            progressLineChart.setLegendVisible(false);
            progressLineChart.setHorizontalGridLinesVisible(false);
            progressLineChart.setVerticalGridLinesVisible(false);
        }
        if (avgScoreLabel != null) {
            avgScoreLabel.setText(NO_DATA_TEXT);
        }
        if (avgTimeLabel != null) {
            avgTimeLabel.setText(NO_DATA_TEXT);
        }
        if (strongestSubjectLabel != null) {
            strongestSubjectLabel.setText(NO_DATA_TEXT);
        }
        if (strongestSubjectMessageLabel != null) {
            strongestSubjectMessageLabel.setText(STRONGEST_SUBJECT_MESSAGE);
        }
        if (improvementAreaLabel != null) {
            improvementAreaLabel.setText(NO_DATA_TEXT);
        }
        if (improvementAreaMessageLabel != null) {
            improvementAreaMessageLabel.setText(IMPROVEMENT_AREA_MESSAGE);
        }
        updateMessagingRequestBadge(-1);
        applyDashboardNoDataState();
        if (dashboardUpcomingCountLabel != null) {
            dashboardUpcomingCountLabel.setText("No data available");
        }
        if (dashboardCompletedCountLabel != null) {
            dashboardCompletedCountLabel.setText("No data available");
        }
        if (dashboardRankLabel != null) {
            dashboardRankLabel.setText("No data available");
        }
        if (teacherRoleChoice != null) {
            teacherRoleChoice.getItems().setAll("Teacher");
            teacherRoleChoice.getSelectionModel().selectFirst();
        }
        refreshViewExamsList();
    }

    private void refreshQuestionBankList() {
        loadQuestionBankRows();
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
        refreshStudentIdentityDisplay();
        refreshDashboardUpcomingCard();
        refreshDashboardAlerts();
        hideAllPanes();
        setPaneVisible(studentDashboardPane, true);
    }

    @FXML
    private void showAvailableExams() {
        loadExamCatalog();
        hideAllPanes();
        setPaneVisible(availableExamsPane, true);
    }

    @FXML
    private void showScheduledExams() {
        setStudentSemesterChoice(studentScheduledTermChoice, activeUserSemester);
        loadStudentScheduledCoursesForTerm(studentScheduledTermChoice != null ? studentScheduledTermChoice.getValue() : activeUserSemester);
        refreshStudentScheduledExamList();
        updateStudentScheduledControls();
        hideAllPanes();
        setPaneVisible(scheduledExamsPane, true);
    }

    @FXML
    private void showExamHistory() {
        hideAllPanes();
        setPaneVisible(progressPane, true);
        Platform.runLater(this::loadExamHistory);
    }

    @FXML
    private void showDiscussion() {
        hideAllPanes();
        setPaneVisible(discussionPane, true);
        refreshDiscussion();
    }

    @FXML
    private void showMessaging() {
        hideAllPanes();
        setPaneVisible(messagingPane, true);
        refreshMessaging();
        startMessagingRefresh();
    }

    @FXML
    private void showProgress() {
        showExamHistory();
    }

    @FXML
    private void openLeaderboardView() {
        try {
            FXMLLoader loader = new FXMLLoader(ExamApplication.class.getResource("Leaderboard.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(ExamApplication.class.getResource("exam.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("Leaderboard");
            if (studentDashboardPane != null && studentDashboardPane.getScene() != null) {
                stage.initOwner(studentDashboardPane.getScene().getWindow());
            }
            stage.setMinWidth(850);
            stage.setMinHeight(560);
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            showError("Leaderboard", "Unable to open leaderboard.");
        }
    }

    @FXML
    private void showLeaderboard() {
        setStudentSemesterChoice(leaderboardTermChoice, activeUserSemester);
        loadLeaderboardCoursesForTerm(leaderboardTermChoice != null ? leaderboardTermChoice.getValue() : activeUserSemester);
        refreshLeaderboardView();
        hideAllPanes();
        setPaneVisible(leaderboardPane, true);
    }

    @FXML
    private void showSettings() {
        hideAllPanes();
        setPaneVisible(studentDashboardPane, true);
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
            teacherWelcomeLabel.setText("Welcome, Instructor");
        }
        refreshTeacherDashboardMetrics();
        showPane(teacherDashboardPane);
    }

    @FXML
    private void showTeacherDashboard() {
        refreshTeacherDashboardMetrics();
        showPane(teacherDashboardPane);
    }

    private void refreshTeacherDashboardMetrics() {
        List<TeacherExamSummary> exams = loadCreatedExams();
        if (teacherActiveExamsBadgeLabel != null) {
            teacherActiveExamsBadgeLabel.setText(exams.isEmpty() ? "No data available" : exams.size() + " ACTIVE");
        }

        List<ResultRow> results = loadResults();
        if (results.isEmpty()) {
            if (teacherActiveStudentsLabel != null) {
                teacherActiveStudentsLabel.setText("No data available");
            }
            if (teacherAverageGradeLabel != null) {
                teacherAverageGradeLabel.setText("No data available");
            }
            if (teacherActiveStudentsProgressBar != null) {
                teacherActiveStudentsProgressBar.setProgress(-1);
            }
            return;
        }

        Set<String> users = new HashSet<>();
        double sum = 0.0;
        int count = 0;
        for (ResultRow row : results) {
            if (row.username != null && !row.username.isBlank()) {
                users.add(row.username.trim().toLowerCase());
            }
            sum += row.percentage;
            count++;
        }

        if (teacherActiveStudentsLabel != null) {
            teacherActiveStudentsLabel.setText(users.isEmpty() ? "No data available" : String.valueOf(users.size()));
        }
        if (teacherAverageGradeLabel != null) {
            if (count == 0) {
                teacherAverageGradeLabel.setText("No data available");
            } else {
                teacherAverageGradeLabel.setText(toGrade(sum / count));
            }
        }
        if (teacherActiveStudentsProgressBar != null) {
            if (users.isEmpty()) {
                teacherActiveStudentsProgressBar.setProgress(-1);
            } else {
                long masteryUsers = users.stream().filter(user -> {
                    double total = 0.0;
                    int attempts = 0;
                    for (ResultRow row : results) {
                        if (row.username != null && row.username.trim().equalsIgnoreCase(user)) {
                            total += row.percentage;
                            attempts++;
                        }
                    }
                    return attempts > 0 && (total / attempts) >= 40.0;
                }).count();
                teacherActiveStudentsProgressBar.setProgress(masteryUsers / (double) users.size());
            }
        }
    }

    private String toGrade(double averagePercentage) {
        if (averagePercentage >= 80.0) {
            return "A";
        }
        if (averagePercentage >= 70.0) {
            return "B";
        }
        if (averagePercentage >= 60.0) {
            return "C";
        }
        if (averagePercentage >= 50.0) {
            return "D";
        }
        return "F";
    }

    @FXML
    private void showQuestionBank() {
        loadTeacherTerms();
        refreshQuestionBankList();
        showPane(questionBankPane);
    }

    @FXML
    private void showCreateExam() {
        if (createExamViewController != null) {
            createExamViewController.refreshFromData();
        } else {
            loadTeacherTerms();
            loadCreateExamQuestionPool();
            updateCreateExamModeView();
            updateCreateExamComputedFields();
        }
        showPane(createExamPane);
    }

    @FXML
    private void showScheduleExam() {
        if (scheduleExamViewController != null) {
            scheduleExamViewController.refreshFromData();
        } else {
            loadTeacherTerms();
            refreshScheduleExamChoices();
        }
        showPane(scheduleExamPane);
    }

    @FXML
    private void showViewExams() {
        if (examLibraryViewController != null) {
            examLibraryViewController.refreshFromData();
        } else {
            refreshViewExamsList();
            updateViewExamControls();
        }
        showPane(viewExamsPane);
    }

    @FXML
    private void showTeacherAnalytics() {
        if (resultsAnalyticsViewController != null) {
            resultsAnalyticsViewController.refreshFromData();
        } else {
            loadTeacherTerms();
            updateTeacherAnalyticsMetrics();
        }
        showPane(teacherAnalyticsPane);
    }

    @FXML
    private void registerUser() {
        String username = registerUsername.getText();
        String semester = registerSemesterChoice != null ? registerSemesterChoice.getValue() : null;
        String password = registerPassword.getText();
        String confirm = registerConfirm.getText();
        setRegisterMessage("");

        String validation = validateRegistration(username, semester, password, confirm);
        if (validation != null) {
            setRegisterMessage(validation);
            return;
        }
        setRegisterMessage("Registering...");
        runNetwork(() -> userService.register(username, password, semester), created -> {
            if (!created) {
                setRegisterMessage("Username already exists or semester is invalid.");
                return;
            }
            registerUsername.clear();
            if (registerSemesterChoice != null) {
                registerSemesterChoice.getSelectionModel().clearSelection();
            }
            registerPassword.clear();
            registerConfirm.clear();
            setRegisterMessage("");
            showPane(loginPane);
        }, error -> setRegisterMessage("Server error: " + error));
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
        runNetwork(() -> userService.authenticate(username, password), profile -> {
            if (profile == null) {
                loginMessageLabel.setText("Invalid username or password.");
                return;
            }
            activeUser = profile.username();
            activeUserSemester = normalizeSemester(profile.semester());
            if (activeUserSemester.isBlank()) {
                loginMessageLabel.setText("No semester is assigned to this account.");
                return;
            }
            UserSession.setUsername(activeUser);
            UserSession.setSemester(activeUserSemester);
            resetDashboardAlertState();
            startDashboardAlertRefresh();
            refreshStudentIdentityDisplay();
            showStudentDashboard();
        }, error -> loginMessageLabel.setText("Server error: " + error));
    }

    @FXML
    private void logout() {
        stopTimer();
        activeUser = null;
        activeUserSemester = null;
        UserSession.clear();
        selectedTerm = null;
        selectedCourseCode = null;
        selectedCourseLabel = null;
        activeAttemptType = "practice";
        activeAttemptExamId = "NA";
        loginPassword.clear();
        clearMessagingLists();
        if (discussionList != null) {
            discussionList.getItems().clear();
        }
        stopDashboardAlertRefresh();
        resetDashboardAlertState();
        refreshStudentIdentityDisplay();
        showPane(loginPane);
    }

    @FXML
    private void handleLogout() {
        logout();
    }

    @FXML
    private void selectCourseExam(ActionEvent event) {
        activeAttemptType = "practice";
        activeAttemptExamId = "NA";

        String term = termChoice != null ? termChoice.getValue() : null;
        String courseLabelFromChoice = courseChoice != null ? courseChoice.getValue() : null;
        String courseCodeFromButton = null;

        if (event != null && event.getSource() instanceof Button clickedButton) {
            Object payload = clickedButton.getUserData();
            if (payload != null) {
                String encoded = payload.toString().trim();
                String[] parts = encoded.split("\\|", 2);
                if (parts.length == 2) {
                    if (!parts[0].isBlank()) {
                        term = parts[0].trim();
                    }
                    if (!parts[1].isBlank()) {
                        courseCodeFromButton = canonicalCourseCode(parts[1]);
                    }
                }
            }
        }

        if (term == null || term.isBlank()) {
            levelMessageLabel.setText("Select a term to continue.");
            return;
        }
        if (!isActiveStudentSemester(term)) {
            levelMessageLabel.setText("You can only access courses from semester " + activeUserSemester + ".");
            return;
        }

        if (courseCodeByLabel.isEmpty() || !term.equals(selectedTerm)) {
            onTermChanged(term);
        }

        if (termChoice != null && termChoice.getItems().contains(term)) {
            termChoice.getSelectionModel().select(term);
        }

        String resolvedCourseCode = courseCodeFromButton;
        if ((resolvedCourseCode == null || resolvedCourseCode.isBlank()) && courseLabelFromChoice != null) {
            resolvedCourseCode = canonicalCourseCode(courseCodeByLabel.get(courseLabelFromChoice));
        }
        if (resolvedCourseCode == null || resolvedCourseCode.isBlank()) {
            levelMessageLabel.setText("Select a course to continue.");
            return;
        }

        selectedTerm = term;
        selectedCourseCode = resolvedCourseCode;
        selectedCourseLabel = resolveSelectedCourseLabel(term, selectedCourseCode, courseLabelFromChoice);

        if (courseChoice != null && selectedCourseLabel != null && courseChoice.getItems().contains(selectedCourseLabel)) {
            courseChoice.getSelectionModel().select(selectedCourseLabel);
        }

        if (selectedCourseCode == null || selectedCourseCode.isBlank()) {
            levelMessageLabel.setText("Invalid course selection.");
            return;
        }

        System.out.println("Selected Course: " + selectedCourseCode);
        levelMessageLabel.setText("");
        levelLabel.setText("Course: " + selectedCourseLabel);
        subtitleLabel.setText("[" + selectedCourseLabel + "]");
        showPane(startPane);
    }

    @FXML
    private void startExam() {
        if (selectedTerm == null || selectedCourseCode == null) {
            levelMessageLabel.setText("Select term and course to continue.");
            showPane(levelPane);
            return;
        }
        if (!isActiveStudentSemester(selectedTerm)) {
            levelMessageLabel.setText("You can only start exams from semester " + activeUserSemester + ".");
            showPane(levelPane);
            return;
        }
        System.out.println("Selected Course: " + selectedCourseCode);
        List<Question> pool;
        try {
            pool = QuestionBank.loadQuestions(selectedTerm, selectedCourseCode);
        } catch (IOException ex) {
            showError("Unable to load questions.", ex.getMessage());
            return;
        }
        if (pool.isEmpty()) {
            showError("Not enough questions.", "No questions found for " + selectedCourseLabel + ".");
            return;
        }
        int requestedCount = examQuestionCountChoice != null && examQuestionCountChoice.getValue() != null
            ? examQuestionCountChoice.getValue()
            : DEFAULT_EXAM_QUESTION_COUNT;
        int selectedCount = Math.min(requestedCount, pool.size());
        questions = pickRandomQuestions(pool, selectedCount);
        answers = new int[questions.size()];
        for (int i = 0; i < answers.length; i++) {
            answers[i] = -1;
        }
        currentIndex = 0;
        allocatedExamSeconds = questions.size() * SECONDS_PER_QUESTION;
        secondsRemaining = allocatedExamSeconds;
        if (examAutosaveLabel != null) {
            examAutosaveLabel.setText("Auto-saving response...");
        }
        if (examCompletionLabel != null) {
            examCompletionLabel.setText("0% Complete");
        }
        showPane(examPane);
        startTimer();
        updateQuestionView();
    }

    private List<Question> pickRandomQuestions(List<Question> source, int count) {
        List<Question> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled);
        return new ArrayList<>(shuffled.subList(0, Math.min(count, shuffled.size())));
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
        boolean confirmed = showStyledConfirmation(
                "Submit Exam",
                "Submit Exam",
                "Are you sure you want to submit your answers?"
        );
        if (confirmed) {
            finishExam(false);
        }
    }

    private boolean showStyledConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title == null ? "Confirmation" : title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setGraphic(null);
        alert.getDialogPane().getStyleClass().add("app-confirm-dialog");

        javafx.stage.Window owner = resolveDialogOwner();
        if (owner != null) {
            alert.initOwner(owner);
        }

        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void showStyledInformation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title == null ? "Information" : title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setGraphic(null);
        alert.getDialogPane().getStyleClass().add("app-info-dialog");

        javafx.stage.Window owner = resolveDialogOwner();
        if (owner != null) {
            alert.initOwner(owner);
        }

        alert.showAndWait();
    }

    private javafx.stage.Window resolveDialogOwner() {
        if (examPane != null && examPane.getScene() != null) {
            return examPane.getScene().getWindow();
        }
        if (studentDashboardPane != null && studentDashboardPane.getScene() != null) {
            return studentDashboardPane.getScene().getWindow();
        }
        if (teacherDashboardPane != null && teacherDashboardPane.getScene() != null) {
            return teacherDashboardPane.getScene().getWindow();
        }
        if (questionBankPane != null && questionBankPane.getScene() != null) {
            return questionBankPane.getScene().getWindow();
        }
        return null;
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
        List<ResultAttemptDetail> detailRows = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            int answer = answers[i];
            boolean correct = answer == question.getCorrectIndex();
            if (correct) {
                correctCount++;
            }
            detailRows.add(new ResultAttemptDetail(
                    question.getId(),
                    answer >= 0 ? optionLetter(answer) : "None",
                    optionLetter(question.getCorrectIndex())
            ));
        }
        double percent = questions.isEmpty() ? 0 : (correctCount / (double) questions.size());
        int timeTakenSeconds = Math.max(0, allocatedExamSeconds - Math.max(secondsRemaining, 0));
        String attemptDate = saveExamHistory(correctCount, percent, timeTakenSeconds, detailRows);
        if (examResultViewController != null) {
            examResultViewController.loadResultForAttempt(activeUser, activeAttemptExamId, attemptDate);
        }
        showPane(resultPane);
    }

    private void updateQuestionView() {
        Question question = questions.get(currentIndex);
        questionLabel.setText(question.getText());
        optionA.setText("A   " + question.getOptions().get(0));
        optionB.setText("B   " + question.getOptions().get(1));
        optionC.setText("C   " + question.getOptions().get(2));
        optionD.setText("D   " + question.getOptions().get(3));
        if (examTopicLabel != null) {
            examTopicLabel.setText("Topic: " + selectedCourseCode);
        }

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
        if (examCompletionLabel != null) {
            int completion = (int) Math.round(((currentIndex + 1) * 100.0) / questions.size());
            examCompletionLabel.setText(completion + "% Complete");
        }
        prevButton.setDisable(currentIndex == 0);
        nextButton.setDisable(currentIndex == questions.size() - 1);
    }

    private void saveAnswer() {
        Toggle toggle = optionsGroup.getSelectedToggle();
        if (toggle != null) {
            answers[currentIndex] = (int) toggle.getUserData();
            if (examAutosaveLabel != null) {
                examAutosaveLabel.setText("Auto-saving response... saved");
            }
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
        hideAllPanes();
        setPaneVisible(pane, true);
    }

    private void hideAllPanes() {
        stopMessagingRefresh();
        setPaneVisible(loginPane, false);
        setPaneVisible(registerPane, false);
        setPaneVisible(studentDashboardPane, false);
        setPaneVisible(availableExamsPane, false);
        setPaneVisible(scheduledExamsPane, false);
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
    }

    private void setPaneVisible(Node pane, boolean visible) {
        if (pane == null) {
            return;
        }
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
        setMessagingFeedback("");
    }

    private void refreshMessaging() {
        if (!ensureLoggedIn()) {
            clearMessagingLists();
            updateMessagingRequestBadge(-1);
            return;
        }
        if (pendingRequestsList != null) {
            runNetwork(() -> messagingService.fetchFriendRequests(activeUser), requests -> {
                pendingRequestsList.getItems().setAll(requests);
                updateMessagingRequestBadge(requests.size());
            }, error -> showError("Unable to load requests", error));
        }
        if (friendsList != null) {
            String currentSelection = friendsList.getSelectionModel().getSelectedItem();
            runNetwork(() -> messagingService.fetchFriends(activeUser), friends -> {
                friendsList.getItems().setAll(friends);
                if (friends.isEmpty()) {
                    updateMessagingHeader("Select a friend", "Active");
                    if (chatHistoryList != null) {
                        chatHistoryList.getItems().clear();
                    }
                    return;
                }
                if (currentSelection != null && friends.contains(currentSelection)) {
                    friendsList.getSelectionModel().select(currentSelection);
                    refreshActiveChat();
                } else {
                    friendsList.getSelectionModel().selectFirst();
                }
            }, error -> showError("Unable to load friends", error));
        }
    }

    private void refreshActiveChat() {
        if (!ensureLoggedIn() || friendsList == null) {
            return;
        }
        String friend = friendsList.getSelectionModel().getSelectedItem();
        if (friend == null || friend.isBlank()) {
            return;
        }
        loadChatHistory(friend);
    }

    private void loadChatHistory(String friend) {
        if (!ensureLoggedIn() || chatHistoryList == null) {
            return;
        }
        runNetwork(() -> messagingService.fetchChat(activeUser, friend), messages -> {
            chatHistoryList.getItems().setAll(messages);
            if (!messages.isEmpty()) {
                chatHistoryList.scrollTo(messages.size() - 1);
            }
            markFriendMessagesAsSeen(friend, messages);
            updateMessagingHeader(friend, "Active");
        }, error -> showError("Unable to load chat", error));
    }

    private void startMessagingRefresh() {
        if (messagingRefreshTimeline == null) {
            messagingRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                if (messagingPane != null && messagingPane.isVisible()) {
                    refreshActiveChat();
                }
            }));
            messagingRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        }
        messagingRefreshTimeline.playFromStart();
    }

    private void stopMessagingRefresh() {
        if (messagingRefreshTimeline != null) {
            messagingRefreshTimeline.stop();
        }
    }

    private void startDashboardAlertRefresh() {
        if (dashboardAlertRefreshTimeline == null) {
            dashboardAlertRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> refreshDashboardAlerts()));
            dashboardAlertRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        }
        refreshDashboardAlerts();
        dashboardAlertRefreshTimeline.playFromStart();
    }

    private void stopDashboardAlertRefresh() {
        if (dashboardAlertRefreshTimeline != null) {
            dashboardAlertRefreshTimeline.stop();
        }
        dashboardAlertRefreshInFlight = false;
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

    private String validateRegistration(String username, String semester, String password, String confirm) {
        if (username == null || username.isBlank()) {
            return "Username is required.";
        }
        if (username.contains(",")) {
            return "Username cannot contain commas.";
        }
        if (normalizeSemester(semester).isBlank()) {
            return "Semester selection is required.";
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
        setRegisterMessage("");
    }

    private void setRegisterMessage(String message) {
        if (registerMessageLabel != null) {
            registerMessageLabel.setText(message == null ? "" : message);
        }
        boolean hasMessage = message != null && !message.isBlank();
        if (registerErrorBanner != null) {
            registerErrorBanner.setVisible(hasMessage);
            registerErrorBanner.setManaged(hasMessage);
        }
    }

    private void setMessagingFeedback(String message) {
        if (messagingFeedbackLabel == null) {
            return;
        }
        String safe = message == null ? "" : message.trim();
        boolean hasMessage = !safe.isBlank();
        messagingFeedbackLabel.setText(safe);
        messagingFeedbackLabel.setVisible(hasMessage);
        messagingFeedbackLabel.setManaged(hasMessage);
    }

    private void refreshDashboardAlerts() {
        if (dashboardAlertsLabel == null) {
            return;
        }
        if (!ensureLoggedIn()) {
            updateDashboardAlertsLabel(0, List.of());
            return;
        }
        if (dashboardAlertRefreshInFlight) {
            return;
        }

        Map<String, Long> seenSnapshot = new LinkedHashMap<>(latestSeenIncomingMessageByFriend);
        boolean initializeSnapshot = !chatAlertSnapshotInitialized;
        dashboardAlertRefreshInFlight = true;
        runNetwork(() -> buildChatAlertSummary(seenSnapshot, initializeSnapshot), summary -> {
            dashboardAlertRefreshInFlight = false;
            latestSeenIncomingMessageByFriend.putAll(summary.seenTimestampUpdates());
            chatAlertSnapshotInitialized = true;
            updateDashboardAlertsLabel(summary.unreadCount(), summary.sendingFriends());
        }, error -> {
            dashboardAlertRefreshInFlight = false;
            updateDashboardAlertsLabel(0, List.of());
        });
    }

    private ChatAlertSummary buildChatAlertSummary(Map<String, Long> seenSnapshot, boolean initializeSnapshot) throws IOException {
        List<String> friends = messagingService.fetchFriends(activeUser);
        Map<String, Long> seenUpdates = new LinkedHashMap<>();
        List<String> sendingFriends = new ArrayList<>();
        int unreadCount = 0;

        for (String friend : friends) {
            if (friend == null || friend.isBlank()) {
                continue;
            }
            List<String> messages = messagingService.fetchChat(activeUser, friend);
            long latestIncomingTimestamp = latestIncomingChatTimestamp(messages);
            if (initializeSnapshot || !seenSnapshot.containsKey(friend)) {
                seenUpdates.put(friend, latestIncomingTimestamp);
                continue;
            }

            long seenTimestamp = Math.max(0L, seenSnapshot.getOrDefault(friend, 0L));
            int friendUnreadCount = countUnreadIncomingMessages(messages, seenTimestamp);
            if (friendUnreadCount > 0) {
                unreadCount += friendUnreadCount;
                sendingFriends.add(friend);
            }
        }
        return new ChatAlertSummary(unreadCount, sendingFriends, seenUpdates);
    }

    private int countUnreadIncomingMessages(List<String> messages, long seenTimestamp) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int unread = 0;
        for (String raw : messages) {
            String sender = extractChatSender(raw);
            if (sender.isBlank() || (activeUser != null && activeUser.equalsIgnoreCase(sender))) {
                continue;
            }
            long timestamp = extractChatTimestamp(raw);
            if (timestamp > seenTimestamp) {
                unread++;
            }
        }
        return unread;
    }

    private long latestIncomingChatTimestamp(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0L;
        }
        long latest = 0L;
        for (String raw : messages) {
            String sender = extractChatSender(raw);
            if (sender.isBlank() || (activeUser != null && activeUser.equalsIgnoreCase(sender))) {
                continue;
            }
            latest = Math.max(latest, extractChatTimestamp(raw));
        }
        return latest;
    }

    private void markFriendMessagesAsSeen(String friend, List<String> messages) {
        if (friend == null || friend.isBlank()) {
            return;
        }
        latestSeenIncomingMessageByFriend.put(friend, latestIncomingChatTimestamp(messages));
        chatAlertSnapshotInitialized = true;
    }

    private String extractChatSender(String raw) {
        String value = raw == null ? "" : raw;
        String[] metadataParts = value.split(CHAT_METADATA_SEPARATOR, 3);
        if (metadataParts.length == 3) {
            return metadataParts[0].trim();
        }
        int split = value.indexOf(": ");
        return split >= 0 ? value.substring(0, split).trim() : "";
    }

    private long extractChatTimestamp(String raw) {
        String value = raw == null ? "" : raw;
        String[] metadataParts = value.split(CHAT_METADATA_SEPARATOR, 3);
        if (metadataParts.length == 3) {
            try {
                return Long.parseLong(metadataParts[1].trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private void updateDashboardAlertsLabel(int unreadCount, List<String> sendingFriends) {
        if (dashboardAlertsLabel == null) {
            return;
        }

        String text;
        String tooltipText;
        if (unreadCount <= 0) {
            text = "Alerts Clear";
            tooltipText = "No new chat messages";
            dashboardAlertsLabel.getStyleClass().remove("dash-header-pill-alert");
        } else if (unreadCount == 1 && sendingFriends != null && sendingFriends.size() == 1) {
            text = sendingFriends.get(0) + " messaged you";
            tooltipText = "1 unread message from " + sendingFriends.get(0);
            if (!dashboardAlertsLabel.getStyleClass().contains("dash-header-pill-alert")) {
                dashboardAlertsLabel.getStyleClass().add("dash-header-pill-alert");
            }
        } else {
            text = unreadCount + " New Messages";
            tooltipText = sendingFriends == null || sendingFriends.isEmpty()
                    ? unreadCount + " unread chat messages"
                    : "New messages from " + String.join(", ", sendingFriends);
            if (!dashboardAlertsLabel.getStyleClass().contains("dash-header-pill-alert")) {
                dashboardAlertsLabel.getStyleClass().add("dash-header-pill-alert");
            }
        }

        dashboardAlertsLabel.setText(text);
        dashboardAlertsLabel.setTooltip(new Tooltip(tooltipText));
    }

    private void resetDashboardAlertState() {
        latestSeenIncomingMessageByFriend.clear();
        chatAlertSnapshotInitialized = false;
        updateDashboardAlertsLabel(0, List.of());
    }

    @FXML
    private void goToLevelSelect() {
        activeAttemptType = "practice";
        activeAttemptExamId = "NA";
        if (activeUser != null) {
            levelSubtitleLabel.setText("Welcome " + activeUser + ". Choose term and course.");
        } else {
            levelSubtitleLabel.setText("Choose term and course.");
        }
        loadExamCatalog();
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
    private void handleDiscussionSubmitKey(KeyEvent event) {
        if (event == null || event.getCode() != KeyCode.ENTER) {
            return;
        }
        if (event.isShiftDown()) {
            return;
        }
        event.consume();
        postDiscussion();
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
                    String normalizedTarget = target.trim();
                    friendRequestField.clear();
                    refreshMessaging();
                    if (status == FriendStore.FriendRequestStatus.SENT) {
                        setMessagingFeedback("Friend request sent to " + normalizedTarget + ".");
                    } else {
                        setMessagingFeedback(normalizedTarget + " accepted the request. You are now friends.");
                    }
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
            setMessagingFeedback("Friend request accepted from " + from + ".");
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
            setMessagingFeedback("Friend request declined from " + from + ".");
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
        String trimmedText = text.trim();
        runNetwork(() -> {
            messagingService.sendChat(activeUser, friend, trimmedText);
            return true;
        }, ignored -> {
            chatMessageField.clear();
            loadChatHistory(friend);
        }, error -> showError("Unable to send message", error));
    }

    @FXML
    private void addQuestion() {
        String term = qbTermChoice != null ? qbTermChoice.getValue() : null;
        String courseLabel = qbCourseChoice != null ? qbCourseChoice.getValue() : null;
        String courseCode = courseLabel == null ? null : qbCourseCodeByLabel.get(courseLabel);
        if (term == null || term.isBlank() || courseCode == null || courseCode.isBlank()) {
            setQbStatus("Select term and course first.");
            return;
        }
        TeacherQuestionDraft draft = showQuestionDialog("Add Question", null);
        if (draft != null) {
            try {
                String courseName = extractCourseName(courseLabel);
                String questionId = nextQuestionId(term, courseCode);
                TeacherQuestionRow row = TeacherQuestionRow.forAppend(term, courseCode, courseName, questionId,
                        draft.question, draft.optionA, draft.optionB, draft.optionC, draft.optionD, draft.correctIndex);
                appendQuestionToCsv(row);
                refreshQuestionBankList();
                setQbStatus("Question added for " + courseCode + ".");
            } catch (IOException ex) {
                setQbStatus("Unable to save question.");
            }
        }
    }

    @FXML
    private void editQuestion() {
        if (questionBankList == null || qbCurrentRows.isEmpty()) {
            return;
        }
        int index = questionBankList.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            setQbStatus("Choose a question from the list to edit.");
            return;
        }
        TeacherQuestionRow current = qbCurrentRows.get(index);
        TeacherQuestionDraft updated = showQuestionDialog("Edit Question", current);
        if (updated != null) {
            TeacherQuestionRow replacement = current.withUpdatedContent(
                    updated.question,
                    updated.optionA,
                    updated.optionB,
                    updated.optionC,
                    updated.optionD,
                    updated.correctIndex
            );
            try {
                updateQuestionInCsv(current, replacement);
                refreshQuestionBankList();
                setQbStatus("Question updated.");
            } catch (IOException ex) {
                setQbStatus("Unable to update question.");
            }
        }
    }

    @FXML
    private void deleteQuestion() {
        if (questionBankList == null || qbCurrentRows.isEmpty()) {
            return;
        }
        int index = questionBankList.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            setQbStatus("Choose a question from the list to delete.");
            return;
        }
        TeacherQuestionRow selected = qbCurrentRows.get(index);
        boolean confirmed = showStyledConfirmation(
                "Delete Question",
                "Delete Question",
                "Are you sure you want to delete the selected question?"
        );
        if (confirmed) {
            try {
                deleteQuestionFromCsv(selected);
                refreshQuestionBankList();
                setQbStatus("Question deleted.");
            } catch (IOException ex) {
                setQbStatus("Unable to delete question.");
            }
        }
    }

    @FXML
    private void createExamFromQuestionBank() {
        String term = createTermChoice != null ? createTermChoice.getValue() : null;
        String courseLabel = createCourseChoice != null ? createCourseChoice.getValue() : null;
        String courseCode = courseLabel == null ? null : createCourseCodeByLabel.get(courseLabel);
        if (term == null || term.isBlank() || courseCode == null || courseCode.isBlank()) {
            setCreateExamStatus("Select term and course.");
            return;
        }
        if (createCourseQuestions.isEmpty()) {
            setCreateExamStatus("No questions found for selected course.");
            return;
        }

        boolean manualMode = isManualModeSelected();
        List<Question> selectedQuestions;
        if (manualMode) {
            List<String> selectedItems = createQuestionList == null
                    ? List.of()
                    : new ArrayList<>(createQuestionList.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) {
                setCreateExamStatus("Manual mode requires at least one selected question.");
                return;
            }
            selectedQuestions = new ArrayList<>();
            for (String selectedItem : selectedItems) {
                int idx = createQuestionList.getItems().indexOf(selectedItem);
                if (idx >= 0 && idx < createCourseQuestions.size()) {
                    selectedQuestions.add(createCourseQuestions.get(idx));
                }
            }
        } else {
            Integer requested = createNumQuestions != null ? createNumQuestions.getValue() : null;
            int count = requested == null ? 0 : requested;
            if (count <= 0) {
                setCreateExamStatus("Automatic mode requires a positive question count.");
                return;
            }
            if (count > createCourseQuestions.size()) {
                setCreateExamStatus("Automatic count exceeds available questions (" + createCourseQuestions.size() + ").");
                return;
            }
            selectedQuestions = pickRandomQuestions(createCourseQuestions, count);
        }

        if (selectedQuestions.isEmpty()) {
            setCreateExamStatus("Teacher cannot create exam with 0 questions.");
            return;
        }

        int totalQuestions = selectedQuestions.size();
        int timeLimitSeconds = totalQuestions * SECONDS_PER_QUESTION;
        int marks = totalQuestions;
        TeacherExamDefinition examDefinition = new TeacherExamDefinition(
                term,
                courseCode,
                extractCourseName(courseLabel),
                selectedQuestions,
                totalQuestions,
                timeLimitSeconds,
                marks,
                manualMode ? "MANUAL" : "AUTO"
        );

        try {
            appendExamDefinition(examDefinition);
            refreshViewExamsList();
            loadExamCatalog();
            setCreateExamStatus("Exam created: " + examDefinition.courseCode + " | " + examDefinition.totalQuestions + " questions.");
        } catch (IOException ex) {
            setCreateExamStatus("Unable to save exam.");
        }
    }

    private TeacherQuestionDraft showQuestionDialog(String title, TeacherQuestionRow existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField questionField = new TextField(existing != null ? existing.question : "");
        TextField optionAField = new TextField(existing != null ? existing.optionA : "");
        TextField optionBField = new TextField(existing != null ? existing.optionB : "");
        TextField optionCField = new TextField(existing != null ? existing.optionC : "");
        TextField optionDField = new TextField(existing != null ? existing.optionD : "");
        ChoiceBox<String> correctChoice = new ChoiceBox<>();
        correctChoice.getItems().setAll("A", "B", "C", "D");
        if (existing != null) {
            correctChoice.getSelectionModel().select(indexToOption(existing.correctIndex));
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
        return new TeacherQuestionDraft(questionText, a, b, c, d, optionToIndex(correct));
    }

    private void loadTeacherTerms() {
        try {
            List<String> terms = QuestionBank.loadTerms();
            if (qbTermChoice != null) {
                String previous = qbTermChoice.getValue();
                qbTermChoice.getItems().setAll(terms);
                if (previous != null && terms.contains(previous)) {
                    qbTermChoice.getSelectionModel().select(previous);
                } else if (!terms.isEmpty()) {
                    qbTermChoice.getSelectionModel().selectFirst();
                }
            }
            if (createTermChoice != null) {
                String previous = createTermChoice.getValue();
                createTermChoice.getItems().setAll(terms);
                if (previous != null && terms.contains(previous)) {
                    createTermChoice.getSelectionModel().select(previous);
                } else if (!terms.isEmpty()) {
                    createTermChoice.getSelectionModel().selectFirst();
                }
            }
            if (scheduleTermChoice != null) {
                String previous = scheduleTermChoice.getValue();
                scheduleTermChoice.getItems().setAll(terms);
                if (previous != null && terms.contains(previous)) {
                    scheduleTermChoice.getSelectionModel().select(previous);
                } else if (!terms.isEmpty()) {
                    scheduleTermChoice.getSelectionModel().selectFirst();
                }
            }
            if (studentScheduledTermChoice != null) {
                String previous = studentScheduledTermChoice.getValue();
                studentScheduledTermChoice.getItems().setAll(terms);
                if (previous != null && terms.contains(previous)) {
                    studentScheduledTermChoice.getSelectionModel().select(previous);
                } else if (!terms.isEmpty()) {
                    studentScheduledTermChoice.getSelectionModel().selectFirst();
                }
            }
            if (analyticsTermChoice != null) {
                String previous = analyticsTermChoice.getValue();
                analyticsTermChoice.getItems().setAll(terms);
                if (previous != null && terms.contains(previous)) {
                    analyticsTermChoice.getSelectionModel().select(previous);
                } else if (!terms.isEmpty()) {
                    analyticsTermChoice.getSelectionModel().selectFirst();
                }
            }
            if (leaderboardTermChoice != null) {
                String previous = leaderboardTermChoice.getValue();
                leaderboardTermChoice.getItems().setAll(terms);
                if (previous != null && terms.contains(previous)) {
                    leaderboardTermChoice.getSelectionModel().select(previous);
                } else if (!terms.isEmpty()) {
                    leaderboardTermChoice.getSelectionModel().selectFirst();
                }
            }
            loadQbCoursesForTerm(qbTermChoice != null ? qbTermChoice.getValue() : null);
            loadCreateExamCoursesForTerm(createTermChoice != null ? createTermChoice.getValue() : null);
            loadScheduleCoursesForTerm(scheduleTermChoice != null ? scheduleTermChoice.getValue() : null);
            loadStudentScheduledCoursesForTerm(studentScheduledTermChoice != null ? studentScheduledTermChoice.getValue() : null);
            loadAnalyticsCoursesForTerm(analyticsTermChoice != null ? analyticsTermChoice.getValue() : null);
            loadLeaderboardCoursesForTerm(leaderboardTermChoice != null ? leaderboardTermChoice.getValue() : null);
        } catch (IOException ex) {
            setQbStatus("Unable to load terms.");
            setCreateExamStatus("Unable to load terms.");
            setScheduleStatus("Unable to load terms.");
        }
    }

    private void loadAnalyticsCoursesForTerm(String term) {
        analyticsCourseCodeByLabel.clear();
        if (analyticsCourseChoice == null) {
            return;
        }
        analyticsCourseChoice.getItems().clear();
        if (term == null || term.isBlank()) {
            return;
        }
        try {
            List<QuestionBank.CourseInfo> courses = QuestionBank.loadCoursesForTerm(term);
            List<String> labels = new ArrayList<>();
            for (QuestionBank.CourseInfo course : courses) {
                String label = course.getDisplayLabel();
                labels.add(label);
                analyticsCourseCodeByLabel.put(label, course.getCourseCode());
            }
            analyticsCourseChoice.getItems().setAll(labels);
            if (!labels.isEmpty()) {
                analyticsCourseChoice.getSelectionModel().selectFirst();
            }
        } catch (IOException ex) {
            if (teacherAnalyticsPassLabel != null) {
                teacherAnalyticsPassLabel.setText("0% (No student attempts yet)");
            }
        }
    }

    private void loadLeaderboardCoursesForTerm(String term) {
        leaderboardCourseCodeByLabel.clear();
        if (leaderboardCourseChoice == null) {
            refreshLeaderboardView();
            return;
        }
        leaderboardCourseChoice.getItems().clear();
        if (term == null || term.isBlank()) {
            refreshLeaderboardView();
            return;
        }
        try {
            List<QuestionBank.CourseInfo> courses = QuestionBank.loadCoursesForTerm(term);
            List<String> labels = new ArrayList<>();
            for (QuestionBank.CourseInfo course : courses) {
                String label = course.getDisplayLabel();
                labels.add(label);
                leaderboardCourseCodeByLabel.put(label, course.getCourseCode());
            }
            leaderboardCourseChoice.getItems().setAll(labels);
            if (!labels.isEmpty()) {
                leaderboardCourseChoice.getSelectionModel().selectFirst();
            }
            refreshLeaderboardView();
        } catch (IOException ex) {
            leaderboardCourseChoice.getItems().clear();
            refreshLeaderboardView();
        }
    }

    @FXML
    private void refreshLeaderboardView() {
        if (leaderboardList == null) {
            return;
        }

        String term = leaderboardTermChoice != null ? normalizeTerm(leaderboardTermChoice.getValue()) : "";
        String courseLabel = leaderboardCourseChoice != null ? leaderboardCourseChoice.getValue() : null;
        String courseCode = courseLabel == null ? "" : normalizeCourseCode(leaderboardCourseCodeByLabel.get(courseLabel));

        if (term.isBlank() || courseCode.isBlank()) {
            setLeaderboardNoDataState();
            return;
        }

        List<ResultRow> filtered = loadResults().stream()
                .filter(r -> term.equals(normalizeTerm(r.term)))
                .filter(r -> courseCode.equals(normalizeCourseCode(r.course)))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            setLeaderboardNoDataState();
            return;
        }

        Map<String, double[]> statsByUser = new LinkedHashMap<>();
        Map<String, Double> bestByUser = new LinkedHashMap<>();
        Map<String, Integer> attemptsByUser = new LinkedHashMap<>();
        Map<String, String> displayByUser = new LinkedHashMap<>();

        for (ResultRow row : filtered) {
            String username = row.username == null ? "" : row.username.trim();
            if (username.isBlank()) {
                continue;
            }
            String key = username.toLowerCase();
            double[] stats = statsByUser.computeIfAbsent(key, ignored -> new double[]{0.0, 0.0});
            stats[0] += row.percentage;
            stats[1] += 1.0;
            bestByUser.merge(key, row.percentage, Math::max);
            attemptsByUser.merge(key, 1, Integer::sum);
            displayByUser.putIfAbsent(key, username);
        }

        if (statsByUser.isEmpty()) {
            setLeaderboardNoDataState();
            return;
        }

        List<String> keys = new ArrayList<>(statsByUser.keySet());
        keys.sort((left, right) -> {
            double[] lStats = statsByUser.get(left);
            double[] rStats = statsByUser.get(right);
            double lAvg = lStats[1] <= 0.0 ? 0.0 : lStats[0] / lStats[1];
            double rAvg = rStats[1] <= 0.0 ? 0.0 : rStats[0] / rStats[1];
            return Double.compare(rAvg, lAvg);
        });

        List<LeaderboardRowCard> cards = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            double[] stats = statsByUser.get(key);
            double avg = stats[1] <= 0.0 ? 0.0 : stats[0] / stats[1];
            double best = bestByUser.getOrDefault(key, 0.0);
            int attempts = attemptsByUser.getOrDefault(key, 0);
            String username = displayByUser.getOrDefault(key, key);
            cards.add(new LeaderboardRowCard(
                    i + 1,
                    username,
                    avg,
                    best,
                    attempts,
                    courseCode,
                    activeUser != null && activeUser.equalsIgnoreCase(username)
            ));
        }
        leaderboardList.getItems().setAll(cards);

        double classAvg = filtered.stream().mapToDouble(r -> r.percentage).average().orElse(0.0);
        long mastery = keys.stream()
                .filter(key -> {
                    double[] stats = statsByUser.get(key);
                    return stats[1] > 0.0 && (stats[0] / stats[1]) >= 90.0;
                })
                .count();

        if (leaderboardSummaryLabel != null) {
            double topAverage = cards.isEmpty() ? 0.0 : cards.get(0).averageScore();
            leaderboardSummaryLabel.setText("Showing " + cards.size() + " ranked student(s)  •  Top average " + String.format("%.1f%%", topAverage));
        }
        if (leaderboardClassAverageValueLabel != null) {
            leaderboardClassAverageValueLabel.setText(String.format("%.1f%%", classAvg));
        }
        if (leaderboardParticipationValueLabel != null) {
            leaderboardParticipationValueLabel.setText(keys.size() + " student(s)");
        }
        if (leaderboardMasteryValueLabel != null) {
            leaderboardMasteryValueLabel.setText(mastery + " student(s)");
        }

        String topKey = keys.get(0);
        if (highestScorerLabel != null) {
            String topUser = displayByUser.getOrDefault(topKey, topKey);
            double[] topStats = statsByUser.get(topKey);
            double topAverage = topStats == null || topStats[1] <= 0.0 ? 0.0 : topStats[0] / topStats[1];
            int topAttempts = attemptsByUser.getOrDefault(topKey, 0);
            highestScorerLabel.setText(topUser + " leads with " + String.format("%.1f%% average", topAverage)
                    + " across " + topAttempts + " attempt(s).");
        }
    }

    private void setLeaderboardNoDataState() {
        if (leaderboardList != null) {
            leaderboardList.getItems().clear();
        }
        if (leaderboardSummaryLabel != null) {
            leaderboardSummaryLabel.setText("No data available");
        }
        if (leaderboardClassAverageValueLabel != null) {
            leaderboardClassAverageValueLabel.setText("No data available");
        }
        if (leaderboardParticipationValueLabel != null) {
            leaderboardParticipationValueLabel.setText("No data available");
        }
        if (leaderboardMasteryValueLabel != null) {
            leaderboardMasteryValueLabel.setText("No data available");
        }
        if (highestScorerLabel != null) {
            highestScorerLabel.setText("No data available");
        }
    }

    private void loadQbCoursesForTerm(String term) {
        qbCourseCodeByLabel.clear();
        if (qbCourseChoice == null) {
            return;
        }
        qbCourseChoice.getItems().clear();
        if (term == null || term.isBlank()) {
            return;
        }
        try {
            List<QuestionBank.CourseInfo> courses = QuestionBank.loadCoursesForTerm(term);
            List<String> labels = new ArrayList<>();
            for (QuestionBank.CourseInfo course : courses) {
                String label = course.getDisplayLabel();
                labels.add(label);
                qbCourseCodeByLabel.put(label, course.getCourseCode());
            }
            qbCourseChoice.getItems().setAll(labels);
            if (!labels.isEmpty()) {
                qbCourseChoice.getSelectionModel().selectFirst();
            }
        } catch (IOException ex) {
            setQbStatus("Unable to load courses for selected term.");
        }
    }

    private void loadCreateExamCoursesForTerm(String term) {
        createCourseCodeByLabel.clear();
        if (createCourseChoice == null) {
            return;
        }
        createCourseChoice.getItems().clear();
        if (term == null || term.isBlank()) {
            return;
        }
        try {
            List<QuestionBank.CourseInfo> courses = QuestionBank.loadCoursesForTerm(term);
            List<String> labels = new ArrayList<>();
            for (QuestionBank.CourseInfo course : courses) {
                String label = course.getDisplayLabel();
                labels.add(label);
                createCourseCodeByLabel.put(label, course.getCourseCode());
            }
            createCourseChoice.getItems().setAll(labels);
            if (!labels.isEmpty()) {
                createCourseChoice.getSelectionModel().selectFirst();
            }
        } catch (IOException ex) {
            setCreateExamStatus("Unable to load courses for selected term.");
        }
    }

    private void loadQuestionBankRows() {
        if (questionBankList == null) {
            return;
        }
        qbCurrentRows.clear();
        String term = qbTermChoice != null ? qbTermChoice.getValue() : null;
        String courseLabel = qbCourseChoice != null ? qbCourseChoice.getValue() : null;
        String courseCode = courseLabel == null ? null : qbCourseCodeByLabel.get(courseLabel);
        if (term == null || term.isBlank() || courseCode == null || courseCode.isBlank()) {
            questionBankList.getItems().clear();
            return;
        }

        try {
            List<String> lines = readQuestionsCsvLines();
            for (int i = 0; i < lines.size(); i++) {
                TeacherQuestionRow row = parseQuestionRow(lines.get(i), i);
                if (row == null) {
                    continue;
                }
                if (term.equals(row.term) && normalizeCourseCode(courseCode).equals(normalizeCourseCode(row.courseCode))) {
                    qbCurrentRows.add(row);
                }
            }
            questionBankList.getItems().clear();
            for (int i = 0; i < qbCurrentRows.size(); i++) {
                TeacherQuestionRow row = qbCurrentRows.get(i);
                questionBankList.getItems().add("Q" + (i + 1) + " [" + row.questionId + "]: " + row.question);
            }
            setQbStatus(qbCurrentRows.isEmpty() ? "No questions found for selected course." : qbCurrentRows.size() + " questions loaded.");
        } catch (IOException ex) {
            questionBankList.getItems().clear();
            setQbStatus("Unable to load question bank.");
        }
    }

    private void loadCreateExamQuestionPool() {
        createCourseQuestions.clear();
        if (createQuestionList != null) {
            createQuestionList.getItems().clear();
        }
        String term = createTermChoice != null ? createTermChoice.getValue() : null;
        String courseLabel = createCourseChoice != null ? createCourseChoice.getValue() : null;
        String courseCode = courseLabel == null ? null : createCourseCodeByLabel.get(courseLabel);
        if (term == null || term.isBlank() || courseCode == null || courseCode.isBlank()) {
            updateCreateExamComputedFields();
            return;
        }
        try {
            List<Question> loaded = QuestionBank.loadQuestions(term, courseCode);
            createCourseQuestions.addAll(loaded);
            if (createQuestionList != null) {
                for (Question question : createCourseQuestions) {
                    createQuestionList.getItems().add(question.getId() + " - " + question.getText());
                }
                createQuestionList.getSelectionModel().clearSelection();
            }
            setCreateExamStatus(createCourseQuestions.isEmpty()
                    ? "No questions available for selected course."
                    : createCourseQuestions.size() + " questions available.");
        } catch (IOException ex) {
            setCreateExamStatus("Unable to load questions.");
        }
        updateCreateExamComputedFields();
    }

    private void updateCreateExamModeView() {
        boolean manualMode = isManualModeSelected();
        if (createQuestionList != null) {
            createQuestionList.setDisable(!manualMode);
        }
        if (createNumQuestions != null) {
            createNumQuestions.setDisable(manualMode);
        }
        updateCreateExamComputedFields();
    }

    private void updateCreateExamComputedFields() {
        int totalQuestions;
        if (isManualModeSelected()) {
            totalQuestions = createQuestionList == null ? 0 : createQuestionList.getSelectionModel().getSelectedIndices().size();
            if (createNumQuestions != null && createNumQuestions.getValueFactory() != null) {
                createNumQuestions.getValueFactory().setValue(totalQuestions);
            }
        } else {
            Integer value = createNumQuestions != null ? createNumQuestions.getValue() : null;
            totalQuestions = value == null ? 0 : value;
        }
        updateTimeAndMarks(totalQuestions);
    }

    private void updateTimeAndMarks(int count) {
        int safeCount = Math.max(count, 0);
        int time = safeCount * SECONDS_PER_QUESTION;
        int marks = safeCount;
        if (createComputedTimeLabel != null) {
            createComputedTimeLabel.setText(time + " sec");
        }
        if (createComputedMarksLabel != null) {
            createComputedMarksLabel.setText(String.valueOf(marks));
        }
    }

    private void loadScheduleCoursesForTerm(String term) {
        scheduleCourseCodeByLabel.clear();
        if (scheduleCourseChoice == null) {
            return;
        }
        scheduleCourseChoice.getItems().clear();
        if (term == null || term.isBlank()) {
            return;
        }
        try {
            List<QuestionBank.CourseInfo> courses = QuestionBank.loadCoursesForTerm(term);
            List<String> labels = new ArrayList<>();
            for (QuestionBank.CourseInfo course : courses) {
                String label = course.getDisplayLabel();
                labels.add(label);
                scheduleCourseCodeByLabel.put(label, course.getCourseCode());
            }
            scheduleCourseChoice.getItems().setAll(labels);
            if (!labels.isEmpty()) {
                scheduleCourseChoice.getSelectionModel().selectFirst();
            }
        } catch (IOException ex) {
            setScheduleStatus("Unable to load courses for selected term.");
        }
    }

    private void loadStudentScheduledCoursesForTerm(String term) {
        studentScheduledCourseCodeByLabel.clear();
        if (studentScheduledCourseChoice == null) {
            return;
        }
        studentScheduledCourseChoice.getItems().clear();
        if (term == null || term.isBlank()) {
            return;
        }
        try {
            List<QuestionBank.CourseInfo> courses = QuestionBank.loadCoursesForTerm(term);
            List<String> labels = new ArrayList<>();
            for (QuestionBank.CourseInfo course : courses) {
                String label = course.getDisplayLabel();
                labels.add(label);
                studentScheduledCourseCodeByLabel.put(label, course.getCourseCode());
            }
            studentScheduledCourseChoice.getItems().setAll(labels);
            if (!labels.isEmpty()) {
                studentScheduledCourseChoice.getSelectionModel().selectFirst();
            }
        } catch (IOException ex) {
            setStudentScheduledStatus("Unable to load courses for selected term.");
        }
    }

    private void refreshStudentScheduledExamList() {
        studentScheduledRowByLabel.clear();
        if (studentScheduledExamList == null) {
            return;
        }
        studentScheduledExamList.getItems().clear();

        String term = studentScheduledTermChoice != null ? studentScheduledTermChoice.getValue() : null;
        String courseLabel = studentScheduledCourseChoice != null ? studentScheduledCourseChoice.getValue() : null;
        String courseCode = courseLabel == null ? null : studentScheduledCourseCodeByLabel.get(courseLabel);
        if (term == null || term.isBlank() || courseCode == null || courseCode.isBlank()) {
            setStudentScheduledStatus("Select term and course.");
            updateStudentScheduledControls();
            return;
        }
        String selectedTerm = normalizeTerm(term);
        String selectedCourseCode = canonicalCourseCode(courseCode);

        Map<String, TeacherExamSummary> examById = new LinkedHashMap<>();
        for (TeacherExamSummary exam : loadCreatedExams()) {
            examById.put(exam.examId, exam);
        }

        List<ScheduleRow> filtered = new ArrayList<>();
        for (ScheduleRow row : loadSchedules()) {
            if (selectedTerm.equals(normalizeTerm(row.term))
                    && selectedCourseCode.equals(canonicalCourseCode(row.courseCode))) {
                filtered.add(row);
            }
        }
        filtered.sort(Comparator.comparing(this::scheduleDateTimeSafe));

        for (ScheduleRow row : filtered) {
            String status = resolveScheduleStatus(row, examById.get(row.examId));
            String label = row.examId + " | " + row.courseCode + " | " + row.date + " " + row.startTime + " | " + status;
            studentScheduledRowByLabel.put(label, row);
        }
        studentScheduledExamList.getItems().setAll(studentScheduledRowByLabel.keySet());
        if (studentScheduledExamList.getItems().isEmpty()) {
            setStudentScheduledStatus("No exams available.");
            if (studentScheduledSubtitleLabel != null) {
                studentScheduledSubtitleLabel.setText("No data available");
            }
        } else {
            studentScheduledExamList.getSelectionModel().selectFirst();
            setStudentScheduledStatus(studentScheduledExamList.getItems().size() + " scheduled exam(s) found.");
            if (studentScheduledSubtitleLabel != null) {
                studentScheduledSubtitleLabel.setText("You have " + studentScheduledExamList.getItems().size() + " scheduled exam(s).");
            }
        }
        updateStudentScheduledControls();
    }

    private void refreshDashboardUpcomingCard() {
        if (dashboardUpcomingExamList == null) {
            return;
        }
        Map<String, TeacherExamSummary> examById = new LinkedHashMap<>();
        for (TeacherExamSummary exam : loadCreatedExams()) {
            examById.put(exam.examId, exam);
        }

        List<ScheduleRow> future = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (ScheduleRow row : loadSchedules()) {
            if (!isActiveStudentSemester(row.term)) {
                continue;
            }
            LocalDateTime at = scheduleDateTimeSafe(row);
            if (at == LocalDateTime.MIN) {
                continue;
            }
            if (at.isAfter(now)) {
                future.add(row);
            }
        }
        future.sort(Comparator.comparing(this::scheduleDateTimeSafe));

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(3, future.size()); i++) {
            ScheduleRow row = future.get(i);
            TeacherExamSummary exam = examById.get(row.examId);
            String courseName = exam == null || exam.courseName == null || exam.courseName.isBlank() ? row.courseCode : exam.courseName;
            lines.add(row.examId + " | " + courseName + " | " + row.date + " " + row.startTime);
        }
        if (lines.isEmpty()) {
            lines.add("No data available");
        }
        dashboardUpcomingExamList.getItems().setAll(lines);
        if (dashboardUpcomingCountLabel != null) {
            dashboardUpcomingCountLabel.setText(future.isEmpty() ? "No data available" : future.size() + " Exams");
        }

        List<ResultRow> completed = loadCurrentUserResultRows();
        if (dashboardCompletedCountLabel != null) {
            dashboardCompletedCountLabel.setText(completed.isEmpty() ? "No data available" : completed.size() + " Exams");
        }
        if (dashboardRankLabel != null) {
            dashboardRankLabel.setText(computeCurrentUserRankLabel());
        }

        if (future.isEmpty()) {
            applyDashboardNoDataState();
            return;
        }

        ScheduleRow next = future.get(0);
        TeacherExamSummary nextExam = examById.get(next.examId);
        String nextCourseName = nextExam == null || nextExam.courseName == null || nextExam.courseName.isBlank()
                ? next.courseCode
                : nextExam.courseName;

        if (dashboardNextExamTitleLabel != null) {
            dashboardNextExamTitleLabel.setText(nextCourseName);
        }
        if (dashboardNextExamStatusLabel != null) {
            dashboardNextExamStatusLabel.setText(formatDashboardStartIn(scheduleDateTimeSafe(next), now));
        }
        if (dashboardNextExamDateValueLabel != null) {
            dashboardNextExamDateValueLabel.setText(next.date == null || next.date.isBlank() ? "No data available" : next.date);
        }
        if (dashboardNextExamTimeValueLabel != null) {
            dashboardNextExamTimeValueLabel.setText(formatDashboardDisplayTime(next.startTime));
        }
    }

    private void updateMessagingRequestBadge(int count) {
        if (messagingRequestBadgeLabel == null) {
            return;
        }
        if (count < 0) {
            messagingRequestBadgeLabel.setText("No data available");
            return;
        }
        messagingRequestBadgeLabel.setText(count + " NEW");
    }

    private void applyDashboardNoDataState() {
        if (dashboardNextExamTitleLabel != null) {
            dashboardNextExamTitleLabel.setText("No data available");
        }
        if (dashboardNextExamStatusLabel != null) {
            dashboardNextExamStatusLabel.setText("No data available");
        }
        if (dashboardNextExamDateValueLabel != null) {
            dashboardNextExamDateValueLabel.setText("No data available");
        }
        if (dashboardNextExamTimeValueLabel != null) {
            dashboardNextExamTimeValueLabel.setText("No data available");
        }
    }

    private List<ExamRecord> loadCurrentUserHistoryRecords() {
        List<ExamRecord> records = new ArrayList<>();
        Path path = Paths.get(EXAM_HISTORY_FILE);
        if (Files.notExists(path)) {
            return records;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.isBlank()) {
                    continue;
                }
                String lower = line.toLowerCase();
                if (i == 0 && (lower.startsWith("username,") || lower.startsWith("coursecode,"))) {
                    continue;
                }
                ExamRecord record = parseHistoryRecord(line);
                if (!matchesProgressFilter(record)) {
                    continue;
                }
                records.add(record);
            }
        } catch (IOException ex) {
            return List.of();
        }
        return records;
    }

    private String computeCurrentUserRankLabel() {
        if (activeUser == null || activeUser.isBlank()) {
            return "No data available";
        }
        List<ResultRow> rows = loadResults();
        String semester = normalizeSemester(activeUserSemester);
        if (!semester.isBlank()) {
            rows = rows.stream()
                    .filter(row -> semester.equals(normalizeSemester(row.term)))
                    .collect(Collectors.toList());
        }
        if (rows.isEmpty()) {
            return "No data available";
        }

        Map<String, double[]> statsByUser = new LinkedHashMap<>();
        for (ResultRow row : rows) {
            String username = row.username == null ? "" : row.username.trim();
            if (username.isBlank()) {
                continue;
            }
            String key = username.toLowerCase();
            double[] stats = statsByUser.computeIfAbsent(key, ignored -> new double[]{0.0, 0.0});
            stats[0] += row.percentage;
            stats[1] += 1.0;
        }
        if (statsByUser.isEmpty()) {
            return "No data available";
        }

        String activeKey = activeUser.trim().toLowerCase();
        double[] currentStats = statsByUser.get(activeKey);
        if (currentStats == null || currentStats[1] <= 0.0) {
            return "No data available";
        }

        double currentAvg = currentStats[0] / currentStats[1];
        int rank = 1;
        for (double[] stats : statsByUser.values()) {
            if (stats[1] <= 0.0) {
                continue;
            }
            double avg = stats[0] / stats[1];
            if (avg > currentAvg) {
                rank++;
            }
        }
        return ordinal(rank) + " in " + (semester.isBlank() ? "All Terms" : semester);
    }

    private String ordinal(int rank) {
        int mod100 = rank % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return rank + "th";
        }
        return switch (rank % 10) {
            case 1 -> rank + "st";
            case 2 -> rank + "nd";
            case 3 -> rank + "rd";
            default -> rank + "th";
        };
    }

    private String formatDashboardStartIn(LocalDateTime scheduledAt, LocalDateTime now) {
        if (scheduledAt == LocalDateTime.MIN) {
            return "No data available";
        }
        long minutes = java.time.Duration.between(now, scheduledAt).toMinutes();
        if (minutes <= 0) {
            return "Starting soon";
        }
        if (minutes < 60) {
            return "Starts in " + minutes + "m";
        }
        long hours = minutes / 60;
        long remaining = minutes % 60;
        if (hours < 24) {
            if (remaining == 0) {
                return "Starts in " + hours + "h";
            }
            return "Starts in " + hours + "h " + remaining + "m";
        }
        long days = hours / 24;
        return "Starts in " + days + "d";
    }

    private String formatDashboardDisplayTime(String value) {
        if (value == null || value.isBlank()) {
            return "No data available";
        }
        try {
            LocalTime parsed = LocalTime.parse(value.trim(), SCHEDULE_TIME_FORMAT);
            return parsed.format(DateTimeFormatter.ofPattern("hh:mm a"));
        } catch (Exception ex) {
            return value;
        }
    }

    private void updateStudentScheduledControls() {
        boolean hasSelection = studentScheduledExamList != null
                && studentScheduledExamList.getSelectionModel().getSelectedItem() != null
                && studentScheduledRowByLabel.containsKey(studentScheduledExamList.getSelectionModel().getSelectedItem());
        if (startScheduledExamButton != null) {
            startScheduledExamButton.setDisable(!hasSelection);
        }
    }

    private void setStudentScheduledStatus(String text) {
        if (studentScheduledStatusLabel != null) {
            studentScheduledStatusLabel.setText(text == null ? "" : text);
        }
    }

    @FXML
    private void startSelectedScheduledExam() {
        if (studentScheduledExamList == null) {
            return;
        }
        String selectedLabel = studentScheduledExamList.getSelectionModel().getSelectedItem();
        ScheduleRow selectedRow = selectedLabel == null ? null : studentScheduledRowByLabel.get(selectedLabel);
        if (selectedRow == null) {
            setStudentScheduledStatus("Select a scheduled exam first.");
            return;
        }
        if (!isActiveStudentSemester(selectedRow.term)) {
            setStudentScheduledStatus("You can only start exams from semester " + activeUserSemester + ".");
            return;
        }
        activeAttemptType = "scheduled";
        activeAttemptExamId = selectedRow.examId;
        selectedTerm = selectedRow.term;
        selectedCourseCode = selectedRow.courseCode;
        selectedCourseLabel = selectedRow.courseCode;
        levelLabel.setText("Course: " + selectedCourseCode);
        subtitleLabel.setText("Scheduled Exam: " + selectedRow.examId + " | " + selectedRow.date + " " + selectedRow.startTime);
        showPane(startPane);
    }

    private LocalDateTime scheduleDateTimeSafe(ScheduleRow row) {
        try {
            return LocalDateTime.of(LocalDate.parse(row.date), LocalTime.parse(row.startTime, DateTimeFormatter.ofPattern("HH:mm")));
        } catch (Exception ex) {
            return LocalDateTime.MIN;
        }
    }

    private String resolveScheduleStatus(ScheduleRow row, TeacherExamSummary exam) {
        LocalDateTime start = scheduleDateTimeSafe(row);
        if (start == LocalDateTime.MIN) {
            return "Unknown";
        }
        int durationSeconds = exam == null ? EXAM_DURATION_SECONDS : Math.max(exam.timeLimitSeconds, 0);
        LocalDateTime end = start.plusSeconds(durationSeconds);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(start)) {
            return "Upcoming";
        }
        if (now.isBefore(end)) {
            return "Ongoing";
        }
        return "Completed";
    }

    private void refreshScheduleExamChoices() {
        scheduleExamByLabel.clear();
        if (scheduleExamChoice != null) {
            scheduleExamChoice.getItems().clear();
        }

        String term = scheduleTermChoice != null ? scheduleTermChoice.getValue() : null;
        String courseLabel = scheduleCourseChoice != null ? scheduleCourseChoice.getValue() : null;
        String courseCode = courseLabel == null ? null : scheduleCourseCodeByLabel.get(courseLabel);
        if (term == null || term.isBlank() || courseCode == null || courseCode.isBlank()) {
            updateScheduleControls();
            return;
        }

        List<TeacherExamSummary> candidates = new ArrayList<>();
        for (TeacherExamSummary exam : loadCreatedExams()) {
            if (term.equals(exam.term) && normalizeCourseCode(courseCode).equals(normalizeCourseCode(exam.courseCode))) {
                candidates.add(exam);
            }
        }

        if (scheduleExamChoice != null) {
            for (TeacherExamSummary exam : candidates) {
                String label = exam.examId + " | " + exam.courseName + " | Q=" + exam.totalQuestions + " | Time=" + exam.timeLimitSeconds + "s";
                scheduleExamByLabel.put(label, exam);
            }
            scheduleExamChoice.getItems().setAll(scheduleExamByLabel.keySet());
            if (!scheduleExamChoice.getItems().isEmpty()) {
                scheduleExamChoice.getSelectionModel().selectFirst();
            }
        }

        if (candidates.isEmpty()) {
            setScheduleStatus("No exam created for this course.");
        } else {
            setScheduleStatus(candidates.size() + " created exam(s) available.");
        }
        updateScheduleControls();
    }

    private void updateScheduleControls() {
        boolean hasCreatedExam = scheduleExamChoice != null && !scheduleExamChoice.getItems().isEmpty()
                && scheduleExamChoice.getSelectionModel().getSelectedItem() != null;
        if (schedulePublishButton != null) {
            schedulePublishButton.setDisable(!hasCreatedExam);
        }
    }

    @FXML
    private void publishSchedule() {
        String term = scheduleTermChoice != null ? scheduleTermChoice.getValue() : null;
        String courseLabel = scheduleCourseChoice != null ? scheduleCourseChoice.getValue() : null;
        String selectedExamLabel = scheduleExamChoice != null ? scheduleExamChoice.getValue() : null;
        if (term == null || term.isBlank() || courseLabel == null || courseLabel.isBlank()) {
            setScheduleStatus("Select term and course.");
            return;
        }
        if (selectedExamLabel == null || selectedExamLabel.isBlank()) {
            setScheduleStatus("Cannot schedule without selecting exam.");
            return;
        }

        TeacherExamSummary selectedExam = scheduleExamByLabel.get(selectedExamLabel);
        if (selectedExam == null) {
            setScheduleStatus("Cannot schedule if no exam exists.");
            return;
        }

        LocalDate date = scheduleDate != null ? scheduleDate.getValue() : null;
        if (date == null) {
            setScheduleStatus("Select a schedule date.");
            return;
        }
        Integer hour = scheduleHourSpinner != null ? scheduleHourSpinner.getValue() : null;
        Integer minute = scheduleMinuteSpinner != null ? scheduleMinuteSpinner.getValue() : null;
        if (hour == null || minute == null) {
            setScheduleStatus("Select a valid start time.");
            return;
        }

        LocalTime startTime = LocalTime.of(hour, minute);

        try {
            appendSchedule(selectedExam.term, selectedExam.courseCode, selectedExam.examId, date.toString(), startTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            setScheduleStatus("Exam scheduled successfully.");
            loadExamCatalog();
        } catch (IOException ex) {
            setScheduleStatus("Unable to save schedule.");
        }
    }

    private void appendSchedule(String term, String courseCode, String examId, String date, String startTime) throws IOException {
        Path path = Paths.get(SCHEDULE_FILE);
        Path parent = path.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        boolean needsHeader = Files.notExists(path) || Files.size(path) == 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile(), true))) {
            if (needsHeader) {
                writer.write("examId,term,course,date,startTime");
                writer.newLine();
            }
            writer.write(String.join(",",
                    csv(examId),
                    csv(normalizeTerm(term)),
                    csv(canonicalCourseCode(courseCode)),
                    csv(date),
                    csv(startTime)
            ));
            writer.newLine();
        }
    }

    private List<ScheduleRow> loadSchedules() {
        List<ScheduleRow> rows = new ArrayList<>();
        Set<String> dedupe = new HashSet<>();
        for (Path path : List.of(Paths.get(SCHEDULE_FILE), Paths.get(LEGACY_SCHEDULE_FILE))) {
            if (Files.notExists(path)) {
                continue;
            }
            try {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line == null || line.isBlank()) {
                        continue;
                    }
                    String lower = line.toLowerCase();
                    if (lower.startsWith("term,coursecode,examid,") || lower.startsWith("examid,term,course,")) {
                        continue;
                    }
                    List<String> values = parseCsvLine(line);
                    if (values.size() < 5) {
                        continue;
                    }
                    ScheduleRow parsed;
                    if (values.get(0).trim().toUpperCase().startsWith("EX")) {
                        parsed = new ScheduleRow(
                                normalizeTerm(values.get(1)),
                                canonicalCourseCode(values.get(2)),
                                values.get(0).trim(),
                                values.get(3).trim(),
                                values.get(4).trim()
                        );
                    } else {
                        parsed = new ScheduleRow(
                                normalizeTerm(values.get(0)),
                                canonicalCourseCode(values.get(1)),
                                values.get(2).trim(),
                                values.get(3).trim(),
                                values.get(4).trim()
                        );
                    }
                    String key = parsed.examId + "|" + parsed.term + "|" + parsed.courseCode + "|" + parsed.date + "|" + parsed.startTime;
                    if (dedupe.add(key)) {
                        rows.add(parsed);
                    }
                }
            } catch (IOException ex) {
                return List.of();
            }
        }
        return rows;
    }

    private void setScheduleStatus(String text) {
        if (scheduleStatusLabel != null) {
            scheduleStatusLabel.setText(text == null ? "" : text);
        }
    }

    private boolean isManualModeSelected() {
        return createAutomaticModeRadio == null || !createAutomaticModeRadio.isSelected();
    }

    private void appendQuestionToCsv(TeacherQuestionRow row) throws IOException {
        Path path = Paths.get(QUESTIONS_FILE);
        Path parent = path.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile(), true))) {
            writer.write(row.toCsvLine());
            writer.newLine();
        }
    }

    private void updateQuestionInCsv(TeacherQuestionRow original, TeacherQuestionRow replacement) throws IOException {
        List<String> lines = readQuestionsCsvLines();
        if (original.sourceLineIndex < 0 || original.sourceLineIndex >= lines.size()) {
            throw new IOException("Question source row missing.");
        }
        lines.set(original.sourceLineIndex, replacement.toCsvLine());
        Files.write(Paths.get(QUESTIONS_FILE), lines, StandardCharsets.UTF_8);
    }

    private void deleteQuestionFromCsv(TeacherQuestionRow target) throws IOException {
        List<String> lines = readQuestionsCsvLines();
        if (target.sourceLineIndex < 0 || target.sourceLineIndex >= lines.size()) {
            throw new IOException("Question source row missing.");
        }
        lines.remove(target.sourceLineIndex);
        Files.write(Paths.get(QUESTIONS_FILE), lines, StandardCharsets.UTF_8);
    }

    private String nextQuestionId(String term, String courseCode) {
        int max = 0;
        String prefix = term + "-" + normalizeCourseCode(courseCode) + "-Q";
        for (TeacherQuestionRow row : qbCurrentRows) {
            String id = row.questionId == null ? "" : row.questionId.trim().toUpperCase();
            if (!id.startsWith(prefix.toUpperCase())) {
                continue;
            }
            String suffix = id.substring(prefix.length());
            int value = parseIntOrDefault(suffix, 0);
            max = Math.max(max, value);
        }
        return prefix + (max + 1);
    }

    private void appendExamDefinition(TeacherExamDefinition definition) throws IOException {
        Path path = Paths.get(EXAMS_FILE);
        Path parent = path.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        boolean needsHeader = Files.notExists(path) || Files.size(path) == 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile(), true))) {
            if (needsHeader) {
                writer.write("term,courseCode,courseName,totalQuestions,timeLimitSeconds,marks,mode,questionIds");
                writer.newLine();
            }
            writer.write(definition.toCsvLine());
            writer.newLine();
        }
    }

    private void refreshViewExamsList() {
        if (viewExamsList == null) {
            return;
        }
        viewExamByLabel.clear();
        List<TeacherExamSummary> exams = loadCreatedExams();
        Map<String, String> statusByExamId = buildViewExamStatus(exams, loadSchedules());
        List<String> rows = new ArrayList<>();
        for (TeacherExamSummary exam : exams) {
            String status = statusByExamId.getOrDefault(exam.examId, "Not Scheduled");
            String label = exam.examId + " | " + exam.courseCode + " - " + exam.courseName + " | Term " + exam.term
                    + " | Q=" + exam.totalQuestions + " | Time=" + exam.timeLimitSeconds + "s | " + status;
            rows.add(label);
            viewExamByLabel.put(label, exam);
        }
        if (rows.isEmpty()) {
            rows.add("No exams created yet.");
        }
        viewExamsList.getItems().setAll(rows);
        updateViewExamControls();
    }

    private Map<String, String> buildViewExamStatus(List<TeacherExamSummary> exams, List<ScheduleRow> schedules) {
        Map<String, String> statusByExamId = new LinkedHashMap<>();
        Map<String, TeacherExamSummary> examById = new LinkedHashMap<>();
        for (TeacherExamSummary exam : exams) {
            examById.put(exam.examId, exam);
            statusByExamId.put(exam.examId, "Not Scheduled");
        }
        LocalDateTime now = LocalDateTime.now();
        for (ScheduleRow schedule : schedules) {
            TeacherExamSummary exam = examById.get(schedule.examId);
            if (exam == null) {
                continue;
            }
            LocalDateTime startAt;
            try {
                startAt = LocalDateTime.of(LocalDate.parse(schedule.date), LocalTime.parse(schedule.startTime, DateTimeFormatter.ofPattern("HH:mm")));
            } catch (Exception ex) {
                continue;
            }
            LocalDateTime endAt = startAt.plusSeconds(Math.max(exam.timeLimitSeconds, 0));
            String status;
            if (now.isBefore(startAt)) {
                status = "Upcoming";
            } else if (now.isBefore(endAt)) {
                status = "Ongoing";
            } else {
                status = "Completed";
            }
            String previous = statusByExamId.get(exam.examId);
            if (statusPriority(status) > statusPriority(previous)) {
                statusByExamId.put(exam.examId, status);
            }
        }
        return statusByExamId;
    }

    private int statusPriority(String status) {
        if ("Ongoing".equalsIgnoreCase(status)) {
            return 3;
        }
        if ("Upcoming".equalsIgnoreCase(status)) {
            return 2;
        }
        if ("Completed".equalsIgnoreCase(status)) {
            return 1;
        }
        return 0;
    }

    private void updateViewExamControls() {
        TeacherExamSummary selected = getSelectedViewExam(false);
        boolean hasSelection = selected != null;
        if (viewExamEditButton != null) {
            viewExamEditButton.setDisable(!hasSelection);
        }
        if (viewExamDeleteButton != null) {
            viewExamDeleteButton.setDisable(!hasSelection);
        }
        if (viewExamStatusLabel != null) {
            viewExamStatusLabel.setText(hasSelection ? "Selected: " + selected.examId : "Select an exam to edit/delete.");
        }
    }

    private TeacherExamSummary getSelectedViewExam(boolean showAlertIfMissing) {
        String selectedExam = viewExamsList != null ? viewExamsList.getSelectionModel().getSelectedItem() : null;
        TeacherExamSummary exam = selectedExam == null ? null : viewExamByLabel.get(selectedExam);
        if (exam == null && showAlertIfMissing) {
            showError("View Exams", "Select an exam first.");
        }
        return exam;
    }

    @FXML
    private void deleteSelectedExam() {
        TeacherExamSummary selected = getSelectedViewExam(true);
        if (selected == null) {
            return;
        }
        boolean confirmed = showStyledConfirmation(
                "Delete Exam",
                "Delete Exam",
                "Delete " + selected.examId + " from created exams?"
        );
        if (!confirmed) {
            return;
        }
        try {
            deleteExamFromCsv(selected);
            refreshViewExamsList();
            refreshScheduleExamChoices();
            loadExamCatalog();
            setScheduleStatus("Exam deleted: " + selected.examId + ".");
            showStyledInformation("Delete Exam", "Delete Exam", "Deleted " + selected.examId + " successfully.");
        } catch (IOException ex) {
            showError("Delete Exam", "Unable to delete selected exam.");
        }
    }

    private void deleteExamFromCsv(TeacherExamSummary target) throws IOException {
        List<String> lines = new ArrayList<>();
        Path path = Paths.get(EXAMS_FILE);
        if (Files.exists(path)) {
            lines = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
        }
        if (target.sourceLineIndex < 0 || target.sourceLineIndex >= lines.size()) {
            throw new IOException("Exam source row missing.");
        }
        lines.remove(target.sourceLineIndex);
        Files.write(path, lines, StandardCharsets.UTF_8);
        reindexSchedulesAfterExamDelete(target.examId);
    }

    private void reindexSchedulesAfterExamDelete(String deletedExamId) throws IOException {
        int deletedNumber = parseExamNumber(deletedExamId);
        if (deletedNumber <= 0) {
            return;
        }
        for (Path path : List.of(Paths.get(SCHEDULE_FILE), Paths.get(LEGACY_SCHEDULE_FILE))) {
            if (Files.notExists(path)) {
                continue;
            }
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<String> updated = new ArrayList<>();
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String lower = line.toLowerCase();
                if (lower.startsWith("term,coursecode,examid,") || lower.startsWith("examid,term,course,")) {
                    updated.add("examId,term,course,date,startTime");
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.size() < 5) {
                    continue;
                }
                boolean examIdFirst = values.get(0).trim().toUpperCase().startsWith("EX");
                int examIndex = examIdFirst ? 0 : 2;
                String examId = values.get(examIndex).trim();
                int number = parseExamNumber(examId);
                if (number == deletedNumber) {
                    continue;
                }
                if (number > deletedNumber) {
                    examId = formatExamId(number - 1);
                }
                String term = examIdFirst ? values.get(1).trim() : values.get(0).trim();
                String course = examIdFirst ? values.get(2).trim() : values.get(1).trim();
                String date = values.get(3).trim();
                String startTime = values.get(4).trim();
                updated.add(String.join(",",
                        csv(examId),
                        csv(normalizeTerm(term)),
                        csv(canonicalCourseCode(course)),
                        csv(date),
                        csv(startTime)
                ));
            }
            Files.write(path, updated, StandardCharsets.UTF_8);
        }
    }

    @FXML
    private void editSelectedExam() {
        TeacherExamSummary selected = getSelectedViewExam(true);
        if (selected == null) {
            return;
        }
        TeacherExamDefinition updated = showEditExamDialog(selected);
        if (updated == null) {
            return;
        }
        try {
            updateExamInCsv(selected, updated);
            refreshViewExamsList();
            refreshScheduleExamChoices();
            loadExamCatalog();
            showStyledInformation("Edit Exam", "Edit Exam", "Updated " + selected.examId + " successfully.");
        } catch (IOException ex) {
            showError("Edit Exam", "Unable to save exam changes.");
        }
    }

    private TeacherExamDefinition showEditExamDialog(TeacherExamSummary selected) {
        List<Question> pool;
        try {
            pool = QuestionBank.loadQuestions(selected.term, selected.courseCode);
        } catch (IOException ex) {
            showError("Edit Exam", "Unable to load question pool for selected exam.");
            return null;
        }
        if (pool.isEmpty()) {
            showError("Edit Exam", "No questions available for selected course.");
            return null;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit " + selected.examId);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Label termLabel = new Label("Term: " + selected.term);
        Label courseLabel = new Label("Course: " + selected.courseCode + " - " + selected.courseName);
        ListView<String> questionListView = new ListView<>();
        questionListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        for (Question question : pool) {
            questionListView.getItems().add(question.getId() + " - " + question.getText());
        }

        Set<String> selectedIds = new HashSet<>();
        for (String id : selected.questionIds) {
            String safe = id == null ? "" : id.trim();
            if (!safe.isEmpty()) {
                selectedIds.add(safe);
            }
        }
        for (int i = 0; i < pool.size(); i++) {
            if (selectedIds.contains(pool.get(i).getId())) {
                questionListView.getSelectionModel().select(i);
            }
        }

        Label countLabel = new Label();
        Label timeLabel = new Label();
        Label marksLabel = new Label();
        Runnable updateMetrics = () -> {
            int count = questionListView.getSelectionModel().getSelectedItems().size();
            countLabel.setText("Count: " + count);
            timeLabel.setText("Time: " + (count * SECONDS_PER_QUESTION) + " sec");
            marksLabel.setText("Marks: " + count);
        };
        questionListView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<String>) change -> updateMetrics.run());
        updateMetrics.run();

        VBox dialogBody = new VBox(10.0,
                termLabel,
                courseLabel,
                new Label("Select Questions"),
                questionListView,
                new HBox(16.0, countLabel, timeLabel, marksLabel)
        );
        dialog.getDialogPane().setContent(dialogBody);
        dialog.showAndWait();
        if (dialog.getResult() != ButtonType.OK) {
            return null;
        }

        List<Integer> selectedIndexes = new ArrayList<>(questionListView.getSelectionModel().getSelectedIndices());
        if (selectedIndexes.isEmpty()) {
            showError("Edit Exam", "Select at least one question.");
            return null;
        }

        List<Question> selectedQuestions = new ArrayList<>();
        for (Integer index : selectedIndexes) {
            if (index != null && index >= 0 && index < pool.size()) {
                selectedQuestions.add(pool.get(index));
            }
        }
        int totalQuestions = selectedQuestions.size();
        return new TeacherExamDefinition(
                selected.term,
                selected.courseCode,
                selected.courseName,
                selectedQuestions,
                totalQuestions,
                totalQuestions * SECONDS_PER_QUESTION,
                totalQuestions,
                selected.mode == null || selected.mode.isBlank() ? "MANUAL" : selected.mode
        );
    }

    private void updateExamInCsv(TeacherExamSummary original, TeacherExamDefinition replacement) throws IOException {
        Path path = Paths.get(EXAMS_FILE);
        List<String> lines = Files.notExists(path)
                ? new ArrayList<>()
                : new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
        if (original.sourceLineIndex < 0 || original.sourceLineIndex >= lines.size()) {
            throw new IOException("Exam source row missing.");
        }
        lines.set(original.sourceLineIndex, replacement.toCsvLine());
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private int parseExamNumber(String examId) {
        if (examId == null) {
            return -1;
        }
        String value = examId.trim().toUpperCase();
        if (!value.startsWith("EX")) {
            return -1;
        }
        return parseIntOrDefault(value.substring(2), -1);
    }

    private String formatExamId(int value) {
        return String.format("EX%03d", Math.max(value, 0));
    }

    private List<TeacherExamSummary> loadCreatedExams() {
        List<TeacherExamSummary> exams = new ArrayList<>();
        Path path = Paths.get(EXAMS_FILE);
        if (Files.notExists(path)) {
            return exams;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int rowNumber = 0;
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                String line = lines.get(lineIndex);
                if (line == null || line.isBlank()) {
                    continue;
                }
                String lower = line.toLowerCase();
                if (lower.startsWith("term,coursecode,")) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.size() < 8) {
                    continue;
                }
                rowNumber++;
                String examId = String.format("EX%03d", rowNumber);
                exams.add(new TeacherExamSummary(
                        examId,
                    lineIndex,
                        values.get(0).trim(),
                        values.get(1).trim(),
                        values.get(2).trim(),
                        parseIntOrDefault(values.get(3).trim(), 0),
                        parseIntOrDefault(values.get(4).trim(), 0),
                        parseIntOrDefault(values.get(5).trim(), 0),
                        values.get(6).trim(),
                        parseQuestionIds(values.get(7))
                ));
            }
        } catch (IOException ex) {
            return List.of();
        }
        return exams;
    }

    private List<String> parseQuestionIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        String[] parts = raw.split(";");
        for (String part : parts) {
            String value = part == null ? "" : part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private List<String> readQuestionsCsvLines() throws IOException {
        Path path = Paths.get(QUESTIONS_FILE);
        if (Files.notExists(path)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
    }

    private TeacherQuestionRow parseQuestionRow(String line, int sourceLineIndex) {
        if (line == null || line.isBlank() || line.startsWith("#")) {
            return null;
        }
        List<String> values = parseCsvLine(line);
        if (values.size() >= 11) {
            return new TeacherQuestionRow(
                    sourceLineIndex,
                    values.get(0).trim(),
                    values.get(1).trim(),
                    values.get(2).trim(),
                    values.get(4).trim(),
                    values.get(5).trim(),
                    values.get(6).trim(),
                    values.get(7).trim(),
                    values.get(8).trim(),
                    values.get(9).trim(),
                    parseIntOrDefault(values.get(10).trim(), 1),
                    true,
                    values.get(3).trim()
            );
        }
        if (values.size() >= 10) {
            return new TeacherQuestionRow(
                    sourceLineIndex,
                    values.get(0).trim(),
                    values.get(1).trim(),
                    values.get(2).trim(),
                    values.get(3).trim(),
                    values.get(4).trim(),
                    values.get(5).trim(),
                    values.get(6).trim(),
                    values.get(7).trim(),
                    values.get(8).trim(),
                    parseIntOrDefault(values.get(9).trim(), 1),
                    false,
                    ""
            );
        }
        return null;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        values.add(current.toString());
        return values;
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private String extractCourseName(String courseLabel) {
        if (courseLabel == null) {
            return "";
        }
        int idx = courseLabel.indexOf(" - ");
        if (idx < 0 || idx + 3 >= courseLabel.length()) {
            return "";
        }
        return courseLabel.substring(idx + 3).trim();
    }

    private int optionToIndex(String option) {
        if (option == null) {
            return 1;
        }
        return switch (option.trim().toUpperCase()) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            default -> 1;
        };
    }

    private String indexToOption(int index) {
        return switch (index) {
            case 1 -> "A";
            case 2 -> "B";
            case 3 -> "C";
            case 4 -> "D";
            default -> "A";
        };
    }

    private void setQbStatus(String text) {
        if (qbStatusLabel != null) {
            qbStatusLabel.setText(text == null ? "" : text);
        }
    }

    private void setCreateExamStatus(String text) {
        if (createExamStatusLabel != null) {
            createExamStatusLabel.setText(text == null ? "" : text);
        }
    }

    private List<ResultRow> loadResults() {
        ensureResultsFileExists();
        List<ResultRow> rows = new ArrayList<>();
        Path path = Paths.get(RESULTS_FILE);
        if (Files.notExists(path)) {
            return rows;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String lower = line.toLowerCase();
                if (lower.startsWith("username,term,course,examid,")) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.size() < 10) {
                    continue;
                }
                String type = values.size() > 10 ? values.get(10).trim().toLowerCase() : "practice";
                rows.add(new ResultRow(
                        values.get(0).trim(),
                        values.get(1).trim(),
                        values.get(2).trim(),
                        values.get(3).trim(),
                        parseIntOrDefault(values.get(4).trim(), 0),
                        parseIntOrDefault(values.get(5).trim(), 0),
                        parseDoubleOrDefault(values.get(6).trim(), 0.0),
                        parseIntOrDefault(values.get(7).trim(), 0),
                        values.get(8).trim(),
                        type
                ));
            }
        } catch (IOException ex) {
            return List.of();
        }
        return rows;
    }

    private void updateTeacherAnalyticsMetrics() {
        String analyticsTerm = analyticsTermChoice != null && analyticsTermChoice.getValue() != null
                ? analyticsTermChoice.getValue().trim()
                : "";
        String courseLabel = analyticsCourseChoice != null ? analyticsCourseChoice.getValue() : null;
        String analyticsCourse = normalizeCourseCode(courseLabel == null ? "" : analyticsCourseCodeByLabel.getOrDefault(courseLabel, ""));
        if (analyticsTerm.isBlank() || analyticsCourse.isBlank()) {
            setTeacherAnalyticsValues(0.0, 0.0, 0.0, 0.0, true);
            return;
        }

        List<ResultRow> filtered = loadResults().stream()
                .filter(r -> analyticsTerm.equals(r.getTerm()))
                .filter(r -> analyticsCourse.equals(normalizeCourseCode(r.getCourse())))
            .filter(r -> "scheduled".equalsIgnoreCase(r.getType()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            setTeacherAnalyticsValues(0.0, 0.0, 0.0, 0.0, true);
            return;
        }

        double avg = filtered.stream().mapToDouble(ResultRow::getPercentage).average().orElse(0.0);
        double highest = filtered.stream().mapToDouble(ResultRow::getPercentage).max().orElse(0.0);
        double lowest = filtered.stream().mapToDouble(ResultRow::getPercentage).min().orElse(0.0);
        long passCount = filtered.stream().filter(r -> r.getPercentage() >= ANALYTICS_PASS_THRESHOLD).count();
        double passPct = (passCount * 100.0) / filtered.size();

        setTeacherAnalyticsValues(avg, highest, lowest, passPct, false);
    }

    private void setTeacherAnalyticsValues(double avg, double highest, double lowest, double passPct, boolean noData) {
        if (teacherAnalyticsAverageLabel != null) {
            teacherAnalyticsAverageLabel.setText(noData ? "0.0" : String.format("%.1f", avg));
        }
        if (teacherAnalyticsHighestLabel != null) {
            teacherAnalyticsHighestLabel.setText(noData ? "0" : String.valueOf((int) Math.round(highest)));
        }
        if (teacherAnalyticsLowestLabel != null) {
            teacherAnalyticsLowestLabel.setText(noData ? "0" : String.valueOf((int) Math.round(lowest)));
        }
        if (teacherAnalyticsPassLabel != null) {
            teacherAnalyticsPassLabel.setText(noData ? "0% (No student attempts yet)" : String.format("%d%%", (int) Math.round(passPct)));
        }
    }

    private void ensureResultsFileExists() {
        try {
            Path path = Paths.get(RESULTS_FILE);
            Path parent = path.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            if (Files.notExists(path)) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile(), true))) {
                    writer.write("username,term,course,examId,totalQuestions,correctAnswers,percentage,timeTaken,date");
                    writer.write(",type");
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            // Keep UI flow uninterrupted if results file cannot be initialized.
        }
    }

    private void appendResultRow(String username, String term, String courseCode, String examId,
                                 int totalQuestions, int correctAnswers, double percentage,
                                 int timeTakenSeconds, String date, String type) {
        try {
            ensureResultsFileExists();
            Path path = Paths.get(RESULTS_FILE);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile(), true))) {
                writer.write(String.join(",",
                        csv(username),
                        csv(term),
                        csv(courseCode),
                        csv(examId),
                        csv(String.valueOf(totalQuestions)),
                        csv(String.valueOf(correctAnswers)),
                        csv(String.format("%.1f", percentage)),
                        csv(String.valueOf(Math.max(timeTakenSeconds, 0))),
                        csv(date),
                        csv(type == null || type.isBlank() ? "practice" : type.toLowerCase())
                ));
                writer.newLine();
            }
        } catch (IOException ex) {
            // Keep exam flow uninterrupted if results persistence fails.
        }
    }

    private String saveExamHistory(int correctCount, double percent, int timeTakenSeconds, List<ResultAttemptDetail> detailRows) {
        String savedAt = LocalDateTime.now().format(HISTORY_DATE_FORMAT);
        if (selectedCourseCode == null || selectedCourseCode.isBlank() || questions == null || questions.isEmpty()) {
            return savedAt;
        }
        try {
            Path path = Paths.get(EXAM_HISTORY_FILE);
            Path parent = path.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }

            boolean needsHeader = Files.notExists(path) || Files.size(path) == 0;
            String normalizedCourse = selectedCourseCode.replace(" ", "").trim().toUpperCase();
            String normalizedTerm = selectedTerm == null ? "" : selectedTerm.trim();
            String username = activeUser == null || activeUser.isBlank() ? "Unknown" : activeUser;
            int scorePercent = (int) Math.round(percent * 100);
            String row = String.join(",",
                    username,
                    normalizedCourse,
                    savedAt,
                    String.valueOf(questions.size()),
                    String.valueOf(correctCount),
                    String.valueOf(scorePercent),
                    String.valueOf(Math.max(timeTakenSeconds, 0))
            );

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile(), true))) {
                if (needsHeader) {
                    writer.write("username,courseCode,date,totalQuestions,correctAnswers,percentage,timeTaken");
                    writer.newLine();
                }
                writer.write(row);
                writer.newLine();
            }

            appendResultRow(
                    username,
                    normalizedTerm,
                    normalizedCourse,
                    activeAttemptExamId,
                    questions.size(),
                    correctCount,
                    percent * 100.0,
                    timeTakenSeconds,
                    savedAt,
                    activeAttemptType
            );

            appendResultDetailRows(
                    activeAttemptExamId,
                    username,
                    normalizedCourse,
                    normalizedTerm,
                    savedAt,
                    detailRows
            );
        } catch (IOException ex) {
            // Keep exam flow uninterrupted if history persistence fails.
        }
        return savedAt;
    }

    private void appendResultDetailRows(String examId,
                                        String username,
                                        String course,
                                        String term,
                                        String attemptDate,
                                        List<ResultAttemptDetail> detailRows) throws IOException {
        Path path = Paths.get(RESULT_DETAILS_FILE);
        Path parent = path.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }

        boolean needsHeader = Files.notExists(path) || Files.size(path) == 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile(), true))) {
            if (needsHeader) {
                writer.write("exam_id,student_id,question_id,selected_option,correct_option,course,term,attempt_date");
                writer.newLine();
            }

            for (ResultAttemptDetail detail : detailRows) {
                writer.write(String.join(",",
                        csv(examId == null || examId.isBlank() ? "NA" : examId),
                        csv(username),
                        csv(detail.questionId() == null ? "" : detail.questionId()),
                        csv(detail.selectedOption()),
                        csv(detail.correctOption()),
                        csv(course),
                        csv(term),
                        csv(attemptDate)
                ));
                writer.newLine();
            }
        }
    }

    private void loadExamHistory() {
        if (historyTable == null) {
            return;
        }
        ensureProgressFilterOptions();
        refreshProgressCourseFilterOptions();

        List<ResultRow> filteredAttempts = getFilteredProgressAttempts();
        courseMap.clear();
        historyTable.getItems().clear();
        if (progressChart != null) {
            progressChart.getData().clear();
        }
        if (progressLineChart != null) {
            progressLineChart.getData().clear();
        }

        if (filteredAttempts.isEmpty()) {
            setProgressEmptyState(true);
            setProgressStatsNoData();
            return;
        }

        Map<String, List<ResultRow>> rowsByCourse = filteredAttempts.stream()
                .collect(Collectors.groupingBy(
                        row -> normalizeCourseCode(row.getCourse()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<String, String> displayCourseByKey = new LinkedHashMap<>();
        for (ResultRow row : filteredAttempts) {
            String key = normalizeCourseCode(row.getCourse());
            if (key.isBlank()) {
                continue;
            }
            displayCourseByKey.putIfAbsent(key, row.getCourseName());
        }

        List<CourseSummaryRow> rows = new ArrayList<>();
        Map<String, Double> courseAverageMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<ResultRow>> entry : rowsByCourse.entrySet()) {
            String courseKey = entry.getKey();
            List<ResultRow> attempts = new ArrayList<>(entry.getValue());
            if (attempts.isEmpty()) {
                continue;
            }
            attempts.sort(Comparator.comparing(ResultRow::getParsedDate));

            String courseName = displayCourseByKey.getOrDefault(courseKey, courseKey);
            List<ExamRecord> chartAttempts = attempts.stream()
                    .map(attempt -> new ExamRecord(
                            courseName,
                            attempt.getDate(),
                            attempt.getTotalQuestions(),
                            attempt.getCorrectAnswers(),
                            attempt.getPercentage(),
                            attempt.getTimeTaken(),
                            attempt.getUsername()
                    ))
                    .sorted(Comparator.comparing(ExamRecord::getParsedDate))
                    .collect(Collectors.toList());
            courseMap.put(courseName, chartAttempts);

            double best = attempts.stream().mapToDouble(ResultRow::getPercentage).max().orElse(0.0);
            double average = attempts.stream().mapToDouble(ResultRow::getPercentage).average().orElse(0.0);
            rows.add(new CourseSummaryRow(courseName, attempts.size(), best, average));
            courseAverageMap.put(courseName, average);
        }

        rows.sort(Comparator.comparing(CourseSummaryRow::getCourseCode));
        System.out.println("courseStats=" + courseAverageMap);

        if (rows.isEmpty()) {
            setProgressEmptyState(true);
            setProgressStatsNoData();
            return;
        }

        setProgressEmptyState(false);
        historyTable.getItems().setAll(rows);
        updateProgressStats(filteredAttempts, courseAverageMap);

        CourseSummaryRow selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null || !courseMap.containsKey(selected.getCourseCode())) {
            historyTable.getSelectionModel().selectFirst();
            selected = historyTable.getSelectionModel().getSelectedItem();
        }
        if (selected != null) {
            loadChartForCourse(courseMap.get(selected.getCourseCode()));
        }
    }

    private void ensureProgressFilterOptions() {
        if (progressTermChoice == null) {
            return;
        }

        String selectedTerm = progressTermChoice.getValue();
        List<ResultRow> userAttempts = loadCurrentUserResultRows();
        List<String> terms = userAttempts.stream()
                .map(ResultRow::getTerm)
                .filter(term -> term != null && !term.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        progressFilterRefreshInProgress = true;
        String activeSemester = normalizeSemester(activeUserSemester);
        if (activeSemester.isBlank()) {
            progressTermChoice.getItems().setAll(FILTER_ALL_TERMS);
            progressTermChoice.getItems().addAll(terms);
            progressTermChoice.setDisable(false);
        } else {
            progressTermChoice.getItems().setAll(activeSemester);
            progressTermChoice.setDisable(true);
        }
        if (selectedTerm != null && progressTermChoice.getItems().contains(selectedTerm)) {
            progressTermChoice.setValue(selectedTerm);
        } else if (!activeSemester.isBlank()) {
            progressTermChoice.setValue(activeSemester);
        } else {
            progressTermChoice.setValue(FILTER_ALL_TERMS);
        }
        progressFilterRefreshInProgress = false;
    }

    private void refreshProgressCourseFilterOptions() {
        if (progressCourseChoice == null) {
            return;
        }

        String selectedCourse = progressCourseChoice.getValue();
        String selectedTerm = progressTermChoice != null ? progressTermChoice.getValue() : FILTER_ALL_TERMS;

        List<String> courses = loadCurrentUserResultRows().stream()
                .filter(row -> FILTER_ALL_TERMS.equals(selectedTerm) || selectedTerm.equals(row.getTerm()))
                .map(ResultRow::getCourseName)
                .filter(course -> course != null && !course.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        progressFilterRefreshInProgress = true;
        progressCourseChoice.getItems().setAll(FILTER_ALL_COURSES);
        progressCourseChoice.getItems().addAll(courses);
        if (selectedCourse != null && progressCourseChoice.getItems().contains(selectedCourse)) {
            progressCourseChoice.setValue(selectedCourse);
        } else {
            progressCourseChoice.setValue(FILTER_ALL_COURSES);
        }
        progressFilterRefreshInProgress = false;
    }

    private List<ResultRow> getFilteredProgressAttempts() {
        String selectedTerm = progressTermChoice == null || progressTermChoice.getValue() == null
                ? FILTER_ALL_TERMS
                : progressTermChoice.getValue();
        String selectedCourse = progressCourseChoice == null || progressCourseChoice.getValue() == null
                ? FILTER_ALL_COURSES
                : progressCourseChoice.getValue();

        return loadCurrentUserResultRows().stream()
                .filter(row -> FILTER_ALL_TERMS.equals(selectedTerm) || selectedTerm.equals(row.getTerm()))
                .filter(row -> FILTER_ALL_COURSES.equals(selectedCourse) || selectedCourse.equals(row.getCourseName()))
                .sorted(Comparator.comparing(ResultRow::getParsedDate))
                .collect(Collectors.toList());
    }

    private List<ResultRow> loadCurrentUserResultRows() {
        if (activeUser == null || activeUser.isBlank()) {
            return List.of();
        }
        String active = activeUser.trim();
        return loadResults().stream()
                .filter(row -> row.getUsername().equalsIgnoreCase(active))
                .filter(row -> {
                    String semester = normalizeSemester(activeUserSemester);
                    return semester.isBlank() || semester.equals(normalizeSemester(row.getTerm()));
                })
                .filter(row -> row.getCourseName() != null && !row.getCourseName().isBlank())
                .collect(Collectors.toList());
    }

    private void updateProgressStats(List<ResultRow> filteredAttempts, Map<String, Double> courseAverageMap) {
        if (avgScoreLabel != null) {
            double averageByCourse = courseAverageMap.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            avgScoreLabel.setText(String.format("%.2f%%", averageByCourse));
        }
        if (avgTimeLabel != null) {
            double totalTime = filteredAttempts.stream().mapToDouble(ResultRow::getTimeTaken).sum();
            double averageTime = filteredAttempts.isEmpty() ? 0.0 : (totalTime / filteredAttempts.size());
            avgTimeLabel.setText(String.format("%.0fs", averageTime));
        }

        updateGlobalStrengthAndImprovement();
    }

    private Map<String, Double> buildCourseAverageMap(List<ResultRow> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return Map.of();
        }

        Map<String, List<ResultRow>> rowsByCourse = attempts.stream()
                .collect(Collectors.groupingBy(
                        row -> normalizeCourseCode(row.getCourse()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<String, String> displayCourseByKey = new LinkedHashMap<>();
        for (ResultRow row : attempts) {
            String key = normalizeCourseCode(row.getCourse());
            if (key.isBlank()) {
                continue;
            }
            displayCourseByKey.putIfAbsent(key, row.getCourseName());
        }

        Map<String, Double> courseAverageMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<ResultRow>> entry : rowsByCourse.entrySet()) {
            List<ResultRow> rows = entry.getValue();
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            String courseName = displayCourseByKey.getOrDefault(entry.getKey(), entry.getKey());
            double average = rows.stream().mapToDouble(ResultRow::getPercentage).average().orElse(0.0);
            courseAverageMap.put(courseName, average);
        }

        return courseAverageMap;
    }

    private void updateGlobalStrengthAndImprovement() {
        Map<String, Double> globalCourseAverageMap = buildCourseAverageMap(loadCurrentUserResultRows());

        if (globalCourseAverageMap.isEmpty()) {
            setStrongestAndImprovementNoData();
            return;
        }

        Map.Entry<String, Double> strongest = globalCourseAverageMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        Map.Entry<String, Double> weakest = globalCourseAverageMap.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .orElse(null);

        if (strongestSubjectLabel != null) {
            strongestSubjectLabel.setText(strongest == null ? NO_DATA_TEXT : strongest.getKey());
        }
        if (strongestSubjectMessageLabel != null) {
            strongestSubjectMessageLabel.setText(STRONGEST_SUBJECT_MESSAGE);
        }
        if (improvementAreaLabel != null) {
            improvementAreaLabel.setText(weakest == null ? NO_DATA_TEXT : weakest.getKey());
        }
        if (improvementAreaMessageLabel != null) {
            improvementAreaMessageLabel.setText(IMPROVEMENT_AREA_MESSAGE);
        }
    }

    private void setProgressStatsNoData() {
        if (avgScoreLabel != null) {
            avgScoreLabel.setText(NO_DATA_TEXT);
        }
        if (avgTimeLabel != null) {
            avgTimeLabel.setText(NO_DATA_TEXT);
        }
        updateGlobalStrengthAndImprovement();
    }

    private void setStrongestAndImprovementNoData() {
        if (strongestSubjectLabel != null) {
            strongestSubjectLabel.setText(NO_DATA_TEXT);
        }
        if (strongestSubjectMessageLabel != null) {
            strongestSubjectMessageLabel.setText(STRONGEST_SUBJECT_MESSAGE);
        }
        if (improvementAreaLabel != null) {
            improvementAreaLabel.setText(NO_DATA_TEXT);
        }
        if (improvementAreaMessageLabel != null) {
            improvementAreaMessageLabel.setText(IMPROVEMENT_AREA_MESSAGE);
        }
    }

    private ExamRecord parseHistoryRecord(String line) {
        String[] parts = line.split(",");
        if (parts.length < 7) {
            return null;
        }

        boolean oldFormat = looksLikeDate(parts[1].trim());
        String username;
        String courseCode;
        String date;
        int total;
        int correct;
        double score;
        int timeTaken;

        if (oldFormat) {
            courseCode = parts[0].trim();
            date = parts[1].trim();
            total = parseIntOrDefault(parts[2].trim(), 0);
            correct = parseIntOrDefault(parts[3].trim(), 0);
            score = parseDoubleOrDefault(parts[4].trim(), total == 0 ? 0.0 : (correct * 100.0) / total);
            timeTaken = parseIntOrDefault(parts[5].trim(), 0);
            username = parts[6].trim();
        } else {
            username = parts[0].trim();
            courseCode = parts[1].trim();
            date = parts[2].trim();
            total = parseIntOrDefault(parts[3].trim(), 0);
            correct = parseIntOrDefault(parts[4].trim(), 0);
            score = parseDoubleOrDefault(parts[5].trim(), total == 0 ? 0.0 : (correct * 100.0) / total);
            timeTaken = parseIntOrDefault(parts[6].trim(), 0);
        }

        if (courseCode.isBlank()) {
            return null;
        }
        return new ExamRecord(
                normalizeCourseCode(courseCode),
                date,
                total,
                correct,
                score,
                timeTaken,
                username.isBlank() ? "Unknown" : username
        );
    }

    private boolean looksLikeDate(String value) {
        return parseHistoryDate(value) != LocalDateTime.MIN;
    }

    private boolean matchesProgressFilter(ExamRecord entry) {
        if (entry == null) {
            return false;
        }
        if (activeUser != null && !activeUser.isBlank()) {
            if (entry.getStudentName() == null || !entry.getStudentName().equalsIgnoreCase(activeUser)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeCourseCode(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(" ", "").trim().toUpperCase();
    }

    private String canonicalCourseCode(String value) {
        String safe = value == null ? "" : value.trim();
        if (safe.isEmpty()) {
            return "";
        }
        String beforeDash = safe.split("-", 2)[0].trim();
        return normalizeCourseCode(beforeDash);
    }

    private String resolveSelectedCourseLabel(String term, String courseCode, String fallbackLabel) {
        String normalizedCode = normalizeCourseCode(courseCode);
        if (fallbackLabel != null && !fallbackLabel.isBlank()) {
            String mappedCode = courseCodeByLabel.get(fallbackLabel);
            if (normalizedCode.equals(normalizeCourseCode(mappedCode))) {
                return fallbackLabel;
            }
        }
        for (Map.Entry<String, String> entry : courseCodeByLabel.entrySet()) {
            if (normalizedCode.equals(normalizeCourseCode(entry.getValue()))) {
                return entry.getKey();
            }
        }
        try {
            List<QuestionBank.CourseInfo> courses = QuestionBank.loadCoursesForTerm(term);
            for (QuestionBank.CourseInfo course : courses) {
                if (normalizedCode.equals(normalizeCourseCode(course.getCourseCode()))) {
                    return course.getDisplayLabel();
                }
            }
        } catch (IOException ignored) {
            // Fallback to code-only label if catalog lookup fails.
        }
        return normalizedCode;
    }

    private String normalizeTerm(String value) {
        return value == null ? "" : value.trim();
    }

    private void loadChartForCourse(List<ExamRecord> attempts) {
        if (progressChart == null || progressLineChart == null) {
            return;
        }
        progressChart.getData().clear();
        progressLineChart.getData().clear();
        progressLineChart.setLegendVisible(false);
        progressLineChart.setHorizontalGridLinesVisible(false);
        progressLineChart.setVerticalGridLinesVisible(false);

        if (attempts == null || attempts.isEmpty()) {
            return;
        }

        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();

        attempts.sort(Comparator.comparing(ExamRecord::getParsedDate));
        for (int i = 0; i < attempts.size(); i++) {
            ExamRecord entry = attempts.get(i);
            String attemptLabel = "A" + (i + 1);
            double score = entry.getScorePercentageValue();
            int timeTaken = entry.getTimeTakenSeconds();

            XYChart.Data<String, Number> barPoint = new XYChart.Data<>(attemptLabel, score);
            XYChart.Data<String, Number> linePoint = new XYChart.Data<>(attemptLabel, score);
            attachBarStyle(barPoint, score);
            attachChartTooltip(barPoint, attemptLabel, score, timeTaken);
            attachChartTooltip(linePoint, score, timeTaken);
            barSeries.getData().add(barPoint);
            lineSeries.getData().add(linePoint);
        }

        progressChart.getData().add(barSeries);
        progressLineChart.getData().add(lineSeries);
        Platform.runLater(this::refreshProgressChartLayout);
    }

    private void refreshProgressChartLayout() {
        if (progressLineChart != null) {
            progressLineChart.applyCss();
            progressLineChart.layout();
        }
        if (progressChart != null) {
            progressChart.applyCss();
            progressChart.layout();
        }
    }

    private void refreshStudentIdentityDisplay() {
        String username = activeUser == null || activeUser.isBlank() ? "Student" : formatDisplayName(activeUser);
        String semester = activeUserSemester == null || activeUserSemester.isBlank()
                ? "Semester not selected"
                : "Semester " + activeUserSemester;

        if (studentWelcomeLabel != null) {
            studentWelcomeLabel.setText("Welcome " + username + " (" + semester.replace("Semester ", "") + ")");
        }
        if (dashboardProfileNameLabel != null) {
            dashboardProfileNameLabel.setText(username);
        }
        if (dashboardProfileSubtitleLabel != null) {
            dashboardProfileSubtitleLabel.setText(semester);
        }
        if (dashboardProfileInitialsLabel != null) {
            dashboardProfileInitialsLabel.setText(buildInitials(username));
        }
    }

    private String formatDisplayName(String value) {
        if (value == null || value.isBlank()) {
            return "Student";
        }
        String[] tokens = value.trim().split("\\s+");
        List<String> formatted = new ArrayList<>();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (token.length() == 1) {
                formatted.add(token.toUpperCase());
            } else {
                formatted.add(Character.toUpperCase(token.charAt(0)) + token.substring(1).toLowerCase());
            }
        }
        return formatted.isEmpty() ? "Student" : String.join(" ", formatted);
    }

    private String buildInitials(String value) {
        if (value == null || value.isBlank()) {
            return "ST";
        }
        String[] tokens = value.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String token : tokens) {
            if (!token.isBlank()) {
                initials.append(Character.toUpperCase(token.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        if (initials.length() == 0) {
            return "ST";
        }
        if (initials.length() == 1 && value.trim().length() > 1) {
            initials.append(Character.toUpperCase(value.trim().charAt(1)));
        }
        return initials.toString();
    }

    private void attachBarStyle(XYChart.Data<String, Number> dataPoint, double score) {
        String color = score > 70.0 ? "#2e7d32" : (score >= 40.0 ? "#ed6c02" : "#d32f2f");
        dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-bar-fill: " + color + ";");
            }
        });
    }

    private void attachChartTooltip(XYChart.Data<String, Number> dataPoint, String attemptLabel, double score, int timeTakenSeconds) {
        String text = String.format("Attempt: %s\nScore: %.0f%%\nTime: %d sec", attemptLabel, score, Math.max(timeTakenSeconds, 0));
        dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                Tooltip.install(newNode, new Tooltip(text));
            }
        });
    }

    private void attachChartTooltip(XYChart.Data<String, Number> dataPoint, double score, int timeTakenSeconds) {
        String text = String.format("Score: %.0f%%\nTime: %ds", score, Math.max(timeTakenSeconds, 0));
        dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                Tooltip.install(newNode, new Tooltip(text));
            }
        });
    }

    private void setProgressEmptyState(boolean visible) {
        if (progressEmptyStateLabel != null) {
            progressEmptyStateLabel.setVisible(visible);
            progressEmptyStateLabel.setManaged(visible);
        }
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private double parseDoubleOrDefault(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private LocalDateTime parseHistoryDate(String value) {
        try {
            return LocalDateTime.parse(value, HISTORY_DATE_FORMAT);
        } catch (Exception ex) {
            return LocalDateTime.MIN;
        }
    }

    private record ChatAlertSummary(int unreadCount,
                                    List<String> sendingFriends,
                                    Map<String, Long> seenTimestampUpdates) {
    }

    private static final class TeacherQuestionDraft {
        private final String question;
        private final String optionA;
        private final String optionB;
        private final String optionC;
        private final String optionD;
        private final int correctIndex;

        private TeacherQuestionDraft(String question, String optionA, String optionB, String optionC, String optionD, int correctIndex) {
            this.question = question;
            this.optionA = optionA;
            this.optionB = optionB;
            this.optionC = optionC;
            this.optionD = optionD;
            this.correctIndex = correctIndex;
        }
    }

    private static final class TeacherQuestionRow {
        private final int sourceLineIndex;
        private final String term;
        private final String courseCode;
        private final String courseName;
        private final String questionId;
        private final String question;
        private final String optionA;
        private final String optionB;
        private final String optionC;
        private final String optionD;
        private final int correctIndex;
        private final boolean hasExamId;
        private final String examId;

        private TeacherQuestionRow(int sourceLineIndex, String term, String courseCode, String courseName, String questionId,
                                   String question, String optionA, String optionB, String optionC, String optionD,
                                   int correctIndex, boolean hasExamId, String examId) {
            this.sourceLineIndex = sourceLineIndex;
            this.term = term;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.questionId = questionId;
            this.question = question;
            this.optionA = optionA;
            this.optionB = optionB;
            this.optionC = optionC;
            this.optionD = optionD;
            this.correctIndex = Math.max(1, Math.min(correctIndex, 4));
            this.hasExamId = hasExamId;
            this.examId = examId;
        }

        private static TeacherQuestionRow forAppend(String term, String courseCode, String courseName, String questionId,
                                                    String question, String optionA, String optionB, String optionC, String optionD,
                                                    int correctIndex) {
            return new TeacherQuestionRow(-1, term, courseCode, courseName, questionId, question,
                    optionA, optionB, optionC, optionD, correctIndex, false, "");
        }

        private TeacherQuestionRow withUpdatedContent(String question, String optionA, String optionB, String optionC, String optionD, int correctIndex) {
            return new TeacherQuestionRow(sourceLineIndex, term, courseCode, courseName, questionId, question,
                    optionA, optionB, optionC, optionD, correctIndex, hasExamId, examId);
        }

        private String toCsvLine() {
            if (hasExamId) {
                return String.join(",",
                        esc(term),
                        esc(courseCode),
                        esc(courseName),
                        esc(examId),
                        esc(questionId),
                        esc(question),
                        esc(optionA),
                        esc(optionB),
                        esc(optionC),
                        esc(optionD),
                        String.valueOf(correctIndex)
                );
            }
            return String.join(",",
                    esc(term),
                    esc(courseCode),
                    esc(courseName),
                    esc(questionId),
                    esc(question),
                    esc(optionA),
                    esc(optionB),
                    esc(optionC),
                    esc(optionD),
                    String.valueOf(correctIndex)
            );
        }

        private static String esc(String value) {
            String safe = value == null ? "" : value;
            if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
                return "\"" + safe.replace("\"", "\"\"") + "\"";
            }
            return safe;
        }
    }

    private static final class TeacherExamDefinition {
        private final String term;
        private final String courseCode;
        private final String courseName;
        private final List<Question> questionList;
        private final int totalQuestions;
        private final int timeLimitSeconds;
        private final int marks;
        private final String mode;

        private TeacherExamDefinition(String term, String courseCode, String courseName, List<Question> questionList,
                                      int totalQuestions, int timeLimitSeconds, int marks, String mode) {
            this.term = term;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.questionList = questionList;
            this.totalQuestions = totalQuestions;
            this.timeLimitSeconds = timeLimitSeconds;
            this.marks = marks;
            this.mode = mode;
        }

        private String toCsvLine() {
            List<String> questionIds = new ArrayList<>();
            for (Question question : questionList) {
                questionIds.add(question.getId());
            }
            return String.join(",",
                    TeacherQuestionRow.esc(term),
                    TeacherQuestionRow.esc(courseCode),
                    TeacherQuestionRow.esc(courseName),
                    String.valueOf(totalQuestions),
                    String.valueOf(timeLimitSeconds),
                    String.valueOf(marks),
                    TeacherQuestionRow.esc(mode),
                    TeacherQuestionRow.esc(String.join(";", questionIds))
            );
        }
    }

    private static final class TeacherExamSummary {
        private final String examId;
        private final int sourceLineIndex;
        private final String term;
        private final String courseCode;
        private final String courseName;
        private final int totalQuestions;
        private final int timeLimitSeconds;
        private final int marks;
        private final String mode;
        private final List<String> questionIds;

        private TeacherExamSummary(String examId, int sourceLineIndex, String term, String courseCode, String courseName,
                                   int totalQuestions, int timeLimitSeconds, int marks, String mode, List<String> questionIds) {
            this.examId = examId;
            this.sourceLineIndex = sourceLineIndex;
            this.term = term;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.totalQuestions = totalQuestions;
            this.timeLimitSeconds = timeLimitSeconds;
            this.marks = marks;
            this.mode = mode;
            this.questionIds = questionIds;
        }
    }

    private static final class ScheduleRow {
        private final String term;
        private final String courseCode;
        private final String examId;
        private final String date;
        private final String startTime;

        private ScheduleRow(String term, String courseCode, String examId, String date, String startTime) {
            this.term = term;
            this.courseCode = courseCode;
            this.examId = examId;
            this.date = date;
            this.startTime = startTime;
        }
    }

    private static final class ResultRow {
        private final String username;
        private final String term;
        private final String course;
        private final String examId;
        private final int totalQuestions;
        private final int correctAnswers;
        private final double percentage;
        private final int timeTaken;
        private final String date;
        private final String type;
        private final LocalDateTime parsedDate;

        private ResultRow(String username, String term, String course, String examId,
                          int totalQuestions, int correctAnswers, double percentage,
                          int timeTaken, String date, String type) {
            this.username = username;
            this.term = term;
            this.course = course;
            this.examId = examId;
            this.totalQuestions = totalQuestions;
            this.correctAnswers = correctAnswers;
            this.percentage = percentage;
            this.timeTaken = timeTaken;
            this.date = date;
            this.type = type;
            this.parsedDate = parseRowDate(date);
        }

        private static LocalDateTime parseRowDate(String value) {
            try {
                return LocalDateTime.parse(value, HISTORY_DATE_FORMAT);
            } catch (Exception ex) {
                return LocalDateTime.MIN;
            }
        }

        private String getUsername() {
            return username == null ? "" : username.trim();
        }

        private String getTerm() {
            return term == null ? "" : term.trim();
        }

        private String getCourse() {
            return course == null ? "" : course.trim();
        }

        private String getCourseName() {
            return getCourse();
        }

        private String getDate() {
            return date == null ? "" : date.trim();
        }

        private int getTotalQuestions() {
            return totalQuestions;
        }

        private int getCorrectAnswers() {
            return correctAnswers;
        }

        private double getPercentage() {
            return percentage;
        }

        private int getTimeTaken() {
            return Math.max(timeTaken, 0);
        }

        private LocalDateTime getParsedDate() {
            return parsedDate;
        }

        private String getType() {
            return type;
        }
    }

    private record ResultAttemptDetail(String questionId, String selectedOption, String correctOption) {
    }

    private record LeaderboardRowCard(int rank,
                                      String username,
                                      double averageScore,
                                      double bestScore,
                                      int attempts,
                                      String courseCode,
                                      boolean currentUser) {
        private String tierLabel() {
            return switch (rank) {
                case 1 -> "CROWN";
                case 2 -> "CHASER";
                case 3 -> "RISING";
                default -> "RANK";
            };
        }

        private String avatarText() {
            if (username == null || username.isBlank()) {
                return "--";
            }
            return username.substring(0, Math.min(2, username.length())).toUpperCase();
        }

        private String subtitle() {
            return (courseCode == null || courseCode.isBlank() ? "Course" : courseCode) + "  •  Semester ranking";
        }

        private double progressValue() {
            return Math.max(0.0, Math.min(1.0, averageScore / 100.0));
        }
    }

    public static final class ExamRecord {
        private final String courseCode;
        private final String examDateTime;
        private final Integer totalQuestions;
        private final Integer correctAnswers;
        private final Double scorePercentageValue;
        private final Integer timeTakenSeconds;
        private final String studentName;
        private final LocalDateTime parsedDate;

        public ExamRecord(String courseCode, String examDateTime, Integer totalQuestions,
                  Integer correctAnswers, Double scorePercentageValue,
                  Integer timeTakenSeconds, String studentName) {
            this.courseCode = courseCode;
            this.examDateTime = examDateTime;
            this.totalQuestions = totalQuestions;
            this.correctAnswers = correctAnswers;
            this.scorePercentageValue = scorePercentageValue;
            this.timeTakenSeconds = timeTakenSeconds;
            this.studentName = studentName;
            this.parsedDate = parseDate(examDateTime);
        }

        private static LocalDateTime parseDate(String value) {
            try {
                return LocalDateTime.parse(value, HISTORY_DATE_FORMAT);
            } catch (Exception ex) {
                return LocalDateTime.MIN;
            }
        }

        public String getCourseCode() {
            return courseCode;
        }

        public String getExamDateTime() {
            return examDateTime;
        }

        public Integer getTotalQuestions() {
            return totalQuestions;
        }

        public Integer getCorrectAnswers() {
            return correctAnswers;
        }

        public String getScorePercentage() {
            return String.format("%.2f%%", scorePercentageValue);
        }

        public Double getScorePercentageValue() {
            return scorePercentageValue;
        }

        public Integer getTimeTakenSeconds() {
            return timeTakenSeconds;
        }

        public String getStudentName() {
            return studentName;
        }

        public LocalDateTime getParsedDate() {
            return parsedDate;
        }
    }

    public static final class CourseSummaryRow {
        private final String courseCode;
        private final Integer attempts;
        private final Double bestPercentageValue;
        private final Double averagePercentageValue;

        public CourseSummaryRow(String courseCode, Integer attempts, Double bestPercentageValue, Double averagePercentageValue) {
            this.courseCode = courseCode;
            this.attempts = attempts;
            this.bestPercentageValue = bestPercentageValue;
            this.averagePercentageValue = averagePercentageValue;
        }

        public String getCourseCode() {
            return courseCode;
        }

        public Integer getAttempts() {
            return attempts;
        }

        public String getBestPercentage() {
            return String.format("%.2f%%", bestPercentageValue);
        }

        public String getAveragePercentage() {
            return String.format("%.2f%%", averagePercentageValue);
        }
    }
}
