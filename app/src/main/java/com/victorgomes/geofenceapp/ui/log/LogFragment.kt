package com.victorgomes.geofenceapp.ui.log

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.victorgomes.geofenceapp.R
import com.victorgomes.geofenceapp.data.database.GeofenceConfigEntity
import com.victorgomes.geofenceapp.data.database.GeofenceEventEntity
import com.victorgomes.geofenceapp.databinding.FragmentLogBinding
import com.victorgomes.geofenceapp.utils.NotificationHelper

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogViewModel by viewModels()
    private lateinit var adapter: EventLogAdapter

    private var isSelectionMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EventLogAdapter(
            onEditObs = { event -> showObsDialog(event) },
            onEnterSelectionMode = { enterSelectionMode() },
            onSelectionChanged = { count -> onSelectionChanged(count) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.toolbar.inflateMenu(R.menu.menu_log)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_stats -> { showStatsDialog(); true }
                R.id.action_delete_selected -> { confirmDeleteSelected(); true }
                else -> false
            }
        }
        binding.toolbar.setNavigationOnClickListener { exitSelectionMode() }

        // Exit selection mode on back press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (isSelectionMode) exitSelectionMode() else isEnabled = false
        }

        viewModel.logState.observe(viewLifecycleOwner) { state ->
            adapter.fenceNames = state.fenceNames
            adapter.submitList(state.events) {
                val fromNotification = activity?.intent
                    ?.getBooleanExtra(NotificationHelper.EXTRA_NAVIGATE_TO_LOG, false) == true
                if (fromNotification && state.events.isNotEmpty()) {
                    activity?.intent?.removeExtra(NotificationHelper.EXTRA_NAVIGATE_TO_LOG)
                    binding.recyclerView.scrollToPosition(0)
                    adapter.highlightTop()
                    Handler(Looper.getMainLooper()).postDelayed({ adapter.clearHighlight() }, 3000)
                }
            }
            binding.tvEmpty.visibility = if (state.events.isEmpty()) View.VISIBLE else View.GONE
            updateFilterChips(state.allFences)
        }

        binding.btnClearLog.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Log")
                .setMessage("Delete all recorded events?")
                .setPositiveButton("Clear") { _, _ -> viewModel.clearAll() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnExportCsv.setOnClickListener {
            viewModel.exportToCsv(
                context = requireContext(),
                onReady = { intent -> startActivity(intent) },
                onError = { msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    // ── Selection mode ────────────────────────────────────────────────────────

    private fun enterSelectionMode() {
        isSelectionMode = true
        adapter.isSelectionMode = true
        binding.toolbar.setNavigationIcon(R.drawable.ic_close)
        binding.toolbar.menu.findItem(R.id.action_stats)?.isVisible = false
        binding.toolbar.menu.findItem(R.id.action_delete_selected)?.isVisible = true
        binding.btnClearLog.visibility = View.GONE
        binding.btnExportCsv.visibility = View.GONE
        updateSelectionTitle(adapter.selectedCount)
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        adapter.isSelectionMode = false   // also clears selectedIds
        binding.toolbar.navigationIcon = null
        binding.toolbar.title = "Event Log"
        binding.toolbar.menu.findItem(R.id.action_stats)?.isVisible = true
        binding.toolbar.menu.findItem(R.id.action_delete_selected)?.isVisible = false
        binding.btnClearLog.visibility = View.VISIBLE
        binding.btnExportCsv.visibility = View.VISIBLE
    }

    private fun onSelectionChanged(count: Int) {
        updateSelectionTitle(count)
        // If the user deselects everything while in selection mode, exit automatically
        if (count == 0) exitSelectionMode()
    }

    private fun updateSelectionTitle(count: Int) {
        binding.toolbar.title = if (count == 1) "1 selected" else "$count selected"
    }

    private fun confirmDeleteSelected() {
        val ids = adapter.getSelectedIds()
        if (ids.isEmpty()) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete events")
            .setMessage("Delete ${ids.size} selected event${if (ids.size == 1) "" else "s"}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteEvents(ids)
                exitSelectionMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Filter chips ──────────────────────────────────────────────────────────

    private fun updateFilterChips(fences: List<GeofenceConfigEntity>) {
        val chipGroup = binding.chipGroupFilter
        val existingTags = (0 until chipGroup.childCount).map {
            (chipGroup.getChildAt(it) as? Chip)?.tag as? String
        }.toSet()
        val newTags = setOf(null) + fences.map { it.id }.toSet()
        if (existingTags == newTags) return

        chipGroup.removeAllViews()

        fun makeChip(label: String, tag: String?): Chip {
            return Chip(requireContext()).apply {
                text = label
                this.tag = tag
                isCheckable = true
                isChecked = tag == null
            }
        }

        chipGroup.addView(makeChip("All", null))
        fences.forEach { fence -> chipGroup.addView(makeChip(fence.name, fence.id)) }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedTag = if (checkedIds.isEmpty()) null
            else group.findViewById<Chip>(checkedIds[0])?.tag as? String
            viewModel.setFilter(selectedTag)
        }

        binding.filterScroll.visibility = if (fences.size > 1) View.VISIBLE else View.GONE
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private fun showStatsDialog() {
        viewModel.getStats { total, enters, exits, topFence ->
            if (!isAdded) return@getStats
            val message = buildString {
                appendLine("Total events: $total")
                appendLine("Entries: $enters")
                appendLine("Exits: $exits")
                if (topFence != null) appendLine("\nMost active fence:\n$topFence")
                else append("\nNo events recorded yet.")
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Statistics")
                .setMessage(message.trim())
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showObsDialog(event: GeofenceEventEntity) {
        val options = arrayOf("Rain", "Traffic", "Other")
        val currentIndex = when (event.obs) {
            "rain"    -> 0
            "traffic" -> 1
            null      -> -1
            else      -> 2
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Observation")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> viewModel.updateObs(event.id, "rain")
                    1 -> viewModel.updateObs(event.id, "traffic")
                    2 -> showOtherObsDialog(event)
                }
            }
            .setNeutralButton("Clear") { _, _ -> viewModel.updateObs(event.id, null) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOtherObsDialog(event: GeofenceEventEntity) {
        val input = TextInputEditText(requireContext())
        val container = TextInputLayout(requireContext()).apply {
            hint = "Reason"
            setPadding(64, 0, 64, 0)
            addView(input)
        }
        if (event.obs != null && event.obs !in listOf("rain", "traffic")) {
            input.setText(event.obs)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Other reason")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val text = input.text?.toString()?.trim()
                if (!text.isNullOrEmpty()) viewModel.updateObs(event.id, text)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
