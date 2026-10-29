package hu.nutrinaplo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hu.nutrinaplo.app.core.Calc
import hu.nutrinaplo.app.core.Goal
import hu.nutrinaplo.app.core.Profile
import hu.nutrinaplo.app.core.Sex
import kotlin.math.roundToInt

private const val STEPS = 6

/** Első indításkor (vagy a Profilból újraindítva) végigvezet a beállításokon. */
@Composable
fun OnboardingScreen(initial: Profile, onFinish: (Profile) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(if (initial.name.isBlank() && !initial.setupDone) 0 else 1) }
    var p by remember { mutableStateOf(initial) }

    BackHandler(enabled = step > 0) { step-- }

    val canNext = when (step) {
        2 -> p.age in 10..110 && p.heightCm in 100.0..250.0 && p.weightKg in 30.0..300.0
        else -> true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding()
    ) {
        if (step > 0) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { step-- }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Vissza") }
                LinearProgressIndicator(
                    progress = { step / (STEPS - 1).toFloat() },
                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(16.dp))
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { s ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                when (s) {
                    0 -> Welcome()
                    1 -> StepNameSex(p) { p = it }
                    2 -> StepBody(p) { p = it }
                    3 -> StepGoal(p) { p = it }
                    4 -> StepActivity(p) { p = it }
                    else -> StepSummary(p)
                }
            }
        }
        Button(
            onClick = {
                if (step < STEPS - 1) step++ else onFinish(p.copy(setupDone = true))
            },
            enabled = canNext,
            modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp),
        ) {
            Text(
                when (step) {
                    0 -> "Kezdjük"
                    STEPS - 1 -> "Indulás"
                    else -> "Tovább"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(28.dp))
}

@Composable
private fun Welcome() {
    Spacer(Modifier.height(40.dp))
    Box(
        Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Spa, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
    }
    Spacer(Modifier.height(24.dp))
    Text("NutriNapló", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        "Kalóriák, makrók, vitaminok és víz – egy helyen, magyarul.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    Feature(Icons.Filled.Science, "Teljes vitaminelemzés", "Megmutatja, mely vitaminokból és ásványi anyagokból van hiányod, és mit egyél.")
    Feature(Icons.Filled.QrCodeScanner, "1500+ étel és vonalkód", "Magyar ételek, alapanyagok és milliónyi bolti termék.")
    Feature(Icons.Filled.PhotoCamera, "Fotó alapján", "Fotózd le a tányért, a többit az AI elvégzi.")
    Feature(Icons.Filled.WaterDrop, "Vízkövetés", "Egy koppintással rögzítheted, mennyit ittál.")
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        IconBadge(icon, MaterialTheme.colorScheme.primary, 44)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChoiceCard(
    selected: Boolean,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StepNameSex(p: Profile, onChange: (Profile) -> Unit) {
    StepTitle("Ismerkedjünk meg", "A nem és a kor alapján számoljuk a vitamin- és ásványianyag-szükségletet.")
    OutlinedTextField(
        value = p.name,
        onValueChange = { onChange(p.copy(name = it.take(30))) },
        label = { Text("Keresztneved (opcionális)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(p.sex == Sex.MALE, "Férfi", icon = Icons.Filled.Male, modifier = Modifier.weight(1f)) {
            onChange(p.copy(sex = Sex.MALE))
        }
        ChoiceCard(p.sex == Sex.FEMALE, "Nő", icon = Icons.Filled.Female, modifier = Modifier.weight(1f)) {
            onChange(p.copy(sex = Sex.FEMALE))
        }
    }
}

@Composable
private fun StepBody(p: Profile, onChange: (Profile) -> Unit) {
    StepTitle("Testadatok", "Ezekből számoljuk az alapanyagcserédet és a napi kalóriaigényedet.")
    BigNumberField("Életkor", p.age.toDouble(), "év") { onChange(p.copy(age = it.roundToInt())) }
    Spacer(Modifier.height(12.dp))
    BigNumberField("Magasság", p.heightCm, "cm") { onChange(p.copy(heightCm = it)) }
    Spacer(Modifier.height(12.dp))
    BigNumberField("Jelenlegi testsúly", p.weightKg, "kg", decimals = 1) { onChange(p.copy(weightKg = it)) }
}

@Composable
private fun BigNumberField(label: String, value: Double, suffix: String, decimals: Int = 0, onValue: (Double) -> Unit) {
    var text by remember { mutableStateOf(if (value > 0) fmt(value, decimals).replace(" ", "") else "") }
    OutlinedTextField(
        value = text,
        onValueChange = { s ->
            text = s.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
            onValue(parseNum(text) ?: 0.0)
        },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StepGoal(p: Profile, onChange: (Profile) -> Unit) {
    StepTitle("Mi a célod?", "Később a Profil fülön bármikor módosíthatod.")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(p.goal == Goal.LOSE, "Fogyás", "Kalóriadeficit, magas fehérjebevitel", Icons.Filled.TrendingDown) {
            onChange(p.copy(goal = Goal.LOSE, targetWeightKg = if (p.targetWeightKg >= p.weightKg) p.weightKg - 5 else p.targetWeightKg))
        }
        ChoiceCard(p.goal == Goal.MAINTAIN, "Súlytartás", "Egészséges, kiegyensúlyozott étrend", Icons.Filled.TrendingFlat) {
            onChange(p.copy(goal = Goal.MAINTAIN, targetWeightKg = p.weightKg))
        }
        ChoiceCard(p.goal == Goal.GAIN, "Izomtömeg-növelés", "Enyhe kalóriatöbblet", Icons.Filled.TrendingUp) {
            onChange(p.copy(goal = Goal.GAIN, targetWeightKg = if (p.targetWeightKg <= p.weightKg) p.weightKg + 3 else p.targetWeightKg))
        }
    }
    if (p.goal != Goal.MAINTAIN) {
        Spacer(Modifier.height(24.dp))
        BigNumberField("Célsúly", p.targetWeightKg, "kg", decimals = 1) { onChange(p.copy(targetWeightKg = it)) }
        Spacer(Modifier.height(16.dp))
        Text("Heti tempó", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0.25 to "Lassú", 0.5 to "Ajánlott", 0.75 to "Gyors").forEach { (r, l) ->
                RateTile(
                    selected = p.weeklyRate == r,
                    title = l,
                    value = "${fmt(r, 2)} kg/hét",
                    modifier = Modifier.weight(1f),
                ) { onChange(p.copy(weeklyRate = r)) }
            }
        }
    }
}

private val activityOptions = listOf(
    Triple(1.2, "Ülő", "Irodai munka, alig mozgok"),
    Triple(1.375, "Enyhén aktív", "Heti 1–3 edzés vagy sok séta"),
    Triple(1.55, "Aktív", "Heti 3–5 edzés"),
    Triple(1.725, "Nagyon aktív", "Heti 6–7 kemény edzés"),
    Triple(1.9, "Extrém", "Fizikai munka + napi edzés"),
)

@Composable
private fun StepActivity(p: Profile, onChange: (Profile) -> Unit) {
    StepTitle("Mennyit mozogsz?", "Egy átlagos hetedre gondolj.")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        activityOptions.forEach { (v, t, s) ->
            ChoiceCard(p.activity == v, t, s) { onChange(p.copy(activity = v)) }
        }
    }
}

@Composable
private fun StepSummary(p: Profile) {
    val kcal = Calc.kcalTarget(p)
    val m = Calc.macroTargets(p, kcal)
    StepTitle("A te napi terved", "Ezekre a célokra optimalizáljuk a naplót. Bármikor módosíthatod.")
    Box(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primaryContainer).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$kcal", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("kcal naponta", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryTile("Fehérje", "${m.protein.roundToInt()} g", AppColors.Protein, Modifier.weight(1f))
        SummaryTile("Szénhidrát", "${m.carbs.roundToInt()} g", AppColors.Carbs, Modifier.weight(1f))
        SummaryTile("Zsír", "${m.fat.roundToInt()} g", AppColors.Fat, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryTile("Víz", "${fmt(Calc.waterGoal(p) / 1000.0, 1)} l", AppColors.Water, Modifier.weight(1f))
        SummaryTile("BMI", fmt(Calc.bmi(p), 1), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
    }
    if (p.goal != Goal.MAINTAIN) {
        val diff = kotlin.math.abs(p.weightKg - p.targetWeightKg)
        val weeks = if (p.weeklyRate > 0) (diff / p.weeklyRate).roundToInt() else 0
        Spacer(Modifier.height(16.dp))
        Text(
            "Ezzel a tempóval kb. $weeks hét alatt éred el a ${fmt(p.targetWeightKg, 1)} kg-os célsúlyt.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun SummaryTile(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier.clip(MaterialTheme.shapes.large).background(color.copy(alpha = 0.12f)).padding(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RateTile(selected: Boolean, title: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
