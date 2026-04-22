# Spec: log-analyst-koog v1

## Objective

A personal CLI tool for analysing mobile test failure logs (iOS XCUITest + Android Espresso/Logcat)
using a Kotlin-native AI agent architecture. The user points it at a single log file and gets a
structured failure analysis: root cause category, confidence, evidence, and recommended action.

**Success looks like:** running one command and getting a clear, actionable diagnosis faster than
reading the log manually.

**User:** Viktar — solo, personal productivity tool.

---

## Tech Stack

| Concern        | Choice                                      |
|----------------|---------------------------------------------|
| Language       | Kotlin (JVM), idiomatic — coroutines, extension functions, DSL where it adds clarity |
| AI Framework   | JetBrains Koog 0.8.0                        |
| LLM Backend    | Ollama local (`qwen2.5-coder:7b`)           |
| Build          | Gradle 8.7 (Kotlin DSL)                     |
| CLI            | Manual args parsing (keep it simple)        |
| Reporting      | JSON (v1), PDF (v2)                         |
| Testing        | JUnit 5 + Kotest assertions                 |

---

## Commands

```bash
# Analyse a log file
./gradlew run --args="analyse --file /path/to/test.log"

# Run tests
./gradlew test

# Build fat JAR
./gradlew shadowJar

# Run compiled JAR
java -jar build/libs/log-analyst-koog-1.0.0.jar analyse --file /path/to/test.log
```

---

## Project Structure

```
log-analyst-koog/
├── src/
│   ├── main/kotlin/
│   │   ├── Main.kt                              # CLI entry point
│   │   ├── cli/
│   │   │   └── AnalyseCommand.kt                # Arg parsing, command dispatch
│   │   ├── agent/
│   │   │   ├── LogAnalystAgent.kt               # Koog agent definition (Task 9a)
│   │   │   ├── AnalysisPrompt.kt                # Prompt template (Task 9a)
│   │   │   └── JsonResponseParser.kt            # LLM response correction (Task 9b)
│   │   ├── classifier/
│   │   │   ├── ClassifierPipeline.kt            # Deterministic-first composition (Task 11)
│   │   │   ├── deterministic/
│   │   │   │   ├── DeterministicClassifier.kt   # Orchestrator (Task 6)
│   │   │   │   ├── IosRuleClassifier.kt         # iOS rules (Task 4)
│   │   │   │   └── AndroidRuleClassifier.kt     # Android rules (Task 5)
│   │   │   └── llm/
│   │   │       └── LlmClassifier.kt             # LLM fallback (Task 10)
│   │   ├── model/
│   │   │   ├── Platform.kt                      ✅ Task 1
│   │   │   ├── FailureCategory.kt               ✅ Task 1
│   │   │   ├── ClassifierSource.kt              ✅ Task 1
│   │   │   └── LogAnalysis.kt                   ✅ Task 1
│   │   ├── parser/
│   │   │   ├── LogParser.kt                     # Platform detection + extraction (Tasks 2-3)
│   │   │   └── ParsedLog.kt                     # Parser output type
│   │   └── report/
│   │       └── JsonReportWriter.kt              # JSON serialisation (Task 7)
│   └── test/kotlin/
│       ├── classifier/
│       │   └── ClassifierPipelineTest.kt        # Mock-LLM integration test (Task 13)
│       ├── parser/
│       └── fixtures/
│           ├── ios_assertion_failure.log
│           └── android_element_not_found.log
├── logs/                                        # Local sample logs (gitignored)
├── build.gradle.kts
├── settings.gradle.kts
└── SPEC.md                                      # This file
```

---

## Core Architecture

The **shrinking AI layer** principle is a first-class architectural constraint:

