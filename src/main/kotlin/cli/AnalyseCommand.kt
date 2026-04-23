package cli

import classifier.deterministic.DeterministicClassifier
import model.FailureCategory
import model.Platform
import parser.LogParser
import parser.UnknownPlatformException
import report.JsonReportWriter
import kotlin.system.exitProcess

private const val USAGE = "Usage: analyse --file <path> [--output <path>] [--platform ios|android]"

class AnalyseCommand(
    private val exitFn: (Int) -> Unit = ::exitProcess
) {

    fun execute(args: Array<String>) {
        val parsed = parseArgs(args)

        val filePath = parsed["--file"]
        if (filePath == null) {
            System.err.println("Error: --file is required.\n$USAGE")
            exitFn(1)
            return
        }

        val log = try {
            LogParser().parse(filePath).let { parsedLog ->
                val override = parsed["--platform"]?.let { platformArg(it) ?: return }
                if (override != null) parsedLog.copy(platform = override) else parsedLog
            }
        } catch (e: IllegalArgumentException) {
            System.err.println("Error: File not found: $filePath")
            exitFn(1)
            return
        } catch (e: UnknownPlatformException) {
            System.err.println("Error: ${e.message}")
            exitFn(1)
            return
        }

        val analysis = DeterministicClassifier().classify(log)
        if (analysis == null) {
            System.err.println("No rule matched. LLM fallback not yet implemented. Category: ${FailureCategory.UNKNOWN}")
            exitFn(2)
            return
        }

        val writer = JsonReportWriter()
        val outputPath = parsed["--output"]
        if (outputPath != null) {
            try {
                writer.writeToFile(analysis, outputPath)
                System.err.println("Report written to $outputPath")
            } catch (e: Exception) {
                System.err.println("Error: Failed to write output file: ${e.message}")
                exitFn(3)
                return
            }
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

    /** Returns the Platform, or null if invalid (error already printed, exitFn called). */
    private fun platformArg(value: String): Platform? = when (value.lowercase()) {
        "ios" -> Platform.IOS
        "android" -> Platform.ANDROID
        else -> {
            System.err.println("Invalid platform: $value. Use ios or android.")
            exitFn(1)
            null
        }
    }
}
