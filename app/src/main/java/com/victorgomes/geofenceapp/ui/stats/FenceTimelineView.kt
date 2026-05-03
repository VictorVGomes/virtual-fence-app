package com.victorgomes.geofenceapp.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FenceTimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val d = context.resources.displayMetrics.density

    private val rowH     = 40 * d
    private val labelW   = 88 * d
    private val padH     = 10 * d
    private val padV     = 8  * d
    private val axisH    = 32 * d
    private val barInset = 0.15f

    private val colors = intArrayOf(
        0xFF4CAF50.toInt(), 0xFF2196F3.toInt(), 0xFFFF9800.toInt(), 0xFFE91E63.toInt(),
        0xFF9C27B0.toInt(), 0xFF00BCD4.toInt(), 0xFF795548.toInt(), 0xFF607D8B.toInt()
    )

    interface OnIntervalClickListener {
        fun onIntervalClick(fenceName: String, startMs: Long, endMs: Long)
    }
    var onIntervalClickListener: OnIntervalClickListener? = null

    private var timelines: List<FenceTimeline> = emptyList()
    private var dayStartMs = 0L
    private val dayMs = 24L * 3_600_000L

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    private data class TooltipState(
        val line1: String,
        val line2: String,
        val anchorX: Float,
        val anchorY: Float
    )
    private var tooltipState: TooltipState? = null

    private val barPaint       = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 12 * d
        textAlign = Paint.Align.LEFT
    }
    private val axisPaint      = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 8 * d
        textAlign = Paint.Align.CENTER
    }
    private val timeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 9 * d
        textAlign = Paint.Align.LEFT
        color     = 0xFFFFFFFF.toInt()
        setShadowLayer(1.5f * d, 0f, 0.5f * d, 0x99000000.toInt())
    }
    private val aboveBarLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 9 * d
        textAlign = Paint.Align.LEFT
    }
    private val baselinePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1 * d
    }
    private val nowPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = 0xFFF44336.toInt()
        strokeWidth = 2 * d
        pathEffect  = DashPathEffect(floatArrayOf(8 * d, 6 * d), 0f)
    }
    private val emptyPaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 14 * d
        textAlign = Paint.Align.CENTER
    }
    private val tooltipBgPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tooltipLine1Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tooltipLine2Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        isClickable = true
        val onSurface = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorOnSurface, 0xFFFFFFFF.toInt()
        )
        val onSurfaceVariant = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFFBDBDBD.toInt()
        )
        labelPaint.color         = onSurface
        emptyPaint.color         = onSurfaceVariant
        axisPaint.color          = onSurfaceVariant
        baselinePaint.color      = (onSurfaceVariant and 0x00FFFFFF) or (0x40 shl 24)
        aboveBarLabelPaint.color = onSurface

        tooltipBgPaint.color = 0xEE1A1A1A.toInt()

        tooltipLine1Paint.apply {
            color          = 0xFFFFFFFF.toInt()
            textSize       = 12 * d
            textAlign      = Paint.Align.LEFT
            isFakeBoldText = true
        }
        tooltipLine2Paint.apply {
            color     = 0xFFDDDDDD.toInt()
            textSize  = 11 * d
            textAlign = Paint.Align.LEFT
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val count = timelines.size
        val h = if (count == 0) (48 * d).toInt()
                else (padV + count * (rowH + padV) + axisH + padV).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), h)
    }

    override fun onDraw(canvas: Canvas) {
        if (timelines.isEmpty()) {
            val cy = height / 2f - (emptyPaint.ascent() + emptyPaint.descent()) / 2f
            canvas.drawText("No activity on this day", width / 2f, cy, emptyPaint)
            return
        }

        val chartLeft  = labelW + padH
        val chartRight = width.toFloat() - padH
        val now        = System.currentTimeMillis()

        timelines.forEachIndexed { index, timeline ->
            val rowTop    = padV + index * (rowH + padV)
            val rowBottom = rowTop + rowH
            val barTop    = rowTop    + rowH * barInset
            val barBottom = rowBottom - rowH * barInset
            val midY      = (rowTop + rowBottom) / 2f

            val labelY = midY - (labelPaint.ascent() + labelPaint.descent()) / 2f
            canvas.drawText(timeline.fenceName.take(10), padH, labelY, labelPaint)

            canvas.drawLine(chartLeft, midY, chartRight, midY, baselinePaint)

            barPaint.color = colors[Math.abs(timeline.fenceName.hashCode()) % colors.size]
            val singleLabelW = timeLabelPaint.measureText("00:00")
            val minBothW     = singleLabelW * 2f + 10 * d
            val labelInsideY = (barTop + barBottom) / 2f -
                               (timeLabelPaint.ascent() + timeLabelPaint.descent()) / 2f
            // Clamp so the text top never goes above the view boundary (affects first row).
            val labelAboveY  = (barTop + timeLabelPaint.ascent() - 2 * d)
                .coerceAtLeast(-timeLabelPaint.ascent())
            for (interval in timeline.intervals) {
                val x1 = msToX(interval.startMs, chartLeft, chartRight)
                val x2 = msToX(interval.endMs,   chartLeft, chartRight)
                if (x2 > x1) {
                    canvas.drawRoundRect(RectF(x1, barTop, x2, barBottom), 4 * d, 4 * d, barPaint)
                    val startLabel = timeFmt.format(Date(interval.startMs))
                    val endLabel   = timeFmt.format(Date(interval.endMs))
                    val endLabelW  = timeLabelPaint.measureText(endLabel)
                    val barW = x2 - x1
                    when {
                        barW >= minBothW -> {
                            // Both labels inside the bar
                            canvas.drawText(startLabel, x1 + 3 * d, labelInsideY, timeLabelPaint)
                            canvas.drawText(endLabel, x2 - endLabelW - 3 * d, labelInsideY, timeLabelPaint)
                        }
                        barW >= singleLabelW + 6 * d -> {
                            // Start inside; end above the right edge
                            canvas.drawText(startLabel, x1 + 3 * d, labelInsideY, timeLabelPaint)
                            val ex = (x2 - endLabelW).coerceIn(chartLeft, chartRight - endLabelW)
                            canvas.drawText(endLabel, ex, labelAboveY, aboveBarLabelPaint)
                        }
                        else -> {
                            // Narrow bar: start above-left, end above-right.
                            // Guard: when the bar is at the very right edge (e.g. an interval that
                            // reaches dayEnd), sx + singleLabelW/2 can exceed chartRight - endLabelW,
                            // making coerceIn's min > max and crashing with IllegalArgumentException.
                            val sx = x1.coerceIn(chartLeft, chartRight - singleLabelW)
                            canvas.drawText(startLabel, sx, labelAboveY, aboveBarLabelPaint)
                            val exMax = chartRight - endLabelW
                            val exMin = minOf(sx + singleLabelW * 0.5f, exMax)
                            val ex = (x2 - endLabelW).coerceIn(exMin, exMax)
                            canvas.drawText(endLabel, ex, labelAboveY, aboveBarLabelPaint)
                        }
                    }
                }
            }
        }

        val nowX       = msToX(now, chartLeft, chartRight)
        val rowsBottom = padV + timelines.size * (rowH + padV)
        canvas.drawLine(nowX, padV, nowX, rowsBottom, nowPaint)

        val tickBaseY  = rowsBottom + padV * 0.5f
        // 30-minute slots: 0..48 covers the full day
        for (slot in 0..48) {
            val x       = chartLeft + (chartRight - chartLeft) * slot / 48f
            val tickLen = when {
                slot % 8 == 0 -> 8 * d   // every 4 h — major
                slot % 2 == 0 -> 5 * d   // every 1 h — minor
                else          -> 2 * d   // every 30 min — micro
            }
            canvas.drawLine(x, tickBaseY, x, tickBaseY + tickLen, axisPaint)
        }
        // Labels every 1 h, positioned below the tallest tick with a small gap
        val axisLabelY = tickBaseY + 8 * d - axisPaint.ascent()
        for (h in 0..23) {
            val x = chartLeft + (chartRight - chartLeft) * h / 24f
            canvas.drawText("%d".format(h), x, axisLabelY, axisPaint)
        }

        drawTooltip(canvas)
    }

    private fun drawTooltip(canvas: Canvas) {
        val tt = tooltipState ?: return
        val pad  = 8 * d
        val gap  = 3 * d
        val r    = 6 * d

        val w1   = tooltipLine1Paint.measureText(tt.line1)
        val w2   = tooltipLine2Paint.measureText(tt.line2)
        val boxW = maxOf(w1, w2) + pad * 2
        val boxH = tooltipLine1Paint.textSize + gap + tooltipLine2Paint.textSize + pad * 2

        var boxLeft = tt.anchorX - boxW / 2f
        var boxTop  = tt.anchorY - boxH - 8 * d

        boxLeft = boxLeft.coerceIn(padH, (width - padH) - boxW)
        if (boxTop < padV) boxTop = tt.anchorY + 8 * d

        canvas.drawRoundRect(RectF(boxLeft, boxTop, boxLeft + boxW, boxTop + boxH), r, r, tooltipBgPaint)

        val textX = boxLeft + pad
        val y1    = boxTop + pad - tooltipLine1Paint.ascent()
        canvas.drawText(tt.line1, textX, y1, tooltipLine1Paint)
        val y2    = y1 + tooltipLine1Paint.descent() + gap - tooltipLine2Paint.ascent()
        canvas.drawText(tt.line2, textX, y2, tooltipLine2Paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y
            val chartLeft  = labelW + padH
            val chartRight = width.toFloat() - padH
            var hit = false
            timelines.forEachIndexed { index, timeline ->
                val rowTop    = padV + index * (rowH + padV)
                val rowBottom = rowTop + rowH
                if (y < rowTop || y > rowBottom) return@forEachIndexed
                for (interval in timeline.intervals) {
                    val x1 = msToX(interval.startMs, chartLeft, chartRight)
                    val x2 = msToX(interval.endMs,   chartLeft, chartRight)
                    if (x >= x1 && x <= x2) {
                        val durationMs = interval.endMs - interval.startMs
                        val h = durationMs / 3_600_000L
                        val m = (durationMs % 3_600_000L) / 60_000L
                        tooltipState = TooltipState(
                            line1   = timeline.fenceName,
                            line2   = "${timeFmt.format(Date(interval.startMs))} → ${timeFmt.format(Date(interval.endMs))}  ${h}h ${m}m",
                            anchorX = (x1 + x2) / 2f,
                            anchorY = (rowTop + rowBottom) / 2f
                        )
                        onIntervalClickListener?.onIntervalClick(timeline.fenceName, interval.startMs, interval.endMs)
                        hit = true
                        performClick()
                        invalidate()
                        return true
                    }
                }
            }
            if (!hit && tooltipState != null) {
                tooltipState = null
                invalidate()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun msToX(ms: Long, left: Float, right: Float): Float {
        val fraction = (ms - dayStartMs).toFloat() / dayMs
        return (left + (right - left) * fraction).coerceIn(left, right)
    }

    fun setData(timelines: List<FenceTimeline>, dayStartMs: Long) {
        this.timelines  = timelines
        this.dayStartMs = dayStartMs
        tooltipState    = null
        requestLayout()
        invalidate()
    }

    fun clearTooltip() {
        if (tooltipState != null) {
            tooltipState = null
            invalidate()
        }
    }
}
