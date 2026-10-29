package hu.nutrinaplo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.nutrinaplo.app.core.Calc
import hu.nutrinaplo.app.core.DiaryEntry
import hu.nutrinaplo.app.core.Meals
import hu.nutrinaplo.app.core.Nutrients
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onAdd: (Int) -> Unit,
    onOpenNutrients: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val data by vm.data.collectAsState()
    val date by vm.date.collectAsState()
    val p = data.profile
    val dayKey = date.toString()
    val entries = remember(data.entries, dayKey) { data.entries.filter { it.date == dayKey } }
    val totals = remember(entries) { Calc.totals(entries) }
    val targets = remember(p) { Calc.targets(p) }
    val kcalGoal = Calc.kcalTarget(p)
    val macros = Calc.macroTargets(p, kcalGoal)
    val water = data.water[dayKey] ?: 0
    val waterGoal = Calc.waterGoal(p)
    var editing by remember { mutableStateOf<DiaryEntry?>(null) }
    var pickDate by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // fejléc: dátumváltó
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { vm.shiftDate(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Előző nap")
                }
                Column(
                    Modifier.weight(1f).clickable { pickDate = true },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(dateLabel(date), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${date.year}. ${date.monthValue}. ${date.dayOfMonth}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { vm.shiftDate(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Következő nap")
                }
            }
        }

        if (!p.setupDone) {
            item {
                AppCard(Modifier.clickable { onOpenProfile() }) {
                    Text("Üdv a NutriNaplóban! 👋", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Állítsd be a profilodat (nem, kor, magasság, súly, cél), hogy pontos kalória-, makró- és vitamincélokat számoljunk. Koppints ide.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // kalória kártya
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        StatLine("Bevitt", "${fmt(totals.kcal, 0)} kcal")
                        Spacer(Modifier.height(10.dp))
                        StatLine("Cél", "${fmt(kcalGoal.toDouble(), 0)} kcal")
                    }
                    val remaining = kcalGoal - totals.kcal
                    val over = remaining < 0
                    Ring(
                        fraction = (totals.kcal / kcalGoal).toFloat(),
                        color = if (over) AppColors.Deficient else AppColors.Kcal,
                        modifier = Modifier.size(150.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                fmt(kotlin.math.abs(remaining), 0),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (over) "kcal túllépés" else "kcal maradt",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MacroColumn("Fehérje", totals.v(Nutrients.PROTEIN), macros.protein, AppColors.Protein, Modifier.weight(1f))
                    MacroColumn("Szénhidrát", totals.v(Nutrients.CARBS), macros.carbs, AppColors.Carbs, Modifier.weight(1f))
                    MacroColumn("Zsír", totals.v(Nutrients.FAT), macros.fat, AppColors.Fat, Modifier.weight(1f))
                }
            }
        }

        // víz
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = AppColors.Water)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Vízfogyasztás", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${fmt(water / 1000.0, 2)} / ${fmt(waterGoal / 1000.0, 2)} liter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalIconButton(onClick = { vm.addWater(-p.glassMl) }, enabled = water > 0) {
                        Icon(Icons.Filled.Remove, contentDescription = "Pohár levonása")
                    }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalIconButton(onClick = { vm.addWater(p.glassMl) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Pohár hozzáadása")
                    }
                }
                Spacer(Modifier.height(10.dp))
                val glasses = (waterGoal + p.glassMl - 1) / p.glassMl
                val drunk = water / p.glassMl
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until glasses.coerceAtMost(16)) {
                        Icon(
                            if (i < drunk) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            tint = if (i < drunk) AppColors.Water else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f).size(22.dp).clickable {
                                vm.addWater((i + 1) * p.glassMl - water)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalDrink, contentDescription = null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "1 pohár = ${p.glassMl} ml · koppints egy cseppre a gyors beállításhoz",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // tápanyag gyorsjelentés
        item {
            val defs = Calc.deficiencies(totals, targets)
            val severe = defs.filter { it.status == hu.nutrinaplo.app.core.Status.DEFICIENT }
            AppCard(Modifier.clickable { onOpenNutrients() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Vitaminok és ásványi anyagok", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                entries.isEmpty() -> "Még nincs bejegyzés ezen a napon."
                                defs.isEmpty() -> "Minden tápanyag megfelelő szinten van. 🎉"
                                else -> "${severe.size} hiányos, ${defs.size - severe.size} alacsony – koppints a részletekért"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
                if (entries.isNotEmpty() && defs.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    defs.take(4).forEach { s ->
                        NutrientRow(s)
                    }
                }
            }
        }

        // étkezések
        Meals.names.forEachIndexed { mi, mealName ->
            item(key = "meal$mi") {
                val list = entries.filter { it.meal == mi }
                MealCard(
                    name = mealName,
                    entries = list,
                    onAdd = { onAdd(mi) },
                    onCopyYesterday = { vm.copyMealFromYesterday(mi) },
                    onEntry = { editing = it },
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    editing?.let { e ->
        FoodSheet(
            food = e.food,
            initialGrams = e.grams,
            initialMeal = e.meal,
            isFavorite = vm.isFavorite(e.food),
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
            dismissButton = {
                TextButton(onClick = { vm.setDate(LocalDate.now()); pickDate = false }) { Text("Ma") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MealCard(
    name: String,
    entries: List<DiaryEntry>,
    onAdd: () -> Unit,
    onCopyYesterday: () -> Unit,
    onEntry: (DiaryEntry) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val kcal = entries.sumOf { it.amount(Nutrients.KCAL) }
    AppCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (entries.isNotEmpty()) {
                    val p = entries.sumOf { it.amount(Nutrients.PROTEIN) }
                    val c = entries.sumOf { it.amount(Nutrients.CARBS) }
                    val f = entries.sumOf { it.amount(Nutrients.FAT) }
                    Text(
                        "${fmt(kcal, 0)} kcal · F ${fmt(p, 0)} g · Sz ${fmt(c, 0)} g · Zs ${fmt(f, 0)} g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
            FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Hozzáadás: $name") }
        }
        if (entries.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            entries.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().clickable { onEntry(e) }.padding(horizontal = 4.dp, vertical = 10.dp),
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
                    Text("${e.amount(Nutrients.KCAL).roundToInt()} kcal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
