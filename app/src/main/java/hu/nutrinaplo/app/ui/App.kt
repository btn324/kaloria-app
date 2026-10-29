package hu.nutrinaplo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hu.nutrinaplo.app.core.Meals

object Routes {
    const val HOME = "home"
    const val NUTRIENTS = "nutrients"
    const val STATS = "stats"
    const val PROFILE = "profile"
    const val ADD = "add"
    const val CUSTOM = "custom"
    const val AI_PHOTO = "ai/photo"
    const val AI_TEXT = "ai/text"
}

private data class NavTab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val leftTabs = listOf(
    NavTab(Routes.HOME, "Napló", Icons.Outlined.Home, Icons.Filled.Home),
    NavTab(Routes.NUTRIENTS, "Tápanyagok", Icons.Outlined.Science, Icons.Filled.Science),
)
private val rightTabs = listOf(
    NavTab(Routes.STATS, "Haladás", Icons.Outlined.Insights, Icons.Filled.Insights),
    NavTab(Routes.PROFILE, "Profil", Icons.Outlined.Person, Icons.Filled.Person),
)

@Composable
fun NutriApp(vm: MainViewModel) {
    val loaded by vm.loaded.collectAsState()
    val data by vm.data.collectAsState()
    if (!loaded) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    if (!data.profile.setupDone) {
        OnboardingScreen(initial = data.profile) { vm.updateProfile(it) }
        return
    }
    MainShell(vm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(vm: MainViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = (leftTabs + rightTabs).any { it.route == current }
    var quickAdd by remember { mutableStateOf(false) }
    var weightDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    fun openAdd(meal: Int) {
        vm.setMeal(meal)
        vm.setQuery("")
        nav.navigate(Routes.ADD)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                    leftTabs.forEach { t ->
                        val selected = current == t.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.goTab(t.route) },
                            icon = { Icon(if (selected) t.selectedIcon else t.icon, contentDescription = t.label) },
                            label = { Text(t.label, maxLines = 1) },
                        )
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        FloatingActionButton(
                            onClick = { quickAdd = true },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                            modifier = Modifier.size(56.dp),
                        ) { Icon(Icons.Filled.Add, contentDescription = "Hozzáadás", modifier = Modifier.size(30.dp)) }
                    }
                    rightTabs.forEach { t ->
                        val selected = current == t.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.goTab(t.route) },
                            icon = { Icon(if (selected) t.selectedIcon else t.icon, contentDescription = t.label) },
                            label = { Text(t.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(inner)) {
            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    onAdd = { meal -> openAdd(meal) },
                    onOpenNutrients = { nav.goTab(Routes.NUTRIENTS) },
                )
            }
            composable(Routes.NUTRIENTS) { NutrientsScreen(vm) }
            composable(Routes.STATS) { StatsScreen(vm) }
            composable(Routes.PROFILE) { ProfileScreen(vm) }
            composable(Routes.ADD) {
                AddFoodScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onCustom = { nav.navigate(Routes.CUSTOM) },
                    onAiPhoto = { vm.resetAi(); nav.navigate(Routes.AI_PHOTO) },
                    onAiText = { vm.resetAi(); nav.navigate(Routes.AI_TEXT) },
                )
            }
            composable(Routes.CUSTOM) { CustomFoodScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.AI_PHOTO) {
                AiScreen(vm, photoMode = true, onBack = { nav.popBackStack() }, onDone = { nav.popBackStack(Routes.HOME, false) })
            }
            composable(Routes.AI_TEXT) {
                AiScreen(vm, photoMode = false, onBack = { nav.popBackStack() }, onDone = { nav.popBackStack(Routes.HOME, false) })
            }
        }
    }

    if (quickAdd) {
        QuickAddSheet(
            vm = vm,
            onDismiss = { quickAdd = false },
            onSearch = { m -> quickAdd = false; openAdd(m) },
            onScan = { m -> quickAdd = false; vm.requestScan(); openAdd(m) },
            onPhoto = { m -> quickAdd = false; vm.setMeal(m); vm.resetAi(); nav.navigate(Routes.AI_PHOTO) },
            onText = { m -> quickAdd = false; vm.setMeal(m); vm.resetAi(); nav.navigate(Routes.AI_TEXT) },
            onWeight = { quickAdd = false; weightDialog = true },
        )
    }
    if (weightDialog) {
        WeightDialog(
            initial = vm.data.value.profile.weightKg,
            onDismiss = { weightDialog = false },
            onSave = { vm.addWeight(it); vm.toast("Testsúly rögzítve: ${fmt(it, 1)} kg"); weightDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onSearch: (Int) -> Unit,
    onScan: (Int) -> Unit,
    onPhoto: (Int) -> Unit,
    onText: (Int) -> Unit,
    onWeight: () -> Unit,
) {
    val meal by vm.meal.collectAsState()
    val data by vm.data.collectAsState()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("Mit szeretnél rögzíteni?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Meals.names.forEachIndexed { i, n ->
                    FilterChip(selected = meal == i, onClick = { vm.setMeal(i) }, label = { Text(n) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile("Keresés", Icons.Filled.Search, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { onSearch(meal) }
                ActionTile("Vonalkód", Icons.Filled.QrCodeScanner, Color(0xFF8B6CF2), Modifier.weight(1f)) { onScan(meal) }
                ActionTile("Fotó (AI)", Icons.Filled.PhotoCamera, AppColors.Kcal, Modifier.weight(1f)) { onPhoto(meal) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile("Leírás (AI)", Icons.Filled.AutoAwesome, Color(0xFFE879A9), Modifier.weight(1f)) { onText(meal) }
                ActionTile("+${data.profile.glassMl} ml víz", Icons.Filled.WaterDrop, AppColors.Water, Modifier.weight(1f)) {
                    vm.addWater(data.profile.glassMl)
                    vm.toast("+${data.profile.glassMl} ml víz rögzítve")
                }
                ActionTile("Testsúly", Icons.Filled.MonitorWeight, AppColors.Protein, Modifier.weight(1f)) { onWeight() }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
fun ActionTile(label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = Color.White) }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun WeightDialog(initial: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf(fmt(initial, 1)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mai testsúly") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6) },
                suffix = { Text("kg") },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(onClick = { parseNum(text)?.let(onSave) }, enabled = (parseNum(text) ?: 0.0) in 20.0..400.0) {
                Text("Mentés")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Mégse") } },
    )
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
