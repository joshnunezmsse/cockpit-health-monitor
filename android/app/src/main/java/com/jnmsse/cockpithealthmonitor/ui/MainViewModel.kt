package com.jnmsse.cockpithealthmonitor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jnmsse.cockpithealthmonitor.datastore.UserPreferencesManager
import com.jnmsse.cockpithealthmonitor.ui.navigation.NavRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class MainUiState(
    val startDestination: NavRoute = NavRoute.Discover
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val userPreferencesManager = UserPreferencesManager(application)

    fun processIntent(serverUrlFromIntent: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val destination = if (!serverUrlFromIntent.isNullOrBlank()) {
                // If a URL comes from the intent, construct the full route with the
                // URL as a parameter. We DON'T save it as the app default.
                //val encodedUrl = URLEncoder.encode(serverUrlFromIntent, StandardCharsets.UTF_8.toString())
                NavRoute.Health(serverUrl = serverUrlFromIntent)
            } else {
                // For a normal app launch, check saved preferences.
                val savedUrl = userPreferencesManager.serverUrl.first()
                if (savedUrl.isNullOrEmpty()) {
                    NavRoute.Discover
                } else {
                    NavRoute.Health(serverUrl = savedUrl)
                }
            }
            _uiState.value = MainUiState(startDestination = destination)
        }
    }

    /**
     * Saves the server URL for the main application flow. This is only called
     * when the user selects a server from the in-app discovery screen.
     */
    fun saveServerUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesManager.saveServerUrl(url)
        }
    }
}
