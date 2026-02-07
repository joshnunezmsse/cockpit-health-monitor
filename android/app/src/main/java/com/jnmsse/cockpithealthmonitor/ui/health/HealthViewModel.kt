package com.jnmsse.cockpithealthmonitor.ui.health

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jnmsse.cockpithealthmonitor.di.Injection
import com.jnmsse.cockpithealthmonitor.data.HealthStatus
import com.jnmsse.cockpithealthmonitor.repository.HealthRepository
import com.jnmsse.cockpithealthmonitor.widget.HealthWidget
import com.jnmsse.cockpithealthmonitor.widget.HealthWidgetStateDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "HealthViewModel"

class HealthViewModel(
    private val application: Application,
    private val healthRepository: HealthRepository,
    private val serverUrlOverride: String?
) : AndroidViewModel(application) {

    companion object {
        fun provideFactory(application: Application, serverUrl: String?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HealthViewModel(
                        application = application,
                        healthRepository = Injection.provideHealthRepository(application),
                        serverUrlOverride = serverUrl
                    ) as T
                }
            }
    }

    private val refreshFlow = MutableSharedFlow<Unit>(replay = 1)

    private val pollingFlow = flow {
        while (true) {
            emit(Unit)
            delay(10_000)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HealthState> = merge(refreshFlow, pollingFlow)
        .flatMapLatest { loadData() }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HealthState(isLoading = true)
        )

    init {
        Log.d(TAG, "ViewModel created with constructor override URL: '$serverUrlOverride'")
        handleEvent(HealthEvent.OnRefresh)
    }

    fun handleEvent(event: HealthEvent) {
        when (event) {
            HealthEvent.OnRefresh -> {
                refreshFlow.tryEmit(Unit)
            }
            else -> { /* No-op */ }
        }
    }

    private fun loadData(): Flow<HealthState> = flow {
        val urlToLoad = serverUrlOverride ?: healthRepository.serverUrl.first()
        Log.d(TAG, "Active URL decided: '$urlToLoad' (from override: '$serverUrlOverride')")

        if (urlToLoad.isNullOrEmpty()) {
            emit(HealthState(isLoading = false, error = "Server not configured"))
            return@flow
        }

        Log.d(TAG, "Attempting to load data from: $urlToLoad")
        val hostname = Uri.parse(urlToLoad).host ?: "Unknown Host"
        healthRepository.getHealthStatus(urlToLoad)
            .fold(
                onSuccess = { healthStatus ->
                    syncWithWidgets(urlToLoad, healthStatus)
                    emit(
                        HealthState(
                            isLoading = false,
                            hostname = hostname,
                            healthStatus = healthStatus,
                            error = null
                        )
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load data for $urlToLoad", error)
                    emit(
                        HealthState(
                            isLoading = false,
                            hostname = hostname,
                            error = error.message
                        )
                    )
                }
            )
    }

    private fun syncWithWidgets(serverUrl: String, healthStatus: List<HealthStatus>) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(HealthWidget::class.java)

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, HealthWidgetStateDefinition, glanceId) { currentState ->
                    // Only update the widget if it's monitoring the same server
                    if (currentState.serverUrl == serverUrl) {
                        Log.d(TAG, "Syncing widget with ID: $glanceId for server URL: $serverUrl")
                        currentState.copy(healthStatus = healthStatus)
                    } else {
                        currentState // No changes needed
                    }
                }
                // Important: We need to explicitly tell the widget to update its UI
                // after we've changed its state.
                HealthWidget().update(context, glanceId)
            }
        }
    }
}
