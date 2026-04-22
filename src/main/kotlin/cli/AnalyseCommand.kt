package cli

/**
 * CLI entry point for the `analyse` command.
 *
 * Task 8 placeholder — wired up once parser, classifier, and report writer are complete.
 *
 * Usage:
 *   analyse --file /path/to/test.log
 *   analyse --file /path/to/test.log --output result.json
 *   analyse --file /path/to/test.log --platform ios
 */
class AnalyseCommand {

    fun execute(args: Array<String>) {
        println("log-analyst-koog v1.0.0")
        println("Usage: analyse --file <path> [--output <path>] [--platform ios|android]")
        println()
        println("Implementation in progress — see task list in SPEC.md")
    }
}
