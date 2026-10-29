package hu.nutrinaplo.app.data

import android.content.Context
import hu.nutrinaplo.app.core.AppData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Folyamatszintű, egyetlen adatforrás. Az app felülete és az értesítések (pl. "+250 ml" gomb)
 * ugyanezt használják, így nem írják felül egymás változtatásait.
 */
object Store {
    private val loadMutex = Mutex()
    private val saveMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var repo: Repository? = null

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    fun repository(context: Context): Repository =
        repo ?: Repository(context.applicationContext).also { repo = it }

    suspend fun ensureLoaded(context: Context) {
        loadMutex.withLock {
            if (_loaded.value) return
            _data.value = repository(context).load()
            _loaded.value = true
        }
    }

    /** Módosítás + mentés a háttérben. */
    fun update(fn: (AppData) -> AppData) {
        synchronized(this) { _data.value = fn(_data.value) }
        scope.launch { persist() }
    }

    /** Módosítás + mentés, megvárva (értesítésekből). */
    suspend fun updateNow(fn: (AppData) -> AppData) {
        synchronized(this) { _data.value = fn(_data.value) }
        persist()
    }

    private suspend fun persist() {
        val r = repo ?: return
        saveMutex.withLock { r.save(_data.value) }
    }
}
