package hu.nutrinaplo.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import hu.nutrinaplo.app.core.Calc
import hu.nutrinaplo.app.core.NutrientStatus
import hu.nutrinaplo.app.core.Nutrients
import hu.nutrinaplo.app.core.Status
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutrientsScreen(vm: MainViewModel) {
    val data by vm.data.collectAsState()
    val date by vm.date.collectAsState()
    val db by vm.db.collectAsState()
    var period by rememberSaveable { mutableIntStateOf(0) }
    val targets = remember(data.profile) { Calc.targets(data.profile) }

    val entries = remember(data.entries, date, period) {
        if (period == 0) data.entries.filter { it.date == date.toString() }
        else {
            val from = date.minusDays(6).toString()
            val to = date.toString()
            data.entries.filter { it.date >= from && it.date <= to }
        }
    }
    val totals = remember(entries, period) { if (period == 0) Calc.totals(entries) else Calc.averageTotals(entries) }
    val deficits = remember(totals, targets) { Calc.deficiencies(totals, targets) }
    val excess = remember(totals, targets) { Calc.excesses(totals, targets) }
    val days = entries.map { it.date }.distinct().size

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Tápanyag-elemzés", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(dateLabel(date), "Utolsó 7 nap átlaga").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = period == i,
                        onClick = { period = i },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(label, maxLines = 1) }
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                AppCard {
                    Text("Nincs adat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Rögzíts ételeket a Naplóban, és itt láthatod, mely vitaminokból és ásványi anyagokból kapsz elég, és miből van hiányod.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@LazyColumn
        }

        // összefoglaló
        item {
            val micros = Calc.statuses(totals, targets, Nutrients.micros).filter { it.target?.isLimit == false }
            val ok = micros.count { it.status == Status.OK }
            val low = micros.count { it.status == Status.LOW }
            val bad = micros.count { it.status == Status.DEFICIENT }
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryNumber(ok, "megfelelő", AppColors.Ok, Modifier.weight(1f))
                    SummaryNumber(low, "alacsony", AppColors.Low, Modifier.weight(1f))
                    SummaryNumber(bad, "hiányos", AppColors.Deficient, Modifier.weight(1f))
                }
                if (period == 1) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$days nap adatai alapján (csak a naplózott napok számítanak).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (totals.microCoveragePct < 85) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = AppColors.Low, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "A bevitt kalóriák csak ${totals.microCoveragePct.roundToInt()}%-ához van teljes vitaminadat " +
                                "(bolti termékeknél, saját ételeknél és AI becsléseknél gyakran hiányzik). " +
                                "Emiatt egyes hiányok a valóságosnál nagyobbnak tűnhetnek.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (deficits.isNotEmpty()) {
            item { SectionTitle("Mire figyelj – hiányzó tápanyagok") }
            deficits.forEach { s ->
                item(key = "def" + s.def.key) {
                    DeficitCard(s, suggestions = remember(db, s.def.key) {
                        db?.let { Calc.suggestions(it.foods, s.def.key, 6) } ?: emptyList()
                    })
                }
            }
        } else {
            item {
                AppCard {
                    Text("Szuper! 🎉", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Minden vitaminból és ásványi anyagból elérted az ajánlott napi bevitel legalább 90%-át.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (excess.isNotEmpty()) {
            item { SectionTitle("Túllépett határértékek") }
            item {
                AppCard {
                    excess.forEach { NutrientRow(it) }
                    Text(
                        "Ezeknél a felső határ alatt érdemes maradni (pl. nátrium = só, telített zsír).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionTitle("Makrotápanyagok") }
        item {
            AppCard { Calc.statuses(totals, targets, Nutrients.all.filter { it.group == hu.nutrinaplo.app.core.NutrientGroup.MACRO }).forEach { NutrientRow(it) } }
        }
        item { SectionTitle("Vitaminok") }
        item { AppCard { Calc.statuses(totals, targets, Nutrients.vitamins).forEach { NutrientRow(it) } } }
        item { SectionTitle("Ásványi anyagok") }
        item { AppCard { Calc.statuses(totals, targets, Nutrients.minerals).forEach { NutrientRow(it) } } }
        item {
            Text(
                "Az ajánlott értékek felnőttekre vonatkozó általános referenciaértékek (NIH/EFSA), a profilodban megadott nem és életkor alapján. " +
                    "Nem helyettesítik az orvosi vagy dietetikusi tanácsot.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun SummaryNumber(n: Int, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DeficitCard(s: NutrientStatus, suggestions: List<hu.nutrinaplo.app.core.Food>) {
    var open by remember { mutableStateOf(false) }
    AppCard(Modifier.clickable { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { NutrientRow(s) }
            Spacer(Modifier.width(4.dp))
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        Pill(s.status.label, statusColor(s.status))
        AnimatedVisibility(open) {
            Column {
                if (s.def.info.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(s.def.info, style = MaterialTheme.typography.bodyMedium)
                }
                val tgt = s.target
                if (tgt != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hiányzik még: ${fmt((tgt.amount - s.amount).coerceAtLeast(0.0), s.def.decimals)} ${s.def.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Jó források (100 g-ban):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    suggestions.forEachIndexed { i, f ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val v = f.value(s.def.key)
                        val pct = if (tgt != null && tgt.amount > 0) (v / tgt.amount * 100).roundToInt() else 0
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(f.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(
                                "${fmt(v, s.def.decimals)} ${s.def.unit} · $pct%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
