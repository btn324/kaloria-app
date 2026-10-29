package hu.nutrinaplo.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import hu.nutrinaplo.app.core.Meals
import hu.nutrinaplo.app.core.Nutrients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/** Kép betöltése, kicsinyítése (max. 1024 px) és JPEG base64 kódolása. */
private fun loadScaled(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 1024 || bounds.outHeight / (sample * 2) >= 1024) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
    val max = maxOf(bmp.width, bmp.height)
    if (max <= 1024) return bmp
    val scale = 1024f / max
    return Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
}

private fun toBase64(bmp: Bitmap): String {
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

@Composable
fun AiScreen(vm: MainViewModel, photoMode: Boolean, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by vm.ai.collectAsState()
    val meal by vm.meal.collectAsState()
    val data by vm.data.collectAsState()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hint by remember { mutableStateOf("") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun analyzeBitmap(b: Bitmap) {
        scope.launch {
            val b64 = withContext(Dispatchers.Default) { toBase64(b) }
            vm.analyzeAi(hint.ifBlank { null }, b64)
        }
    }

    fun handleUri(uri: Uri) {
        scope.launch {
            val b = withContext(Dispatchers.IO) {
                try { loadScaled(context, uri) } catch (e: Exception) { null }
            }
            if (b == null) {
                vm.toast("A kép nem tölthető be.")
            } else {
                bitmap = b
                analyzeBitmap(b)
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = cameraUri
        if (ok && u != null) handleUri(u)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u ->
        if (u != null) handleUri(u)
    }

    fun openCamera() {
        try {
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val f = File(dir, "etel_${System.currentTimeMillis()}.jpg")
            val u = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
            cameraUri = u
            takePicture.launch(u)
        } catch (e: Exception) {
            vm.toast("A kamera nem indítható: ${e.message}")
        }
    }

    LaunchedEffect(Unit) {
        if (photoMode && bitmap == null && state is AiState.Idle && data.profile.geminiKey.isNotBlank()) openCamera()
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(if (photoMode) "Fotó alapján (AI)" else "Leírás alapján (AI)", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (data.profile.geminiKey.isBlank()) {
                AppCard {
                    Text("Hiányzik az API-kulcs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Az AI felismeréshez add meg az ingyenes Google Gemini API-kulcsodat a Profil fül „AI ételfelismerés” részében. Ott leírjuk, hogyan szerezheted meg pár kattintással.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (photoMode) {
                bitmap?.let { b ->
                    Image(
                        b.asImageBitmap(),
                        contentDescription = "Étel fotó",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large),
                    )
                }
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Megjegyzés (opcionális)") },
                    placeholder = { Text("pl. „a tányér 26 cm-es”, „light kóla”") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { openCamera() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Fotó")
                    }
                    OutlinedButton(
                        onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Galéria")
                    }
                }
                if (bitmap != null && state !is AiState.Loading) {
                    OutlinedButton(onClick = { bitmap?.let { analyzeBitmap(it) } }, modifier = Modifier.fillMaxWidth()) {
                        Text("Újraelemzés a megjegyzéssel")
                    }
                }
            } else {
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Mit ettél?") },
                    placeholder = { Text("pl. „2 tojásos rántotta, 1 szelet teljes kiőrlésű kenyér vajjal, egy bögre tejeskávé”") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { vm.analyzeAi(hint, null) },
                    enabled = hint.isNotBlank() && state !is AiState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Elemzés")
                }
            }

            when (val s = state) {
                is AiState.Idle -> {}
                is AiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Az AI elemzi az ételt…")
                }
                is AiState.Error -> AppCard {
                    Text("Hiba", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = AppColors.Deficient)
                    Text(s.message, style = MaterialTheme.typography.bodyMedium)
                }
                is AiState.Done -> {
                    if (s.note.isNotBlank()) {
                        Text(s.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (s.candidates.isEmpty()) {
                        EmptyHint("Nem ismertem fel ételt. Próbáld újra másik képpel vagy leírással.")
                    }
                    s.candidates.forEachIndexed { i, c -> CandidateCard(vm, i, c) }
                    if (s.candidates.isNotEmpty()) {
                        val total = s.candidates.filter { it.selected }.sumOf { it.food.value(Nutrients.KCAL) * it.grams / 100 }
                        Text("Összesen: ${fmt(total, 0)} kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Meals.names.forEachIndexed { mi, n ->
                                FilterChip(selected = meal == mi, onClick = { vm.setMeal(mi) }, label = { Text(n) })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        val done = state as? AiState.Done
        if (done != null && done.candidates.any { it.selected }) {
            Button(
                onClick = { vm.commitAi(meal); onDone() },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            ) { Text("Hozzáadás: ${Meals.names[meal]}", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun CandidateCard(vm: MainViewModel, index: Int, c: AiCandidate) {
    var gramsText by remember(c.item.name) { mutableStateOf(fmt(c.grams, 0)) }
    AppCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = c.selected, onCheckedChange = { vm.updateCandidate(index, c.copy(selected = it)) })
            Column(Modifier.weight(1f)) {
                Text(c.item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${fmt(c.food.value(Nutrients.KCAL) * c.grams / 100, 0)} kcal · " +
                        "F ${fmt(c.food.value(Nutrients.PROTEIN) * c.grams / 100, 0)} g · " +
                        "Sz ${fmt(c.food.value(Nutrients.CARBS) * c.grams / 100, 0)} g · " +
                        "Zs ${fmt(c.food.value(Nutrients.FAT) * c.grams / 100, 0)} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = gramsText,
                onValueChange = { t ->
                    gramsText = t.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
                    parseNum(gramsText)?.let { if (it > 0) vm.updateCandidate(index, c.copy(grams = it)) }
                },
                suffix = { Text("g") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(100.dp),
            )
        }
        val m = c.match
        if (m != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, top = 6.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (c.useMatch) "Adatbázisból: ${m.name}" else "AI becslés (adatbázis: ${m.name})",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        if (c.useMatch) "Teljes vitamin- és ásványianyag-adattal" else "Csak makrók (vitaminadat nélkül)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = c.useMatch, onCheckedChange = { vm.updateCandidate(index, c.copy(useMatch = it)) })
            }
        } else {
            Text(
                "Nincs adatbázis-találat – AI becslés (vitaminadat nélkül)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp),
            )
        }
    }
}
