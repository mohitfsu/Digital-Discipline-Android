package com.digitaldiscipline.spike.logging

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

object EventLogger {
    private const val TAG = "DigitalDiscipline"
    private const val MAX_LOGS = 200

    private val logList = CopyOnWriteArrayList<LogEvent>()
    private val _logsFlow = MutableStateFlow<List<LogEvent>>(emptyList())
    val logsFlow: StateFlow<List<LogEvent>> = _logsFlow.asStateFlow()

    fun log(
        source: String,
        packageName: String,
        eventType: String,
        latencyMs: Long? = null,
        details: String = ""
    ) {
        val event = LogEvent(
            source = source,
            packageName = packageName,
            eventType = eventType,
            latencyMs = latencyMs,
            details = details
        )
        val formatted = event.toFormattedString()
        Log.i(TAG, formatted)

        logList.add(0, event)
        while (logList.size > MAX_LOGS) {
            logList.removeAt(logList.size - 1)
        }
        _logsFlow.value = logList.toList()
    }

    fun clear() {
        logList.clear()
        _logsFlow.value = emptyList()
    }
}
