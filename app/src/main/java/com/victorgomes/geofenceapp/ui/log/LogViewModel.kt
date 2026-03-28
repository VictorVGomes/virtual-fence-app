package com.victorgomes.geofenceapp.ui.log

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.victorgomes.geofenceapp.data.database.GeofenceConfigEntity
import com.victorgomes.geofenceapp.data.database.GeofenceEventEntity
import com.victorgomes.geofenceapp.data.repository.GeofenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogState(
    val events: List<GeofenceEventEntity>,
    val fenceNames: Map<String, String>,
    val allFences: List<GeofenceConfigEntity>
)

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GeofenceRepository(application)
    private val _filterFenceId = MutableStateFlow<String?>(null)

    val logState = combine(
        repository.allEvents,
        repository.allFenceConfigs,
        _filterFenceId
    ) { events, configs, fenceId ->
        val nameMap = configs.associate { it.id to it.name }
        val filtered = if (fenceId == null) events else events.filter { it.geofenceId == fenceId }
        LogState(filtered, nameMap, configs)
    }.asLiveData()

    fun setFilter(fenceId: String?) { _filterFenceId.value = fenceId }

    fun updateObs(id: Long, obs: String?) {
        viewModelScope.launch { repository.updateEventObs(id, obs) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAllEvents() }
    }

    fun deleteEvents(ids: Collection<Long>) {
        viewModelScope.launch { repository.deleteEvents(ids) }
    }

    fun getStats(onResult: (total: Int, enters: Int, exits: Int, topFence: String?) -> Unit) {
        viewModelScope.launch {
            val allEvents = repository.getAllEventsList()
            val configs = repository.getAllFenceConfigsList()
            val nameMap = configs.associate { it.id to it.name }
            val enters = allEvents.count { it.eventType == "ENTER" }
            val exits = allEvents.count { it.eventType == "EXIT" }
            val topEntry = allEvents.groupBy { it.geofenceId }.maxByOrNull { it.value.size }
            val topFence = topEntry?.let { (id, events) ->
                "${nameMap[id] ?: id} (${events.size} crossings)"
            }
            withContext(Dispatchers.Main) {
                onResult(allEvents.size, enters, exits, topFence)
            }
        }
    }

    fun exportToCsv(context: Context, onReady: (Intent) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val events = repository.getAllEventsList()

                if (events.isEmpty()) {
                    withContext(Dispatchers.Main) { onError("No events to export.") }
                    return@launch
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                val csv = buildString {
                    appendLine("id,date,time,event_type,fence_name,fence_id,latitude,longitude,obs")
                    events.forEach { event ->
                        val fenceName = repository.getFenceById(event.geofenceId)?.name
                            ?: event.geofenceId
                        val date = dateFormat.format(Date(event.timestamp))
                        val time = timeFormat.format(Date(event.timestamp))
                        val obs = "\"${event.obs ?: ""}\""
                        appendLine(
                            "${event.id},$date,$time,${event.eventType}," +
                            "\"$fenceName\",${event.geofenceId}," +
                            "${event.latitude},${event.longitude},$obs"
                        )
                    }
                }

                val exportsDir = File(context.cacheDir, "exports").also { it.mkdirs() }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(Date())
                val file = File(exportsDir, "geofence_events_$timestamp.csv")
                file.writeText(csv)

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "GeoFence Events Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    onReady(Intent.createChooser(shareIntent, "Export CSV via…"))
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Export failed: ${e.message}") }
            }
        }
    }
}
