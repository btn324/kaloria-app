package hu.nutrinaplo.app.core

import kotlin.math.max
import kotlin.math.roundToInt

/** Napi célérték egy tápanyagra. isLimit = felső határ (pl. nátrium), nem elérendő cél. */
data class Target(val key: String, val amount: Double, val isLimit: Boolean = false)

enum class Status(val label: String) {
    DEFICIENT("Hiányos"),
    LOW("Alacsony"),
    OK("Megfelelő"),
    HIGH("Túl sok"),
}

data class NutrientStatus(
    val def: NutrientDef,
    val amount: Double,
    val target: Target?,
) {
    val pct: Double get() = if (target == null || target.amount <= 0) 0.0 else amount / target.amount * 100.0
    val status: Status
        get() {
            val t = target ?: return Status.OK
            return if (t.isLimit) {
                if (pct > 100) Status.HIGH else Status.OK
            } else when {
                pct < 50 -> Status.DEFICIENT
                pct < 90 -> Status.LOW
                else -> Status.OK
            }
        }
}

data class DayTotals(
    val values: Map<String, Double>,
    val kcal: Double,
    /** a kalóriák hány %-a jön olyan ételből, amihez van részletes vitamin/ásványi anyag adat */
    val microCoveragePct: Double,
    val entryCount: Int,
) {
    fun v(key: String) = values[key] ?: 0.0
}

object Calc {

    fun bmr(p: Profile): Double {
        val base = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age
        return if (p.sex == Sex.MALE) base + 5 else base - 161
    }

    fun tdee(p: Profile): Double = bmr(p) * p.activity

    fun kcalTarget(p: Profile): Int {
        if (p.kcalOverride > 0) return p.kcalOverride
        val daily = p.weeklyRate * 7700.0 / 7.0
        val raw = when (p.goal) {
            Goal.LOSE -> tdee(p) - daily
            Goal.MAINTAIN -> tdee(p)
            Goal.GAIN -> tdee(p) + daily
        }
        val floor = if (p.sex == Sex.MALE) 1500.0 else 1200.0
        return (max(raw, floor) / 10).roundToInt() * 10
    }

    fun waterGoal(p: Profile): Int {
        if (p.waterGoalOverride > 0) return p.waterGoalOverride
        val ml = p.weightKg * 35
        return ((ml / 250).roundToInt() * 250).coerceIn(1500, 4500)
    }

    data class Macros(val protein: Double, val carbs: Double, val fat: Double)

    fun macroTargets(p: Profile, kcal: Int = kcalTarget(p)): Macros = Macros(
        protein = kcal * p.proteinPct / 100.0 / 4.0,
        carbs = kcal * p.carbPct / 100.0 / 4.0,
        fat = kcal * p.fatPct / 100.0 / 9.0,
    )

    /** Ajánlott napi bevitel felnőtteknek (NIH / EFSA DRI alapján, életkor és nem szerint). */
    fun targets(p: Profile): Map<String, Target> {
        val m = p.sex == Sex.MALE
        val age = p.age
        val kcal = kcalTarget(p)
        val mac = macroTargets(p, kcal)
        val t = mutableListOf(
            Target(Nutrients.KCAL, kcal.toDouble()),
            Target(Nutrients.PROTEIN, mac.protein),
            Target(Nutrients.CARBS, mac.carbs),
            Target(Nutrients.FAT, mac.fat),
            Target(Nutrients.FIBER, if (m) (if (age > 50) 30.0 else 38.0) else (if (age > 50) 21.0 else 25.0)),
            Target(Nutrients.SATFAT, kcal * 0.10 / 9.0, isLimit = true),
            Target(Nutrients.SUGAR, kcal * 0.10 / 4.0 * 2, isLimit = true), // össz-cukor: ~20% kcal felső határ
            Target("chol", 300.0, isLimit = true),

            Target("vita", if (m) 900.0 else 700.0),
            Target("vitc", if (m) 90.0 else 75.0),
            Target("vitd", if (age > 70) 20.0 else 15.0),
            Target("vite", 15.0),
            Target("vitk", if (m) 120.0 else 90.0),
            Target("b1", if (m) 1.2 else 1.1),
            Target("b2", if (m) 1.3 else 1.1),
            Target("b3", if (m) 16.0 else 14.0),
            Target("b5", 5.0),
            Target("b6", if (age > 50) (if (m) 1.7 else 1.5) else 1.3),
            Target("dfe", 400.0),
            Target("b12", 2.4),
            Target("choline", if (m) 550.0 else 425.0),

            Target("ca", if ((!m && age > 50) || age > 70) 1200.0 else 1000.0),
            Target("fe", if (m || age > 50) 8.0 else 18.0),
            Target("mg", if (m) (if (age > 30) 420.0 else 400.0) else (if (age > 30) 320.0 else 310.0)),
            Target("p", 700.0),
            Target("k", if (m) 3400.0 else 2600.0),
            Target(Nutrients.SODIUM, 2300.0, isLimit = true),
            Target("zn", if (m) 11.0 else 8.0),
            Target("cu", 0.9),
            Target("mn", if (m) 2.3 else 1.8),
            Target("se", 55.0),
        )
        return t.associateBy { it.key }
    }

