# log-analyst-koog

## What Is This Project

A command-line tool that reads a mobile test failure log — iOS XCUITest or Android Espresso — and produces a structured diagnosis: what went wrong, why, and what to do about it.
It exists because reading raw test logs takes time; this tool gives you the root cause in one command.
Classification works rule-first: fast, deterministic pattern matching fires before any AI is involved, so the LLM (Ollama running locally) is only consulted when the rules don't cover the failure.

---

## How To Run

### Prerequisites

| Requirement | Notes |
|-------------|-------|
| Java 17+ | `java -version` to check |
| Gradle | Bundled via `./gradlew` — no separate install needed |
| Ollama | Only needed for unrecognised log patterns. [Install Ollama](https://ollama.com), then run `ollama pull qwen2.5-coder:7b` |

### Build

```bash
./gradlew build
```

### Command Examples

```bash
# a. Analyse an iOS log file — result printed to stdout
./gradlew run --args="analyse --file /path/to/ios_test.log"

# b. Analyse an Android log file — result printed to stdout
./gradlew run --args="analyse --file /path/to/android_test.log"

# c. Save the result to a JSON file instead of printing it
./gradlew run --args="analyse --file /path/to/test.log --output /tmp/result.json"

# d. Override the auto-detected platform (useful when detection is ambiguous)
./gradlew run --args="analyse --file /path/to/test.log --platform ios"

# e. Skip deterministic rules and send the log directly to the LLM (requires Ollama running)
./gradlew run --args="analyse --file /path/to/test.log --force-llm"
```

### Successful Output

```json
{
    "platform": "IOS",
    "failureCategory": "ASSERTION_FAILURE",
    "confidence": 1.0,
    "classifiedBy": "DETERMINISTIC",
    "evidence": [
        "LoginUITests.swift:42: error: XCTAssertEqual failed: (\"Welcome, User!\") is not equal to (\"Welcome, Admin!\")"
    ],
    "recommendedAction": "Check the assertion values and verify the expected test state matches actual app state",
    "rawSummary": "XCTest assertion failed — actual value did not match expected value"
}
```

`classifiedBy` tells you whether a rule matched (`DETERMINISTIC`) or the LLM was used (`LLM`). `evidence` contains the exact log lines that led to the diagnosis.

### Error Output

```
Error: File not found: /path/to/missing.log
```

All error messages go to stderr; only the JSON result goes to stdout, so you can pipe the output safely.

**Exit codes:** `0` success · `1` bad arguments · `2` no rule matched · `3` output file write failure

---

## Project Structure

```
src/main/kotlin/
├── Main.kt                          # Entry point — delegates to AnalyseCommand
├── cli/
│   └── AnalyseCommand.kt            # Argument parsing and command dispatch
├── model/
│   ├── Platform.kt                  # IOS | ANDROID
│   ├── FailureCategory.kt           # 8 failure categories (ASSERTION_FAILURE, TIMEOUT, …)
│   ├── ClassifierSource.kt          # DETERMINISTIC | LLM
│   └── LogAnalysis.kt               # The result type returned by every classifier
├── parser/
│   ├── LogParser.kt                 # Detects platform and extracts relevant lines
│   └── ParsedLog.kt                 # Output of the parser stage
├── classifier/
│   ├── ClassifierPipeline.kt        # Runs deterministic first; falls back to LLM on miss
│   ├── deterministic/
│   │   ├── DeterministicClassifier.kt  # Routes to the right rule classifier by platform
│   │   ├── IosRuleClassifier.kt        # Regex rules for XCUITest logs
│   │   └── AndroidRuleClassifier.kt    # Regex rules for Espresso / Logcat
│   └── llm/
│       └── LlmClassifier.kt            # Calls the Koog agent; parses its response
├── agent/
│   ├── LogAnalystAgent.kt           # Koog agent wired to local Ollama
│   ├── AnalysisPrompt.kt            # Prompt template — instructs the LLM on output shape
│   └── JsonResponseParser.kt        # Cleans up malformed LLM JSON before parsing
└── report/
    └── JsonReportWriter.kt          # Serialises LogAnalysis to pretty-printed JSON
```

---

## Tech Stack

| Concern | Technology | Version |
|---------|------------|---------|
| Language | Kotlin (JVM) | 2.3.10 |
| Build | Gradle (Kotlin DSL) | 9.0 |
| AI Framework | JetBrains Koog | 0.8.0 |
| LLM Backend | Ollama (`qwen2.5-coder:7b`) | local |
| Serialization | kotlinx-serialization-json | 1.8.1 |
| Coroutines | kotlinx-coroutines-core | 1.10.2 |
| Testing | JUnit 5 + Kotest assertions | 5.10.2 / 5.9.1 |
