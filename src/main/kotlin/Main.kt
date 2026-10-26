import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import java.awt.Frame
import java.awt.Point
import java.awt.Window
import java.awt.event.WindowEvent
import java.io.File
import java.util.UUID
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

enum class Filter(val label: String) { ALL("Wszystko"), ACTIVE("Aktywne"), DONE("Zrobione") }

fun main() {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Exception) {}

    application {
        val state = rememberWindowState(
            size = DpSize(720.dp, 680.dp),
            position = WindowPosition(Alignment.Center)
        )
        val icon = painterResource("icon.png")
        Window(
            onCloseRequest = ::exitApplication,
            title = "Uran To-Do",
            state = state,
            undecorated = true,
            resizable = false,
            icon = icon,
        ) {
            UranTheme { Root(window) }
        }
    }
}

@Composable
fun Root(window: Window) {
    var tasks by remember { mutableStateOf(TaskStorage.load()) }
    var filter by remember { mutableStateOf(Filter.ALL) }
    var input by remember { mutableStateOf("") }

    fun persist() {
        TaskStorage.save(tasks)
        tasks = tasks.toList().toMutableList()
    }

    fun addTask() {
        if (input.isBlank()) return
        tasks.add(Task(id = UUID.randomUUID().toString(), title = input.trim()))
        input = ""
        persist()
    }

    fun toggle(id: String) {
        tasks.find { it.id == id }?.let { it.done = !it.done }
        persist()
    }

    fun remove(id: String) {
        tasks.removeAll { it.id == id }
        persist()
    }

    fun doExport() {
        val chooser = JFileChooser()
        chooser.dialogTitle = "Eksportuj do pliku"
        chooser.selectedFile = File("uran-todo-backup.json")
        chooser.fileFilter = FileNameExtensionFilter("Lista zadań (*.txt, *.json)", "txt", "json")
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            var file = chooser.selectedFile
            if (!file.name.endsWith(".json")) {
                file = File(file.parentFile, file.name + ".json")
            }
            TaskStorage.saveTo(file, tasks)
        }
    }

    fun doImport() {
        val chooser = JFileChooser()
        chooser.dialogTitle = "Importuj z pliku"
        chooser.fileFilter = FileNameExtensionFilter("Lista zadań (*.txt, *.json)", "txt", "json")
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val chosen = chooser.selectedFile
            val loaded = when {
                chosen.name.lowercase().endsWith(".txt") -> TaskStorage.parseTxt(chosen)
                else -> TaskStorage.loadFrom(chosen)
            }
            if (loaded != null && loaded.isNotEmpty()) {
                tasks = loaded
                TaskStorage.save(tasks)
            }
        }
    }

    val visible = when (filter) {
        Filter.ALL -> tasks
        Filter.ACTIVE -> tasks.filter { !it.done }
        Filter.DONE -> tasks.filter { it.done }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UranBackground)
    ) {
        TitleBar(window, onExport = ::doExport, onImport = ::doImport)

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Uran To-Do", fontSize = 32.sp, color = UranPink)
            Text(
                "${tasks.count { !it.done }} aktywne · ${tasks.count { it.done }} zrobione",
                color = UranTextDim, fontSize = 13.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Filter.values().forEach { f ->
                    val selected = filter == f
                    Box(
                        modifier = Modifier
                            .background(
                                if (selected) UranPink.copy(alpha = 0.18f) else UranSurface2,
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) UranPink else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { filter = f }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            f.label,
                            color = if (selected) UranPink else UranTextDim,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(UranSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, UranPink.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                    textStyle = TextStyle(color = UranText, fontSize = 15.sp),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (input.isEmpty()) Text("Dodaj nowe zadanie...", color = UranTextDim, fontSize = 15.sp)
                        inner()
                    }
                )
                Button(
                    onClick = { addTask() },
                    colors = ButtonDefaults.buttonColors(containerColor = UranPink),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Dodaj", color = Color.White) }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visible, key = { it.id }) { task ->
                    TaskRow(task, onToggle = { toggle(task.id) }, onDelete = { remove(task.id) })
                }
            }
        }
    }
}

@Composable
fun TitleBar(window: Window, onExport: () -> Unit, onImport: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(38.dp).background(Color(0xFF0D0714)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(14.dp))
        TrafficLight(Color(0xFFFF5F57), onClick = {
            window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
        })
        Spacer(Modifier.width(8.dp))
        TrafficLight(Color(0xFFFEBC2E), onClick = {
            if (window is Frame) window.extendedState = Frame.ICONIFIED
        })
        Spacer(Modifier.width(8.dp))
        TrafficLight(Color(0xFF28C840), enabled = false, onClick = {})

        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .pointerInput(window) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        window.location = Point(
                            window.x + dragAmount.x.toInt(),
                            window.y + dragAmount.y.toInt()
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text("Uran To-Do", color = UranTextDim, fontSize = 12.sp)
        }

        TextButton(onClick = onExport) {
            Text("Eksportuj", color = UranTextDim, fontSize = 11.sp)
        }
        TextButton(onClick = onImport) {
            Text("Importuj", color = UranTextDim, fontSize = 11.sp)
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
fun TrafficLight(color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(if (enabled) color else color.copy(alpha = 0.35f), CircleShape)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
    )
}

@Composable
fun TaskRow(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(UranSurface, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (task.done) UranSurface2 else UranPink.copy(alpha = 0.25f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    if (task.done) UranCyan else Color.Transparent,
                    RoundedCornerShape(6.dp)
                )
                .border(
                    2.dp,
                    if (task.done) UranCyan else UranTextDim,
                    RoundedCornerShape(6.dp)
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (task.done) Text("✓", color = UranBackground, fontSize = 16.sp)
        }

        Text(
            task.title,
            modifier = Modifier.weight(1f),
            color = if (task.done) UranTextDim else UranText,
            fontSize = 15.sp
        )

        Text(
            "✕",
            color = UranTextDim.copy(alpha = 0.6f),
            fontSize = 18.sp,
            modifier = Modifier.clickable { onDelete() }.padding(4.dp)
        )
    }
}
