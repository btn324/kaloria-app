package hu.nutrinaplo.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.nutrinaplo.app.core.NutrientStatus
import hu.nutrinaplo.app.core.Status
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

val HU: Locale = Locale("hu", "HU")

/** Szám formázása magyarul (tizedesvessző), felesleges nullák nélkül. */
fun fmt(v: Double, decimals: Int = 1): String {
    if (decimals <= 0 || abs(v) >= 100) {
        return "%,d".format(HU, v.roundToInt()).replace(' ', ' ').replace(' ', ' ')
    }
    val s = String.format(HU, "%.${decimals}f", v)
    return if (s.contains(',')) s.trimEnd('0').trimEnd(',') else s
}

fun fmtG(v: Double) = fmt(v, 1) + " g"

fun dateLabel(d: LocalDate): String {
    val today = LocalDate.now()
    return when (d) {
        today -> "Ma"
        today.minusDays(1) -> "Tegnap"
        today.plusDays(1) -> "Holnap"
        else -> {
            val dow = d.dayOfWeek.getDisplayName(TextStyle.FULL, HU).replaceFirstChar { it.uppercase() }
            "${d.monthValue}. ${d.dayOfMonth}. – $dow"
        }
    }
}

fun shortDay(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.SHORT, HU).take(2).replaceFirstChar { it.uppercase() }

fun statusColor(s: Status): Color = when (s) {
    Status.DEFICIENT -> AppColors.Deficient
    Status.LOW -> AppColors.Low
    Status.OK -> AppColors.Ok
    Status.HIGH -> AppColors.High
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** Kerek, lekerekített végű folyamatjelző. */
@Composable
fun Bar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(anim)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

/** Kalória gyűrű. */
@Composable
fun Ring(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 14.dp,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit = {},
) {
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(800), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val d = size.minDimension - sw
            val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 135f, 270f, false, topLeft, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
            if (anim > 0f) {
                drawArc(color, 135f, 270f * anim, false, topLeft, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

@Composable
fun MacroColumn(label: String, value: Double, target: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Bar(if (target > 0) (value / target).toFloat() else 0f, color, height = 6.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            "${fmt(value, 0)} / ${fmt(target, 0)} g",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun Dot(color: Color, size: Dp = 10.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun NutrientRow(s: NutrientStatus, modifier: Modifier = Modifier, showBar: Boolean = true) {
    val color = statusColor(s.status)
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.def.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val tgt = s.target
            Text(
                buildString {
                    append(fmt(s.amount, s.def.decimals))
                    if (tgt != null) append(" / ").append(fmt(tgt.amount, s.def.decimals))
                    append(" ").append(s.def.unit)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tgt != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "${s.pct.roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(48.dp),
                    maxLines = 1,
                )
            }
        }
        if (showBar && s.target != null) {
            Spacer(Modifier.height(4.dp))
            Bar((s.pct / 100.0).toFloat(), color, height = 5.dp)
        }
    }
}

/** Egyszerű oszlopdiagram célvonallal. */
@Composable
fun BarChart(
    values: List<Double>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    goal: Double? = null,
    highlightLast: Boolean = true,
) {
    val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(goal ?: 0.0).coerceAtLeast(1.0) * 1.1
    val goalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val faded = color.copy(alpha = 0.45f)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val bw = slot * 0.56f
            values.forEachIndexed { i, v ->
                val h = (v / max * size.height).toFloat()
                val x = i * slot + (slot - bw) / 2
                val c = if (!highlightLast || i == values.lastIndex) color else faded
                if (h > 0f) {
                    drawRoundRect(
                        color = c,
                        topLeft = Offset(x, size.height - h),
                        size = Size(bw, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(bw / 3, bw / 3),
                    )
                }
            }
            if (goal != null && goal > 0) {
                val y = (size.height - goal / max * size.height).toFloat()
                val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                drawLine(goalColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f, pathEffect = dash)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

/** Vonaldiagram (pl. testsúly). */
@Composable
fun LineChart(values: List<Double>, color: Color, modifier: Modifier = Modifier, goal: Double? = null) {
    if (values.isEmpty()) return
    val lo0 = values.min().coerceAtMost(goal ?: Double.MAX_VALUE)
    val hi0 = values.max().coerceAtLeast(goal ?: -Double.MAX_VALUE)
    val pad = ((hi0 - lo0) * 0.15).coerceAtLeast(0.5)
    val lo = lo0 - pad
    val hi = hi0 + pad
    val goalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = color.copy(alpha = 0.12f)
    Canvas(modifier.fillMaxWidth().height(160.dp)) {
        fun y(v: Double) = (size.height - (v - lo) / (hi - lo) * size.height).toFloat()
        val n = values.size
        fun x(i: Int) = if (n == 1) size.width / 2 else i * size.width / (n - 1)
        if (goal != null) {
            val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            drawLine(goalColor, Offset(0f, y(goal)), Offset(size.width, y(goal)), strokeWidth = 2f, pathEffect = dash)
        }
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
        if (n > 1) {
            val area = Path()
            area.addPath(path)
            area.lineTo(x(n - 1), size.height)
            area.lineTo(x(0), size.height)
            area.close()
            drawPath(area, fill)
        }
        drawPath(path, color, style = Stroke(width = 5f, cap = StrokeCap.Round))
        values.forEachIndexed { i, v -> drawCircle(color, radius = 7f, center = Offset(x(i), y(v))) }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun KeyValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
