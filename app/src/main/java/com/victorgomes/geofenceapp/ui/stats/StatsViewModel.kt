package com.victorgomes.geofenceapp.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.victorgomes.geofenceapp.data.repository.GeofenceRepository
import com.victorgomes.geofenceapp.utils.HolidayUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar

enum class TimeFilter { ALL, WEEKDAYS, WEEKENDS, NO_HOLIDAYS, WORKDAYS }

data class HourlyCount(val hour: Int, val enterCount: Int, val exitCount: Int)
data class FenceAvgTime(val fenceName: String, val avgHours: Float)
data class FenceInterval(val startMs: Long, val endMs: Long)
data class FenceTimeline(val fenceName: String, val intervals: List<FenceInterval>)
data class TimelineResult(val dayStartMs: Long, val timelines: List<FenceTimeline>)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository    = GeofenceRepository(application)
    private val selectedFenceId = MutableStateFlow<String?>(null)
    private val timeFilter      = MutableStateFlow(TimeFilter.ALL)
    private val selectedDate    = MutableStateFlow(startOfDay(System.currentTimeMillis()))

    val allFences      = repository.allFenceConfigs.asLiveData()
    val selectedDateMs = selectedDate.asLiveData()

    val histogram = combine(selectedFenceId, repository.allEvents) { fenceId, events ->
        if (fenceId == null) return@combine emptyList<HourlyCount>()
        val filtered = events.filter { it.geofenceId == fenceId }
        val cal = Calendar.getInstance()
        (0..287).map { slot ->
            HourlyCount(
                hour = slot,
                enterCount = filtered.count { e ->
                    e.eventType == "ENTER" && run {
                        cal.timeInMillis = e.timestamp
                        (cal.get(Calendar.HOUR_OF_DAY) * 12 + cal.get(Calendar.MINUTE) / 5) == slot
                    }
                },
                exitCount = filtered.count { e ->
                    e.eventType == "EXIT" && run {
                        cal.timeInMillis = e.timestamp
                        (cal.get(Calendar.HOUR_OF_DAY) * 12 + cal.get(Calendar.MINUTE) / 5) == slot
                    }
                }
            )
        }
    }.flowOn(Dispatchers.Default).asLiveData()

    val avgTimePerFence = combine(timeFilter, repository.allFenceConfigs, repository.allEvents) { filter, configs, events ->
        val now = System.currentTimeMillis()
        val todayStart = startOfDay(now)
        configs.mapNotNull { config ->
            val fenceEvents = events.filter { it.geofenceId == config.id }.sortedBy { it.timestamp }
            val allIntervals = mutableListOf<FenceInterval>()
            var openEnter: Long? = null
            for (e in fenceEvents) {
                when (e.eventType) {
                    "ENTER" -> openEnter = e.timestamp
                    "EXIT"  -> if (openEnter != null) {
                        allIntervals.add(FenceInterval(openEnter, e.timestamp))
                        openEnter = null
                    }
                }
            }
            if (openEnter != null && openEnter >= todayStart) {
                allIntervals.add(FenceInterval(openEnter, now))
            }
            val intervals = when (filter) {
                TimeFilter.ALL         -> allIntervals
                TimeFilter.WEEKDAYS    -> allIntervals.filter { isWeekday(it.startMs) }
                TimeFilter.WEEKENDS    -> allIntervals.filter { isWeekend(it.startMs) }
                TimeFilter.NO_HOLIDAYS -> allIntervals.filter { !HolidayUtils.isHoliday(it.startMs) }
                TimeFilter.WORKDAYS    -> allIntervals.filter { isWeekday(it.startMs) && !HolidayUtils.isHoliday(it.startMs) }
            }
            if (intervals.isEmpty()) return@mapNotNull null
            val byDay = intervals.groupBy { startOfDay(it.startMs) }
            val dailySums = byDay.values.map { day -> day.sumOf { it.endMs - it.startMs } }
            FenceAvgTime(config.name, (dailySums.average() / 3_600_000.0).toFloat())
        }
    }.flowOn(Dispatchers.Default).asLiveData()

    val timelineForDay = combine(selectedDate, repository.allFenceConfigs, repository.allEvents) { dayStart, configs, events ->
        val now    = System.currentTimeMillis()
        val dayEnd = dayStart + 24L * 3_600_000L
        val cap    = minOf(dayEnd, now)
        val timelines = configs.mapNotNull { config ->
            val fenceEvents = events.filter { it.geofenceId == config.id }.sortedBy { it.timestamp }
            val intervals   = mutableListOf<FenceInterval>()
            val lastBefore  = fenceEvents.lastOrNull { it.timestamp < dayStart }
            var openEnter: Long? = if (lastBefore?.eventType == "ENTER") dayStart else null
            for (e in fenceEvents.filter { it.timestamp in dayStart until dayEnd }) {
                when (e.eventType) {
                    "ENTER" -> openEnter = e.timestamp
                    "EXIT"  -> if (openEnter != null) {
                        intervals.add(FenceInterval(openEnter, e.timestamp))
                        openEnter = null
                    }
                }
            }
            if (openEnter != null) intervals.add(FenceInterval(openEnter, cap))
            if (intervals.isEmpty()) null else FenceTimeline(config.name, intervals)
        }
        TimelineResult(dayStart, timelines)
    }.flowOn(Dispatchers.Default).asLiveData()

    fun selectFence(fenceId: String) { selectedFenceId.value = fenceId }
    fun setTimeFilter(filter: TimeFilter) { timeFilter.value = filter }

    fun prevDay() {
        selectedDate.value = selectedDate.value - 24L * 3_600_000L
    }

    fun nextDay() {
        val next = selectedDate.value + 24L * 3_600_000L
        if (next <= startOfDay(System.currentTimeMillis())) selectedDate.value = next
    }

    fun selectDay(dayStartMs: Long) {
        selectedDate.value = dayStartMs.coerceAtMost(startOfDay(System.currentTimeMillis()))
    }

    private fun isWeekday(ms: Long): Boolean {
        val dow = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.DAY_OF_WEEK)
        return dow in Calendar.MONDAY..Calendar.FRIDAY
    }

    private fun isWeekend(ms: Long): Boolean {
        val dow = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    private fun startOfDay(ms: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
