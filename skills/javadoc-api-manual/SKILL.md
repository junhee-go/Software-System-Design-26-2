---
name: javadoc-api-manual
description: Write, update, generate, and verify DayMate Javadoc that serves as the assignment API manual.
---

# Javadoc API Manual

Use this skill when a public/protected Java API changes or when preparing the Javadoc manual and report screenshots.

## Documentation standard

- Document each public class, interface, enum, constructor, and method that belongs in the API manual.
- Start with a concise purpose sentence, then explain non-obvious behavior or constraints.
- Use `@param`, `@return`, and `@throws` whenever applicable. Do not add empty tags or impossible exceptions.
- Describe observable contracts rather than guesses. Keep documentation aligned with validation, persistence, cache, and threading behavior.
- Add `package-info.java` when a package overview materially helps the manual.
- Korean descriptions are acceptable; keep identifiers and Java terms consistent with code.

## Verification

Run `./gradlew.bat javadoc`, then inspect `build/docs/javadoc/index.html` and relevant class pages. Check navigation, Korean text, signatures, parameter/return descriptions, warnings, and broken links.

For the report, capture an overview/class list and a representative class page showing method details. Generated HTML under `build/docs/javadoc` is the source of truth; do not maintain a separate handwritten API manual.
