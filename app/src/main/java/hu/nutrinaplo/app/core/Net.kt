package hu.nutrinaplo.app.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class NetException(message: String) : IOException(message)

object Http {
    private const val UA = "NutriNaplo/1.0 (Android; szemelyes hasznalat)"

    fun get(url: String): String = request(url, null)

    fun postJson(url: String, body: String): String = request(url, body)

    private fun request(url: String, body: String?): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15000
            c.readTimeout = 60000
            c.setRequestProperty("User-Agent", UA)
            c.setRequestProperty("Accept", "application/json")
            if (body != null) {
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw NetException("HTTP $code: " + text.take(300))
            }
            return text
        } finally {
            c.disconnect()
        }
    }
}

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

private fun JsonObject.num(key: String): Double? {
    val p = this[key] as? JsonPrimitive ?: return null
    return p.doubleOrNull ?: p.contentOrNull?.replace(',', '.')?.toDoubleOrNull()
}

/** Open Food Facts – ingyenes, nyílt termékadatbázis (vonalkód + keresés). */
object OpenFoodFacts {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val FIELDS =
        "code,product_name,product_name_hu,generic_name_hu,brands,nutriments,serving_quantity,serving_size,categories_tags"

    /** OFF nutriment kulcs -> (saját kulcs, szorzó a g/100g értékből) */
    private val map: List<Triple<String, String, Double>> = listOf(
        Triple("proteins_100g", "protein", 1.0),
        Triple("carbohydrates_100g", "carbs", 1.0),
        Triple("sugars_100g", "sugar", 1.0),
        Triple("fiber_100g", "fiber", 1.0),
        Triple("fat_100g", "fat", 1.0),
        Triple("saturated-fat_100g", "satfat", 1.0),
        Triple("monounsaturated-fat_100g", "mufa", 1.0),
        Triple("polyunsaturated-fat_100g", "pufa", 1.0),
        Triple("cholesterol_100g", "chol", 1000.0),
        Triple("sodium_100g", "na", 1000.0),
        Triple("potassium_100g", "k", 1000.0),
        Triple("calcium_100g", "ca", 1000.0),
        Triple("iron_100g", "fe", 1000.0),
        Triple("magnesium_100g", "mg", 1000.0),
        Triple("phosphorus_100g", "p", 1000.0),
        Triple("zinc_100g", "zn", 1000.0),
        Triple("copper_100g", "cu", 1000.0),
        Triple("manganese_100g", "mn", 1000.0),
        Triple("selenium_100g", "se", 1_000_000.0),
        Triple("vitamin-a_100g", "vita", 1_000_000.0),
        Triple("vitamin-c_100g", "vitc", 1000.0),
        Triple("vitamin-d_100g", "vitd", 1_000_000.0),
        Triple("vitamin-e_100g", "vite", 1000.0),
        Triple("vitamin-k_100g", "vitk", 1_000_000.0),
        Triple("vitamin-b1_100g", "b1", 1000.0),
        Triple("vitamin-b2_100g", "b2", 1000.0),
        Triple("vitamin-pp_100g", "b3", 1000.0),
        Triple("pantothenic-acid_100g", "b5", 1000.0),
        Triple("vitamin-b6_100g", "b6", 1000.0),
        Triple("vitamin-b9_100g", "dfe", 1_000_000.0),
        Triple("folates_100g", "dfe", 1_000_000.0),
        Triple("vitamin-b12_100g", "b12", 1_000_000.0),
        Triple("choline_100g", "choline", 1000.0),
    )

    fun parseProduct(p: JsonObject, fallbackCode: String? = null): Food? {
        val nut = p["nutriments"] as? JsonObject ?: return null
        val per100 = HashMap<String, Double>()
        val kcal = nut.num("energy-kcal_100g") ?: nut.num("energy_100g")?.let { it / 4.184 }
        if (kcal != null) per100[Nutrients.KCAL] = kcal
        for ((offKey, key, mul) in map) {
            if (per100.containsKey(key)) continue
            val v = nut.num(offKey) ?: continue
            if (v < 0) continue
            per100[key] = v * mul
        }
        if (!per100.containsKey("na")) {
            nut.num("salt_100g")?.let { per100["na"] = it / 2.5 * 1000.0 }
        }
        if (!per100.containsKey(Nutrients.KCAL)) return null
        val code = p.str("code") ?: fallbackCode ?: ""
        val name = p.str("product_name_hu") ?: p.str("generic_name_hu") ?: p.str("product_name") ?: return null
        val brand = p.str("brands")?.substringBefore(',')?.trim()
        val servings = mutableListOf<Serving>()
        val sq = p.num("serving_quantity")
        if (sq != null && sq > 0) {
            val label = p.str("serving_size")?.let { "1 adag ($it)" } ?: "1 adag"
            servings.add(Serving(label, sq))
        }
        return Food(
            id = "off:$code",
            name = name.trim(),
            category = "Bolti termék",
            per100 = per100,
            servings = servings,
            source = "off",
            brand = brand,
            barcode = code.ifBlank { null },
        )
    }

