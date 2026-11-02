---
name: daymate-maintainer
description: Implement or modify DayMate Java Swing production code while preserving its local SQLite architecture, package boundaries, and assignment design.
---

# DayMate Maintainer

Use this skill for changes under `src/main`.

## Required context

Before editing, read `AI.md`, relevant neighboring code, and affected artifacts under `docs/diagrams`. Preserve unrelated working-tree changes.

## Architecture rules

- Use Java 17 and Swing; do not introduce a web framework, server, Docker, or server database.
- Keep the dependency direction `ui -> controller -> data` and use `model`/`dto` for shared values.
- Swing views render state and collect input. They must not issue SQL.
- Controllers validate inputs and coordinate use cases. They must not import `java.sql`.
- `DayMateDataManager` coordinates cached data and persistence. `LocalDatabase` owns JDBC connections, schema initialization, and SQL details.
- Do not use IntelliJ GUI Designer `.form` files; keep the UI buildable by Gradle alone.

## Swing and SQLite rules

- Create and update components on the Event Dispatch Thread. Do not block it with long database work.
- Make timers and notification scheduling idempotent so settings refreshes cannot create overlapping timers.
- Give validation failures actionable Korean messages and retain user input for correction.
- Use `${user.home}/.daymate/daymate.db` in production and a temporary database in tests.
- Create the parent directory, database, and schema when absent. Enable `PRAGMA foreign_keys = ON` for every connection.
- Use `PreparedStatement`, try-with-resources, and transactions for multi-step writes. Never interpolate user input into SQL.
- Update the cache only after a successful write, or restore it when a transaction fails.

## Change workflow

1. Trace the affected use case from UI through controller to data.
2. Implement the smallest coherent change without speculative abstractions.
3. Add focused tests. Do not widen a production API only to make testing convenient.
4. Add accurate Javadoc to changed public/protected APIs.
5. Run the narrowest relevant Gradle tasks and report changed paths, decisions, and results for the verifier handoff required by `AI.md`.

Do not commit, push, or rewrite user changes unless the user explicitly requests it.
