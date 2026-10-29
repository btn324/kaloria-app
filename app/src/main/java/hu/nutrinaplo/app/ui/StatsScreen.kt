package hu.nutrinaplo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.nutrinaplo.app.core.Calc
import hu.nutrinaplo.app.core.Nutrients
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: MainViewModel) {
    val data by vm.data.collectAsState()
    val p = data.profile
    var range by rememberSaveable { mutableIntStateOf(0) }
    val nDays = if (range == 0) 7 else 30
    val today = LocalDate.now()
    val days = (nDays - 1 downTo 0).map { today.minusDays(it.toLong()) }
    val byDay = remember(data.entries) { data.entries.groupBy { it.date } }
    val kcalList = days.map { d -> byDay[d.toString()]?.sumOf { it.amount(Nutrients.KCAL) } ?: 0.0 }
    val waterList = days.map { d -> (data.water[d.toString()] ?: 0) / 1000.0 }
    val labels = days.mapIndexed { i, d ->
        if (nDays == 7) shortDay(d) else if (i % 5 == 0 || i == days.lastIndex) "${d.dayOfMonth}" else ""
    }
    val kcalGoal = Calc.kcalTarget(p)
    val logged = kcalList.filter { it > 0 }
    val avgKcal = if (logged.isEmpty()) 0.0 else logged.average()

    // sorozat: hány egymást követő napon volt bejegyzés (ma vagy tegnaptól visszafelé)
    val streak = remember(byDay) {
        var d = if (byDay.containsKey(today.toString())) today else today.minusDays(1)
        var n = 0
        while (byDay.containsKey(d.toString())) { n++; d = d.minusDays(1) }
        n
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Statisztika", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("7 nap", "30 nap").forEachIndexed { i, l ->
                    SegmentedButton(
                        selected = range == i,
                        onClick = { range = i },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(l) }
                }
            }
        }
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = AppColors.Kcal)
                    Spacer(Modifier.width(8.dp))
                    Text("$streak napos sorozat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Ennyi egymást követő napon rögzítettél ételt. Így tovább!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AppCard {
                Text("Kalóriabevitel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Átlag (naplózott napok): ${fmt(avgKcal, 0)} kcal · cél: $kcalGoal kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                BarChart(kcalList, labels, AppColors.Kcal, goal = kcalGoal.toDouble())
            }
        }
        item {
            val entries = data.entries.filter { it.date >= days.first().toString() }
            val avg = Calc.averageTotals(entries)
            val pk = avg.v(Nutrients.PROTEIN) * 4
            val ck = avg.v(Nutrients.CARBS) * 4
            val fk = avg.v(Nutrients.FAT) * 9
            val sum = (pk + ck + fk).coerceAtLeast(1.0)
            AppCard {
                Text("Átlagos makró-eloszlás", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                MacroShare("Fehérje", avg.v(Nutrients.PROTEIN), pk / sum, p.proteinPct, AppColors.Protein)
                MacroShare("Szénhidrát", avg.v(Nutrients.CARBS), ck / sum, p.carbPct, AppColors.Carbs)
                MacroShare("Zsír", avg.v(Nutrients.FAT), fk / sum, p.fatPct, AppColors.Fat)
                MacroShare("Rost", avg.v(Nutrients.FIBER), -1.0, -1, AppColors.Fiber)
            }
        }
        item {
            AppCard {
                Text("Vízfogyasztás (liter)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                BarChart(waterList, labels, AppColors.Water, goal = Calc.waterGoal(p) / 1000.0)
            }
        }
        item { WeightCard(vm) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MacroShare(label: String, grams: Double, share: Double, targetPct: Int, color: androidx.compose.ui.graphics.Color) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(color, 8.dp)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                if (share >= 0) "${fmt(grams, 0)} g · ${fmt(share * 100, 0)}% (cél $targetPct%)" else "${fmt(grams, 1)} g / nap",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (share >= 0) {
            Spacer(Modifier.height(4.dp))
            Bar(share.toFloat(), color, height = 6.dp)
        }
    }
}

@Composable
private fun WeightCard(vm: MainViewModel) {
    val data by vm.data.collectAsState()
    val p = data.profile
    var text by remember { mutableStateOf("") }
    val weights = data.weights.takeLast(60)
    AppCard {
        Text("Testsúly", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        val last = weights.lastOrNull()
        val first = weights.firstOrNull()
        Text(
            if (last == null) "Még nincs mérés. Add meg a mai súlyodat!"
            else {
                val diff = last.kg - (first?.kg ?: last.kg)
                "Jelenlegi: ${fmt(last.kg, 1)} kg · változás: ${if (diff > 0) "+" else ""}${fmt(diff, 1)} kg · cél: ${fmt(p.targetWeightKg, 1)} kg"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6) },
                label = { Text("Mai súly") },
                suffix = { Text("kg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Button(onClick = {
                val kg = parseNum(text)
                if (kg != null) {
                    vm.addWeight(kg)
                    text = ""
                }
            }, enabled = parseNum(text) != null) { Text("Mentés") }
        }
        if (weights.size >= 2) {
            Spacer(Modifier.height(14.dp))
            LineChart(weights.map { it.kg }, MaterialTheme.colorScheme.primary, goal = p.targetWeightKg)
        }
        if (weights.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            weights.takeLast(5).reversed().forEachIndexed { i, w ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(w.date.replace('-', '.') + ".", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("${fmt(w.kg, 1)} kg", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    IconButton(onClick = { vm.deleteWeight(w.date) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Törlés", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
