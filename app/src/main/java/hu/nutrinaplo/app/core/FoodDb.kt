package hu.nutrinaplo.app.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object Text {
    private val map = mapOf(
        'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ö' to 'o', 'ő' to 'o',
        'ú' to 'u', 'ü' to 'u', 'ű' to 'u', 'ä' to 'a', 'è' to 'e', 'ñ' to 'n', 'ç' to 'c',
    )

    /** kisbetűs, ékezet nélküli alak kereséshez */
    fun norm(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s.lowercase()) sb.append(map[c] ?: c)
        return sb.toString()
    }

    fun tokens(s: String): List<String> =
        norm(s).split(' ', ',', '(', ')', '/', '-', '.', ';').filter { it.isNotBlank() }
}

/** A beépített élelmiszer-adatbázis és a keresés. */
class FoodDb(val foods: List<Food>) {

    private val normNames: List<String> = foods.map { Text.norm(it.name) }
    private val normWords: List<List<String>> = normNames.map { Text.tokens(it) }
    private val normCats: List<String> = foods.map { Text.norm(it.category) }
    val byId: Map<String, Food> = foods.associateBy { it.id }

    fun search(query: String, limit: Int = 60, extra: List<Food> = emptyList()): List<Food> {
        val q = Text.tokens(query)
        if (q.isEmpty()) return emptyList()
        val scored = ArrayList<Pair<Food, Double>>()

        fun scoreOf(name: String, words: List<String>, cat: String): Double? {
            var score = 0.0
            for (t in q) {
                val exact = words.indexOfFirst { it == t }
                val prefix = words.indexOfFirst { it.startsWith(t) }
                score += when {
                    exact == 0 -> 45.0
                    exact > 0 -> 35.0 - exact.coerceAtMost(6)
                    prefix == 0 -> 28.0
                    prefix > 0 -> 18.0 - prefix.coerceAtMost(6)
                    // magyar összetett szavak: "sertéspörkölt" ~ "pörkölt"
                    t.length >= 4 && words.any { it.endsWith(t) } -> 36.0
                    name.contains(t) -> 10.0
                    cat.contains(t) -> 3.0
                    else -> return null
                }
            }
            score -= name.length / 12.0
            return score
        }

        for (i in foods.indices) {
            val s = scoreOf(normNames[i], normWords[i], normCats[i]) ?: continue
            val f = foods[i]
            var bonus = 0.0
            if (f.id.startsWith("r")) bonus += 2.0 // magyar ételek
            if (f.name.contains("nyers") && q.none { it.startsWith("nyers") }) bonus -= 1.5
            scored.add(f to s + bonus)
        }
        for (f in extra) {
            val n = Text.norm(f.name)
            val s = scoreOf(n, Text.tokens(n), Text.norm(f.category)) ?: continue
            scored.add(f to s + 10.0) // saját ételek előre
        }
        return scored.sortedByDescending { it.second }.map { it.first }.distinctBy { it.id }.take(limit)
    }

    /** A legjobb egyezés egy (pl. AI által adott) ételnévre. */
    fun bestMatch(query: String): Food? = search(query, 1).firstOrNull()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): FoodDb {
            val arr: JsonArray = json.parseToJsonElement(text).jsonArray
            val list = ArrayList<Food>(arr.size)
            for (el in arr) {
                val o: JsonObject = el.jsonObject
                val n = o["n"]?.jsonObject ?: JsonObject(emptyMap())
                val per100 = HashMap<String, Double>()
                for ((k, v) in n) {
                    val d = v.jsonPrimitive.doubleOrNull ?: continue
                    per100[k] = d
                }
                val servings = o["s"]?.jsonArray?.mapNotNull { s ->
                    val so = s.jsonObject
                    val name = so["n"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val g = so["g"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                    Serving(name, g)
                } ?: emptyList()
                list.add(
                    Food(
                        id = o["id"]!!.jsonPrimitive.content,
                        name = o["name"]!!.jsonPrimitive.content,
                        category = o["cat"]?.jsonPrimitive?.content ?: "",
                        per100 = per100,
                        servings = servings,
                        source = "db",
                    )
                )
            }
            return FoodDb(list)
        }
    }
}
