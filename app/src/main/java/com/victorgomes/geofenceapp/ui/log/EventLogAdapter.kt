package com.victorgomes.geofenceapp.ui.log

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.victorgomes.geofenceapp.data.database.GeofenceEventEntity
import com.victorgomes.geofenceapp.databinding.ItemGeofenceEventBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventLogAdapter(
    private val onEditObs: (GeofenceEventEntity) -> Unit,
    private val onEnterSelectionMode: () -> Unit,
    private val onSelectionChanged: (count: Int) -> Unit
) : ListAdapter<GeofenceEventEntity, EventLogAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.getDefault())
    private var highlightedPosition: Int = -1
    var fenceNames: Map<String, String> = emptyMap()

    // ── Selection state ───────────────────────────────────────────────────────

    var isSelectionMode = false
        set(value) {
            field = value
            if (!value) selectedIds.clear()
            notifyItemRangeChanged(0, itemCount)
        }

    private val selectedIds = mutableSetOf<Long>()

    val selectedCount get() = selectedIds.size
    fun getSelectedIds(): Set<Long> = selectedIds.toSet()

    fun clearSelection() {
        selectedIds.clear()
        notifyItemRangeChanged(0, itemCount)
    }

    // ── Highlight (notification deep-link) ───────────────────────────────────

    fun highlightTop() {
        val prev = highlightedPosition
        highlightedPosition = 0
        if (prev != 0) notifyItemChanged(prev)
        notifyItemChanged(0)
    }

    fun clearHighlight() {
        val prev = highlightedPosition
        highlightedPosition = -1
        if (prev >= 0) notifyItemChanged(prev)
    }

    // ── ViewHolder ───────────────────────────────────────────────────────────

    inner class ViewHolder(private val binding: ItemGeofenceEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: GeofenceEventEntity, highlighted: Boolean) {
            val isEnter = event.eventType == "ENTER"
            binding.chipEventType.text = if (isEnter) "ENTERED" else "EXITED"

            val bgAttr = if (isEnter)
                com.google.android.material.R.attr.colorTertiaryContainer
            else
                com.google.android.material.R.attr.colorErrorContainer
            val textAttr = if (isEnter)
                com.google.android.material.R.attr.colorOnTertiaryContainer
            else
                com.google.android.material.R.attr.colorOnErrorContainer

            binding.chipEventType.chipBackgroundColor =
                ColorStateList.valueOf(MaterialColors.getColor(itemView, bgAttr, 0))
            binding.chipEventType.setTextColor(
                MaterialColors.getColor(itemView, textAttr, 0)
            )

            val fenceName = fenceNames[event.geofenceId]
            if (fenceName != null) {
                binding.tvFenceName.visibility = View.VISIBLE
                binding.tvFenceName.text = fenceName
            } else {
                binding.tvFenceName.visibility = View.GONE
            }

            binding.tvTimestamp.text = dateFormat.format(Date(event.timestamp))
            binding.tvCoords.text = "%.5f, %.5f".format(event.latitude, event.longitude)

            if (event.obs != null) {
                binding.tvObs.visibility = View.VISIBLE
                binding.tvObs.text = event.obs
            } else {
                binding.tvObs.visibility = View.GONE
            }

            // Hide edit button in selection mode — the whole card is the touch target
            binding.btnEditObs.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
            binding.btnEditObs.setOnClickListener {
                if (!isSelectionMode) onEditObs(event)
            }

            // Card selection visual
            val card = binding.root as com.google.android.material.card.MaterialCardView
            val selected = event.id in selectedIds
            card.isCheckable = isSelectionMode
            card.isChecked = selected

            val density = card.context.resources.displayMetrics.density
            when {
                selected -> {
                    card.strokeColor = MaterialColors.getColor(
                        card, com.google.android.material.R.attr.colorPrimary, 0)
                    card.strokeWidth = (3 * density).toInt()
                }
                highlighted -> {
                    card.strokeColor = MaterialColors.getColor(
                        card, com.google.android.material.R.attr.colorPrimary, 0)
                    card.strokeWidth = (3 * density).toInt()
                }
                else -> {
                    card.strokeColor = MaterialColors.getColor(
                        card, com.google.android.material.R.attr.colorOutlineVariant, 0)
                    card.strokeWidth = (1 * density).toInt()
                }
            }

            card.setOnClickListener {
                if (isSelectionMode) {
                    if (selected) selectedIds.remove(event.id) else selectedIds.add(event.id)
                    notifyItemChanged(bindingAdapterPosition)
                    onSelectionChanged(selectedIds.size)
                }
                // In normal mode clicks are handled by child views (btnEditObs)
            }

            card.setOnLongClickListener {
                if (!isSelectionMode) {
                    selectedIds.add(event.id)
                    onEnterSelectionMode()          // fragment switches toolbar
                    notifyItemRangeChanged(0, itemCount)
                    onSelectionChanged(selectedIds.size)
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGeofenceEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == highlightedPosition)
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GeofenceEventEntity>() {
            override fun areItemsTheSame(a: GeofenceEventEntity, b: GeofenceEventEntity) = a.id == b.id
            override fun areContentsTheSame(a: GeofenceEventEntity, b: GeofenceEventEntity) = a == b
        }
    }
}
