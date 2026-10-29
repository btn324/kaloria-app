package hu.nutrinaplo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.nutrinaplo.app.core.Food
import hu.nutrinaplo.app.core.Nutrients
import hu.nutrinaplo.app.core.Serving

@Composable
fun CustomFoodScreen(vm: MainViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var servingName by remember { mutableStateOf("") }
    var servingGrams by remember { mutableStateOf("") }
    var saltText by remember { mutableStateOf("") }
    var showMicros by remember { mutableStateOf(false) }
    val values = remember { mutableStateMapOf<String, String>() }

    val mainKeys = listOf(
        Nutrients.KCAL, Nutrients.PROTEIN, Nutrients.CARBS, Nutrients.SUGAR,
        Nutrients.FIBER, Nutrients.FAT, Nutrients.SATFAT,
    )
    val canSave = name.isNotBlank() && parseNum(values[Nutrients.KCAL] ?: "") != null

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader("Saját étel", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Írd be a csomagoláson lévő tápértéktáblázat 100 g-ra vonatkozó adatait.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(name, { name = it }, label = { Text("Megnevezés *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(brand, { brand = it }, label = { Text("Márka") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    barcode, { barcode = it.filter { c -> c.isDigit() } }, label = { Text("Vonalkód") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
            }
            SectionTitle("Tápérték / 100 g")
            mainKeys.forEach { k ->
                val d = Nutrients.byKey.getValue(k)
                NutrientInput(d.label + if (k == Nutrients.KCAL) " *" else "", d.unit, values[k] ?: "") { values[k] = it }
            }
            NutrientInput("Só", "g", saltText) { saltText = it }

            SectionTitle("Adag (opcionális)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(servingName, { servingName = it }, label = { Text("pl. 1 szelet") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    servingGrams, { servingGrams = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Tömeg") }, suffix = { Text("g") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                )
            }

            TextButton(onClick = { showMicros = !showMicros }) {
                Text(if (showMicros) "Vitaminok és ásványi anyagok elrejtése" else "Vitaminok és ásványi anyagok megadása (opcionális)")
            }
            if (showMicros) {
                Nutrients.micros.filter { it.key != Nutrients.SODIUM }.forEach { d ->
                    NutrientInput(d.label, d.unit, values[d.key] ?: "") { values[d.key] = it }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                val per100 = HashMap<String, Double>()
                for ((k, v) in values) parseNum(v)?.let { per100[k] = it }
                parseNum(saltText)?.let { per100[Nutrients.SODIUM] = it / 2.5 * 1000.0 }
                val sg = parseNum(servingGrams)
                val servings = if (sg != null && sg > 0) listOf(Serving(servingName.ifBlank { "1 adag" }, sg)) else emptyList()
                vm.saveCustomFood(
                    Food(
                        id = "",
                        name = name.trim(),
                        category = "Saját étel",
                        per100 = per100,
                        servings = servings,
                        source = "custom",
                        brand = brand.trim().ifBlank { null },
                        barcode = barcode.ifBlank { null },
                    )
                )
                onBack()
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
        ) { Text("Mentés", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun NutrientInput(label: String, unit: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(8)) },
        label = { Text(label) },
        suffix = { Text(unit) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
