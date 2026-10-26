import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val UranBackground = Color(0xFF0D0714)
val UranSurface   = Color(0xFF1A1124)
val UranSurface2  = Color(0xFF241833)
val UranPink      = Color(0xFFFF2E88)
val UranCyan      = Color(0xFF05D9E8)
val UranText      = Color(0xFFE8E0F0)
val UranTextDim   = Color(0xFF9A8FA8)

private val DarkColors = darkColorScheme(
    primary = UranPink,
    onPrimary = Color.White,
    secondary = UranCyan,
    background = UranBackground,
    onBackground = UranText,
    surface = UranSurface,
    onSurface = UranText,
    surfaceVariant = UranSurface2,
    onSurfaceVariant = UranTextDim,
)

@Composable
fun UranTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
