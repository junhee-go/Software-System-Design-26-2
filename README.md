# DayMate

DayMate is a Java Swing personal schedule management application for university students. It is designed to manage classes, exams, meetings, personal appointments, courses, and assignments in one place while providing a clear view of today's schedule and assignment deadlines.

## Current Status

The repository currently contains the Java 17/Gradle project skeleton and the development harness. Schedule management, calendar views, notifications, and SQLite persistence are planned for later implementation stages.

## Technology Stack

- Java 17
- Java Swing
- Gradle Wrapper (Groovy DSL)
- JUnit 5
- SQLite + Xerial SQLite JDBC
- Javadoc API manual

The application does not use a server or Docker. The target deliverable is a locally executable runnable JAR.

## Run and Verify

Use the following commands in Windows PowerShell:

```powershell
.\gradlew.bat run
.\gradlew.bat test
.\gradlew.bat javadoc
.\gradlew.bat verifyDayMate
```

`verifyDayMate` runs the tests, generates Javadoc, and builds a runnable JAR that contains its runtime dependencies.

## Data Storage

After persistence is implemented, data will be stored in the embedded SQLite database at `${user.home}/.daymate/daymate.db`. No separate database server or database account is required. The SQLite schema and persistence logic have not been implemented yet.

## Recommended Package Structure

```text
kr.ac.jnu.daymate
├─ ui
├─ controller
├─ data
├─ model
└─ dto
```

## Project Documentation

- Development harness: `AI.md`
- Repository skills: `skills/`
- Editable UML sources: `docs/diagrams/`
- Manual test checklist: `docs/manual-test-checklist.md`

## Current Limitations

- The project is in its initial stage, so the DayMate screens and application features are not implemented yet.
- Notifications are planned as Swing notifications that operate while the application is running.
- The application targets a single local user and does not provide account synchronization or server backups.
