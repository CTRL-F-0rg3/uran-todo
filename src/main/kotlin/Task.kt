import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class Task(
    val id: String,
    val title: String,
    var done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

object TaskStorage {
    private val file = File(System.getProperty("user.home"), ".local/share/uran/todo/data.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun load(): MutableList<Task> = loadFrom(file) ?: mutableListOf()
    fun save(tasks: List<Task>) = saveTo(file, tasks)

    fun loadFrom(target: File): MutableList<Task>? {
        return try {
            if (target.exists()) {
                json.decodeFromString<MutableList<Task>>(target.readText()).toMutableList()
            } else null
        } catch (_: Exception) { null }
    }

    fun saveTo(target: File, tasks: List<Task>) {
        try {
            target.parentFile?.mkdirs()
            target.writeText(json.encodeToString(tasks))
        } catch (_: Exception) {}
    }

    private val numberRe = Regex("^\\d+\\.\\s+(.+)$")

    fun parseTxt(source: File): MutableList<Task> {
        val tasks = mutableListOf<Task>()
        var section = ""
        var currentText: StringBuilder? = null
        var afterBlank = true

        fun flush() {
            currentText?.let { sb ->
                val text = sb.toString().trim()
                if (text.isNotBlank()) {
                    val title = if (section.isNotBlank()) "$section: $text" else text
                    tasks.add(Task(id = UUID.randomUUID().toString(), title = title))
                }
            }
            currentText = null
        }

        source.forEachLine { rawLine ->
            val line = rawLine.trim()

            if (line.isEmpty() || line.all { it in "-=_*─ " }) {
                flush()
                afterBlank = true
                return@forEachLine
            }

            val m = numberRe.find(line)
            if (m != null) {
                flush()
                currentText = StringBuilder(m.groupValues[1])
                afterBlank = false
            } else {
                if (currentText != null) {
                    currentText!!.append(" ").append(line)
                } else if (afterBlank) {
                    section = line
                    afterBlank = false
                } else {
                    section = if (section.isBlank()) line else "$section $line"
                }
            }
        }
        flush()
        return tasks
    }
}
