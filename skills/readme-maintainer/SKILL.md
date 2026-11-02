---
name: readme-maintainer
description: Keep the DayMate README accurate for implemented features, Java 17 setup, Gradle commands, SQLite storage, and known limitations.
---

# README Maintainer

Update `README.md` when implemented features, execution, build commands, storage behavior, package layout, or limitations change.

- Write for a student, evaluator, or developer running the project for the first time.
- Describe only behavior present in the current revision. Label future work as planned.
- Keep Java 17, Swing, Gradle Wrapper, and embedded SQLite requirements explicit.
- State the production DB path `${user.home}/.daymate/daymate.db` and that no DB server or Docker setup is required.
- Use commands actually checked on Windows. Prefer `gradlew.bat` examples.
- Keep sections concise: purpose, current status/features, prerequisites, run/verify, data location, structure, documentation, limitations.
- Never claim tests, a runnable JAR, or a feature succeeded without evidence.

After editing, verify links, paths, and commands against the repository.
