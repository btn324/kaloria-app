package hu.nutrinaplo.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.nutrinaplo.app.core.AiItem
import hu.nutrinaplo.app.core.AppData
import hu.nutrinaplo.app.core.Calc
import hu.nutrinaplo.app.core.DiaryEntry
import hu.nutrinaplo.app.core.Food
import hu.nutrinaplo.app.core.FoodDb
import hu.nutrinaplo.app.core.Gemini
import hu.nutrinaplo.app.core.OpenFoodFacts
import hu.nutrinaplo.app.core.Profile
import hu.nutrinaplo.app.core.WeightEntry
import hu.nutrinaplo.app.core.Reminders
import hu.nutrinaplo.app.data.Store
import hu.nutrinaplo.app.notify.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

/** Egy AI által felismert tétel, a helyi adatbázisbeli találattal. */
data class AiCandidate(
    val item: AiItem,
    val match: Food?,
    val useMatch: Boolean,
    val grams: Double,
    val selected: Boolean = true,
) {
    /** Az AI saját becsléséből képzett étel (ha nincs jó találat). */
    val aiFood: Food
        get() = Food(
            id = "ai:" + item.name.lowercase().hashCode(),
            name = item.name,
            category = "AI becslés",
            per100 = item.per100,
            source = "ai",
        )
    val food: Food get() = if (useMatch && match != null) match else aiFood
}

sealed interface AiState {
    data object Idle : AiState
    data object Loading : AiState
    data class Done(val candidates: List<AiCandidate>, val note: String) : AiState
    data class Error(val message: String) : AiState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Store.repository(app)

    private val _data get() = Store.data
    val data: StateFlow<AppData> = Store.data

    val loaded: StateFlow<Boolean> = Store.loaded

    private val _db = MutableStateFlow<FoodDb?>(null)
    val db: StateFlow<FoodDb?> = _db

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    // --- keresés ---
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _results = MutableStateFlow<List<Food>>(emptyList())
    val results: StateFlow<List<Food>> = _results
    private val _online = MutableStateFlow<List<Food>>(emptyList())
    val online: StateFlow<List<Food>> = _online
    private val _onlineLoading = MutableStateFlow(false)
    val onlineLoading: StateFlow<Boolean> = _onlineLoading
    private var searchJob: Job? = null

    // --- AI ---
    private val _ai = MutableStateFlow<AiState>(AiState.Idle)
    val ai: StateFlow<AiState> = _ai

    /** A hozzáadás képernyőn kiválasztott étkezés. */
    private val _meal = MutableStateFlow(Meals_suggest())
    val meal: StateFlow<Int> = _meal

    /** A Hozzáadás képernyő megnyitásakor azonnal induljon-e a vonalkód-olvasó. */
    private var scanRequested = false

    fun requestScan() {
        scanRequested = true
    }

    fun consumeScanRequest(): Boolean {
        val r = scanRequested
        scanRequested = false
        return r
    }

    /** Gyors hozzáadás alapértelmezett adaggal. */
    fun quickAdd(food: Food, meal: Int = _meal.value) {
        val g = food.servings.firstOrNull()?.grams ?: 100.0
        addEntry(food, g, meal)
        toast("${food.name} hozzáadva (${g.toInt()} g) – ${hu.nutrinaplo.app.core.Meals.names[meal]}")
    }

    init {
        viewModelScope.launch {
            Store.ensureLoaded(app)
            // régebbi verzióról: a rendszerhang helyett az app saját hangjai legyenek az alapértelmezettek
            val r0 = Store.data.value.reminders
            if (!r0.soundsMigrated) {
                Store.update {
                    it.copy(
                        reminders = r0.copy(
                            waterSound = if (r0.waterSound == "default") "app:nn_drop" else r0.waterSound,
                            mealSound = if (r0.mealSound == "default") "app:nn_chime" else r0.mealSound,
                            soundsMigrated = true,
                        )
                    )
                }
            }
            Notifications.schedule(app, Store.data.value.reminders)
        }
        viewModelScope.launch {
            try {
                _db.value = repo.loadFoodDb()
                if (_query.value.isNotBlank()) runLocalSearch(_query.value)
            } catch (e: Exception) {
                _messages.tryEmit("Az ételadatbázis betöltése nem sikerült: ${e.message}")
            }
        }
    }

    private fun update(fn: (AppData) -> AppData) {
        Store.update(fn)
    }

    fun updateReminders(r: Reminders) {
        update { it.copy(reminders = r) }
        Notifications.schedule(getApplication(), r)
    }

