package hu.nutrinaplo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.nutrinaplo.app.core.Calc
import hu.nutrinaplo.app.core.DiaryEntry
import hu.nutrinaplo.app.core.Meals
import hu.nutrinaplo.app.core.Nutrients
import hu.nutrinaplo.app.core.Status
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

val mealIcons: List<ImageVector> = listOf(
    Icons.Filled.FreeBreakfast,
    Icons.Filled.BakeryDining,
    Icons.Filled.LunchDining,
    Icons.Filled.Icecream,
    Icons.Filled.DinnerDining,
    Icons.Filled.Cookie,
)

val mealColors: List<Color> = listOf(
    Color(0xFFF5A524), Color(0xFF8B6CF2), Color(0xFF12A15F),
    Color(0xFFEF5D60), Color(0xFF2F9BF5), Color(0xFFE879A9),
)

private val dayLetters = listOf("H", "K", "Sze", "Cs", "P", "Szo", "V")

private const val WEEKS_BACK = 260

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onAdd: (Int) -> Unit,
    onOpenNutrients: () -> Unit,
    onReminders: () -> Unit,
) {
    val data by vm.data.collectAsState()
    val date by vm.date.collectAsState()
    val p = data.profile
    val dayKey = date.toString()
    val entries = remember(data.entries, dayKey) { data.entries.filter { it.date == dayKey } }
    val loggedDays = remember(data.entries) { data.entries.map { it.date }.toHashSet() }
    val totals = remember(entries) { Calc.totals(entries) }
    val targets = remember(p) { Calc.targets(p) }
    val kcalGoal = Calc.kcalTarget(p)
    val macros = Calc.macroTargets(p, kcalGoal)
    val water = data.water[dayKey] ?: 0
    val waterGoal = Calc.waterGoal(p)
    var editing by remember { mutableStateOf<DiaryEntry?>(null) }
    var pickDate by remember { mutableStateOf(false) }
    var showDay by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Header(
                name = p.name,
                date = date,
                onCalendar = { pickDate = true },
                onToday = { vm.setDate(LocalDate.now()) },
            )
        }
        item {
            WeekStrip(selected = date, loggedDays = loggedDays, onSelect = { vm.setDate(it) })
        }
        item {
            HeroCard(
                onClick = { showDay = true },
                eaten = totals.kcal,
                goal = kcalGoal,
                protein = totals.v(Nutrients.PROTEIN) to macros.protein,
                carbs = totals.v(Nutrients.CARBS) to macros.carbs,
                fat = totals.v(Nutrients.FAT) to macros.fat,
            )
        }
        item {
            WaterCard(
                water = water,
                goal = waterGoal,
                glassMl = p.glassMl,
                onAdd = { vm.addWater(p.glassMl) },
                onRemove = { vm.addWater(-p.glassMl) },
                remindersOn = data.reminders.enabled && data.reminders.waterEnabled,
                onReminders = onReminders,
            )
        }
        item {
            VitaminCard(
                hasEntries = entries.isNotEmpty(),
                totals = totals,
                targets = targets,
                coverage = totals.microCoveragePct,
                onClick = onOpenNutrients,
            )
        }
        item {
            Text(
                "Étkezések",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Meals.names.forEachIndexed { mi, mealName ->
            item(key = "meal$mi") {
                MealCard(
                    index = mi,
                    name = mealName,
                    target = kcalGoal * Meals.shares[mi],
                    entries = entries.filter { it.meal == mi },
                    onAdd = { onAdd(mi) },
                    onCopyYesterday = { vm.copyMealFromYesterday(mi) },
                    onEntry = { editing = it },
                    onDelete = { e ->
                        vm.deleteEntry(e.id)
                        vm.toast("${e.food.name} törölve")
                    },
                )
            }
        }
    }

    if (showDay) {
        DaySheet(
            date = date,
            entries = entries,
            onDismiss = { showDay = false },
            onEntry = { showDay = false; editing = it },
        )
    }

    editing?.let { e ->
        FoodSheet(
            food = e.food,
            initialGrams = e.grams,
            initialMeal = e.meal,
            isFavorite = data.favorites.any { it.id == e.food.id },
            confirmLabel = "Mentés",
            onDismiss = { editing = null },
            onConfirm = { g, m -> vm.updateEntry(e.id, g, m); editing = null },
            onToggleFavorite = { vm.toggleFavorite(e.food) },
            onDelete = { vm.deleteEntry(e.id); editing = null },
        )
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Mégse") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun Header(name: String, date: LocalDate, onCalendar: () -> Unit, onToday: () -> Unit) {
    val today = LocalDate.now()
    val greeting = when (java.time.LocalTime.now().hour) {
        in 4..9 -> "Jó reggelt"
        in 10..17 -> "Szia"
        else -> "Jó estét"
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                if (name.isBlank()) "$greeting!" else "$greeting, ${name.trim()}!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val month = date.month.getDisplayName(TextStyle.FULL, HU)
            val dow = date.dayOfWeek.getDisplayName(TextStyle.FULL, HU)
            Text(
                (if (date == today) "Ma · " else if (date == today.minusDays(1)) "Tegnap · " else "") +
                    "$month ${date.dayOfMonth}., $dow",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (date != today) {
            IconButton(onClick = onToday) { Icon(Icons.Filled.Today, contentDescription = "Ugrás a mai napra") }
        }
        FilledTonalIconButton(onClick = onCalendar) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = "Naptár")
        }
    }
}

