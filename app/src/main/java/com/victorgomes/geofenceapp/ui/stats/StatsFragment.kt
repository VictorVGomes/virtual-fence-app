package com.victorgomes.geofenceapp.ui.stats

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.victorgomes.geofenceapp.R
import com.victorgomes.geofenceapp.data.database.GeofenceConfigEntity
import com.victorgomes.geofenceapp.databinding.FragmentStatsBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatsViewModel by viewModels()

    private var chartTextColor = 0xFFFFFFFF.toInt()

    private var lastHistogramData: List<HourlyCount> = emptyList()
    private var lastAvgTimeData:   List<FenceAvgTime>  = emptyList()

    private val barColors = listOf(
        0xFF4CAF50.toInt(), 0xFF2196F3.toInt(), 0xFFFF9800.toInt(), 0xFFE91E63.toInt(),
        0xFF9C27B0.toInt(), 0xFF00BCD4.toInt(), 0xFF795548.toInt(), 0xFF607D8B.toInt()
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setupFilterChips()
        setupTimeline()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupCharts() {
        chartTextColor = MaterialColors.getColor(
            requireContext(), com.google.android.material.R.attr.colorOnSurface, 0xFFFFFFFF.toInt()
        )

        listOf(binding.chartHistogram, binding.chartAvgTime).forEach { chart ->
            chart.description.isEnabled = false
            chart.setDrawGridBackground(false)
            chart.setDrawBarShadow(false)
            chart.setScaleEnabled(false)
            chart.isDragEnabled = false
            chart.setDoubleTapToZoomEnabled(false)
            chart.axisRight.isEnabled = false
            chart.legend.isEnabled = true
            chart.legend.textColor = chartTextColor
            chart.setNoDataTextColor(chartTextColor)
        }

        binding.chartHistogram.apply {
            setNoDataText("Select a fence to view distribution")
            xAxis.position           = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity        = 1f
            xAxis.textSize           = 9f
            xAxis.textColor          = chartTextColor
            xAxis.setCenterAxisLabels(true)
            xAxis.labelRotationAngle = -45f
            xAxis.setLabelCount(24, true)
            axisLeft.axisMinimum     = 0f
            axisLeft.textColor       = chartTextColor
            isScaleXEnabled          = true
            isScaleYEnabled          = false
            isDragEnabled            = true
            setDoubleTapToZoomEnabled(true)
        }

        binding.chartAvgTime.apply {
            setNoDataText("No fence data available")
            xAxis.position           = XAxis.XAxisPosition.BOTTOM
            xAxis.labelRotationAngle = -30f
            xAxis.textColor          = chartTextColor
            axisLeft.axisMinimum     = 0f
            axisLeft.textColor       = chartTextColor
            legend.isEnabled         = false
            setExtraBottomOffset(20f)
        }

        binding.chartHistogram.marker = StatsMarkerView(requireContext()) { e, _ ->
            val slot = e.x.toInt()
            val item = lastHistogramData.getOrNull(slot)
            if (item != null) {
                val h = slot / 12
                val m = (slot % 12) * 5
                "%02d:%02d\n↑ %d  ↓ %d".format(h, m, item.enterCount, item.exitCount)
            } else ""
        }
        binding.chartAvgTime.marker = StatsMarkerView(requireContext()) { e, _ ->
            val item = lastAvgTimeData.getOrNull(e.x.toInt())
            if (item != null) "${item.fenceName}\n${"%.1f".format(item.avgHours)} h/day avg"
            else ""
        }

        // Within-chart empty-space tap: clear own highlight.
        binding.chartHistogram.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) { binding.chartAvgTime.highlightValues(null) }
            override fun onNothingSelected() {
                binding.chartHistogram.highlightValues(null)
                binding.chartAvgTime.highlightValues(null)
            }
        })
        binding.chartAvgTime.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) { binding.chartHistogram.highlightValues(null) }
            override fun onNothingSelected() {
                binding.chartAvgTime.highlightValues(null)
                binding.chartHistogram.highlightValues(null)
            }
        })

        // Any touch anywhere in the scroll view clears both charts before dispatching to children.
        // This fires even when a child (chart, button, chip) consumes the event.
        binding.nestedScrollView.onDownTouch = {
            binding.chartHistogram.highlightValues(null)
            binding.chartAvgTime.highlightValues(null)
            binding.timelineView.clearTooltip()
        }
    }

    private fun setupFilterChips() {
        updateFilterDescription(TimeFilter.ALL)
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                R.id.chipWeekdays   in checkedIds -> TimeFilter.WEEKDAYS
                R.id.chipWeekends   in checkedIds -> TimeFilter.WEEKENDS
                R.id.chipNoHolidays in checkedIds -> TimeFilter.NO_HOLIDAYS
                R.id.chipWorkdays   in checkedIds -> TimeFilter.WORKDAYS
                else                              -> TimeFilter.ALL
            }
            viewModel.setTimeFilter(filter)
            updateFilterDescription(filter)
        }
    }

    private fun updateFilterDescription(filter: TimeFilter) {
        binding.tvFilterDescription.text = when (filter) {
            TimeFilter.ALL ->
                "Average across all recorded days."
            TimeFilter.WEEKDAYS ->
                "Only days where the interval started on Mon–Fri."
            TimeFilter.WEEKENDS ->
                "Only days where the interval started on Sat or Sun."
            TimeFilter.NO_HOLIDAYS ->
                "Excludes intervals that started on a public holiday; weekends are kept."
            TimeFilter.WORKDAYS ->
                "Only Mon–Fri intervals that did not start on a public holiday (i.e. working days)."
        }
    }

    private fun setupTimeline() {
        binding.btnPrevDay.setOnClickListener { viewModel.prevDay() }
        binding.btnNextDay.setOnClickListener { viewModel.nextDay() }
        binding.tvSelectedDate.setOnClickListener { showDatePicker() }
        // Edge-to-edge places btnPrevDay at ~16dp from the screen's left physical edge, which is
        // within Android's back-gesture interception zone. Exclude it so taps reach the button.
        binding.btnPrevDay.doOnLayout { v ->
            ViewCompat.setSystemGestureExclusionRects(v, listOf(Rect(0, 0, v.width, v.height)))
        }
    }

    private fun observeViewModel() {
        viewModel.allFences.observe(viewLifecycleOwner) { fences ->
            setupSpinner(fences ?: emptyList())
        }
        viewModel.histogram.observe(viewLifecycleOwner) { data ->
            renderHistogram(data ?: emptyList())
        }
        viewModel.avgTimePerFence.observe(viewLifecycleOwner) { data ->
            renderAvgTime(data ?: emptyList())
        }
        viewModel.selectedDateMs.observe(viewLifecycleOwner) { dayStartMs ->
            updateDateLabel(dayStartMs)
            val today = startOfDay(System.currentTimeMillis())
            binding.btnNextDay.isEnabled = dayStartMs < today
            binding.btnNextDay.alpha     = if (dayStartMs < today) 1f else 0.38f
        }
        viewModel.timelineForDay.observe(viewLifecycleOwner) { result ->
            val r = result ?: return@observe
            binding.timelineView.setData(r.timelines, r.dayStartMs)
        }
    }

    private fun showDatePicker() {
        val currentMs = viewModel.selectedDateMs.value ?: System.currentTimeMillis()
        // Convert local day start → UTC calendar date for MaterialDatePicker.
        val localCal = Calendar.getInstance().apply { timeInMillis = currentMs }
        val utcCal   = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(localCal.get(Calendar.YEAR), localCal.get(Calendar.MONTH),
                localCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select day")
            .setSelection(utcCal.timeInMillis)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                    .build()
            )
            .build()
        picker.addOnPositiveButtonClickListener { utcMidnightMs ->
            // Convert the picker's UTC date back to the local day start.
            val sel = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMidnightMs }
            val localDayStart = Calendar.getInstance().apply {
                set(sel.get(Calendar.YEAR), sel.get(Calendar.MONTH),
                    sel.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            viewModel.selectDay(localDayStart)
        }
        picker.show(childFragmentManager, "date_picker")
    }

    private fun updateDateLabel(dayStartMs: Long) {
        val today     = startOfDay(System.currentTimeMillis())
        val yesterday = today - 24L * 3_600_000L
        binding.tvSelectedDate.text = when (dayStartMs) {
            today     -> "Today"
            yesterday -> "Yesterday"
            else      -> SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(dayStartMs))
        }
    }

    private fun setupSpinner(fences: List<GeofenceConfigEntity>) {
        if (fences.isEmpty()) return
        val names   = fences.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
        binding.spinnerFence.setAdapter(adapter)
        binding.spinnerFence.setOnItemClickListener { _, _, pos, _ ->
            viewModel.selectFence(fences[pos].id)
        }
        binding.spinnerFence.setText(names[0], false)
        viewModel.selectFence(fences[0].id)
    }

    private fun renderHistogram(data: List<HourlyCount>) {
        lastHistogramData = data
        if (data.isEmpty()) {
            binding.chartHistogram.clear()
            binding.chartHistogram.invalidate()
            return
        }

        val groupSpace = 0.3f
        val barSpace   = 0.05f
        val barWidth   = 0.3f

        val countFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float) = if (value == 0f) "" else value.toInt().toString()
        }

        val enterSet = BarDataSet(
            data.mapIndexed { i, h -> BarEntry(i.toFloat(), h.enterCount.toFloat()) }, "Enter"
        ).apply {
            color = 0xFF4CAF50.toInt()
            setDrawValues(false)
        }

        val exitSet = BarDataSet(
            data.mapIndexed { i, h -> BarEntry(i.toFloat(), h.exitCount.toFloat()) }, "Exit"
        ).apply {
            color = 0xFFF44336.toInt()
            setDrawValues(false)
        }

        val barData = BarData(enterSet, exitSet).apply { this.barWidth = barWidth }

        binding.chartHistogram.apply {
            this.data = barData
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = barData.getGroupWidth(groupSpace, barSpace) * 288
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = "${(value / 12).toInt()}h"
            }
            groupBars(0f, groupSpace, barSpace)
            invalidate()
        }
    }

    private fun renderAvgTime(data: List<FenceAvgTime>) {
        lastAvgTimeData = data
        if (data.isEmpty()) {
            binding.chartAvgTime.clear()
            binding.chartAvgTime.invalidate()
            return
        }

        val hourFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float) =
                if (value < 0.1f) "<0.1h" else "%.1fh".format(value)
        }

        val dataSet = BarDataSet(
            data.mapIndexed { i, f -> BarEntry(i.toFloat(), f.avgHours) }, "Avg time"
        ).apply {
            colors         = data.mapIndexed { i, _ -> barColors[i % barColors.size] }
            setDrawValues(true)
            valueTextColor = chartTextColor
            valueTextSize  = 10f
            valueFormatter = hourFormatter
        }

        binding.chartAvgTime.apply {
            this.data = BarData(dataSet)
            xAxis.valueFormatter       = IndexAxisValueFormatter(data.map { it.fenceName })
            xAxis.labelCount           = data.size
            xAxis.isGranularityEnabled = true
            xAxis.granularity          = 1f
            axisLeft.valueFormatter    = hourFormatter
            invalidate()
        }
    }

    private fun startOfDay(ms: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