    fun testNotification() {
        Notifications.showTest(getApplication(), _data.value)
    }

    fun toast(msg: String) {
        _messages.tryEmit(msg)
    }

    // --- dátum ---
    fun setDate(d: LocalDate) {
        _date.value = d
    }

    fun shiftDate(days: Long) {
        _date.value = _date.value.plusDays(days)
    }

    fun setMeal(m: Int) {
        _meal.value = m
    }

    // --- napló ---
    fun addEntry(food: Food, grams: Double, meal: Int, date: LocalDate = _date.value) {
        if (grams <= 0) return
        update { d ->
            val e = DiaryEntry(
                id = d.nextId,
                date = date.toString(),
                meal = meal,
                food = food,
                grams = grams,
                time = System.currentTimeMillis(),
            )
            val rec = (listOf(food) + d.recents.filter { it.id != food.id }).take(40)
            d.copy(entries = d.entries + e, nextId = d.nextId + 1, recents = rec)
        }
    }

    fun updateEntry(id: Long, grams: Double, meal: Int) {
        update { d -> d.copy(entries = d.entries.map { if (it.id == id) it.copy(grams = grams, meal = meal) else it }) }
    }

    fun deleteEntry(id: Long) {
        update { d -> d.copy(entries = d.entries.filter { it.id != id }) }
    }

    /** Egy étkezés tételeinek másolása az előző napról a kiválasztott napra. */
    fun copyMealFromYesterday(meal: Int) {
        val today = _date.value
        val y = today.minusDays(1).toString()
        val src = _data.value.entries.filter { it.date == y && it.meal == meal }
        if (src.isEmpty()) {
            toast("Tegnap nem volt bejegyzés ennél az étkezésnél.")
            return
        }
        update { d ->
            var next = d.nextId
            val copies = src.map { it.copy(id = next++, date = today.toString(), time = System.currentTimeMillis()) }
            d.copy(entries = d.entries + copies, nextId = next)
        }
        toast("${src.size} tétel átmásolva tegnapról.")
    }

    fun entriesFor(date: LocalDate): List<DiaryEntry> = _data.value.entries.filter { it.date == date.toString() }

    // --- víz ---
    fun addWater(ml: Int) {
        val key = _date.value.toString()
        update { d ->
            val cur = d.water[key] ?: 0
            d.copy(water = d.water + (key to (cur + ml).coerceAtLeast(0)))
        }
    }

    // --- testsúly ---
    fun addWeight(kg: Double, date: LocalDate = LocalDate.now()) {
        if (kg <= 20 || kg > 400) return
        update { d ->
            val list = d.weights.filter { it.date != date.toString() } + WeightEntry(date.toString(), kg)
            val sorted = list.sortedBy { it.date }
            val latest = sorted.last()
            val prof = if (latest.date == date.toString()) d.profile.copy(weightKg = kg) else d.profile
            d.copy(weights = sorted, profile = prof)
        }
    }

    fun deleteWeight(date: String) {
        update { d -> d.copy(weights = d.weights.filter { it.date != date }) }
    }

    // --- profil ---
    fun updateProfile(p: Profile) {
        update { d -> d.copy(profile = p) }
    }

    // --- kedvencek / saját ételek ---
    fun isFavorite(food: Food): Boolean = _data.value.favorites.any { it.id == food.id }

    fun toggleFavorite(food: Food) {
        update { d ->
            if (d.favorites.any { it.id == food.id }) d.copy(favorites = d.favorites.filter { it.id != food.id })
            else d.copy(favorites = listOf(food) + d.favorites)
        }
    }

    fun saveCustomFood(food: Food) {
        update { d ->
            val f = if (food.id.isBlank()) food.copy(id = "custom:${System.currentTimeMillis()}", source = "custom") else food
            d.copy(customFoods = listOf(f) + d.customFoods.filter { it.id != f.id })
        }
        toast("Saját étel elmentve.")
    }

    fun deleteCustomFood(id: String) {
        update { d -> d.copy(customFoods = d.customFoods.filter { it.id != id }, favorites = d.favorites.filter { it.id != id }) }
    }

