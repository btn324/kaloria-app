package hu.nutrinaplo.app.core

import kotlinx.serialization.Serializable

/** Egy adag/mértékegység, pl. "1 szelet" = 30 g. */
@Serializable
data class Serving(val name: String, val grams: Double)

/**
 * Egy élelmiszer. A tápértékek 100 g-ra vonatkoznak, a kulcsokat lásd: [Nutrients].
 * source: "db" (beépített), "off" (Open Food Facts), "custom" (saját), "ai" (AI becslés)
 */
@Serializable
data class Food(
    val id: String,
    val name: String,
    val category: String = "",
    val per100: Map<String, Double> = emptyMap(),
    val servings: List<Serving> = emptyList(),
    val source: String = "db",
    val brand: String? = null,
    val barcode: String? = null,
) {
    fun value(key: String): Double = per100[key] ?: 0.0
    val kcal: Double get() = value(Nutrients.KCAL)

    /** Van-e érdemi mikrotápanyag adat (vitaminok, ásványi anyagok). */
    val hasMicros: Boolean
        get() = Nutrients.micros.count { per100.containsKey(it.key) } >= 8
}

object Meals {
    val names = listOf("Reggeli", "Tízórai", "Ebéd", "Uzsonna", "Vacsora", "Nassolás")

    /** Napszak alapján javasolt étkezés. */
    fun suggest(hour: Int): Int = when (hour) {
        in 4..9 -> 0
        in 10..11 -> 1
        in 12..14 -> 2
        in 15..17 -> 3
        in 18..21 -> 4
        else -> 5
    }
}

@Serializable
data class DiaryEntry(
    val id: Long,
    val date: String, // yyyy-MM-dd
    val meal: Int,
    val food: Food,
    val grams: Double,
    val time: Long = 0L,
) {
    fun amount(key: String): Double = food.value(key) * grams / 100.0
}

@Serializable
data class WeightEntry(val date: String, val kg: Double)

enum class Sex { MALE, FEMALE }
enum class Goal { LOSE, MAINTAIN, GAIN }

@Serializable
data class Profile(
    val name: String = "",
    val sex: Sex = Sex.MALE,
    val age: Int = 30,
    val heightCm: Double = 175.0,
    val weightKg: Double = 75.0,
    val targetWeightKg: Double = 70.0,
    /** PAL érték: 1.2 ülő ... 1.9 nagyon aktív */
    val activity: Double = 1.375,
    val goal: Goal = Goal.MAINTAIN,
    /** kg / hét */
    val weeklyRate: Double = 0.5,
    /** ha > 0, felülírja a számított kalóriacélt */
    val kcalOverride: Int = 0,
    val proteinPct: Int = 25,
    val carbPct: Int = 45,
    val fatPct: Int = 30,
    /** ha > 0, felülírja a számított vízcélt (ml) */
    val waterGoalOverride: Int = 0,
    val glassMl: Int = 250,
    val geminiKey: String = "",
    val geminiModel: String = "gemini-flash-latest",
    val setupDone: Boolean = false,
)

@Serializable
data class AppData(
    val profile: Profile = Profile(),
    val entries: List<DiaryEntry> = emptyList(),
    /** dátum -> ml */
    val water: Map<String, Int> = emptyMap(),
    val weights: List<WeightEntry> = emptyList(),
    val customFoods: List<Food> = emptyList(),
    val favorites: List<Food> = emptyList(),
    val recents: List<Food> = emptyList(),
    val nextId: Long = 1L,
)
