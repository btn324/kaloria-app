package hu.nutrinaplo.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

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

private val tabs = listOf(
    NavTab(Routes.HOME, "Napló", Icons.Outlined.Home, Icons.Filled.Home),
    NavTab(Routes.NUTRIENTS, "Tápanyagok", Icons.Outlined.Science, Icons.Filled.Science),
    NavTab(Routes.STATS, "Statisztika", Icons.Outlined.Insights, Icons.Filled.Insights),
    NavTab(Routes.PROFILE, "Profil", Icons.Outlined.Person, Icons.Filled.Person),
)

@Composable
fun NutriApp(vm: MainViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = tabs.any { it.route == current }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    tabs.forEach { t ->
                        val selected = current == t.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.goTab(t.route) },
                            icon = { Icon(if (selected) t.selectedIcon else t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
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
                    onAdd = { meal -> vm.setMeal(meal); vm.setQuery(""); nav.navigate(Routes.ADD) },
                    onOpenNutrients = { nav.goTab(Routes.NUTRIENTS) },
                    onOpenProfile = { nav.goTab(Routes.PROFILE) },
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
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
