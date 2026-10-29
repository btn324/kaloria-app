package hu.nutrinaplo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import hu.nutrinaplo.app.core.Food
import hu.nutrinaplo.app.core.Meals
import hu.nutrinaplo.app.core.Nutrients

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Vissza") }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onCustom: () -> Unit,
    onAiPhoto: () -> Unit,
    onAiText: () -> Unit,
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val data by vm.data.collectAsState()
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val online by vm.online.collectAsState()
    val onlineLoading by vm.onlineLoading.collectAsState()
    val meal by vm.meal.collectAsState()
    val db by vm.db.collectAsState()
    var selected by remember { mutableStateOf<Food?>(null) }
    var tab by remember { mutableIntStateOf(0) }

    fun scan() {
        val opts = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
            )
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, opts).startScan()
            .addOnSuccessListener { bc ->
                val code = bc.rawValue
                if (code.isNullOrBlank()) vm.toast("Nem sikerült beolvasni a vonalkódot.")
                else vm.lookupBarcode(code) { selected = it }
            }
            .addOnFailureListener { vm.toast("A vonalkód-olvasó nem érhető el: ${it.message}") }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader("Hozzáadás – ${Meals.names[meal]}", onBack)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Meals.names.forEachIndexed { i, n ->
                FilterChip(selected = meal == i, onClick = { vm.setMeal(i) }, label = { Text(n) })
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { vm.setQuery(it) },
            placeholder = { Text("Keresés, pl. csirkemell, túró, pörkölt…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Törlés")
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionChip("Vonalkód", Icons.Filled.QrCodeScanner) { scan() }
            ActionChip("Fotó (AI)", Icons.Filled.PhotoCamera, onAiPhoto)
            ActionChip("Leírás (AI)", Icons.Filled.AutoAwesome, onAiText)
            ActionChip("Saját étel", Icons.Filled.EditNote, onCustom)
        }
        Spacer(Modifier.height(8.dp))
        if (onlineLoading && query.isBlank()) {
            // vonalkód-keresés folyamatban
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Termék keresése…")
            }
        }

        if (query.isBlank()) {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                listOf("Legutóbbi", "Kedvencek", "Saját").forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                }
            }
            val list = when (tab) {
                0 -> data.recents
                1 -> data.favorites
                else -> data.customFoods
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (list.isEmpty()) {
                    item {
                        EmptyHint(
                            when (tab) {
                                0 -> "Itt jelennek meg a legutóbb hozzáadott ételeid. Kezdj el gépelni a kereséshez – több mint 1500 étel és magyar fogás közül választhatsz, vagy olvass be egy vonalkódot."
                                1 -> "Még nincs kedvenced. Egy étel részleteinél a szív ikonnal jelölheted meg."
                                else -> "Még nincs saját ételed. A „Saját étel” gombbal vehetsz fel újat (pl. a csomagolás tápértéktáblázata alapján)."
                            },
                            Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                items(list, key = { "l" + it.id }) { f -> FoodRow(f) { selected = f } }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (db == null) {
                    item {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Adatbázis betöltése…")
                        }
                    }
                }
                if (db != null && results.isEmpty()) {
                    item { EmptyHint("Nincs találat a beépített adatbázisban.", Modifier.padding(horizontal = 16.dp)) }
                }
                items(results, key = { "r" + it.id }) { f -> FoodRow(f) { selected = f } }
                item {
                    Column(Modifier.padding(16.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("Bolti termékek (Open Food Facts)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Több millió termék márkanévvel – a vitaminadatok itt gyakran hiányosak.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (onlineLoading) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else if (online.isEmpty()) {
                            OutlinedButton(onClick = { keyboard?.hide(); vm.searchOnline() }) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Keresés online: „$query”")
                            }
                        }
                    }
                }
                items(online, key = { "o" + it.id }) { f -> FoodRow(f) { selected = f } }
            }
        }
    }

    selected?.let { f ->
        FoodSheet(
            food = f,
            initialGrams = null,
            initialMeal = meal,
            isFavorite = data.favorites.any { it.id == f.id },
            confirmLabel = "Hozzáadás",
            onDismiss = { selected = null },
            onConfirm = { g, m ->
                vm.addEntry(f, g, m)
                vm.toast("${f.name} hozzáadva (${fmt(g, 0)} g) – ${Meals.names[m]}")
                selected = null
            },
            onToggleFavorite = { vm.toggleFavorite(f) },
        )
    }
}

@Composable
private fun ActionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
    )
}

@Composable
fun FoodRow(f: Food, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(f.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val sub = buildList {
                f.brand?.let { add(it) }
                add(f.category.ifBlank { sourceLabel(f) })
                add("F ${fmt(f.value(Nutrients.PROTEIN), 1)} · Sz ${fmt(f.value(Nutrients.CARBS), 1)} · Zs ${fmt(f.value(Nutrients.FAT), 1)}")
            }.joinToString(" · ")
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("${fmt(f.kcal, 0)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("kcal/100 g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Filled.AddCircleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