    fun totals(entries: List<DiaryEntry>): DayTotals {
        val sums = HashMap<String, Double>()
        var kcal = 0.0
        var covered = 0.0
        for (e in entries) {
            for ((k, v) in e.food.per100) {
                sums[k] = (sums[k] ?: 0.0) + v * e.grams / 100.0
            }
            val ek = e.amount(Nutrients.KCAL)
            kcal += ek
            if (e.food.hasMicros) covered += ek
        }
        val cov = if (kcal > 0) covered / kcal * 100.0 else 100.0
        return DayTotals(sums, kcal, cov, entries.size)
    }

    /** Több nap átlaga (csak azokat a napokat számolva, ahol van bejegyzés). */
    fun averageTotals(entries: List<DiaryEntry>): DayTotals {
        val byDay = entries.groupBy { it.date }
        if (byDay.isEmpty()) return DayTotals(emptyMap(), 0.0, 100.0, 0)
        val days = byDay.size.toDouble()
        val all = totals(entries)
        return DayTotals(all.values.mapValues { it.value / days }, all.kcal / days, all.microCoveragePct, entries.size)
    }

    fun statuses(totals: DayTotals, targets: Map<String, Target>, defs: List<NutrientDef>): List<NutrientStatus> =
        defs.map { NutrientStatus(it, totals.v(it.key), targets[it.key]) }

    /** Hiányos/alacsony mikrotápanyagok, a legsúlyosabb elöl. */
    fun deficiencies(totals: DayTotals, targets: Map<String, Target>): List<NutrientStatus> =
        statuses(totals, targets, Nutrients.micros + listOf(Nutrients.byKey.getValue(Nutrients.FIBER)))
            .filter { it.target != null && !it.target.isLimit && it.status != Status.OK }
            .sortedBy { it.pct }

    fun excesses(totals: DayTotals, targets: Map<String, Target>): List<NutrientStatus> =
        statuses(totals, targets, Nutrients.all)
            .filter { it.target != null && it.target.isLimit && it.status == Status.HIGH }

    private val excludedSuggestionCats = setOf(
        "Fűszerek", "Zsiradékok és öntetek", "Gyorsételek", "Édességek", "Snackek", "Gabonapelyhek és kásák",
    )
    private val excludedSuggestionWords = listOf("élesztő", "liszt", "por", "izolátum", "sikér", "korpa", "olaj", "száraz")

    /**
     * Ételjavaslatok egy tápanyaghoz: a legtöbbet tartalmazó, "normális" ételek 100 g-ra.
     * Ismétlődő ételfajták kiszűrve (első szó alapján).
     */
    fun suggestions(db: List<Food>, key: String, limit: Int = 8): List<Food> {
        val seen = HashSet<String>()
        return db.asSequence()
            .filter { it.category !in excludedSuggestionCats && it.kcal > 10 && it.per100.containsKey(key) }
            .filter { f -> excludedSuggestionWords.none { w -> f.name.contains(w, ignoreCase = true) } }
            .sortedByDescending { it.value(key) }
            .filter { seen.add(it.name.substringBefore(',').substringBefore(' ').lowercase()) }
            .take(limit)
            .toList()
    }

    fun bmi(p: Profile): Double = p.weightKg / ((p.heightCm / 100.0) * (p.heightCm / 100.0))

    fun bmiLabel(bmi: Double): String = when {
        bmi < 18.5 -> "Sovány"
        bmi < 25 -> "Normál"
        bmi < 30 -> "Túlsúly"
        else -> "Elhízás"
    }
}
