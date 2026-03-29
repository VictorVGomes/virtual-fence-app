package com.victorgomes.geofenceapp.ui.fences

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.victorgomes.geofenceapp.databinding.DialogCreateFenceBinding
import com.victorgomes.geofenceapp.databinding.FragmentFencesBinding
import com.victorgomes.geofenceapp.utils.PersonalizationPrefs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

class FencesFragment : Fragment() {

    private var _binding: FragmentFencesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FencesViewModel by viewModels()
    private lateinit var adapter: FenceListAdapter

    private val tickHandler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            // Piggyback on the last fix already cached by the monitor service — no new
            // radio request needed. The inside/outside indicator and time counter both
            // refresh every 60 s, so this resolution is sufficient.
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) updateInsideStatus(loc)
            }
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
            tickHandler.postDelayed(this, 60_000L)
        }
    }

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }
    // Tracks when GPS first detected the user inside each fence (fenceId → epochMs).
    // Used as the "since" reference when no DB ENTER event exists yet.
    private val localEntryTimes = mutableMapOf<String, Long>()

    private fun updateInsideStatus(loc: Location) {
        adapter.userLocation = loc

        // Compare GPS position against every fence boundary.
        val configs = viewModel.fences.value ?: return
        val prevInside = adapter.insideFenceIds
        val inside = mutableSetOf<String>()
        val distResults = FloatArray(1)
        configs.forEach { config ->
            Location.distanceBetween(
                loc.latitude, loc.longitude,
                config.latitude, config.longitude,
                distResults
            )
            if (distResults[0] <= config.radiusMeters) inside.add(config.id)
        }

        // Record entry time the first moment GPS detects "inside".
        inside.forEach { id -> if (id !in prevInside) localEntryTimes[id] = System.currentTimeMillis() }
        // Remove stale entry times for fences the user has left.
        prevInside.forEach { id -> if (id !in inside) localEntryTimes.remove(id) }

        adapter.localEntryTimes = localEntryTimes.toMap()
        adapter.insideFenceIds = inside
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFencesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FenceListAdapter(
            onToggle = { config, activate -> viewModel.toggleFence(config, activate) },
            onEdit = { config ->
                val dialogBinding = DialogCreateFenceBinding.inflate(layoutInflater)
                var selectedRadius = config.radiusMeters
                var selectedIcon = config.markerIcon

                dialogBinding.etFenceName.setText(config.name)
                dialogBinding.tvDialogRadius.text = "${selectedRadius.roundToInt()} m"
                dialogBinding.seekBarDialogRadius.value = radiusToProgress(selectedRadius).toFloat()
                dialogBinding.seekBarDialogRadius.addOnChangeListener { _, value, _ ->
                    selectedRadius = progressToRadius(value.toInt())
                    dialogBinding.tvDialogRadius.text = "${selectedRadius.roundToInt()} m"
                }

                setupDialogIconPicker(dialogBinding, selectedIcon) { key -> selectedIcon = key }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Edit Fence")
                    .setView(dialogBinding.root)
                    .setPositiveButton("Save") { _, _ ->
                        val name = dialogBinding.etFenceName.text?.toString()?.trim()
                        if (name.isNullOrEmpty()) {
                            Toast.makeText(requireContext(), "Please enter a fence name.", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        viewModel.editFence(config, name, selectedRadius, selectedIcon)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onDelete = { config ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Fence")
                    .setMessage("Delete \"${config.name}\"?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteFence(config) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.recyclerViewFences.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewFences.adapter = adapter

        viewModel.fences.observe(viewLifecycleOwner) { fences ->
            adapter.submitList(fences)
            binding.tvEmptyFences.visibility = if (fences.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewFences.visibility = if (fences.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.eventCounts.observe(viewLifecycleOwner) { counts ->
            adapter.eventCounts = counts
        }

        viewModel.insideStatus.observe(viewLifecycleOwner) { timestamps ->
            adapter.enterTimestamps = timestamps
        }
    }

    override fun onResume() {
        super.onResume()
        tickHandler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        tickHandler.removeCallbacks(tickRunnable)
    }

    private fun setupDialogIconPicker(
        dialogBinding: DialogCreateFenceBinding,
        initialKey: String,
        onSelected: (String) -> Unit
    ) {
        val options = listOf(
            PersonalizationPrefs.FENCE_PIN   to dialogBinding.cardIconPin,
            PersonalizationPrefs.FENCE_HOME  to dialogBinding.cardIconHome,
            PersonalizationPrefs.FENCE_FLAG  to dialogBinding.cardIconFlag,
            PersonalizationPrefs.FENCE_STAR  to dialogBinding.cardIconStar,
            PersonalizationPrefs.FENCE_BELL  to dialogBinding.cardIconBell,
            PersonalizationPrefs.FENCE_HEART to dialogBinding.cardIconHeart,
        )
        var currentKey = initialKey
        fun refresh() {
            options.forEach { (key, card) ->
                if (key == currentKey) {
                    card.strokeColor = MaterialColors.getColor(card, com.google.android.material.R.attr.colorPrimary)
                    card.setCardBackgroundColor(MaterialColors.getColor(card, com.google.android.material.R.attr.colorPrimaryContainer))
                } else {
                    card.strokeColor = MaterialColors.getColor(card, com.google.android.material.R.attr.colorOutlineVariant)
                    card.setCardBackgroundColor(MaterialColors.getColor(card, com.google.android.material.R.attr.colorSurface))
                }
            }
        }
        refresh()
        options.forEach { (key, card) ->
            card.setOnClickListener {
                currentKey = key
                onSelected(key)
                refresh()
            }
        }
    }

    private fun progressToRadius(progress: Int): Float {
        val min = 50f; val max = 5000f
        return (min * (max / min).toDouble().pow(progress / 99.0)).toFloat()
    }

    private fun radiusToProgress(radius: Float): Int {
        val min = 50f; val max = 5000f
        return ((ln((radius / min).toDouble()) / ln((max / min).toDouble())) * 99)
            .roundToInt().coerceIn(0, 99)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
