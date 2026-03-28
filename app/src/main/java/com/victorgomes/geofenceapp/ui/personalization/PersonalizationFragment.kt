package com.victorgomes.geofenceapp.ui.personalization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.victorgomes.geofenceapp.databinding.FragmentPersonalizationBinding
import com.victorgomes.geofenceapp.utils.PersonalizationPrefs

class PersonalizationFragment : Fragment() {

    private var _binding: FragmentPersonalizationBinding? = null
    private val binding get() = _binding!!

    // If true, onViewCreated will switch to the Tutorial tab (used on first launch)
    var openTutorialOnStart = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalizationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupCharSelector()
        setupFenceSelector()
        setupNotificationsToggle()
        setupVersionTap()

        // Auto-open Tutorial on first launch or if explicitly requested
        if (openTutorialOnStart || !PersonalizationPrefs.hasSeenTutorial(requireContext())) {
            PersonalizationPrefs.setTutorialSeen(requireContext())
            openTutorialOnStart = false
            binding.tabLayout.getTabAt(1)?.select()
        }
    }

    // ── Tab switching ─────────────────────────────────────────────────────────

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Settings"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Tutorial"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val isTutorial = tab.position == 1
                binding.contentSettings.visibility = if (isTutorial) View.GONE else View.VISIBLE
                binding.contentTutorial.visibility = if (isTutorial) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // ── Personalization ───────────────────────────────────────────────────────

    private fun setupCharSelector() {
        val options = listOf(
            PersonalizationPrefs.CHAR_MALE   to binding.cardCharMale,
            PersonalizationPrefs.CHAR_FEMALE to binding.cardCharFemale,
            PersonalizationPrefs.CHAR_CAT    to binding.cardCharCat,
            PersonalizationPrefs.CHAR_DOG    to binding.cardCharDog,
        )
        val current = PersonalizationPrefs.getCharIcon(requireContext())
        options.forEach { (key, card) ->
            setCardSelected(card, key == current)
            card.setOnClickListener {
                PersonalizationPrefs.setCharIcon(requireContext(), key)
                options.forEach { (k, c) -> setCardSelected(c, k == key) }
            }
        }
    }

    private fun setupFenceSelector() {
        val options = listOf(
            PersonalizationPrefs.FENCE_PIN   to binding.cardFencePin,
            PersonalizationPrefs.FENCE_HOME  to binding.cardFenceHome,
            PersonalizationPrefs.FENCE_FLAG  to binding.cardFenceFlag,
            PersonalizationPrefs.FENCE_STAR  to binding.cardFenceStar,
            PersonalizationPrefs.FENCE_BELL  to binding.cardFenceBell,
            PersonalizationPrefs.FENCE_HEART to binding.cardFenceHeart,
        )
        val current = PersonalizationPrefs.getFenceIcon(requireContext())
        options.forEach { (key, card) ->
            setCardSelected(card, key == current)
            card.setOnClickListener {
                PersonalizationPrefs.setFenceIcon(requireContext(), key)
                options.forEach { (k, c) -> setCardSelected(c, k == key) }
            }
        }
    }

    private fun setupNotificationsToggle() {
        val ctx = requireContext()
        binding.switchEventNotifications.isChecked =
            PersonalizationPrefs.isEventNotificationsEnabled(ctx)
        binding.switchEventNotifications.setOnCheckedChangeListener { _, checked ->
            PersonalizationPrefs.setEventNotificationsEnabled(ctx, checked)
        }
        binding.btnNotificationSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            })
        }
    }

    // ── Version Easter egg ────────────────────────────────────────────────────

    private fun setupVersionTap() {
        val ctx = requireContext()
        val versionName = try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        } catch (_: Exception) { "—" }

        if (PersonalizationPrefs.isDebugUnlocked(ctx)) {
            showDebugUnlockedLabel(versionName)
            return
        }

        binding.tvAppVersion.text = "v$versionName"

        val tapTimestamps = ArrayDeque<Long>()
        val resetHandler = Handler(Looper.getMainLooper())
        var resetRunnable: Runnable? = null

        binding.tvAppVersion.setOnClickListener {
            val now = System.currentTimeMillis()
            tapTimestamps.addLast(now)
            while (tapTimestamps.isNotEmpty() && now - tapTimestamps.first() > 5_000L) {
                tapTimestamps.removeFirst()
            }

            resetRunnable?.let { resetHandler.removeCallbacks(it) }

            val count = tapTimestamps.size
            if (count >= 5) {
                PersonalizationPrefs.setDebugUnlocked(ctx, true)
                showDebugUnlockedLabel(versionName)
                binding.tvAppVersion.setOnClickListener(null)
                resetHandler.removeCallbacks(resetRunnable ?: return@setOnClickListener)
            } else {
                val remaining = 5 - count
                // Show countdown in the label itself — no Toast blocking the next tap
                binding.tvAppVersion.text = "v$versionName  ($remaining more tap${if (remaining == 1) "" else "s"})"
                // Reset label after 3 s of inactivity
                resetRunnable = Runnable { binding.tvAppVersion.text = "v$versionName" }
                resetHandler.postDelayed(resetRunnable!!, 3_000L)
            }
        }
    }

    private fun showDebugUnlockedLabel(versionName: String) {
        binding.tvAppVersion.text = "v$versionName  🐛"
        // Long-press to deactivate debug mode
        binding.tvAppVersion.setOnLongClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Disable debug mode")
                .setMessage("The debug controls on the map will be hidden. You can re-enable debug mode by tapping the version number 5 times again.")
                .setPositiveButton("Disable") { _, _ ->
                    val vn = try {
                        requireContext().packageManager
                            .getPackageInfo(requireContext().packageName, 0).versionName
                    } catch (_: Exception) { "—" }
                    PersonalizationPrefs.setDebugUnlocked(requireContext(), false)
                    binding.tvAppVersion.setOnLongClickListener(null)
                    // Re-setup the tap Easter egg
                    binding.tvAppVersion.text = "v$vn"
                    setupVersionTap()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }

    private fun setCardSelected(card: MaterialCardView, selected: Boolean) {
        if (selected) {
            card.strokeColor = MaterialColors.getColor(
                card, com.google.android.material.R.attr.colorPrimary)
            card.setCardBackgroundColor(MaterialColors.getColor(
                card, com.google.android.material.R.attr.colorPrimaryContainer))
        } else {
            card.strokeColor = MaterialColors.getColor(
                card, com.google.android.material.R.attr.colorOutlineVariant)
            card.setCardBackgroundColor(MaterialColors.getColor(
                card, com.google.android.material.R.attr.colorSurface))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
