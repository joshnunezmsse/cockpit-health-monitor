package com.jnmsse.cockpithealthmonitor.ui.navigation

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jnmsse.cockpithealthmonitor.ui.MainViewModel
import com.jnmsse.cockpithealthmonitor.ui.discover.ServerDiscoveryScreen
import com.jnmsse.cockpithealthmonitor.ui.health.HealthScreen
import kotlinx.serialization.Serializable
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import com.jnmsse.cockpithealthmonitor.di.Injection
import com.jnmsse.cockpithealthmonitor.ui.discover.ServerDiscoveryViewModel
import com.jnmsse.cockpithealthmonitor.ui.health.HealthViewModel

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = uiState.startDestination) {
        composable<NavRoute.Discover> {
            val serverDiscoveryViewModel: ServerDiscoveryViewModel = viewModel()
            val state by serverDiscoveryViewModel.state.collectAsStateWithLifecycle()

            ServerDiscoveryScreen(
                state = state,
                handleEvent = serverDiscoveryViewModel::handleEvent,
                navController = navController,
                onServerSelected = { serverUrl ->
                    viewModel.saveServerUrl(serverUrl)
                    navController.navigate(
                        route = NavRoute.Health(serverUrl = serverUrl),
                        navOptions = NavOptions.Builder()
                            .setPopUpTo(route = NavRoute.Discover, inclusive = true)
                            .build()
                    )
                }
            )
        }
        composable<NavRoute.Health> { backStackEntry ->
            val serverUrl = backStackEntry.arguments?.getString("serverUrl")

            val healthViewModel: HealthViewModel = viewModel(
                key = serverUrl,
                factory = HealthViewModel.provideFactory(
                    application = LocalContext.current.applicationContext as Application,
                    serverUrl = serverUrl
                )
            )

            val state by healthViewModel.state.collectAsStateWithLifecycle()

            HealthScreen(
                state = state,
                handleEvent = healthViewModel::handleEvent,
                onChangeServer = {
                    navController.navigate(route = NavRoute.Discover)
                },
                onCardClicked = { url ->
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }

}
