package com.lambda.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

val LambdaJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    isLenient = true
    prettyPrint = false
}

/**
 * Keeps all app data in one JSON file inside the app's private storage
 * (/data/data/com.lambda.app/files). Nothing ever leaves the phone.
 */
class Repository(private val context: Context) {
    private val file = File(context.filesDir, "lambda_data.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var saveJob: Job? = null

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppData> = _state.asStateFlow()

    val attachmentsDir: File
        get() = File(context.filesDir, "attachments").also { it.mkdirs() }

    private fun load(): AppData {
        if (!file.exists()) return AppData()
        return try {
            LambdaJson.decodeFromString(AppData.serializer(), file.readText())
        } catch (e: Exception) {
            // keep the unreadable file so nothing is silently lost, then start fresh
            runCatching { file.copyTo(File(context.filesDir, "lambda_data.corrupt.json"), overwrite = true) }
            AppData()
        }
    }

    fun update(block: (AppData) -> AppData) {
        _state.update(block)
        scheduleSave()
    }

    fun replace(data: AppData) {
        _state.value = data
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            writeNow()
        }
    }

    /** Called when the app goes to background so nothing is lost. */
    fun flush() {
        saveJob?.cancel()
        writeNow()
    }

    private fun writeNow() {
        synchronized(lock) {
            try {
                val json = LambdaJson.encodeToString(AppData.serializer(), _state.value)
                val tmp = File(context.filesDir, "lambda_data.json.tmp")
                tmp.writeText(json)
                if (!tmp.renameTo(file)) {
                    file.writeText(json)
                    tmp.delete()
                }
            } catch (_: Exception) {
                // storage full etc. - will retry on next change
            }
        }
    }
}
