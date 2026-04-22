package report

import model.LogAnalysis

/**
 * Task 7: Serialises LogAnalysis to JSON.
 *
 * Output destination:
 *   - stdout (default)
 *   - file path (when --output flag provided)
 */
class JsonReportWriter {

    fun write(analysis: LogAnalysis): String {
        TODO("Task 7: implement kotlinx.serialization JSON output")
    }

    fun writeToFile(analysis: LogAnalysis, path: String) {
        TODO("Task 7: implement file output")
    }
}
