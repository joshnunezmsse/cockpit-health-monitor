package com.jnmsse.cockpithealthmonitor.ui.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoute {
    @Serializable
    object Discover: NavRoute

    @Serializable
    data class Health(val serverUrl: String): NavRoute
}