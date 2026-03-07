# Contributing to ProjectFusion Warpzones

Thank you for your interest in contributing!

## Getting Started

1. **Fork** the repository on GitHub and clone your fork.
2. Ensure you have:
   - JDK 21 (or a version compatible with the Paper 1.21.11 API).
   - A recent version of Gradle (the project includes a Gradle wrapper).
3. Import the project into your IDE as a Gradle project.

## Building

Use the Gradle wrapper where possible:

```bash
./gradlew build
```

The shaded plugin JAR will be produced under `build/libs/`.

## Running Locally

- Set up a local Paper server for the target Minecraft version.
- Install WorldEdit.
- Copy the built JAR into the server's `plugins` folder.
- Start the server and verify that warp zones can be created, linked, and used.

## Code Style

- Use standard modern Java style consistent with existing code.
- Prefer clear, descriptive names over abbreviations.
- Keep changes focused and small where possible.
- Avoid introducing new dependencies unless there is a clear benefit.

## Tests

- Where practical, add or update tests for new behavior.
- Keep tests deterministic and fast.

## Pull Requests

When opening a pull request:

- Describe **what** you changed and **why**.
- Mention any user-facing behavior changes.
- Call out breaking changes, configuration changes, or data migration concerns.
- Keep the diff focused; unrelated refactors are best done in separate PRs.

By participating in this project, you agree that your contributions may be licensed under the same license as this repository.
