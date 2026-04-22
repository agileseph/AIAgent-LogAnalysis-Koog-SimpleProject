# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build                    # Compile and build
./gradlew test                     # Run all tests
./gradlew test --tests "parser.LogParserTest"              # Run a single test class
./gradlew test --tests "parser.LogParserTest.given*"       # Run tests matching a pattern
./gradlew run --args="analyse --file /path/to/test.log"    # Run the CLI
./gradlew run --args="analyse --file /path/to/test.log --output result.json"
```

## Architecture

This is a CLI tool that analyzes mobile test failure logs (iOS XCUITest + Android Espresso) and classifies failures. The core design principle is **"shrinking AI layer"**: deterministic rules fire first (zero LLM cost), with the Koog/Ollama agent as fallback only when rules don't match.

### Data Flow

```
LogFile → [LogParser] → ParsedLog → [ClassifierPipeline]
                                          ├── DeterministicClassifier (rules-first)
                                          │     ├── IosRuleClassifier
                                          │     └── AndroidRuleClassifier
                                          └── LlmClassifier (fallback via Koog → Ollama)
                                    → LogAnalysis → [JsonReportWriter] → stdout / file
```

### Key Packages

- **`model/`** — Domain types: `Platform` (IOS/ANDROID), `FailureCategory` (8 types), `ClassifierSource` (DETERMINISTIC/LLM), `LogAnalysis` (result with confidence, evidence, recommendedAction)
- **`parser/`** — `LogParser` detects platform via signal scoring and extracts relevant lines (noise-filtered, ≤50 lines). Returns `ParsedLog`.
- **`classifier/deterministic/`** — `DeterministicClassifier` routes to `IosRuleClassifier` / `AndroidRuleClassifier` by platform; each applies regex rules and returns `LogAnalysis?` (null = no match).
- **`agent/`** — `LogAnalystAgent` wraps Koog framework talking to Ollama (`qwen2.5-coder:7b`). `AnalysisPrompt` builds the prompt. `JsonResponseParser` handles malformed JSON (strips markdown fences, fixes trailing commas).
- **`classifier/`** — `ClassifierPipeline` composes deterministic → LLM: tries deterministic, calls LLM only on null result.
- **`report/`** — `JsonReportWriter` serializes `LogAnalysis` via `kotlinx.serialization`.
- **`cli/`** — `AnalyseCommand` parses CLI args; `Main.kt` is the entry point.

### Testing

Tests use JUnit 5 + Kotest assertions. Test fixtures (sample `.log` files) live in `src/test/kotlin/fixtures/`. New classifier tests should cover the real fixture logs end-to-end as well as unit-test individual rules.

### Dependencies

- `ai.koog:koog-agents:0.8.0` — JetBrains Koog AI agent framework
- `kotlinx-serialization-json` — JSON output
- `kotlinx-coroutines-core` — async/suspend support (Koog is coroutine-based)
- Requires a running **Ollama** instance with `qwen2.5-coder:7b` pulled for LLM path