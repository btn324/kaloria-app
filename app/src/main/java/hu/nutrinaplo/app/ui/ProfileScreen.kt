package hu.nutrinaplo.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import hu.nutrinaplo.app.core.Calc
import hu.nutrinaplo.app.core.Goal
import hu.nutrinaplo.app.core.Profile
import hu.nutrinaplo.app.core.Sex
import java.time.LocalDate
import kotlin.math.roundToInt

private val activities = listOf(
    1.2 to "Ülő életmód (irodai munka, kevés mozgás)",
    1.375 to "Enyhén aktív (heti 1–3 edzés)",
    1.55 to "Mérsékelten aktív (heti 3–5 edzés)",
    1.725 to "Nagyon aktív (heti 6–7 edzés)",
    1.9 to "Extrém aktív (fizikai munka + edzés)",
)

/** Szám beviteli mező, ami csak érvényes értéknél frissít. */
@Composable
fun NumberField(
    label: String,
    value: Double,
    suffix: String,
    modifier: Modifier = Modifier,
    decimals: Int = 0,
    onValue: (Double) -> Unit,
) {
    var text by remember(value) { mutableStateOf(if (value == 0.0) "" else fmt(value, decimals).replace(" ", "")) }
    OutlinedTextField(
        value = text,
        onValueChange = { s ->
            text = s.filter { it.isDigit() || it == ',' || it == '.' }.take(7)
            parseNum(text)?.let(onValue) ?: run { if (text.isEmpty()) onValue(0.0) }
        },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel) {
    val data by vm.data.collectAsState()
    val p = data.profile
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    fun set(np: Profile) = vm.updateProfile(np.copy(setupDone = true))
    var showKey by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        if (u != null) {
            try {
                context.contentResolver.openOutputStream(u)?.use { it.write(vm.exportJson().toByteArray(Charsets.UTF_8)) }
                vm.toast("Mentés elkészült.")
            } catch (e: Exception) {
                vm.toast("Mentés sikertelen: ${e.message}")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            try {
                val txt = context.contentResolver.openInputStream(u)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (txt != null && vm.importJson(txt)) vm.toast("Adatok visszatöltve.")
            } catch (e: Exception) {
                vm.toast("Visszatöltés sikertelen: ${e.message}")
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Profil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            p.name.trim().take(1).uppercase().ifBlank { "🙂" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name.ifBlank { "Névtelen" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${if (p.sex == Sex.MALE) "Férfi" else "Nő"} · ${p.age} év · ${fmt(p.heightCm, 0)} cm · ${fmt(p.weightKg, 1)} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { vm.updateProfile(p.copy(setupDone = false)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Beállítóvarázsló újraindítása")
                }
            }
        }

        // összefoglaló
        item {
            val kcal = Calc.kcalTarget(p)
            val mac = Calc.macroTargets(p, kcal)
            val bmi = Calc.bmi(p)
            AppCard {
                Text("Napi célok", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                KeyValue("Kalória", "$kcal kcal")
                KeyValue("Fehérje / Szénhidrát / Zsír", "${mac.protein.roundToInt()} / ${mac.carbs.roundToInt()} / ${mac.fat.roundToInt()} g")
                KeyValue("Víz", "${fmt(Calc.waterGoal(p) / 1000.0, 2)} l")
                KeyValue("Alapanyagcsere (BMR)", "${Calc.bmr(p).roundToInt()} kcal")
                KeyValue("Napi energiaigény (TDEE)", "${Calc.tdee(p).roundToInt()} kcal")
                KeyValue("BMI", "${fmt(bmi, 1)} – ${Calc.bmiLabel(bmi)}")
            }
        }

        item { SectionTitle("Adataid") }
        item {
            AppCard {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(Sex.MALE to "Férfi", Sex.FEMALE to "Nő").forEachIndexed { i, (s, l) ->
                        SegmentedButton(
                            selected = p.sex == s,
                            onClick = { set(p.copy(sex = s)) },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(l) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("Életkor", p.age.toDouble(), "év", Modifier.weight(1f)) {
                        if (it in 10.0..110.0) set(p.copy(age = it.roundToInt()))
                    }
                    NumberField("Magasság", p.heightCm, "cm", Modifier.weight(1f)) {
                        if (it in 100.0..250.0) set(p.copy(heightCm = it))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("Testsúly", p.weightKg, "kg", Modifier.weight(1f), decimals = 1) {
                        if (it in 30.0..300.0) {
                            set(p.copy(weightKg = it))
                        }
                    }
                    NumberField("Célsúly", p.targetWeightKg, "kg", Modifier.weight(1f), decimals = 1) {
                        if (it in 30.0..300.0) set(p.copy(targetWeightKg = it))
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { vm.addWeight(p.weightKg, LocalDate.now()) }) {
                    Text("Mai súly rögzítése a statisztikába")
                }
            }
        }

        item { SectionTitle("Aktivitás") }
        item {
            AppCard(padding = 8.dp) {
                activities.forEach { (v, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { set(p.copy(activity = v)) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = p.activity == v, onClick = { set(p.copy(activity = v)) })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item { SectionTitle("Cél") }
        item {
            AppCard {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(Goal.LOSE to "Fogyás", Goal.MAINTAIN to "Szinten tartás", Goal.GAIN to "Tömegnövelés")
                        .forEachIndexed { i, (g, l) ->
                            SegmentedButton(
                                selected = p.goal == g,
                                onClick = { set(p.copy(goal = g)) },
                                shape = SegmentedButtonDefaults.itemShape(i, 3),
                            ) { Text(l, maxLines = 1) }
                        }
                }
                if (p.goal != Goal.MAINTAIN) {
                    Spacer(Modifier.height(10.dp))
                    Text("Tempó (kg / hét)", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.25, 0.5, 0.75, 1.0).forEach { r ->
                            FilterChip(
                                selected = p.weeklyRate == r,
                                onClick = { set(p.copy(weeklyRate = r)) },
                                label = { Text("${fmt(r, 2)} kg") },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                NumberField("Egyéni kalóriacél (opcionális)", p.kcalOverride.toDouble(), "kcal", Modifier.fillMaxWidth()) {
                    val v = it.roundToInt()
                    if (v == 0 || v in 800..10000) set(p.copy(kcalOverride = v))
                }
                Text(
                    "Ha üresen hagyod, a fenti adatok alapján számoljuk (Mifflin–St Jeor képlet).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { SectionTitle("Makrók aránya") }
        item {
            AppCard {
                MacroSlider("Fehérje", p.proteinPct, AppColors.Protein) { v ->
                    val rest = 100 - v
                    val carb = (rest * p.carbPct / (p.carbPct + p.fatPct).coerceAtLeast(1).toDouble()).roundToInt()
                    set(p.copy(proteinPct = v, carbPct = carb, fatPct = rest - carb))
                }
                MacroSlider("Szénhidrát", p.carbPct, AppColors.Carbs) { v ->
                    val fat = (100 - p.proteinPct - v).coerceAtLeast(0)
                    set(p.copy(carbPct = v.coerceAtMost(100 - p.proteinPct), fatPct = fat))
                }
                MacroSlider("Zsír", p.fatPct, AppColors.Fat) { v ->
                    val carb = (100 - p.proteinPct - v).coerceAtLeast(0)
                    set(p.copy(fatPct = v.coerceAtMost(100 - p.proteinPct), carbPct = carb))
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(false, onClick = { set(p.copy(proteinPct = 25, carbPct = 45, fatPct = 30)) }, label = { Text("Kiegyensúlyozott") })
                    FilterChip(false, onClick = { set(p.copy(proteinPct = 35, carbPct = 35, fatPct = 30)) }, label = { Text("Magas fehérje") })
                    FilterChip(false, onClick = { set(p.copy(proteinPct = 25, carbPct = 15, fatPct = 60)) }, label = { Text("Low-carb") })
                }
            }
        }

        item { SectionTitle("Víz") }
        item {
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("Napi vízcél (opcionális)", p.waterGoalOverride.toDouble(), "ml", Modifier.weight(1f)) {
                        val v = it.roundToInt()
                        if (v == 0 || v in 500..8000) set(p.copy(waterGoalOverride = v))
                    }
                    NumberField("Pohár mérete", p.glassMl.toDouble(), "ml", Modifier.weight(1f)) {
                        if (it >= 50 && it <= 1500) set(p.copy(glassMl = it.roundToInt()))
                    }
                }
                Text(
                    "Alapértelmezés: testsúly × 35 ml (${fmt(Calc.waterGoal(p.copy(waterGoalOverride = 0)) / 1000.0, 2)} l).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { SectionTitle("AI ételfelismerés (ingyenes)") }
        item {
            AppCard {
                Text(
                    "A fotós és szöveges ételfelismeréshez a Google Gemini ingyenes keretét használjuk. " +
                        "Ehhez egy saját, ingyenes API-kulcs kell (bankkártya nélkül):\n" +
                        "1. Nyisd meg: aistudio.google.com/apikey\n" +
                        "2. Jelentkezz be Google-fiókkal, majd „Create API key”.\n" +
                        "3. Másold be ide a kulcsot.\n" +
                        "Az ingyenes keretben a Google felhasználhatja a beküldött képeket a szolgáltatás fejlesztésére, és napi limit van.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { uri.openUri("https://aistudio.google.com/apikey") }) {
                    Text("Google AI Studio megnyitása")
                }
                OutlinedTextField(
                    value = p.geminiKey,
                    onValueChange = { set(p.copy(geminiKey = it.trim())) },
                    label = { Text("Gemini API-kulcs") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Megjelenítés")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                val testing by vm.aiTesting.collectAsState()
                val test by vm.aiTest.collectAsState()
                OutlinedButton(
                    onClick = { vm.testAi() },
                    enabled = p.geminiKey.isNotBlank() && !testing,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (testing) "Ellenőrzés…" else "Kulcs ellenőrzése") }
                test?.let { (ok, msg) ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ok) AppColors.Ok else AppColors.Deficient,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = p.geminiModel,
                    onValueChange = { set(p.copy(geminiModel = it.trim())) },
                    label = { Text("Modell") },
                    supportingText = { Text("Alapértelmezett: gemini-flash-latest. Ha nem elérhető vagy túlterhelt, az app automatikusan másik ingyenes modellt próbál.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item { SectionTitle("Adatok") }
        item {
            AppCard {
                Text(
                    "Minden adatod csak ezen a telefonon tárolódik. Készíts néha biztonsági mentést!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("nutrinaplo_mentes_${LocalDate.now()}.json") }, modifier = Modifier.weight(1f)) {
                        Text("Mentés fájlba")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.weight(1f)) {
                        Text("Visszatöltés")
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { confirmClear = true }) { Text("Napló törlése", color = AppColors.Deficient) }
            }
        }

        item {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("Adatforrások", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "• USDA National Nutrient Database for Standard Reference (SR28) – közkincs, magyarra fordítva.\n" +
                        "• Magyar ételek: a fenti alapanyagokból, receptek alapján számított értékek.\n" +
                        "• Open Food Facts – nyílt termékadatbázis (ODbL licenc).\n" +
                        "• Ajánlott napi bevitel: NIH / EFSA referenciaértékek.\n" +
                        "Az értékek tájékoztató jellegűek, nem helyettesítik a szakember tanácsát.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Napló törlése?") },
            text = { Text("Minden étel-, víz- és súlybejegyzés törlődik (a profil megmarad). Ez nem vonható vissza.") },
            confirmButton = {
                TextButton(onClick = { vm.clearAll(); confirmClear = false }) { Text("Törlés", color = AppColors.Deficient) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Mégse") } },
        )
    }
}

@Composable
private fun MacroSlider(label: String, value: Int, color: androidx.compose.ui.graphics.Color, onChange: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(color, 8.dp)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("$value%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 5f..80f,
            steps = 74,
        )
    }
}
