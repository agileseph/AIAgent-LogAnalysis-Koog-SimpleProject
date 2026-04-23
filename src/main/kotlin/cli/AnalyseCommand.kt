package cli

import classifier.deterministic.DeterministicClassifier
import model.Platform
import parser.LogParser
import report.JsonReportWriter
import kotlin.system.exitProcess

class AnalyseCommand(
    private val exitFn: (Int) -> Unit = ::exitProcess
) {

    fun execute(args: Array<String>) {
        val parsed = parseArgs(args)

        val filePath = parsed["--file"]
        if (filePath == null) {
            System.err.println("Usage: analyse --file <path> [--output <path>] [--platform ios|android]")
            exitFn(1)
            return
        }

        val log = LogParser().parse(filePath).let { parsedLog ->
            val override = parsed["--platform"]?.let { platformArg(it) }
            if (override != null) parsedLog.copy(platform = override) else parsedLog
        }

        val analysis = DeterministicClassifier().classify(log)
        if (analysis == null) {
            System.err.println("No deterministic rule matched. LLM fallback not yet implemented.")
            exitFn(2)
            return
        }

        val writer = JsonReportWriter()
        val outputPath = parsed["--output"]
        if (outputPath != null) {
            writer.writeToFile(analysis, outputPath)
        } else {
            println(writer.write(analysis))
        }

        System.err.println("Analysis complete.")
    }

    private fun parseArgs(args: Array<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var i = 0
        while (i < args.size) {
            val key = args[i]
            if (key.startsWith("--") && i + 1 < args.size) {
                result[key] = args[i + 1]
                i += 2
            } else {
                i++
            }
        }
        return result
    }

    private fun platformArg(value: String): Platform = when (value.lowercase()) {
        "ios" -> Platform.IOS
        "android" -> Platform.ANDROID
        else -> {
            System.err.println("Unknown platform '$value'. Expected ios or android.")
            exitFn(1)
            throw IllegalArgumentException("Unknown platform: $value")
        }
    }
}
