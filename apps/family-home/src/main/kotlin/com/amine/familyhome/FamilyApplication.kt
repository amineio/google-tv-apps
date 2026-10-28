package com.amine.familyhome

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId

private val Context.familyData by preferencesDataStore("family")

class DataStoreFamilyRepository(private val context: Context) : FamilyRepository {
    private val key = stringPreferencesKey("state_v1")
    override suspend fun load(): FamilyState {
        val text = context.familyData.data.first()[key] ?: return FamilyState()
        return FamilyStateCodec.decode(text)
    }
    override suspend fun save(state: FamilyState) {
        val text = FamilyStateCodec.encode(state)
        context.familyData.edit { it[key] = text }
    }
}

class FamilyApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var controller: FamilyController
        private set
    override fun onCreate() {
        super.onCreate()
        controller = FamilyController(DataStoreFamilyRepository(this), SystemClock::elapsedRealtime) {
            UsageSample(SystemClock.elapsedRealtime(), System.currentTimeMillis(), ZoneId.systemDefault())
        }
        scope.launch { controller.load() }
    }
}

val Context.family: FamilyController get() = (applicationContext as FamilyApplication).controller