```
LogFile
   │
   ▼
Parser (platform detection → ParsedLog)
   │
   ▼
ClassifierPipeline:
  1. DeterministicClassifier   ← always runs first, zero LLM cost
       │  hit → LogAnalysis (high confidence, classifiedBy=DETERMINISTIC)
       │  miss ↓
  2. LlmClassifier (Koog Agent) ← only when deterministic fails
       │
       ▼
  LogAnalysis (lower confidence, classifiedBy=LLM)
   │
   ▼
JsonReportWriter → stdout / file
```

---

## Domain Model

```kotlin
enum class Platform { IOS, ANDROID }

enum class FailureCategory {
    ASSERTION_FAILURE, ELEMENT_NOT_FOUND, TIMEOUT,
    APP_CRASH, NETWORK_ERROR, FLAKY_TEST, ENVIRONMENT_ISSUE, UNKNOWN
}

enum class ClassifierSource { DETERMINISTIC, LLM }

data class LogAnalysis(
    val platform: Platform,
    val failureCategory: FailureCategory,
    val confidence: Float,          // 0.0–1.0
    val classifiedBy: ClassifierSource,
    val evidence: List<String>,     // log excerpts supporting the diagnosis
    val recommendedAction: String,
    val rawSummary: String
)
```

---

## Code Style

Idiomatic Kotlin — no Java-isms. Extension functions, expression bodies, `when` as expression.
Naming: `PascalCase` classes, `camelCase` functions/vals, `SCREAMING_SNAKE` constants.

---

## Testing Strategy

- **Framework:** JUnit 5 + Kotest assertions
- **Unit tests:** every deterministic rule has a positive + negative fixture test
- **Integration tests:** LLM path tested with mocked Ollama (no real network in CI)
- **Fixtures:** real anonymised log snippets in `src/test/kotlin/fixtures/`
- **Coverage:** deterministic classifier 100%; agent layer 80%+
- **Naming:** `given_xctest_assertion_failure_when_classified_then_returns_assertion_category()`

---

## Boundaries

**Always:**
- Deterministic classifier runs before any LLM call
- `LogAnalysis` always includes `evidence` (empty only when category is UNKNOWN)
- `classifiedBy` always populated — transparency on what made the call
- New log patterns get a fixture test before a rule is written

**Ask first:**
- Adding a new `FailureCategory`
- Changing the `LogAnalysis` model shape
- Introducing a new Gradle dependency

**Never:**
- Call Ollama on every analysis — LLM is fallback only
- Commit real production log files
- Return `UNKNOWN` without populating `rawSummary`

---

## Task List

### Stage 1 — Foundation
- [x] Task 1: Domain model (`model/` package)
- [ ] Task 2: Log parser — platform detection
- [ ] Task 3: Log parser — text extraction → ParsedLog
- [ ] Task 4: Deterministic classifier — iOS rules (3 minimum)
- [ ] Task 5: Deterministic classifier — Android rules (3 minimum)
- [ ] Task 6: Deterministic classifier — pipeline entry point
- [ ] Task 7: Report writer — JSON serialisation
- [ ] Task 8: CLI entry point — `analyse --file` wiring

### Stage 2 — AI Layer
- [ ] Task 9a: Koog agent scaffolding + prompt
- [ ] Task 9b: JSON response parser + correction
- [ ] Task 10: LLM classifier
- [ ] Task 11: Classifier pipeline composition

### Stage 3 — Hardening
- [ ] Task 12: Fixture tests for all deterministic rules
- [ ] Task 13: Mock-LLM integration test
- [ ] Task 14: CLI polish (`--output`, `--platform`, error messages)

---

## Success Criteria for v1

- [ ] `analyse --file ios_failure.log` produces valid JSON `LogAnalysis`
- [ ] `analyse --file android_failure.log` produces valid JSON `LogAnalysis`
- [ ] Deterministic classifier handles at least 3 categories per platform
- [ ] LLM fallback fires only when no deterministic rule matches
- [ ] `classifiedBy` correctly identifies who made the call
- [ ] All deterministic rules have passing fixture tests
- [ ] No Ollama call made when a deterministic rule matches (verifiable via test)
