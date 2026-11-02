---
name: git-commit-korean
description: Prepare focused Korean Git commits for DayMate after inspecting status, diffs, and recent history; use when the user asks to commit or requests a commit message.
---

# Korean Git Commit

Inspect `git status --short`, staged and unstaged diffs, and recent commit subjects before proposing or creating a commit.

- Keep each commit focused on one coherent purpose and exclude unrelated user changes.
- Use a concise Korean imperative-style subject that explains the outcome, not the editing process.
- Follow recent repository convention. When none exists, use `<type>: <Korean summary>`.
- Suggested types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`.
- Add a body only when motivation, migration, or a non-obvious tradeoff needs explanation.
- Never commit generated build output, the user-home SQLite database, secrets, IDE metadata, or unrelated diagrams.
- Do not amend, push, force-push, or create a commit unless the user explicitly authorizes it.

Before committing, show or summarize exact staged paths and run checks appropriate to the change.
