---
name: daymate-design-sync
description: Reconcile DayMate implementation with requirements, use cases, UML diagrams, UI designs, and function specifications without inventing unimplemented behavior.
---

# DayMate Design Sync

Use this skill after changes to class structure, public method signatures, UI flows, persistence, or use-case behavior, and when updating checkpoint design artifacts.

## Sources to compare

Inspect the current implementation and editable sources under `docs/diagrams`. Compare the report requirements, use cases, UI descriptions, and function specifications when present in the repository.

The five core flows are:

1. Schedule management, including optional repeated schedules.
2. Course and assignment management.
3. Monthly/weekly calendar lookup.
4. Today's schedule and assignment-deadline summary.
5. Daily-summary notification settings and display.

## Synchronization rules

- Prefer correcting stale artifacts to pretending the code implements a planned feature.
- Preserve editable `.drawio` sources; export images separately only when a report needs them.
- Keep names consistent across Java classes, messages, function specifications, and diagrams.
- Sequence-diagram messages should map to real or intentionally planned method responsibilities.
- UI notes should describe visible Swing behavior, not internal database mechanics.
- Mark planned but unimplemented items explicitly as planned.

For the class diagram, follow the professor's constraint: do **not** draw dependency relationships. Show only relevant structural relationships such as association, aggregation, composition, and inheritance/generalization, with meaningful multiplicities. Do not add a relation merely because one method temporarily calls another class.

Report which artifacts were checked or changed and any remaining mismatch.
