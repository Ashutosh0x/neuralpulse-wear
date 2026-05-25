# Contributing Guidelines

Thank you for contributing to the NeuralPulse project. To maintain clinical-grade code quality and ensure build safety, please adhere to the following guidelines.

---

## 1. Code Style and Constraints

* **Language Standard**: Kotlin 1.9+ and JDK 17 / 21 compatibility.
* **No Emojis**: Emojis are strictly banned from all source code files, comments, commit messages, and documentation files to ensure standard clinical logs and readability.
* **Annotation Placement**: File-level annotations (such as `@file:Suppress`) must be placed at the absolute top of the source file, preceding the `package` declaration block.
* **Package Shading**: All compile-time stubs must mirror the exact package names of their target library components (e.g. `androidx.health.connect.client` or `com.samsung.android.service.health.tracking`).

---

## 2. Git Workflow & Branching

* **Branches**:
  * `master`: Production branch. Keep compiles passing at all times.
  * `feature/*`: New features or implementation modules.
  * `bugfix/*`: Bug fixes and regression repairs.
* **Pull Request Requirements**:
  * All PRs must compile cleanly without errors using:
    ```bash
    ./gradlew assembleDebug
    ```
  * All unit tests must pass locally:
    ```bash
    ./gradlew testDebugUnitTest
    ```
  * Every pull request automatically triggers the GitHub Actions CI build to verify these checks.

---

## 3. Testing Requirements

* Any modification or addition to signal processing modules, digital filtering, wearable data sync protocols, or security managers **must** be accompanied by unit tests.
* Ensure tests run successfully on local JVMs without requiring external hardware connections or full Android device setups.
