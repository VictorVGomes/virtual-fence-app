package com.victorgomes.geofenceapp.ui.stats

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.victorgomes.geofenceapp.R

class StatsMarkerView(
    context: Context,
    private val format: (entry: Entry, highlight: Highlight) -> String
) : MarkerView(context, R.layout.marker_chart) {

    private val tvText: TextView = findViewById(R.id.tvMarkerText)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null && highlight != null) tvText.text = format(e, highlight)
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF = MPPointF(-(width / 2f), -height.toFloat() - 4f)
}
