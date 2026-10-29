package hu.nutrinaplo.app.ui

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import hu.nutrinaplo.app.core.Reminders
import hu.nutrinaplo.app.notify.Notifications

fun hhmm(min: Int): String = "%02d:%02d".format(min / 60, min % 60)

@Composable
fun RemindersScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val data by vm.data.collectAsState()
    val r = data.reminders
    fun set(nr: Reminders) = vm.updateReminders(nr)

    var timeEdit by remember { mutableStateOf<Pair<String, Int>?>(null) } // (melyik, perc)
    var soundTarget by remember { mutableStateOf("water") }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) set(r.copy(enabled = true))
        else vm.toast("Értesítési engedély nélkül nem tudunk emlékeztetni. A rendszerbeállításokban bármikor engedélyezheted.")
    }

    val soundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = res.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val value = when {
                uri == null -> ""
                uri == Settings.System.DEFAULT_NOTIFICATION_URI -> "default"
                else -> uri.toString()
            }
            if (soundTarget == "water") set(r.copy(waterSound = value)) else set(r.copy(mealSound = value))
        }
    }

    fun pickSound(target: String) {
        soundTarget = target
        val current = if (target == "water") r.waterSound else r.mealSound
        val existing: Uri? = when (current) {
            "" -> null
            "default" -> Settings.System.DEFAULT_NOTIFICATION_URI
            else -> Uri.parse(current)
        }
        val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Értesítési hang")
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
        try {
            soundLauncher.launch(i)
        } catch (e: Exception) {
            vm.toast("A hangválasztó nem nyitható meg ezen a telefonon.")
        }
    }

    fun enable(on: Boolean) {
        if (!on) {
            set(r.copy(enabled = false)); return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            set(r.copy(enabled = true))
        }
    }

    val pm = context.getSystemService(PowerManager::class.java)
    val batteryOk = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    val am = context.getSystemService(AlarmManager::class.java)
    val exactOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am?.canScheduleExactAlarms() == true

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Emlékeztetők", onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.NotificationsActive, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Értesítések", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (r.enabled) "Bekapcsolva" else "Kikapcsolva",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = r.enabled, onCheckedChange = { enable(it) })
                    }
                }
            }

            // víz
            item {
                ReminderCard(
                    icon = Icons.Filled.WaterDrop, color = AppColors.Water, title = "Víz emlékeztető",
                    subtitle = "Megmutatja, mennyi víz van még hátra, és egy gombbal rögzítheted a poharat.",
                    checked = r.waterEnabled, enabled = r.enabled,
                    onChecked = { set(r.copy(waterEnabled = it)) },
                ) {
                    Text("Gyakoriság", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60 to "Óránként", 90 to "1,5 óránként", 120 to "2 óránként", 180 to "3 óránként").forEach { (m, l) ->
                            FilterChip(selected = r.waterIntervalMin == m, onClick = { set(r.copy(waterIntervalMin = m)) }, label = { Text(l) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TimeRow("Első emlékeztető", r.waterStart) { timeEdit = "waterStart" to r.waterStart }
                    TimeRow("Utolsó emlékeztető", r.waterEnd) { timeEdit = "waterEnd" to r.waterEnd }
                    SoundRow(Notifications.soundName(context, r.waterSound)) { pickSound("water") }
                    Text(
                        "Ha aznap már elérted a vízcélt, nem kapsz több emlékeztetőt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // étkezések
            item {
                ReminderCard(
                    icon = Icons.Filled.Restaurant, color = AppColors.Kcal, title = "Étkezések",
                    subtitle = "Emlékeztet, mennyi kalória és fehérje fér még bele a napba. Ha már rögzítetted az étkezést, nem szól.",
                    checked = r.mealsEnabled, enabled = r.enabled,
                    onChecked = { set(r.copy(mealsEnabled = it)) },
                ) {
                    TimeRow("Reggeli", r.breakfast) { timeEdit = "breakfast" to r.breakfast }
                    TimeRow("Ebéd", r.lunch) { timeEdit = "lunch" to r.lunch }
                    TimeRow("Vacsora", r.dinner) { timeEdit = "dinner" to r.dinner }
                    SoundRow(Notifications.soundName(context, r.mealSound)) { pickSound("meal") }
                }
            }

            // összegzés
            item {
                ReminderCard(
                    icon = Icons.Filled.Insights, color = AppColors.Fat, title = "Esti összegzés",
                    subtitle = "Kalória, víz és a hiányzó vitaminok a nap végén.",
                    checked = r.summaryEnabled, enabled = r.enabled,
                    onChecked = { set(r.copy(summaryEnabled = it)) },
                ) {
                    TimeRow("Időpont", r.summary) { timeEdit = "summary" to r.summary }
                    Text(
                        "Ugyanazt a hangot használja, mint az étkezési emlékeztető.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rezgés", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Switch(checked = r.vibrate, onCheckedChange = { set(r.copy(vibrate = it)) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.testNotification() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Próba értesítés küldése")
                    }
                    Text(
                        "Kipróbálhatod a kiválasztott hangot és a megjelenést.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            if (r.enabled && (!batteryOk || !exactOk)) {
                item {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.BatteryAlert, AppColors.Low)
                            Spacer(Modifier.width(12.dp))
                            Text("Pontos emlékeztetők", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Egyes telefonok (pl. Xiaomi, Samsung, Huawei) az energiatakarékosság miatt késleltetik vagy elnyelik az emlékeztetőket. " +
                                "Kapcsold ki az akkumulátor-optimalizálást a NutriNaplónál, Xiaomin az „Automatikus indítást” is engedélyezd.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (!batteryOk) {
                            OutlinedButton(onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                                    )
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Akkumulátor-optimalizálás kikapcsolása") }
                        }
                        if (!exactOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            OutlinedButton(onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                                } catch (_: Exception) {
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Pontos időzítés engedélyezése") }
                        }
                        TextButton(onClick = {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                )
                            } catch (_: Exception) {
                            }
                        }) { Text("Alkalmazásbeállítások megnyitása") }
                    }
                }
            }
        }
    }

    timeEdit?.let { (which, minutes) ->
        TimeDialog(
            initial = minutes,
            onDismiss = { timeEdit = null },
            onConfirm = { m ->
                timeEdit = null
                set(
                    when (which) {
                        "waterStart" -> r.copy(waterStart = m.coerceAtMost(r.waterEnd))
                        "waterEnd" -> r.copy(waterEnd = m.coerceAtLeast(r.waterStart))
                        "breakfast" -> r.copy(breakfast = m)
                        "lunch" -> r.copy(lunch = m)
                        "dinner" -> r.copy(dinner = m)
                        else -> r.copy(summary = m)
                    }
                )
            },
        )
    }
}

@Composable
private fun ReminderCard(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked && enabled, enabled = enabled, onCheckedChange = onChecked)
        }
        if (checked && enabled) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(hhmm(minutes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SoundRow(name: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Hang", style = MaterialTheme.typography.bodyLarge)
            Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Időpont") },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Mégse") } },
    )
}
