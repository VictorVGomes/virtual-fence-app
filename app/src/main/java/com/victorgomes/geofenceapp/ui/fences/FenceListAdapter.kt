package com.victorgomes.geofenceapp.ui.fences

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.victorgomes.geofenceapp.data.database.GeofenceConfigEntity
import com.victorgomes.geofenceapp.databinding.ItemFenceBinding
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

class FenceListAdapter(
    private val onToggle: (GeofenceConfigEntity, Boolean) -> Unit,
    private val onEdit: (GeofenceConfigEntity) -> Unit,
    private val onDelete: (GeofenceConfigEntity) -> Unit
) : ListAdapter<GeofenceConfigEntity, FenceListAdapter.ViewHolder>(DIFF_CALLBACK) {

    var userLocation: Location? = null
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    var eventCounts: Map<String, Int> = emptyMap()
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    /**
     * Fence IDs where the user is currently inside, determined by real-time GPS position
     * in the fragment. Drives visibility of the "Inside" badge independently of the DB.
     */
    var insideFenceIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    /**
     * Maps fenceId → ENTER timestamp from the database (for fences whose last event is ENTER).
     * Used only to show the "since HH:MM" clock time — not for inside/outside determination.
     */
    var enterTimestamps: Map<String, Long> = emptyMap()
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    /**
     * Maps fenceId → epoch ms of the first GPS fix that detected "inside".
     * Used as fallback when no DB ENTER event exists yet (e.g. confirmation still pending).
     */
    var localEntryTimes: Map<String, Long> = emptyMap()
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    inner class ViewHolder(private val binding: ItemFenceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(config: GeofenceConfigEntity) {
            binding.tvFenceName.text = config.name
            binding.tvFenceRadius.text = "${config.radiusMeters.roundToInt()} m radius"

            val iconRes = when (config.markerIcon) {
                "bell"   -> com.victorgomes.geofenceapp.R.drawable.ic_fence_bell
                "flag"   -> com.victorgomes.geofenceapp.R.drawable.ic_fence_flag
                "heart"  -> com.victorgomes.geofenceapp.R.drawable.ic_fence_heart
                "home"   -> com.victorgomes.geofenceapp.R.drawable.ic_fence_home
                "star"   -> com.victorgomes.geofenceapp.R.drawable.ic_fence_star
                else     -> com.victorgomes.geofenceapp.R.drawable.ic_fence_pin
            }
            binding.ivFenceIcon.setImageResource(iconRes)

            val loc = userLocation
            if (loc != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    loc.latitude, loc.longitude,
                    config.latitude, config.longitude,
                    results
                )
                val dist = results[0]
                binding.tvFenceDistance.visibility = View.VISIBLE
                binding.tvFenceDistance.text = if (dist >= 1000f) {
                    "${"%.1f".format(dist / 1000f)} km away"
                } else {
                    "${dist.roundToInt()} m away"
                }
            } else {
                binding.tvFenceDistance.visibility = View.GONE
            }

            val count = eventCounts[config.id]
            if (count != null && count > 0) {
                binding.tvFenceEvents.visibility = View.VISIBLE
                binding.tvFenceEvents.text = "$count crossing${if (count == 1) "" else "s"}"
            } else {
                binding.tvFenceEvents.visibility = View.GONE
            }

            val isInside = config.id in insideFenceIds
            if (isInside) {
                binding.tvFenceInside.visibility = View.VISIBLE
                // Prefer DB ENTER timestamp; fall back to the moment GPS first saw us inside.
                val ref = enterTimestamps[config.id] ?: localEntryTimes[config.id]
                if (ref != null) {
                    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ref))
                    val duration = formatDuration(System.currentTimeMillis() - ref)
                    binding.tvFenceInside.text = "Inside since $time · $duration"
                } else {
                    binding.tvFenceInside.text = "Inside"
                }
                binding.root.strokeColor = MaterialColors.getColor(
                    binding.root, com.google.android.material.R.attr.colorPrimary
                )
                binding.root.strokeWidth = (3 * binding.root.context.resources.displayMetrics.density).toInt()
            } else {
                binding.tvFenceInside.visibility = View.GONE
                binding.root.strokeColor = MaterialColors.getColor(
                    binding.root, com.google.android.material.R.attr.colorOutlineVariant
                )
                binding.root.strokeWidth = (1 * binding.root.context.resources.displayMetrics.density).toInt()
            }

            binding.switchActive.setOnCheckedChangeListener(null)
            binding.switchActive.isChecked = config.isActive
            binding.switchActive.setOnCheckedChangeListener { _, isChecked ->
                onToggle(config, isChecked)
            }

            binding.btnEditFence.setOnClickListener { onEdit(config) }
            binding.btnDeleteFence.setOnClickListener { onDelete(config) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFenceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GeofenceConfigEntity>() {
            override fun areItemsTheSame(a: GeofenceConfigEntity, b: GeofenceConfigEntity) =
                a.id == b.id
            override fun areContentsTheSame(a: GeofenceConfigEntity, b: GeofenceConfigEntity) =
                a == b
        }

        fun formatDuration(ms: Long): String {
            val totalMinutes = ms / 60_000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                hours > 0    -> "${hours}h ${minutes}m"
                totalMinutes > 0 -> "${totalMinutes}m"
                else         -> "< 1m"
            }
        }
    }
}