    fun byBarcode(code: String): Food? {
        val clean = code.filter { it.isDigit() }
        val txt = Http.get("https://world.openfoodfacts.org/api/v2/product/$clean.json?fields=$FIELDS")
        val root = json.parseToJsonElement(txt).jsonObject
        val status = (root["status"] as? JsonPrimitive)?.contentOrNull
        if (status != "1") return null
        val p = root["product"] as? JsonObject ?: return null
        return parseProduct(p, clean)
    }

    fun search(query: String): List<Food> {
        val q = URLEncoder.encode(query, "UTF-8")
        val txt = Http.get(
            "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$q&search_simple=1&action=process&json=1" +
                "&page_size=30&lc=hu&fields=$FIELDS"
        )
        return parseSearch(txt)
    }

    fun parseSearch(txt: String): List<Food> {
        val root = json.parseToJsonElement(txt).jsonObject
        val arr = root["products"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull { (it as? JsonObject)?.let { o -> parseProduct(o) } }.distinctBy { it.id }
    }
}

/** Az AI által felismert ételtétel. */
data class AiItem(
    val name: String,
    val search: String,
    val grams: Double,
    val per100: Map<String, Double>,
)

data class AiResult(val items: List<AiItem>, val note: String)

/** Google Gemini – ingyenes kerettel használható képfelismerés / szövegértelmezés. */
object Gemini {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val PROMPT = """
Táplálkozási szakértő vagy. Azonosítsd az összes ételt és italt, ami a bemeneten szerepel,
és becsüld meg mindegyik tömegét grammban (italoknál ml ≈ g). Legyél reális az adagméretekkel.
Minden tételhez adj:
- "name": rövid, egyértelmű magyar ételnév (pl. "Rántott csirkemell", "Főtt rizs", "Paradicsom", "Sertéspörkölt"),
- "search": 1–3 szavas magyar keresőkifejezés egy élelmiszer-adatbázishoz (pl. "csirkemell sült", "rizs főtt", "pörkölt sertés"),
- "grams": becsült tömeg grammban,
- 100 g-ra vonatkozó becsült tápértékek: "kcal", "protein", "carbs", "fat", "fiber", "sugar".
Válaszolj KIZÁRÓLAG ebben a JSON formátumban:
{"items":[{"name":"","search":"","grams":0,"kcal":0,"protein":0,"carbs":0,"fat":0,"fiber":0,"sugar":0}],"note":"rövid megjegyzés magyarul"}
Ha nincs étel a bemeneten, adj üres items listát és írd le a note mezőben, miért.
"""

    private fun endpoint(model: String, key: String) =
        "https://generativelanguage.googleapis.com/v1beta/models/${model.trim()}:generateContent?key=${key.trim()}"

    fun buildRequest(text: String?, imageBase64: String?, mime: String = "image/jpeg"): String {
        val body = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject {
                            put("text", PROMPT.trim() + (if (text.isNullOrBlank()) "" else "\n\nA felhasználó leírása: $text"))
                        })
                        if (imageBase64 != null) {
                            add(buildJsonObject {
                                put("inline_data", buildJsonObject {
                                    put("mime_type", mime)
                                    put("data", imageBase64)
                                })
                            })
                        }
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("response_mime_type", "application/json")
                put("temperature", 0.2)
            })
        }
        return body.toString()
    }

    fun analyze(key: String, model: String, text: String?, imageBase64: String?): AiResult {
        if (key.isBlank()) throw NetException("Nincs megadva Gemini API-kulcs (Profil → AI beállítások).")
        val resp = Http.postJson(endpoint(model.ifBlank { "gemini-flash-latest" }, key), buildRequest(text, imageBase64))
        return parseResponse(resp)
    }

    fun parseResponse(resp: String): AiResult {
        val root = json.parseToJsonElement(resp).jsonObject
        val cands = root["candidates"] as? JsonArray
        val parts = (cands?.firstOrNull() as? JsonObject)
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray
        val text = parts?.mapNotNull { (it as? JsonObject)?.str("text") }?.joinToString("")
            ?: throw NetException("Az AI nem adott választ (lehet, hogy a kép tartalma le lett tiltva).")
        return parseItems(text)
    }

    fun parseItems(rawText: String): AiResult {
        var t = rawText.trim()
        if (t.startsWith("```")) t = t.substringAfter('\n').substringBeforeLast("```")
        val start = t.indexOf('{')
        val end = t.lastIndexOf('}')
        if (start < 0 || end <= start) throw NetException("Nem értelmezhető AI válasz.")
        val obj: JsonElement = json.parseToJsonElement(t.substring(start, end + 1))
        val o = obj.jsonObject
        val items = (o["items"] as? JsonArray)?.mapNotNull { el ->
            val io = el as? JsonObject ?: return@mapNotNull null
            val name = io.str("name") ?: return@mapNotNull null
            val per100 = HashMap<String, Double>()
            for (k in listOf("kcal", "protein", "carbs", "fat", "fiber", "sugar")) {
                io.num(k)?.let { v -> per100[k] = v }
            }
            AiItem(
                name = name,
                search = io.str("search") ?: name,
                grams = (io.num("grams") ?: 100.0).coerceIn(1.0, 3000.0),
                per100 = per100,
            )
        } ?: emptyList()
        return AiResult(items, o.str("note") ?: "")
    }
}
