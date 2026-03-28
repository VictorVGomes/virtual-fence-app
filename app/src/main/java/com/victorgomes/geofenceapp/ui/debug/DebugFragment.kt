package com.victorgomes.geofenceapp.ui.debug

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.victorgomes.geofenceapp.databinding.FragmentDebugBinding
import kotlin.math.cos

class DebugFragment : Fragment() {

    private var _binding: FragmentDebugBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DebugViewModel by viewModels()

    private lateinit var fusedClient: FusedLocationProviderClient

    private var mockLat: Double = 0.0
    private var mockLon: Double = 0.0
    private var mockInitialized = false
    private var isMockActive = false

    // Heartbeat: re-push mock location every 2 s so the OS doesn't revert to real GPS
    private val mockHandler = Handler(Looper.getMainLooper())
    private val mockRunnable = object : Runnable {
        override fun run() {
            if (isMockActive && mockInitialized) {
                pushMockLocation(mockLat, mockLon)
                mockHandler.postDelayed(this, 2_000L)
            }
        }
    }

    // ── Snap-to-GPS state ─────────────────────────────────────────────────────

    private var snapWasMockActive = false
    private val snapHandler = Handler(Looper.getMainLooper())
    private var snapTimeout: Runnable? = null

    private val snapCallback = object : LocationCallback() {
        @SuppressLint("MissingPermission")
        override fun onLocationResult(result: LocationResult) {
            snapHandler.removeCallbacks(snapTimeout ?: return)
            fusedClient.removeLocationUpdates(this)

            val loc = result.lastLocation ?: return
            mockLat = loc.latitude
            mockLon = loc.longitude
            mockInitialized = true
            viewModel.mockLat = mockLat
            viewModel.mockLon = mockLon

            updateCoordsDisplay()
            viewModel.updateFenceStatus(mockLat, mockLon)
            finishSnap()

            // Re-enable mock if it was on before snap
            if (snapWasMockActive) {
                fusedClient.setMockMode(true).addOnSuccessListener {
                    pushMockLocation(mockLat, mockLon)
                    mockHandler.removeCallbacks(mockRunnable)
                    mockHandler.postDelayed(mockRunnable, 2_000L)
                }
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebugBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Restore position from ViewModel across config changes
        viewModel.mockLat?.let { lat ->
            viewModel.mockLon?.let { lon ->
                mockLat = lat
                mockLon = lon
                mockInitialized = true
                updateCoordsDisplay()
            }
        }

        binding.switchMock.setOnCheckedChangeListener { _, checked ->
            if (checked) enableMock() else disableMock()
        }

        binding.btnUp.setOnClickListener    { move(1, 0) }
        binding.btnDown.setOnClickListener  { move(-1, 0) }
        binding.btnLeft.setOnClickListener  { move(0, -1) }
        binding.btnRight.setOnClickListener { move(0, 1) }
        binding.btnCenter.setOnClickListener {
            if (!mockInitialized) { showNotReady(); return@setOnClickListener }
            if (!isMockActive)    { showMockOff();  return@setOnClickListener }
            pushMockLocation(mockLat, mockLon)
        }

        binding.btnResetGps.setOnClickListener { snapToRealGps() }

        viewModel.fenceStatus.observe(viewLifecycleOwner) { lines ->
            binding.tvFenceStatus.text = if (lines.isEmpty()) "No fences configured." else lines
        }
    }

    // ── Step size ─────────────────────────────────────────────────────────────

    private val stepMeters: Double
        get() = when (binding.chipGroupStep.checkedChipId) {
            binding.chip5m.id   -> 5.0
            binding.chip100m.id -> 100.0
            binding.chip500m.id -> 500.0
            else                -> 20.0
        }

    // ── Movement ─────────────────────────────────────────────────────────────

    private fun move(dLat: Int, dLon: Int) {
        if (!mockInitialized) { showNotReady(); return }
        if (!isMockActive)    { showMockOff();  return }

        val step = stepMeters
        mockLat += dLat  * (step / 111_111.0)
        mockLon += dLon  * (step / (111_111.0 * cos(Math.toRadians(mockLat))))

        viewModel.mockLat = mockLat
        viewModel.mockLon = mockLon

        updateCoordsDisplay()
        pushMockLocation(mockLat, mockLon)
        viewModel.updateFenceStatus(mockLat, mockLon)
    }

    // ── Mock location ─────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun enableMock() {
        isMockActive = true
        fusedClient.setMockMode(true)
            .addOnSuccessListener {
                if (mockInitialized) {
                    pushMockLocation(mockLat, mockLon)
                    mockHandler.postDelayed(mockRunnable, 2_000L)
                } else {
                    snapToRealGps()
                }
            }
            .addOnFailureListener {
                isMockActive = false
                binding.switchMock.isChecked = false
                Toast.makeText(
                    requireContext(),
                    "Cannot enable mock mode — set this app as 'Mock location app' in Developer Options.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    @SuppressLint("MissingPermission")
    private fun disableMock() {
        isMockActive = false
        mockHandler.removeCallbacks(mockRunnable)
        fusedClient.setMockMode(false)
    }

    @SuppressLint("MissingPermission")
    private fun pushMockLocation(lat: Double, lon: Double) {
        val loc = Location("mock").apply {
            latitude  = lat
            longitude = lon
            accuracy  = 3f
            altitude  = 0.0
            time      = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        fusedClient.setMockLocation(loc)
        updateCoordsDisplay()
        viewModel.updateFenceStatus(lat, lon)
    }

    // ── Snap to real GPS ──────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun snapToRealGps() {
        binding.btnResetGps.isEnabled = false
        binding.btnResetGps.text = "Getting GPS…"

        // Disable mock so FusedLocation delivers a real GPS fix
        snapWasMockActive = isMockActive
        if (isMockActive) {
            mockHandler.removeCallbacks(mockRunnable)
            fusedClient.setMockMode(false)
        }

        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMaxUpdates(1)
            .setWaitForAccurateLocation(false)
            .build()
        fusedClient.requestLocationUpdates(req, snapCallback, Looper.getMainLooper())

        // Give up after 15 s
        snapTimeout = Runnable {
            fusedClient.removeLocationUpdates(snapCallback)
            finishSnap()
            if (snapWasMockActive && isMockActive) {
                fusedClient.setMockMode(true)
                if (mockInitialized) {
                    pushMockLocation(mockLat, mockLon)
                    mockHandler.postDelayed(mockRunnable, 2_000L)
                }
            }
            if (isAdded) Toast.makeText(
                requireContext(), "GPS fix timed out — try again outdoors.", Toast.LENGTH_SHORT
            ).show()
        }
        snapHandler.postDelayed(snapTimeout!!, 15_000L)
    }

    private fun finishSnap() {
        if (!isAdded) return
        binding.btnResetGps.isEnabled = true
        binding.btnResetGps.text = "Snap to real GPS"
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun updateCoordsDisplay() {
        binding.tvCoords.text = if (mockInitialized)
            "%.6f,  %.6f".format(mockLat, mockLon)
        else "—"
    }

    private fun showNotReady() = Toast.makeText(
        requireContext(), "Tap 'Snap to real GPS' first to set a starting position.", Toast.LENGTH_SHORT
    ).show()

    private fun showMockOff() = Toast.makeText(
        requireContext(), "Enable Mock Location first.", Toast.LENGTH_SHORT
    ).show()

    override fun onDestroyView() {
        super.onDestroyView()
        mockHandler.removeCallbacks(mockRunnable)
        snapHandler.removeCallbacks(snapTimeout ?: Runnable {})
        fusedClient.removeLocationUpdates(snapCallback)
        if (isMockActive) fusedClient.setMockMode(false)
        _binding = null
    }
}
