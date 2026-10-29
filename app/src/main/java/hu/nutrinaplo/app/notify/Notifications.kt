package hu.nutrinaplo.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import hu.nutrinaplo.app.MainActivity
import hu.nutrinaplo.app.R
import hu.nutrinaplo.app.core.AppData
import hu.nutrinaplo.app.core.Calc
import hu.nutrinaplo.app.core.Nutrients
import hu.nutrinaplo.app.core.Reminders
import hu.nutrinaplo.app.core.Status
import hu.nutrinaplo.app.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

object Notifications {
    const val ACTION_FIRE = "hu.nutrinaplo.app.action.FIRE"
    const val ACTION_ADD_WATER = "hu.nutrinaplo.app.action.ADD_WATER"
    const val EXTRA_TYPES = "types"
    const val EXTRA_AT = "at"

    const val T_WATER = "water"
    const val T_BREAKFAST = "breakfast"
    const val T_LUNCH = "lunch"
    const val T_DINNER = "dinner"
    const val T_SUMMARY = "summary"

    private const val ID_WATER = 101
    private const val ID_MEAL = 102
    private const val ID_SUMMARY = 103

    private val HU = Locale("hu", "HU")

    // ---------- időzítés ----------

    fun events(r: Reminders, day: LocalDate): List<Pair<LocalDateTime, String>> {
        val list = ArrayList<Pair<LocalDateTime, String>>()
        fun at(min: Int) = day.atStartOfDay().plusMinutes(min.toLong())
        if (r.waterEnabled && r.waterIntervalMin >= 15) {
            var m = r.waterStart
            while (m <= r.waterEnd) {
                list.add(at(m) to T_WATER)
                m += r.waterIntervalMin
            }
        }
        if (r.mealsEnabled) {
            list.add(at(r.breakfast) to T_BREAKFAST)
            list.add(at(r.lunch) to T_LUNCH)
            list.add(at(r.dinner) to T_DINNER)
        }
        if (r.summaryEnabled) list.add(at(r.summary) to T_SUMMARY)
        return list
    }

    /** A következő esedékes időpont és az akkor esedékes értesítéstípusok. */
    fun next(r: Reminders, now: LocalDateTime): Pair<LocalDateTime, List<String>>? {
        if (!r.enabled) return null
        val all = (events(r, now.toLocalDate()) + events(r, now.toLocalDate().plusDays(1)))
            .filter { it.first.isAfter(now) }
        val first = all.minByOrNull { it.first } ?: return null
        return first.first to all.filter { it.first == first.first }.map { it.second }.distinct()
    }

