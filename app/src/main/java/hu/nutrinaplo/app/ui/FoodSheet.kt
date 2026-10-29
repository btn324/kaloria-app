package hu.nutrinaplo.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.nutrinaplo.app.core.Food
import hu.nutrinaplo.app.core.Meals
import hu.nutrinaplo.app.core.NutrientGroup
import hu.nutrinaplo.app.core.Nutrients

fun sourceLabel(f: Food): String = when (f.source) {
    "off" -> "Open Food Facts"
    "custom" -> "Saját étel"
    "ai" -> "AI becslés"
    else -> if (f.id.startsWith("r")) "Magyar recept (számított)" else "USDA adatbázis"
}

fun parseNum(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

/**
 * Étel részletei + mennyiség megadása. Hozzáadáshoz és meglévő bejegyzés szerkesztéséhez is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSheet(
    food: Food,
    initialGrams: Double?,
    initialMeal: Int,
    isFavorite: Boolean,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (grams: Double, meal: Int) -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultGrams = initialGrams ?: food.servings.firstOrNull()?.grams ?: 100.0
    var gramsText by remember(food.id) { mutableStateOf(fmt(defaultGrams, 1)) }
    var meal by remember(food.id) { mutableStateOf(initialMeal) }
    var showAll by remember { mutableStateOf(false) }
    val grams = parseNum(gramsText) ?: 0.0
    fun amt(key: String) = food.value(key) * grams / 100.0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(food.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    val sub = listOfNotNull(food.brand, sourceLabel(food)).joinToString(" · ")
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onToggleFavorite != null) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Kedvenc",
                            tint = if (isFavorite) AppColors.Deficient else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // mennyiség
            OutlinedTextField(
                value = gramsText,
                onValueChange = { gramsText = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(7) },
                label = { Text("Mennyiség") },
                suffix = { Text("g") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val opts = (listOf("100 g" to 100.0) + food.servings.map { "${it.name} (${fmt(it.grams, 0)} g)" to it.grams })
                    .distinctBy { it.first }
                opts.forEach { (label, g) ->
                    FilterChip(
                        selected = grams == g,
                        onClick = { gramsText = fmt(g, 1) },
                        label = { Text(label) },
                    )
                }
                listOf(0.5, 2.0).forEach { mul ->
                    FilterChip(
                        selected = false,
                        onClick = { if (grams > 0) gramsText = fmt(grams * mul, 1) },
                        label = { Text(if (mul < 1) "½×" else "2×") },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // étkezés
            Text("Étkezés", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Meals.names.forEachIndexed { i, n ->
                    FilterChip(selected = meal == i, onClick = { meal = i }, label = { Text(n) })
                }
            }
            Spacer(Modifier.height(16.dp))

            // összefoglaló
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MacroDonut(
                        food.value(Nutrients.PROTEIN), food.value(Nutrients.CARBS), food.value(Nutrients.FAT),
                        Modifier.size(56.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${fmt(amt(Nutrients.KCAL), 0)} kcal",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Kcal,
                        )
                        Text(
                            "${fmt(food.kcal, 0)} kcal / 100 g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacroStat("Fehérje", amt(Nutrients.PROTEIN), AppColors.Protein, Modifier.weight(1f))
                    MacroStat("Szénhidrát", amt(Nutrients.CARBS), AppColors.Carbs, Modifier.weight(1f))
                    MacroStat("Zsír", amt(Nutrients.FAT), AppColors.Fat, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showAll = !showAll }) {
                Text(if (showAll) "Részletes tápértékek elrejtése" else "Összes tápanyag és vitamin megjelenítése")
            }
            if (showAll) {
                AppCard {
                    if (!food.hasMicros) {
                        Text(
                            "Ehhez az ételhez nincs teljes vitamin- és ásványianyag-adat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    NutrientGroup.entries.forEach { g ->
                        val defs = Nutrients.all.filter { it.group == g && food.per100.containsKey(it.key) }
                        if (defs.isNotEmpty()) {
                            Text(g.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                            defs.forEach { d ->
                                KeyValue(d.label, "${fmt(amt(d.key), d.decimals.coerceAtLeast(1))} ${d.unit}")
                            }
                            HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onDelete != null) {
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f).height(52.dp)) { Text("Törlés") }
                }
                Button(
                    onClick = { onConfirm(grams, meal) },
                    enabled = grams > 0,
                    modifier = Modifier.weight(2f).height(52.dp),
                ) { Text(confirmLabel) }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun MacroStat(label: String, grams: Double, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(color, 8.dp)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(fmtG(grams), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
