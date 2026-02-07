package com.jnmsse.cockpithealthmonitor.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.jnmsse.cockpithealthmonitor.ui.navigation.AppNavigation
import com.jnmsse.cockpithealthmonitor.ui.theme.CockpitHealthMonitorTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SERVER_URL = "serverUrl"
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serverUrlFromIntent = intent?.getStringExtra(EXTRA_SERVER_URL)
        viewModel.processIntent(serverUrlFromIntent)

        setContent {
            CockpitHealthMonitorTheme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}