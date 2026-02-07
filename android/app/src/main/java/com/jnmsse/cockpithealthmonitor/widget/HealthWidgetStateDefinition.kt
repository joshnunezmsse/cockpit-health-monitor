package com.jnmsse.cockpithealthmonitor.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object HealthWidgetStateDefinition : GlanceStateDefinition<HealthWidgetState> {

    private const val DATA_STORE_FILENAME = "health_widget_store"

    private val Context.healthWidgetStore: DataStore<HealthWidgetState> by dataStore(
        fileName = DATA_STORE_FILENAME,
        serializer = HealthWidgetStateSerializer
    )

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<HealthWidgetState> {
        return context.healthWidgetStore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return context.filesDir.resolve(DATA_STORE_FILENAME)
    }
}

object HealthWidgetStateSerializer : Serializer<HealthWidgetState> {
    override val defaultValue: HealthWidgetState
        get() = HealthWidgetState()

    override suspend fun readFrom(input: InputStream): HealthWidgetState {
        return try {
            Json.decodeFromString(
                HealthWidgetState.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: HealthWidgetState, output: OutputStream) {
        output.write(
            Json.encodeToString(HealthWidgetState.serializer(), t).encodeToByteArray()
        )
    }
}