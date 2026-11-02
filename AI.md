# DayMate AI Development Guide

## Project baseline

- DayMate is a local desktop application for university students to manage schedules, courses, assignments, summaries, and daily notifications.
- Use Java 17, Java Swing, Gradle Wrapper with the Groovy DSL, JUnit, Javadoc, and embedded SQLite.
- Do not introduce Spring, a web server, Docker, MySQL, PostgreSQL, or a separately installed database unless the user changes the architecture.
- Store the production database at `${user.home}/.daymate/daymate.db`. The application must create its directory, database, and schema when absent.
- The application must remain usable offline. Bundle the SQLite JDBC driver in the distributable JAR.

## Repository skills

- `$daymate-maintainer`: implement or modify DayMate Java production code.
- `$daymate-style-verifier`: perform the required read-only review of production-code changes.
- `$javadoc-api-manual`: write and verify Javadoc used as the API manual.
- `$daymate-design-sync`: keep requirements, UML/design artifacts, and implementation consistent.
- `$readme-maintainer`: update user-facing project setup and status documentation.
- `$git-commit-korean`: prepare small, accurate Korean Git commits when the user asks to commit.

Read the matching `skills/<skill-name>/SKILL.md` before doing work covered by a skill. Use the smallest set of skills that covers the request.

## Architecture boundaries

Use these packages as the default structure:

- `ui`: Swing frames, panels, dialogs, and view models used only for display.
- `controller`: application flows, input validation, and coordination between views and data services.
- `data`: SQLite access, schema initialization, repositories, and in-memory cache coordination.
- `model`: domain entities and enums.
- `dto`: values passed between layers when a domain object is not appropriate.

The normal dependency direction is:

`UI -> Controller -> DayMateDataManager -> LocalDatabase`

Views must not issue SQL. Controllers must not use `java.sql` directly. `LocalDatabase` owns connections and JDBC details; `DayMateDataManager` coordinates persisted data and the application cache.

## Mandatory production-code workflow

This workflow applies whenever a task changes files under `src/main`. It does not apply to documentation-only, diagram-only, test-only, or build-configuration-only changes.

1. Record the existing dirty paths and preserve unrelated user changes.
2. A developer agent reads `$daymate-maintainer`, implements the smallest coherent change, and runs relevant checks.
3. The developer hands off the requirement, changed paths, design decisions, and check results.
4. A fresh read-only verifier agent reads `$daymate-style-verifier` and reviews the change without editing files or running Gradle.
5. Blocking findings return to the same developer for correction.
6. A fresh verifier reviews the corrected result.
7. Work is complete only when relevant checks pass and the verifier reports no blocking findings.

Stop after two correction cycles. If blocking findings remain, report the unresolved risk to the user instead of silently widening the change.

## Documentation gates

- When a public or protected Java API changes, use `$javadoc-api-manual` and regenerate Javadoc.
- When class structure, method signatures, UI flows, persistence, or use-case behavior changes, use `$daymate-design-sync` to check the matching design artifacts.
- When implemented features, run instructions, storage behavior, or limitations change, use `$readme-maintainer`.

## Verification commands

Use the Gradle Wrapper from the repository root:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat javadoc
.\gradlew.bat verifyDayMate
```

Prefer the narrowest relevant task while iterating. Run `verifyDayMate` before a milestone handoff or release artifact is claimed ready.

## Completion criteria

- The implementation matches the stated requirement and the current design.
- Layer boundaries and Swing/SQLite constraints are preserved.
- Relevant automated checks pass and manual checks are reported honestly.
- Javadoc and design/readme artifacts are updated when their gates apply.
- No unrelated user changes are overwritten or committed.
