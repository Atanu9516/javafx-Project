# Online MCQ Exam (JavaFX)

This project is a JavaFX-based Online MCQ Exam system with student and teacher workflows, discussion, messaging, and network-backed user operations.

## Demo Accounts

- Student: `atanu` / `atanuu`
- Student: `rahim` / `rahim1`
- Student: `sadia` / `sadia1`
- Student: `nabila` / `nabila1`
- Teacher: `teacher` / `teacher123`

## Prerequisites

- Windows 10/11
- Java JDK 21
- Internet connection (first build only, to download Maven dependencies)

## Project Structure

- `ONLINEMCQEXAM/` - main Maven project
- `ONLINEMCQEXAM/src/main/java` - Java source code
- `ONLINEMCQEXAM/src/main/resources` - FXML, CSS, and CSV resources
- `ONLINEMCQEXAM/data` - runtime data files

## Installation and Run Guide

1. Extract the zip file `(2405116,2405117).zip` to any folder you prefer.

2. Open PowerShell and go to the project folder:
   - `cd "<extracted-folder>\javafx-Project\ONLINEMCQEXAM"`

3. Build the project:
   - `.\mvnw.cmd clean package`

4. Run the executable JAR:
   - `java -jar target\javafx-Project-1.0.jar`

## Notes

- The app starts its local exam server automatically if no server is already running.
- Default network port is `5050`.
- To change host/port, you can use environment variables:
  - `EXAM_HOST`
  - `EXAM_PORT`
  - `EXAM_BIND`

## Optional: Server-only Launch

If needed, you can run only the server process:

- `java -cp target\javafx-Project-1.0.jar com.example.onlinemcqexam.ExamServer`

## Build Windows EXE (Installer)

From `ONLINEMCQEXAM`, you can build Windows executable output with one command:

- `.\build-exe.ps1`

Output:

- If WiX is installed: installer `.exe` inside `ONLINEMCQEXAM\dist\`
- If WiX is not installed: portable app with launcher at `ONLINEMCQEXAM\dist\OnlineMCQExam\OnlineMCQExam.exe`

If your JDK is installed in a different location:

- `.\build-exe.ps1 -JdkPath "C:\Path\To\Your\jdk-21"`

Force installer mode (requires WiX):

- `.\build-exe.ps1 -Mode installer`

Force portable mode (no WiX needed):

- `.\build-exe.ps1 -Mode portable`

Manual command (without script):

- `.\mvnw.cmd clean package`
- `jpackage --type exe --name "OnlineMCQExam" --input "target" --main-jar "javafx-Project-1.0.jar" --main-class "com.example.onlinemcqexam.Launcher" --win-shortcut --win-menu --dest "dist"`