    private fun firePending(ctx: Context, types: List<String>, at: Long): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_TYPES, types.joinToString(","))
            .putExtra(EXTRA_AT, at)
        return PendingIntent.getBroadcast(ctx, 1000, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(ctx: Context, r: Reminders) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val nxt = next(r, LocalDateTime.now())
        if (nxt == null) {
            am.cancel(firePending(ctx, emptyList(), 0))
            return
        }
        val at = nxt.first.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = firePending(ctx, nxt.second, at)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    // ---------- csatornák és hangok ----------

    fun soundName(ctx: Context, sound: String): String = when (sound) {
        "" -> "Néma"
        "default" -> "Alapértelmezett"
        else -> try {
            RingtoneManager.getRingtone(ctx, Uri.parse(sound))?.getTitle(ctx) ?: "Egyedi hang"
        } catch (e: Exception) {
            "Egyedi hang"
        }
    }

    /**
     * Az Android 8+ a hangot a csatornához köti, ezért hangváltáskor új csatorna készül
     * (a régi törlődik).
     */
    private fun channel(ctx: Context, prefix: String, name: String, desc: String, sound: String, vibrate: Boolean): String {
        val id = prefix + "_" + Integer.toHexString((sound + "|" + vibrate).hashCode())
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return id
        if (nm.getNotificationChannel(id) == null) {
            nm.notificationChannels
                .filter { it.id.startsWith(prefix + "_") && it.id != id }
                .forEach { nm.deleteNotificationChannel(it.id) }
            val ch = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            ch.description = desc
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            when (sound) {
                "" -> ch.setSound(null, null)
                "default" -> ch.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, attrs)
                else -> ch.setSound(Uri.parse(sound), attrs)
            }
            ch.enableVibration(vibrate)
            if (vibrate) ch.vibrationPattern = longArrayOf(0, 250, 150, 250)
            nm.createNotificationChannel(ch)
        }
        return id
    }

    private fun waterChannel(ctx: Context, r: Reminders) =
        channel(ctx, "water", "Víz emlékeztető", "Emlékeztet, hogy igyál vizet", r.waterSound, r.vibrate)

    private fun mealChannel(ctx: Context, r: Reminders) =
        channel(ctx, "meal", "Étkezés és összegzés", "Étkezési emlékeztetők és napi összegzés", r.mealSound, r.vibrate)

    // ---------- értesítések ----------

    private fun canNotify(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    private fun openApp(ctx: Context): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun post(ctx: Context, id: Int, n: android.app.Notification) {
        if (!canNotify(ctx)) return
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (e: SecurityException) {
            // nincs engedély
        }
    }

    private fun liters(ml: Int) = String.format(HU, "%.2f", ml / 1000.0).trimEnd('0').trimEnd(',') + " l"

    private fun kcalStr(v: Double) = String.format(HU, "%,d", v.roundToInt()).replace(' ', ' ').replace(' ', ' ')

    fun waterText(d: AppData): Pair<String, String>? {
        val p = d.profile
        val today = LocalDate.now().toString()
        val drunk = d.water[today] ?: 0
        val goal = Calc.waterGoal(p)
        val left = goal - drunk
        if (left <= 0) return null
        val glasses = (left + p.glassMl - 1) / p.glassMl
        val title = "💧 Ideje inni egy pohár vizet"
        val text = "Ma eddig ${liters(drunk)} – még ${liters(left)} van hátra (kb. $glasses pohár)."
        return title to text
    }

    private fun mealText(d: AppData, type: String): Pair<String, String>? {
        val p = d.profile
        val today = LocalDate.now().toString()
        val entries = d.entries.filter { it.date == today }
        val mealIndex = when (type) {
            T_BREAKFAST -> 0
            T_LUNCH -> 2
            else -> 4
        }
        // ha már rögzítette ezt az étkezést, nem zavarjuk
        if (entries.any { it.meal == mealIndex }) return null
        val t = Calc.totals(entries)
        val goal = Calc.kcalTarget(p)
        val mac = Calc.macroTargets(p, goal)
        val kcalLeft = goal - t.kcal
        val protLeft = (mac.protein - t.v(Nutrients.PROTEIN)).coerceAtLeast(0.0)
        val title = when (type) {
            T_BREAKFAST -> "🍳 Jó reggelt! Ideje reggelizni"
            T_LUNCH -> "🍽️ Ebédidő"
            else -> "🥗 Vacsoraidő"
        }
        val text = if (kcalLeft > 0) {
            "Ma még ${kcalStr(kcalLeft)} kcal fér bele, fehérjéből még ${protLeft.roundToInt()} g kell. " +
                "Ne felejtsd el rögzíteni!"
        } else {
            "Elérted a mai kalóriacélt (${kcalStr(goal.toDouble())} kcal). Ha eszel még, válassz könnyű, fehérjedús ételt."
        }
        return title to text
    }

    private fun summaryText(d: AppData): Pair<String, String>? {
        val p = d.profile
        val today = LocalDate.now().toString()
        val entries = d.entries.filter { it.date == today }
        val water = d.water[today] ?: 0
        val goal = Calc.kcalTarget(p)
        val title = "📊 Mai összegzés"
        if (entries.isEmpty()) {
            return title to "Ma még nem rögzítettél ételt. Pár másodperc az egész – így pontos marad az elemzésed!"
        }
        val t = Calc.totals(entries)
        val defs = Calc.deficiencies(t, Calc.targets(p)).filter { it.status == Status.DEFICIENT }
        val sb = StringBuilder()
        sb.append("${kcalStr(t.kcal)} / ${kcalStr(goal.toDouble())} kcal · víz ${liters(water)} / ${liters(Calc.waterGoal(p))}")
        if (defs.isNotEmpty()) {
            sb.append("\nHiányos: ").append(defs.take(4).joinToString(", ") { it.def.label.substringBefore(" (") })
        } else {
            sb.append("\nMinden vitaminból és ásványi anyagból jól állsz. 👏")
        }
        return title to sb.toString()
    }

    fun show(ctx: Context, d: AppData, types: List<String>) {
        val r = d.reminders
        for (type in types) {
            when (type) {
                T_WATER -> waterText(d)?.let { (title, text) -> postWater(ctx, r, title, text) }
                T_BREAKFAST, T_LUNCH, T_DINNER -> mealText(d, type)?.let { (title, text) ->
                    post(ctx, ID_MEAL, build(ctx, mealChannel(ctx, r), title, text).build())
                }
                T_SUMMARY -> summaryText(d)?.let { (title, text) ->
                    post(ctx, ID_SUMMARY, build(ctx, mealChannel(ctx, r), title, text).build())
                }
            }
        }
    }

    private fun build(ctx: Context, channel: String, title: String, text: String): NotificationCompat.Builder =
        NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF12A15F.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

    private fun postWater(ctx: Context, r: Reminders, title: String, text: String, silent: Boolean = false) {
        val glass = Store.data.value.profile.glassMl
        val add = PendingIntent.getBroadcast(
            ctx, 2000,
            Intent(ctx, ReminderReceiver::class.java).setAction(ACTION_ADD_WATER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val b = build(ctx, waterChannel(ctx, r), title, text)
            .addAction(0, "+$glass ml megittam", add)
            .setOnlyAlertOnce(silent)
            .setSilent(silent)
        post(ctx, ID_WATER, b.build())
    }

    /** "+pohár" gomb után frissített (néma) értesítés. */
    fun showWaterLogged(ctx: Context, d: AppData) {
        val w = waterText(d)
        if (w == null) {
            post(
                ctx, ID_WATER,
                build(ctx, waterChannel(ctx, d.reminders), "🎉 Megvan a mai vízcél!", "Szuper, ma már ${liters(d.water[LocalDate.now().toString()] ?: 0)} vizet ittál.")
                    .setSilent(true).setTimeoutAfter(10_000).build(),
            )
        } else {
            postWater(ctx, d.reminders, "✅ Rögzítve", w.second, silent = true)
        }
    }

    /** Próba értesítés a beállítások ellenőrzéséhez (hang, rezgés). */
    fun showTest(ctx: Context, d: AppData) {
        val w = waterText(d) ?: ("💧 Ideje inni egy pohár vizet" to "Ez egy próba értesítés – így fog kinézni a víz emlékeztető.")
        postWater(ctx, d.reminders, w.first, w.second)
        val m = mealText(d.copy(entries = emptyList()), T_LUNCH) ?: ("🍽️ Ebédidő" to "Próba értesítés")
        post(ctx, ID_MEAL, build(ctx, mealChannel(ctx, d.reminders), m.first, m.second).build())
    }
}

/** Időzített emlékeztetők és az értesítés gombjainak kezelése. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Store.ensureLoaded(ctx)
                when (intent.action) {
                    Notifications.ACTION_FIRE -> {
                        val at = intent.getLongExtra(Notifications.EXTRA_AT, 0L)
                        val late = System.currentTimeMillis() - at > 45 * 60 * 1000L
                        val types = intent.getStringExtra(Notifications.EXTRA_TYPES)?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
                        if (!late && Store.data.value.reminders.enabled) Notifications.show(ctx, Store.data.value, types)
                        Notifications.schedule(ctx, Store.data.value.reminders)
                    }
                    Notifications.ACTION_ADD_WATER -> {
                        Store.updateNow { d ->
                            val key = LocalDate.now().toString()
                            d.copy(water = d.water + (key to ((d.water[key] ?: 0) + d.profile.glassMl)))
                        }
                        Notifications.showWaterLogged(ctx, Store.data.value)
                    }
                    else -> Notifications.schedule(ctx, Store.data.value.reminders)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Újraindítás / időváltás / frissítés után újraütemez. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Store.ensureLoaded(ctx)
                Notifications.schedule(ctx, Store.data.value.reminders)
            } finally {
                pending.finish()
            }
        }
    }
}
