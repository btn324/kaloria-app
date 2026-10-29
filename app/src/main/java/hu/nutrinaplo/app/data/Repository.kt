package hu.nutrinaplo.app.data

import android.content.Context
import hu.nutrinaplo.app.core.AppData
import hu.nutrinaplo.app.core.FoodDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Az összes felhasználói adat egyetlen JSON fájlban, az app privát tárhelyén. */
class Repository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val file: File get() = File(context.filesDir, "data.json")

    suspend fun load(): AppData = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) json.decodeFromString<AppData>(file.readText()) else AppData()
        } catch (e: Exception) {
            // sérült fájl esetén mentés másolat, majd üres adat
            try {
                file.copyTo(File(context.filesDir, "data_hibas_${System.currentTimeMillis()}.json"), overwrite = true)
            } catch (_: Exception) {
            }
            AppData()
        }
    }

    suspend fun save(data: AppData) = withContext(Dispatchers.IO) {
        val tmp = File(context.filesDir, "data.json.tmp")
        tmp.writeText(json.encodeToString(data))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
        Unit
    }

    fun export(data: AppData): String = json.encodeToString(data)

    fun import(text: String): AppData = json.decodeFromString<AppData>(text)

    suspend fun loadFoodDb(): FoodDb = withContext(Dispatchers.Default) {
        val text = context.assets.open("foods.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        FoodDb.parse(text)
    }
}