/** Heti naptársáv: lapozható hetek, a mai hét az utolsó oldal. */
@Composable
private fun WeekStrip(selected: LocalDate, loggedDays: Set<String>, onSelect: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val selMonday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    fun pageOf(monday: LocalDate) =
        (WEEKS_BACK - 1 - ChronoUnit.WEEKS.between(monday, thisMonday).toInt()).coerceIn(0, WEEKS_BACK - 1)

    val pager = rememberPagerState(initialPage = pageOf(selMonday)) { WEEKS_BACK }
    LaunchedEffect(selMonday) {
        val target = pageOf(selMonday)
        if (pager.currentPage != target) pager.animateScrollToPage(target)
    }

    HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth()) { page ->
        val monday = thisMonday.minusWeeks((WEEKS_BACK - 1 - page).toLong())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0..6) {
                val d = monday.plusDays(i.toLong())
                val isSel = d == selected
                val isToday = d == today
                val future = d.isAfter(today)
                val logged = loggedDays.contains(d.toString())
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !future) { onSelect(d) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        dayLetters[i],
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(6.dp))
                    val bg = when {
                        isSel -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    }
                    val fg = when {
                        isSel -> MaterialTheme.colorScheme.onPrimary
                        future -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(bg)
                            .then(
                                if (isToday && !isSel) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${d.dayOfMonth}", color = fg, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (logged) AppColors.Kcal else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    onClick: () -> Unit,
    eaten: Double,
    goal: Int,
    protein: Pair<Double, Double>,
    carbs: Pair<Double, Double>,
    fat: Pair<Double, Double>,
) {
    val remaining = goal - eaten
    val over = remaining < 0
    val gradient = Brush.linearGradient(listOf(Color(0xFF12A15F), Color(0xFF0B7F86)))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(
                    fraction = (eaten / goal).toFloat(),
                    color = if (over) Color(0xFFFFC2B3) else Color.White,
                    track = Color.White.copy(alpha = 0.22f),
                    stroke = 12.dp,
                    modifier = Modifier.size(140.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            fmt(kotlin.math.abs(remaining), 0),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            if (over) "kcal felett" else "kcal maradt",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroStat("Napi cél", "${fmt(goal.toDouble(), 0)} kcal")
                    HeroStat("Elfogyasztva", "${fmt(eaten, 0)} kcal")
                    HeroStat("Teljesítve", "${(eaten / goal * 100).roundToInt()}%")
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeroMacro("Fehérje", protein, Modifier.weight(1f))
                HeroMacro("Szénhidrát", carbs, Modifier.weight(1f))
                HeroMacro("Zsír", fat, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Mai ételek megtekintése",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun HeroMacro(label: String, v: Pair<Double, Double>, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
        Spacer(Modifier.height(5.dp))
        Bar(
            if (v.second > 0) (v.first / v.second).toFloat() else 0f,
            Color.White,
            height = 6.dp,
            track = Color.White.copy(alpha = 0.22f),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "${fmt(v.first, 0)} / ${fmt(v.second, 0)} g",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
private fun WaterCard(
    water: Int,
    goal: Int,
    glassMl: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    remindersOn: Boolean,
    onReminders: () -> Unit,
) {
    val pct = (water * 100 / goal.coerceAtLeast(1))
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.WaterDrop, AppColors.Water)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Víz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${fmt(water / 1000.0, 2)} / ${fmt(goal / 1000.0, 2)} liter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                "$pct%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.Water,
            )
            IconButton(onClick = onReminders) {
                Icon(
                    if (remindersOn) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                    contentDescription = "Emlékeztetők",
                    tint = if (remindersOn) AppColors.Water else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Bar((water.toDouble() / goal.coerceAtLeast(1)).toFloat(), AppColors.Water, height = 10.dp)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.OutlinedButton(
                onClick = onRemove,
                enabled = water > 0,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("$glassMl ml", maxLines = 1)
            }
            androidx.compose.material3.Button(
                onClick = onAdd,
                modifier = Modifier.weight(2f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AppColors.Water, contentColor = Color.White),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("1 pohár ($glassMl ml)", maxLines = 1)
            }
        }
    }
}

@Composable
private fun VitaminCard(
    hasEntries: Boolean,
    totals: hu.nutrinaplo.app.core.DayTotals,
    targets: Map<String, hu.nutrinaplo.app.core.Target>,
    coverage: Double,
    onClick: () -> Unit,
) {
    val defs = remember(totals, targets) { Calc.deficiencies(totals, targets) }
    val micros = remember(totals, targets) {
        Calc.statuses(totals, targets, Nutrients.micros).filter { it.target?.isLimit == false }
    }
    AppCard(Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.CheckCircle, AppColors.Ok)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Vitaminok és ásványi anyagok", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (!hasEntries) "Rögzíts ételt, és megmutatjuk, mi hiányzik." else "Koppints a részletes elemzésért",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
        if (hasEntries) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountChip(micros.count { it.status == Status.OK }, "rendben", AppColors.Ok, Modifier.weight(1f))
                CountChip(micros.count { it.status == Status.LOW }, "alacsony", AppColors.Low, Modifier.weight(1f))
                CountChip(micros.count { it.status == Status.DEFICIENT }, "hiányos", AppColors.Deficient, Modifier.weight(1f))
            }
            if (defs.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Legnagyobb hiány: " + defs.take(3).joinToString(", ") { "${it.def.label.substringBefore(" (")} ${it.pct.roundToInt()}%" },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (coverage < 85) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "A kalóriák ${coverage.roundToInt()}%-ához van vitaminadat – a hiány valójában kisebb lehet.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CountChip(n: Int, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$n", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun IconBadge(icon: ImageVector, color: Color, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size((size * 0.55).dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealCard(
    index: Int,
    name: String,
    target: Double,
    entries: List<DiaryEntry>,
    onAdd: () -> Unit,
    onCopyYesterday: () -> Unit,
    onEntry: (DiaryEntry) -> Unit,
    onDelete: (DiaryEntry) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val kcal = entries.sumOf { it.amount(Nutrients.KCAL) }
    val color = mealColors[index]
    AppCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(mealIcons[index], color, 44)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${fmt(kcal, 0)} / ${fmt(target, 0)} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Továbbiak") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Másolás tegnapról") },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        onClick = { menu = false; onCopyYesterday() },
                    )
                }
            }
            FilledIconButton(
                onClick = onAdd,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Icon(Icons.Filled.Add, contentDescription = "Hozzáadás: $name") }
        }
        if (entries.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Bar((kcal / target.coerceAtLeast(1.0)).toFloat(), color, height = 4.dp)
            Spacer(Modifier.height(6.dp))
            entries.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                key(e.id) {
                    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
                        if (v == SwipeToDismissBoxValue.EndToStart) {
                            onDelete(e); true
                        } else false
                    })
                    SwipeToDismissBox(
                        state = state,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize().clip(MaterialTheme.shapes.small).background(AppColors.Deficient.copy(alpha = 0.15f))
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) { Icon(Icons.Filled.Delete, contentDescription = "Törlés", tint = AppColors.Deficient) }
                        },
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable { onEntry(e) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(e.food.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${fmt(e.grams, 0)} g" + (e.food.brand?.let { " · $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "${e.amount(Nutrients.KCAL).roundToInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(
    date: LocalDate,
    entries: List<DiaryEntry>,
    onDismiss: () -> Unit,
    onEntry: (DiaryEntry) -> Unit,
) {
    val sheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val t = remember(entries) { Calc.totals(entries) }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    if (date == LocalDate.now()) "Mai ételek" else "${date.monthValue}. ${date.dayOfMonth}. ételei",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${fmt(t.kcal, 0)} kcal · F ${fmt(t.v(Nutrients.PROTEIN), 0)} g · Sz ${fmt(t.v(Nutrients.CARBS), 0)} g · Zs ${fmt(t.v(Nutrients.FAT), 0)} g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            if (entries.isEmpty()) {
                item { EmptyHint("Ezen a napon még nincs rögzített étel. Az étkezéseknél a + gombbal adhatsz hozzá.") }
            }
            Meals.names.forEachIndexed { mi, name ->
                val list = entries.filter { it.meal == mi }.sortedBy { it.time }
                if (list.isNotEmpty()) {
                    item(key = "h$mi") {
                        Row(Modifier.padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(mealIcons[mi], mealColors[mi], 28)
                            Spacer(Modifier.width(8.dp))
                            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(
                                "${fmt(list.sumOf { it.amount(Nutrients.KCAL) }, 0)} kcal",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    list.forEach { e ->
                        item(key = "e${e.id}") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onEntry(e) }
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(e.food.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val time = if (e.time > 0) java.time.Instant.ofEpochMilli(e.time)
                                        .atZone(java.time.ZoneId.systemDefault()).toLocalTime().let { "%02d:%02d · ".format(it.hour, it.minute) } else ""
                                    Text(
                                        time + "${fmt(e.grams, 0)} g · F ${fmt(e.amount(Nutrients.PROTEIN), 0)} · Sz ${fmt(e.amount(Nutrients.CARBS), 0)} · Zs ${fmt(e.amount(Nutrients.FAT), 0)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text("${e.amount(Nutrients.KCAL).roundToInt()} kcal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
