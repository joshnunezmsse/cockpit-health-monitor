package com.jnmsse.cockpithealthmonitor.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentWidth
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jnmsse.cockpithealthmonitor.R
import com.jnmsse.cockpithealthmonitor.data.HealthStatus
import com.jnmsse.cockpithealthmonitor.ui.MainActivity
import kotlinx.serialization.Serializable

private const val TAG = "HealthWidget"

@Serializable
data class HealthWidgetState(
    val serverUrl: String? = null,
    val serverName: String = "Not Configured",
    val healthStatus: List<HealthStatus> = emptyList()
)

class HealthWidget : GlanceAppWidget() {
    companion object {
        val SMALL = DpSize(60.dp, 60.dp)
        val LARGE = DpSize(60.dp, 84.dp)
    }

    override val stateDefinition: GlanceStateDefinition<HealthWidgetState> = HealthWidgetStateDefinition

    override val sizeMode = SizeMode.Responsive(sizes = setOf(SMALL, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d(TAG, "provideGlance called: $id")
        provideContent {
            Log.d(TAG, "Actual size: ${LocalSize.current}: ${LocalSize.current.width}X${LocalSize.current.height}")
            GlanceTheme {
                val state = currentState<HealthWidgetState>()
                Log.d(TAG, "provideGlance: Current state = $state")
                HealthWidgetContent(state = state, widgetSize = LocalSize.current)
            }
        }
    }
}

@Composable
fun HealthWidgetContent(state: HealthWidgetState, widgetSize: DpSize) {
    val aggregateHealth = getAggregateHealth(state.healthStatus)

    val openMainActivityAction = actionStartActivity(
        Intent(LocalContext.current, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_SERVER_URL, state.serverUrl)
        }
    )

    Log.d(TAG, "HealthWidgetContent: Aggregate health = $aggregateHealth")

    val (tintColor, contentDesc) = when (aggregateHealth) {
        "green" -> Pair(ColorFilter.tint(ColorProvider(R.color.color_green_healthy)), "Status OK")
        "orange" -> Pair(ColorFilter.tint(ColorProvider(R.color.color_orange_warning)), "Status Warning")
        "red" -> Pair(ColorFilter.tint(ColorProvider(R.color.color_red_unhealthy)), "Status Error")
        else -> Pair(ColorFilter.tint(ColorProvider(resId = R.color.color_gray_unknown)), "Status Unknown")
    }.also {
        Log.d(TAG, "HealthWidgetContent: Selected icon content description = ${it.second}")
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(openMainActivityAction),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // background shape
                Box(
                    modifier = GlanceModifier
                        .size(60.dp)
                        .background(ImageProvider(R.drawable.shape_rectangle_background)),
                    content = {}
                )

                // The logo, drawn on top of the circle
                androidx.glance.Image(
                    provider = ImageProvider(R.drawable.ic_cockpit_logo),
                    contentDescription = contentDesc,
                    colorFilter = tintColor,
                    modifier = GlanceModifier.size(48.dp) // Match the circle size
                )

            }

            if (widgetSize == HealthWidget.LARGE) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Box(
                    modifier = GlanceModifier
                        .wrapContentWidth()
                        .height(22.dp)
                        .background(ImageProvider(R.drawable.shape_rectangle_background)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        modifier = GlanceModifier.padding(start = 4.dp, end = 4.dp),
                        text = state.serverName,
                        style = TextStyle(color = ColorProvider(resId = R.color.widget_text_color)),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun getAggregateHealth(statuses: List<HealthStatus>): String {
    if (statuses.any { it.health.equals("red", ignoreCase = true) }) {
        return "red"
    }
    if (statuses.any { it.health.equals("orange", ignoreCase = true) }) {
        return "orange"
    }
    if (statuses.isNotEmpty() && statuses.all { it.health.equals("green", ignoreCase = true) }) {
        return "green"
    }
    return "unknown"
}
