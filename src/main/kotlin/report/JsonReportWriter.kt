package report

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.LogAnalysis
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

private val json = Json { prettyPrint = true }

class JsonReportWriter {

    fun write(analysis: LogAnalysis): String = json.encodeToString(analysis)

    fun writeToFile(analysis: LogAnalysis, path: String) {
        Path.of(path)
            .also { it.createParentDirectories() }
            .writeText(write(analysis))
    }
}
