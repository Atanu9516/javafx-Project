# JavaFX Project

## Submission Note

⚠️ Important:
The final, runnable version of this project is available on the `final` branch.
Please do not evaluate the `main` branch.
``

Desktop-based **Online MCQ Exam System** built with JavaFX.

## Table of Contents

- [Quick Start](#quick-start)
- [Project Overview](#project-overview)
- [Academic Report: Detailed System Description](#academic-report-detailed-system-description)
  - [Abstract](#abstract)
  - [Problem Context and Motivation](#problem-context-and-motivation)
  - [Project Objectives](#project-objectives)
  - [System Scope and User Roles](#system-scope-and-user-roles)
  - [Functional Features in Detail](#functional-features-in-detail)
  - [System Architecture and Design](#system-architecture-and-design)
  - [Data Management and CSV Persistence Layer](#data-management-and-csv-persistence-layer)
  - [Network and Multi-Client Operation](#network-and-multi-client-operation)
  - [Evaluation Perspective and Academic Value](#evaluation-perspective-and-academic-value)
  - [Limitations and Improvement Opportunities](#limitations-and-improvement-opportunities)
- [System Requirements](#system-requirements)
- [Installation (Recommended Method)](#installation-recommended-method)
  - [Step 1: Install the Application](#step-1-install-the-application)
  - [Step 2: Run the Application](#step-2-run-the-application)
- [Demo Login](#demo-login)
- [Troubleshooting (If Installer Does Not Work)](#troubleshooting-if-installer-does-not-work)
  - [Step A: Set JDK 21](#step-a-set-jdk-21)
  - [Step B: Build the Project](#step-b-build-the-project)
  - [Step C: Run the Application (Development Mode)](#step-c-run-the-application-development-mode)
  - [Step D: Package the Application](#step-d-package-the-application)
  - [Step E: Manual Packaging (Advanced)](#step-e-manual-packaging-advanced)
  - [Step F: Server-Client Testing (Optional)](#step-f-server-client-testing-optional)
- [Important Notes](#important-notes)

## Quick Start

1. End-user install: run `ONLINEMCQEXAM/dist/OnlineMCQExam-1.0.0.exe`, then launch `OnlineMCQExam` from the Start Menu.
2. Developer run: open PowerShell in `ONLINEMCQEXAM/`, then run:

```powershell
cmd /c mvnw.cmd -q javafx:run
```

3. Sign in with any account listed in `Demo Login`.
4. If startup or installer fails, follow `Troubleshooting (If Installer Does Not Work)`.

## Project Overview

This project provides a desktop-based online MCQ exam platform developed with JavaFX. It supports exam creation, scheduling, participation, leaderboard ranking, progress tracking, and result analytics.

## Academic Report: Detailed System Description

### Abstract

The Online MCQ Exam System is a desktop academic assessment platform implemented with JavaFX and a CSV-backed storage model. The system digitizes the complete examination lifecycle: user authentication, question-bank curation, exam generation, schedule publication, controlled exam attempts, leaderboard ranking, progress monitoring, and analytics reporting. In addition to core assessment features, the platform includes collaborative and engagement modules such as discussion feed, friend requests, and direct messaging. A lightweight socket-based server-client mode enables multi-client operation over a custom text protocol.

### Problem Context and Motivation

Conventional classroom MCQ workflows are often fragmented: question storage is separate from scheduling, grading is often manual or semi-manual, and performance analysis is delayed. This project addresses those operational gaps by integrating:

- centralized question and exam template management,
- timed and structured student exam sessions,
- immediate scoring and persistence of attempts,
- data-driven academic insights for both students and instructors,
- and classroom communication workflows inside the same software environment.

The project is therefore positioned as both a software engineering artifact and an educational technology prototype.

### Project Objectives

The implementation targets the following measurable objectives:

1. Provide role-specific interfaces for students and teachers.
2. Support robust exam authoring with manual and auto-selection modes.
3. Enable schedule planning through draft and publish states.
4. Deliver timed exam participation with progress indicators and auto-feedback.
5. Maintain longitudinal learning records for analytics and ranking.
6. Support collaborative academic interaction (discussion and messaging).
7. Allow optional client-server operation for multi-user behavior.

### System Scope and User Roles

The system currently supports the following practical role behavior:

- **Student role**
  - Account registration and login with semester association.
  - Semester-locked access to course and exam catalogs.
  - Exam participation (practice and scheduled contexts).
  - Progress review, leaderboard view, discussion, and messaging.
  - Semester change request submission workflow.

- **Teacher role**
  - Dedicated teacher authentication flow.
  - Question bank browsing, filtering, and question insertion.
  - Exam creation from question pools (manual selection or automatic count-based generation).
  - Exam scheduling with draft and final publish actions.
  - Exam library review with search and status filtering.
  - Result analytics with summary metrics, distributions, and student-level breakdown tables.
  - Review of pending semester change requests.

- **Administrative data role (dataset-level)**
  - The repository includes admin/demo credentials and structured CSV datasets for users, schedules, exams, and results to support demonstration and evaluation scenarios.

### Functional Features in Detail

#### 1) Authentication, Session, and Access Control

- Student authentication validates username/password and loads semester context into session state.
- Registration requires confirmation checks and semester validity.
- Teacher access uses a dedicated login view and separate teacher dashboard route.
- Invalid teacher login is redirected to an explicit auth-failure screen.

#### 2) Semester-Aware Academic Navigation

- Terms are constrained to supported semester formats (e.g., `1-1` through `4-2`).
- Students are restricted to their assigned semester during course and exam selection.
- Semester selection feeds into dashboard, scheduled exam visibility, leaderboard filtering, and analytics consistency.

#### 3) Question Bank Management

- Question repositories are loaded from CSV with support for multiple header variants and legacy row layouts.
- Filter stack includes term, course, difficulty (All/Easy/Medium/Hard), and free-text search.
- Repository health is computed from question completeness and displayed as a progress metric.
- Teachers can add new questions through a dialog-driven form (question text, options, difficulty, and correct answer).
- New question IDs are auto-generated in structured format and appended to CSV safely.

#### 4) Exam Authoring and Template Generation

- Exam creation screen supports two modes:
  - **Manual mode:** teacher explicitly selects question cards.
  - **Auto mode:** teacher sets target count and system selects from filtered question pool.
- Computed exam characteristics update in real time:
  - total selected questions,
  - computed marks,
  - computed duration based on per-question timing.
- Saved exam templates are appended to `exams.csv` with metadata including term, course, mode, marks, duration, and question IDs.

#### 5) Scheduling Workflow (Draft and Publish)

- Scheduling module loads terms, courses, and template metadata from CSV sources with fallback logic.
- Teachers can set date and time through date picker and constrained hour/minute spinners.
- Draft schedules are stored in `schedule_drafts.csv` for pre-publication planning.
- Published schedules are written to `schedule.csv` and become visible to students.
- UI-level validation ensures incomplete forms are not published.

#### 6) Student Exam Experience

- Students can browse available courses as card-based views and initiate exam sessions.
- Timed MCQ sessions include progress bar, question navigation, answer selection, and submission path.
- Scheduled exams and practice attempts are both supported through attempt type state.
- Completion triggers score computation and persistence for progress and leaderboard reuse.

#### 7) Performance Tracking and Leaderboard

- Progress module includes:
  - historical course attempt table,
  - bar and line visualizations,
  - average score/time indicators,
  - strongest-subject and improvement-area labels.
- Leaderboard supports term/course filtering, username search, ranking by average then best score, and current-user highlighting.
- Participation and mastery indicators are presented in dedicated leaderboard summary labels.

#### 8) Results Analytics for Teachers

- Analytics module reads terms, courses, exams, students, and results from CSV with schema-flexible parsing.
- Filter model supports year-level and course-level analysis.
- KPI cards compute and show:
  - average score,
  - highest score,
  - lowest score,
  - pass rate,
  - and delta versus baseline.
- Distribution chart bins results into score ranges (`0-20`, `20-40`, `40-60`, `60-80`, `80-100`).
- Student performance table includes identity, date, grade band, percentage, status, and score.
- Insight labels generate narrative observations from filtered datasets.

#### 9) Discussion and Messaging Subsystem

- Discussion feed supports posting and reading class-wide messages with persistent storage in `discussion.csv`.
- Friend model supports request sending, acceptance, decline, and friend-list retrieval.
- One-to-one chat is available between confirmed friends only.
- Chat history is persisted in `chat_history.csv` with metadata timestamps and bounded retention logic.
- Message payloads are Base64-encoded for transport/storage safety (encoding, not cryptographic protection).

#### 10) Semester Change Governance

- Students can request transfer from current semester to a target semester.
- Teachers can review pending requests and approve or decline.
- On approval, user semester is updated and selected progress datasets are reset for consistency:
  - `results.csv`,
  - `result_details.csv`,
  - `exam_history.csv`.

### System Architecture and Design

The project follows a layered JavaFX application pattern with modular controllers:

- **Presentation layer:** JavaFX FXML views and controller classes for each module (login, dashboard, exam, scheduling, analytics, etc.).
- **Application/service layer:** interfaces and adapters for user, discussion, and messaging services.
- **Persistence layer:** CSV-backed stores and model adapters for terms, courses, exams, results, and social interaction data.
- **Network layer:** optional socket server (`ExamServer`) and client (`ExamClient`) using a delimiter-based protocol with Base64 payload encoding.

The main orchestration controller coordinates cross-view navigation, session state, and module refresh behavior.

### Data Management and CSV Persistence Layer

The system intentionally uses flat-file CSV storage to remain lightweight and portable. This design supports rapid prototyping and educational deployment without database setup overhead. Key datasets include:

- `users.csv` for credentials and semester context,
- `questions.csv` for MCQ banks,
- `exams.csv` for created templates,
- `schedule.csv` and `schedule_drafts.csv` for planning/publication,
- `results.csv`, `result_details.csv`, and `exam_history.csv` for outcomes,
- `discussion.csv`, `friends.csv`, `friend_requests.csv`, and `chat_history.csv` for collaboration features,
- `semester_change_requests.csv` for governance workflow.

Parsing logic is designed to tolerate legacy and modern header patterns, which improves backward compatibility of sample datasets.

### Network and Multi-Client Operation

In optional server-client mode, the application can run with a dedicated exam server process and one or more clients:

- command-based protocol supports login, registration, discussion, messaging, friend operations, and semester change operations,
- client handlers run concurrently via thread pool,
- read timeout and bind address are externally configurable through network configuration utilities.

This architecture enables realistic multi-user classroom demonstrations while preserving standalone desktop execution mode.

### Evaluation Perspective and Academic Value

From an academic software engineering perspective, the project demonstrates:

- end-to-end workflow integration from content authoring to analytics,
- multi-module GUI composition and controller coordination in JavaFX,
- practical data engineering for heterogeneous CSV schemas,
- operational handling of user roles and contextual access restrictions,
- and a hybrid local/network execution model suitable for classroom experiments.

### Limitations and Improvement Opportunities

Current implementation strengths are accompanied by recognized limitations:

1. CSV storage is simple and portable but not ideal for high concurrency or very large datasets.
2. Credential and payload handling are designed for prototype safety, not full production-grade security hardening.
3. Teacher authentication includes a hardcoded path for demonstration and should be replaced with role-based credential management.
4. Reporting is rich at UI level but can be extended with exportable PDF/CSV reports and cohort comparisons.
5. Automated testing coverage can be expanded (unit tests for parser edge cases, integration tests for network commands, and UI smoke tests).

These limitations define a clear roadmap toward enterprise readiness while preserving the educational value of the current version.

## System Requirements

- Windows 10/11 (64-bit)
- No manual Java installation is required for the installer version (runtime is bundled)

## Installation (Recommended Method)

### Step 1: Install the Application

Navigate to:

`ONLINEMCQEXAM/dist/OnlineMCQExam-1.0.0.exe`

Double-click the installer and complete the setup process.

### Step 2: Run the Application

After installation:

- Open from the Start Menu, **or**
- Navigate to:

`C:\Program Files\OnlineMCQExam\`

Then run:

`OnlineMCQExam.exe`

## Demo Login

- `student1 / password`
- `student2 / student2`
- `student3 / student3`
- `teacher1 / teacher1`
- `admin / admin123`

These demo accounts are auto-restored on application startup, so they should remain valid for classroom demonstrations even after previous data changes.

If a machine still shows login mismatch from old local data, close the app and delete:

`%LOCALAPPDATA%\OnlineMCQExam\data\users.csv`

Then relaunch the app once to regenerate demo users.

Hardcoded teacher login:

- `teacher / teacher123`

## Troubleshooting (If Installer Does Not Work)

If the installer fails or the application does not run, use the steps below.

### Step A: Set JDK 21

Ensure JDK 21 is installed and configured:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.10"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
Set-Location "E:\javafx-Project\ONLINEMCQEXAM"
```

### Step B: Build the Project

```powershell
cmd /c mvnw.cmd clean package -DskipTests
```

### Step C: Run the Application (Development Mode)

```powershell
cmd /c mvnw.cmd -q javafx:run
```

### Step D: Package the Application

Option 1: Create app image (no installer required)

```powershell
powershell -ExecutionPolicy Bypass -File .\package-exe.ps1 -Type app-image
```

Option 2: Create EXE installer (recommended)

```powershell
powershell -ExecutionPolicy Bypass -File .\package-exe.ps1 -Type exe
```

### Step E: Manual Packaging (Advanced)

```powershell
jpackage --type exe --name OnlineMCQExam --app-version 1.0.0 --dest dist --input .jpackage-input --main-jar onlinemcqexam-1.0-SNAPSHOT.jar --main-class com.example.onlinemcqexam.Launcher --module-path "$env:JAVA_HOME\jmods;target\lib" --add-modules java.base,javafx.controls,javafx.fxml
```

### Step F: Server-Client Testing (Optional)

Start server:

```powershell
java -cp target\classes com.example.onlinemcqexam.ExamServer
```

Start client:

```powershell
cmd /c mvnw.cmd -q javafx:run
```

## Important Notes

- EXE packaging requires WiX Toolset (`candle.exe` and `light.exe` must be available in `PATH`)
- Output folder: `dist/`
- Temporary packaging folder: `.jpackage-input/`

If PowerShell blocks scripts:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