    // --- keresés ---
    fun setQuery(q: String) {
        _query.value = q
        _online.value = emptyList()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(120)
            runLocalSearch(q)
        }
    }

    private suspend fun runLocalSearch(q: String) {
        val db = _db.value
        val extra = _data.value.customFoods
        _results.value = if (q.isBlank() || db == null) emptyList()
        else withContext(Dispatchers.Default) { db.search(q, 80, extra) }
    }

    fun searchOnline() {
        val q = _query.value.trim()
        if (q.length < 2) return
        _onlineLoading.value = true
        viewModelScope.launch {
            try {
                _online.value = withContext(Dispatchers.IO) { OpenFoodFacts.search(q) }
                if (_online.value.isEmpty()) toast("Nincs online találat erre: \"$q\"")
            } catch (e: Exception) {
                toast("Online keresés sikertelen: ${e.message ?: "hálózati hiba"}")
            } finally {
                _onlineLoading.value = false
            }
        }
    }

    fun lookupBarcode(code: String, onFound: (Food) -> Unit) {
        viewModelScope.launch {
            _onlineLoading.value = true
            try {
                val local = (_data.value.customFoods + _data.value.recents).firstOrNull { it.barcode == code }
                val food = local ?: withContext(Dispatchers.IO) { OpenFoodFacts.byBarcode(code) }
                if (food == null) toast("A termék ($code) nem található az Open Food Facts adatbázisban. Felveheted saját ételként.")
                else onFound(food)
            } catch (e: Exception) {
                toast("Vonalkód keresés sikertelen: ${e.message ?: "hálózati hiba"}")
            } finally {
                _onlineLoading.value = false
            }
        }
    }

    // --- AI ---
    fun analyzeAi(text: String?, imageBase64: String?) {
        val p = _data.value.profile
        if (p.geminiKey.isBlank()) {
            _ai.value = AiState.Error("Az AI felismeréshez add meg az ingyenes Gemini API-kulcsodat a Profil → AI beállítások menüben.")
            return
        }
        _ai.value = AiState.Loading
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { Gemini.analyze(p.geminiKey, p.geminiModel, text, imageBase64) }
                val db = _db.value
                val cands = res.items.map { item ->
                    val match = db?.bestMatch(item.search) ?: db?.bestMatch(item.name)
                    // csak akkor használjuk az adatbázis találatot, ha a kalóriatartalom nem tér el túlságosan
                    val aiKcal = item.per100["kcal"]
                    val plausible = match != null && (aiKcal == null || aiKcal <= 0 ||
                        (match.kcal in (aiKcal * 0.5)..(aiKcal * 1.8)))
                    AiCandidate(item = item, match = match, useMatch = plausible, grams = item.grams)
                }
                _ai.value = AiState.Done(cands, res.note)
            } catch (e: Exception) {
                _ai.value = AiState.Error(Gemini.friendlyError(e))
            }
        }
    }

    fun updateCandidate(index: Int, c: AiCandidate) {
        val s = _ai.value as? AiState.Done ?: return
        _ai.value = s.copy(candidates = s.candidates.mapIndexed { i, old -> if (i == index) c else old })
    }

    fun commitAi(meal: Int) {
        val s = _ai.value as? AiState.Done ?: return
        val chosen = s.candidates.filter { it.selected }
        chosen.forEach { addEntry(it.food, it.grams, meal) }
        toast("${chosen.size} tétel hozzáadva.")
        _ai.value = AiState.Idle
    }

    /** API-kulcs ellenőrzés állapota: null = nincs, "..." = folyamatban, egyébként eredmény szöveg. */
    private val _aiTest = MutableStateFlow<Pair<Boolean, String>?>(null)
    val aiTest: StateFlow<Pair<Boolean, String>?> = _aiTest
    private val _aiTesting = MutableStateFlow(false)
    val aiTesting: StateFlow<Boolean> = _aiTesting

    fun testAi() {
        val p = _data.value.profile
        _aiTesting.value = true
        _aiTest.value = null
        viewModelScope.launch {
            try {
                val model = withContext(Dispatchers.IO) { Gemini.test(p.geminiKey, p.geminiModel) }
                _aiTest.value = true to "Működik! ✓ (modell: $model)"
            } catch (e: Exception) {
                _aiTest.value = false to Gemini.friendlyError(e)
            } finally {
                _aiTesting.value = false
            }
        }
    }

    fun resetAi() {
        _ai.value = AiState.Idle
    }

    // --- mentés / visszatöltés ---
    fun exportJson(): String = repo.export(_data.value)

    fun importJson(text: String): Boolean = try {
        val d = repo.import(text)
        update { d }
        true
    } catch (e: Exception) {
        toast("Hibás mentésfájl: ${e.message}")
        false
    }

    fun clearAll() {
        update { AppData(profile = it.profile) }
    }

    fun targets() = Calc.targets(_data.value.profile)
}

@Suppress("FunctionName")
private fun Meals_suggest(): Int = hu.nutrinaplo.app.core.Meals.suggest(LocalTime.now().hour)
