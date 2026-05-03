package com.victorgomes.geofenceapp.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.activity.result.contract.ActivityResultContracts
import java.text.DateFormat
import java.util.Date
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.victorgomes.geofenceapp.R
import com.victorgomes.geofenceapp.data.database.GeofenceConfigEntity
import com.victorgomes.geofenceapp.databinding.DialogCreateFenceBinding
import com.victorgomes.geofenceapp.databinding.FragmentMapBinding
import com.victorgomes.geofenceapp.utils.PermissionHelper
import com.victorgomes.geofenceapp.utils.PersonalizationPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.roundToInt

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()

    private lateinit var myLocationOverlay: MyLocationNewOverlay

    private val fenceOverlays = mutableMapOf<String, Pair<Polygon, Marker>>()
    private val markerToFenceId = mutableMapOf<Marker, String>()
    private var lastKnownConfigs: List<GeofenceConfigEntity> = emptyList()
    private var currentlyOpenMarker: Marker? = null
    private var isSearchOpen = false
    private var useAltMap = false

    // ── Debug / mock location ─────────────────────────────────────────────────
    private var isDebugPanelOpen = false
    private lateinit var fusedClient: FusedLocationProviderClient
    private var mockLat = 0.0
    private var mockLon = 0.0
    private var mockInitialized = false
    private var isMockActive = false

    private val mockHandler = Handler(Looper.getMainLooper())
    private val mockRunnable = object : Runnable {
        override fun run() {
            if (isMockActive && mockInitialized) {
                pushMockLocation(mockLat, mockLon)
                mockHandler.postDelayed(this, 2_000L)
            }
        }
    }

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
            updateDebugCoords()
            finishSnap()
            if (snapWasMockActive) {
                fusedClient.setMockMode(true).addOnSuccessListener {
                    pushMockLocation(mockLat, mockLon)
                    mockHandler.removeCallbacks(mockRunnable)
                    mockHandler.postDelayed(mockRunnable, 2_000L)
                }
            }
        }
    }
    private var defaultStatusText = "Long-press map to add a fence"
    private var enterTimestamps: Map<String, Long> = emptyMap()
    // Tracks when GPS first detected "inside" for each fence (fallback when no DB event yet).
    private val mapLocalEntryTimes = mutableMapOf<String, Long>()
    private val locationHandler = Handler(Looper.getMainLooper())
    private val insideOutsideChecker = object : Runnable {
        override fun run() {
            updateInsideOutsideColors()
            locationHandler.postDelayed(this, 5_000L)
        }
    }

    // CartoDB Voyager: a polished, modern map style with sharper typography and
    // higher cartographic quality than standard MAPNIK. Free, no API key required.
    private val cartoVoyagerTileSource = XYTileSource(
        "CartoDB.Voyager", 0, 19, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
        )
    )

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            if (!PermissionHelper.hasBackgroundLocationPermission(requireContext())) {
                requestBackgroundLocationPermission()
            } else {
                initMap()
            }
        } else {
            Toast.makeText(requireContext(), "Location permission required.", Toast.LENGTH_LONG).show()
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> initMap() }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    private val activityRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Configuration.getInstance().also {
            it.userAgentValue = requireContext().packageName
            // More concurrent tile downloads so the map fills in faster.
            it.tileDownloadThreads = 4.toShort()
            it.tileDownloadMaxQueueSize = 16.toShort()
            // Keep more tiles in memory so adjacent zoom levels are already available
            // when the user pinches in or out, avoiding a stretch-then-reload flash.
            it.cacheMapTileCount = 25.toShort()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.cardStatus) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = statusBarHeight + 12
            }
            binding.btnSearchToggle.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = statusBarHeight + 12
            }
            insets
        }

        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())
        checkAndRequestPermissions()
        setupButtons()
        observeViewModel()
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionHelper.hasNotificationPermission(requireContext())
        ) {
            notificationPermissionLauncher.launch(PermissionHelper.NOTIFICATION_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !PermissionHelper.hasActivityRecognitionPermission(requireContext())
        ) {
            activityRecognitionLauncher.launch(PermissionHelper.ACTIVITY_RECOGNITION_PERMISSION)
        }
        when {
            !PermissionHelper.hasLocationPermissions(requireContext()) ->
                locationPermissionLauncher.launch(PermissionHelper.LOCATION_PERMISSIONS)
            !PermissionHelper.hasBackgroundLocationPermission(requireContext()) ->
                requestBackgroundLocationPermission()
            else -> initMap()
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(requireContext().packageName)) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Disable Battery Optimisation")
            .setMessage(
                "To reliably detect fence crossings in the background, this app needs to be " +
                "excluded from battery optimisation.\n\n" +
                "Tap \"Allow\" on the next screen to keep the app running when your phone is idle."
            )
            .setPositiveButton("Allow") { _, _ ->
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                })
            }
            .setNegativeButton("Skip", null)
            .show()
    }

    private fun requestBackgroundLocationPermission() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Background Location Required")
            .setMessage(
                "To monitor fences while the app is in the background, please grant " +
                "\"Allow all the time\" location access.\n\n" +
                "Samsung: Settings → Apps → GeoFenceApp → Permissions → Location → Allow all the time."
            )
            .setPositiveButton("Grant") { _, _ ->
                backgroundLocationLauncher.launch(PermissionHelper.BACKGROUND_LOCATION_PERMISSION)
            }
            .setNegativeButton("Skip") { _, _ -> initMap() }
            .show()
    }

    private fun initMap() {
        requestBatteryOptimizationExemption()
        val mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(false)
        mapView.minZoomLevel = 3.0
        mapView.maxZoomLevel = 19.0

        val rotationOverlay = org.osmdroid.views.overlay.gestures.RotationGestureOverlay(mapView)
        rotationOverlay.isEnabled = true
        mapView.overlays.add(rotationOverlay)
        mapView.controller.setZoom(15.0)

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
        myLocationOverlay.enableMyLocation()
        applyCharIcon()

        val locationOverlay = myLocationOverlay
        locationOverlay.runOnFirstFix {
            val location = locationOverlay.myLocation ?: return@runOnFirstFix
            activity?.runOnUiThread { mapView.controller.animateTo(location) }
        }
        mapView.overlays.add(myLocationOverlay)

        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                currentlyOpenMarker?.closeInfoWindow()
                currentlyOpenMarker = null
                InfoWindow.closeAllInfoWindowsOn(mapView)
                if (isSearchOpen) closeSearch()
                return false
            }
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let { showCreateFenceDialog(it) }
                return true
            }
        })
        mapView.overlays.add(mapEventsOverlay)
    }

    // ── Personalization helpers ──────────────────────────────────────────────

    private fun applyCharIcon() {
        if (!::myLocationOverlay.isInitialized) return
        val key = PersonalizationPrefs.getCharIcon(requireContext())
        val res = PersonalizationPrefs.charDrawableRes(key)
        val sizePx = (48 * resources.displayMetrics.density + 0.5f).toInt()
        val drawable = AppCompatResources.getDrawable(requireContext(), res) ?: return
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(Canvas(bitmap))
        myLocationOverlay.setPersonIcon(bitmap)
        myLocationOverlay.setDirectionIcon(bitmap)
    }

    private fun fenceIconDrawable(config: GeofenceConfigEntity) =
        AppCompatResources.getDrawable(
            requireContext(),
            PersonalizationPrefs.fenceDrawableRes(config.markerIcon)
        )!!.mutate().also { d ->
            DrawableCompat.setTint(
                d,
                if (config.isActive) 0xFF00AA00.toInt() else 0xFF888888.toInt()
            )
        }

    // ── Fence overlays ───────────────────────────────────────────────────────

    private fun showCreateFenceDialog(location: GeoPoint) {
        if (!PermissionHelper.hasLocationPermissions(requireContext())) {
            Toast.makeText(requireContext(), "Location permission required.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogCreateFenceBinding.inflate(layoutInflater)
        var selectedRadius = 200f
        var selectedIcon = PersonalizationPrefs.FENCE_PIN

        val existingCount = (viewModel.fenceConfigs.value?.size ?: 0) + 1
        dialogBinding.etFenceName.setText("Fence $existingCount")

        dialogBinding.tvDialogRadius.text = "${selectedRadius.roundToInt()} m"
        dialogBinding.seekBarDialogRadius.value = radiusToProgress(selectedRadius).toFloat()
        dialogBinding.seekBarDialogRadius.addOnChangeListener { _, value, _ ->
            selectedRadius = progressToRadius(value.toInt())
            dialogBinding.tvDialogRadius.text = "${selectedRadius.roundToInt()} m"
        }

        setupDialogIconPicker(dialogBinding, selectedIcon) { key -> selectedIcon = key }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Fence")
            .setView(dialogBinding.root)
            .setPositiveButton("Create") { _, _ ->
                val name = dialogBinding.etFenceName.text?.toString()?.trim()
                if (name.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a fence name.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.createFence(name, location.latitude, location.longitude, selectedRadius, selectedIcon)
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                    card.strokeColor = com.google.android.material.color.MaterialColors
                        .getColor(card, com.google.android.material.R.attr.colorPrimary)
                    card.setCardBackgroundColor(com.google.android.material.color.MaterialColors
                        .getColor(card, com.google.android.material.R.attr.colorPrimaryContainer))
                } else {
                    card.strokeColor = com.google.android.material.color.MaterialColors
                        .getColor(card, com.google.android.material.R.attr.colorOutlineVariant)
                    card.setCardBackgroundColor(com.google.android.material.color.MaterialColors
                        .getColor(card, com.google.android.material.R.attr.colorSurface))
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

    private fun observeViewModel() {
        viewModel.fenceConfigs.observe(viewLifecycleOwner) { configs ->
            lastKnownConfigs = configs
            updateFenceOverlays(configs)
            val activeCount = configs.count { it.isActive }
            defaultStatusText = when {
                configs.isEmpty() -> "Long-press map to add a fence"
                activeCount == 0  -> "${configs.size} fence(s) — none active"
                else              -> "$activeCount of ${configs.size} fence(s) active"
            }
            // Recompute inside status immediately with the new config list
            updateInsideOutsideColors()
        }

        viewModel.enterTimestamps.observe(viewLifecycleOwner) { timestamps ->
            enterTimestamps = timestamps
            updateInsideOutsideColors()
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFenceOverlays(configs: List<GeofenceConfigEntity>) {
        val mapView = binding.mapView
        val currentIds = configs.map { it.id }.toSet()

        val toRemove = fenceOverlays.keys.filter { it !in currentIds }
        toRemove.forEach { id ->
            fenceOverlays[id]?.let { (circle, marker) ->
                mapView.overlays.remove(circle)
                mapView.overlays.remove(marker)
                markerToFenceId.remove(marker)
            }
            fenceOverlays.remove(id)
        }

        configs.forEach { config ->
            val center   = GeoPoint(config.latitude, config.longitude)
            val existing = fenceOverlays[config.id]

            if (existing == null) {
                val circle = Polygon(mapView).apply {
                    points      = Polygon.pointsAsCircle(center, config.radiusMeters.toDouble())
                    fillColor   = if (config.isActive) 0x2200AA00 else 0x22888888
                    strokeColor = if (config.isActive) 0xFF00AA00.toInt() else 0xFF888888.toInt()
                    strokeWidth = 3f
                    title       = config.name
                }
                val marker = Marker(mapView).apply {
                    position = center
                    setAnchor(Marker.ANCHOR_CENTER, PersonalizationPrefs.fenceAnchorY(config.markerIcon))
                    title       = config.name
                    snippet     = "${config.radiusMeters.roundToInt()} m | ${if (config.isActive) "Active" else "Inactive"}"
                    isDraggable = true
                    icon        = fenceIconDrawable(config)
                }
                marker.setOnMarkerClickListener { clickedMarker, map ->
                    currentlyOpenMarker?.closeInfoWindow()
                    currentlyOpenMarker = null
                    InfoWindow.closeAllInfoWindowsOn(map)

                    val fenceId = markerToFenceId[clickedMarker]
                    val currentConfig = lastKnownConfigs.find { it.id == fenceId } ?: config
                    val userLoc = if (::myLocationOverlay.isInitialized) myLocationOverlay.myLocation else null
                    val distanceText = if (userLoc != null) {
                        val distanceM = userLoc.distanceToAsDouble(clickedMarker.position)
                        if (distanceM >= 1000.0) "%.1f km away".format(distanceM / 1000.0)
                        else "${distanceM.roundToInt()} m away"
                    } else "Distance unavailable"

                    clickedMarker.snippet = "${currentConfig.radiusMeters.roundToInt()} m radius | ${if (currentConfig.isActive) "Active" else "Inactive"} | $distanceText"
                    clickedMarker.showInfoWindow()
                    currentlyOpenMarker = clickedMarker
                    mapView.controller.animateTo(clickedMarker.position, 18.0, 1000L)
                    true
                }
                marker.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDragStart(marker: Marker) {
                        currentlyOpenMarker?.closeInfoWindow()
                        currentlyOpenMarker = null
                        InfoWindow.closeAllInfoWindowsOn(mapView)
                    }
                    override fun onMarkerDrag(marker: Marker) {
                        circle.points = Polygon.pointsAsCircle(marker.position, config.radiusMeters.toDouble())
                        mapView.invalidate()
                    }
                    override fun onMarkerDragEnd(marker: Marker) {
                        circle.points = Polygon.pointsAsCircle(marker.position, config.radiusMeters.toDouble())
                        viewModel.moveFence(config.id, marker.position.latitude, marker.position.longitude)
                    }
                })
                circle.setOnClickListener { polygon, mapView2, _ ->
                    currentlyOpenMarker?.closeInfoWindow()
                    currentlyOpenMarker = null
                    InfoWindow.closeAllInfoWindowsOn(mapView2)
                    val userLoc = if (::myLocationOverlay.isInitialized) myLocationOverlay.myLocation else null
                    val distanceText = if (userLoc != null) {
                        val distanceM = userLoc.distanceToAsDouble(marker.position)
                        if (distanceM >= 1000.0) "%.1f km away".format(distanceM / 1000.0)
                        else "${distanceM.roundToInt()} m away"
                    } else "Distance unavailable"
                    marker.snippet = "${config.radiusMeters.roundToInt()} m radius | ${if (config.isActive) "Active" else "Inactive"} | $distanceText"
                    marker.showInfoWindow()
                    currentlyOpenMarker = marker
                    true
                }
                mapView.overlays.add(circle)
                mapView.overlays.add(marker)
                fenceOverlays[config.id] = Pair(circle, marker)
                markerToFenceId[marker]  = config.id
            } else {
                val (circle, marker) = existing
                circle.points      = Polygon.pointsAsCircle(center, config.radiusMeters.toDouble())
                circle.fillColor   = if (config.isActive) 0x2200AA00 else 0x22888888
                circle.strokeColor = if (config.isActive) 0xFF00AA00.toInt() else 0xFF888888.toInt()
                marker.position    = center
                marker.title       = config.name
                marker.snippet     = "${config.radiusMeters.roundToInt()} m | ${if (config.isActive) "Active" else "Inactive"}"
                marker.icon        = fenceIconDrawable(config)
            }
        }

        mapView.invalidate()
    }

    private fun setupButtons() {
        binding.btnMyLocation.setOnClickListener {
            myLocationOverlay.myLocation?.let {
                binding.mapView.controller.animateTo(it, 19.0, 1000L)
            }
        }
        binding.btnZoomIn.setOnClickListener  { binding.mapView.controller.zoomIn() }
        binding.btnZoomOut.setOnClickListener { binding.mapView.controller.zoomOut() }
        binding.btnZoomWorld.setOnClickListener { binding.mapView.controller.zoomTo(13.0, 1500L) }

        binding.btnMapType.setOnClickListener {
            useAltMap = !useAltMap
            binding.mapView.setTileSource(
                if (useAltMap) cartoVoyagerTileSource else TileSourceFactory.MAPNIK
            )
            binding.mapView.invalidate()
        }

        binding.btnSearchToggle.setOnClickListener {
            if (isSearchOpen) closeSearch() else openSearch()
        }
        binding.btnSearch.setOnClickListener { searchLocation() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { searchLocation(); true } else false
        }

        // ── Debug panel ───────────────────────────────────────────────────────
        binding.btnDebugToggle.setOnClickListener {
            isDebugPanelOpen = !isDebugPanelOpen
            binding.debugPanel.visibility = if (isDebugPanelOpen) View.VISIBLE else View.GONE
        }

        binding.switchDebugMock.setOnCheckedChangeListener { _, checked ->
            if (checked) enableMock() else disableMock()
        }

        binding.debugBtnUp.setOnClickListener    { mockMove(1, 0) }
        binding.debugBtnDown.setOnClickListener  { mockMove(-1, 0) }
        binding.debugBtnLeft.setOnClickListener  { mockMove(0, -1) }
        binding.debugBtnRight.setOnClickListener { mockMove(0, 1) }
        binding.debugBtnCenter.setOnClickListener {
            if (!mockInitialized || !isMockActive) return@setOnClickListener
            pushMockLocation(mockLat, mockLon)
        }

        binding.debugBtnSnap.setOnClickListener { snapToRealGps() }
    }

    private fun openSearch() {
        isSearchOpen = true
        val slideOffset = 48f * resources.displayMetrics.density
        binding.cardSearch.apply {
            alpha = 0f
            translationY = -slideOffset
            visibility = View.VISIBLE
            animate().alpha(1f).translationY(0f).setDuration(220).start()
        }
        binding.etSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        if (!isSearchOpen) return
        isSearchOpen = false
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.etSearch.clearFocus()
        binding.etSearch.text?.clear()
        val slideOffset = 48f * resources.displayMetrics.density
        binding.cardSearch.animate()
            .alpha(0f)
            .translationY(-slideOffset)
            .setDuration(180)
            .withEndAction { binding.cardSearch.visibility = View.GONE }
            .start()
    }

    @Suppress("DEPRECATION")
    private fun searchLocation() {
        val query = binding.etSearch.text?.toString()?.trim()
        if (query.isNullOrEmpty()) return

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)

        if (!Geocoder.isPresent()) {
            Toast.makeText(requireContext(), "Geocoder not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                try { Geocoder(requireContext()).getFromLocationName(query, 1) }
                catch (e: Exception) { null }
            }
            if (results.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Location \"$query\" not found", Toast.LENGTH_SHORT).show()
            } else {
                val loc = results[0]
                binding.mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude), 19.0, 1500L)
                closeSearch()
            }
        }
    }

    private fun progressToRadius(progress: Int): Float {
        val min = 50f; val max = 5000f
        return (min * Math.pow((max / min).toDouble(), progress / 99.0)).toFloat()
    }

    private fun radiusToProgress(radius: Float): Int {
        val min = 50f; val max = 5000f
        return ((Math.log((radius / min).toDouble()) / Math.log((max / min).toDouble())) * 99)
            .roundToInt().coerceIn(0, 99)
    }

    private fun updateInsideOutsideColors() {
        if (!isAdded) return
        val userLoc = if (::myLocationOverlay.isInitialized) myLocationOverlay.myLocation else null

        // fenceId → enterTimestamp (null if we know we're inside but have no DB record)
        val insideFences = mutableListOf<Pair<String, Long?>>()

        lastKnownConfigs.forEach { config ->
            val (circle, _) = fenceOverlays[config.id] ?: return@forEach
            if (!config.isActive) return@forEach

            val isInside = if (userLoc != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLoc.latitude, userLoc.longitude,
                    config.latitude, config.longitude,
                    results
                )
                results[0] <= config.radiusMeters
            } else false

            circle.fillColor = if (isInside) 0x5500AA00 else 0x2200AA00
            if (isInside) {
                if (config.id !in mapLocalEntryTimes) mapLocalEntryTimes[config.id] = System.currentTimeMillis()
                insideFences.add(config.name to (enterTimestamps[config.id] ?: mapLocalEntryTimes[config.id]))
            } else {
                mapLocalEntryTimes.remove(config.id)
            }
        }

        binding.mapView.invalidate()
        updateStatusPill(insideFences)
    }

    private fun formatDuration(ms: Long): String {
        val totalMinutes = ms / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0        -> "${hours}h ${minutes}m"
            totalMinutes > 0 -> "${totalMinutes}m"
            else             -> "< 1m"
        }
    }

    private fun updateStatusPill(insideFences: List<Pair<String, Long?>>) {
        if (!isAdded) return
        if (insideFences.isEmpty()) {
            binding.tvMapStatus.text = defaultStatusText
            return
        }
        val timeFmt = DateFormat.getTimeInstance(DateFormat.SHORT)
        val now = System.currentTimeMillis()
        binding.tvMapStatus.text = insideFences.joinToString("  |  ") { (name, ts) ->
            if (ts != null) {
                "Inside $name · ${timeFmt.format(Date(ts))} (${formatDuration(now - ts)})"
            } else {
                "Inside $name"
            }
        }
    }

    private fun showOnboardingIfNeeded() {
        val ctx = requireContext()
        if (PersonalizationPrefs.hasSeenOnboarding(ctx)) return
        PersonalizationPrefs.setOnboardingSeen(ctx)
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Welcome!")
            .setMessage(
                "• Long-press anywhere on the map to create a geofence.\n\n" +
                "• Tap the switch on a fence in My Fences to start monitoring it.\n\n" +
                "• The app monitors your location in the background — you can close it after activating a fence.\n\n" +
                "• Tap a fence pin to see distance and status.\n\n" +
                "• For a full guide, go to Settings → Tutorial."
            )
            .setPositiveButton("Got it", null)
            .setNeutralButton("View Tutorial") { _, _ ->
                findNavController().navigate(R.id.personalizationFragment)
                // hasSeenTutorial stays false → PersonalizationFragment will auto-open Tutorial tab
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        // Debug FAB is hidden unless the user has unlocked it via the Settings Easter egg
        binding.btnDebugToggle.visibility =
            if (PersonalizationPrefs.isDebugUnlocked(requireContext())) View.VISIBLE else View.GONE
        if (::myLocationOverlay.isInitialized) {
            myLocationOverlay.enableMyLocation()
            locationHandler.post(insideOutsideChecker)
            applyCharIcon()
            // Rebuild fence markers so any icon change takes effect immediately
            if (lastKnownConfigs.isNotEmpty()) {
                fenceOverlays.values.forEach { (circle, marker) ->
                    binding.mapView.overlays.remove(circle)
                    binding.mapView.overlays.remove(marker)
                }
                fenceOverlays.clear()
                markerToFenceId.clear()
                currentlyOpenMarker = null
                updateFenceOverlays(lastKnownConfigs)
            }
        }
        showOnboardingIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        locationHandler.removeCallbacks(insideOutsideChecker)
        if (::myLocationOverlay.isInitialized) myLocationOverlay.disableMyLocation()
    }

    // ── Debug mock location methods ───────────────────────────────────────────

    private val debugStepMeters: Double
        get() = when (binding.debugChipGroupStep.checkedChipId) {
            binding.debugChip5m.id   -> 5.0
            binding.debugChip100m.id -> 100.0
            binding.debugChip500m.id -> 500.0
            else                     -> 20.0
        }

    private fun mockMove(dLat: Int, dLon: Int) {
        if (!mockInitialized || !isMockActive) return
        val step = debugStepMeters
        mockLat += dLat * (step / 111_111.0)
        mockLon += dLon * (step / (111_111.0 * kotlin.math.cos(Math.toRadians(mockLat))))
        updateDebugCoords()
        pushMockLocation(mockLat, mockLon)
    }

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
                binding.switchDebugMock.isChecked = false
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
        removeLocationManagerMock()
    }

    @SuppressLint("MissingPermission")
    private fun pushMockLocation(lat: Double, lon: Double) {
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtimeNanos()

        // 1. FusedLocationProviderClient — drives the geofencing pipeline
        val fusedLoc = Location("mock").apply {
            latitude  = lat
            longitude = lon
            accuracy  = 3f
            altitude  = 0.0
            time      = now
            elapsedRealtimeNanos = elapsed
        }
        fusedClient.setMockLocation(fusedLoc)

        // 2. LocationManager — drives the OSMdroid blue dot
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE)
                as android.location.LocationManager
        listOf(
            android.location.LocationManager.GPS_PROVIDER,
            android.location.LocationManager.NETWORK_PROVIDER
        ).forEach { provider ->
            try { addTestProviderCompat(lm, provider) } catch (_: Exception) {}
            try {
                lm.setTestProviderEnabled(provider, true)
                val mockLoc = Location(provider).apply {
                    latitude  = lat
                    longitude = lon
                    accuracy  = 3f
                    altitude  = 0.0
                    time      = now
                    elapsedRealtimeNanos = elapsed
                }
                lm.setTestProviderLocation(provider, mockLoc)
            } catch (_: Exception) {}
        }

        updateDebugCoords()
    }

    @SuppressLint("WrongConstant")
    @Suppress("DEPRECATION")
    private fun addTestProviderCompat(lm: android.location.LocationManager, provider: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val props = android.location.provider.ProviderProperties.Builder()
                .setPowerUsage(1 /* POWER_USAGE_LOW */)
                .setAccuracy(1  /* ACCURACY_FINE */)
                .setHasAltitudeSupport(true)
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .build()
            lm.addTestProvider(provider, props)
        } else {
            lm.addTestProvider(
                provider, false, false, false, false, true, true, true,
                android.location.Criteria.POWER_LOW, android.location.Criteria.ACCURACY_FINE
            )
        }
    }

    private fun removeLocationManagerMock() {
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE)
                as android.location.LocationManager
        listOf(
            android.location.LocationManager.GPS_PROVIDER,
            android.location.LocationManager.NETWORK_PROVIDER
        ).forEach { provider ->
            try { lm.removeTestProvider(provider) } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun snapToRealGps() {
        binding.debugBtnSnap.isEnabled = false
        binding.debugBtnSnap.text = "Getting GPS…"

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
        binding.debugBtnSnap.isEnabled = true
        binding.debugBtnSnap.text = "Snap to GPS"
    }

    private fun updateDebugCoords() {
        if (!isAdded) return
        binding.tvDebugCoords.text = if (mockInitialized)
            "%.5f, %.5f".format(mockLat, mockLon)
        else "—"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mockHandler.removeCallbacks(mockRunnable)
        snapHandler.removeCallbacks(snapTimeout ?: Runnable {})
        fusedClient.removeLocationUpdates(snapCallback)
        if (isMockActive) fusedClient.setMockMode(false)
        _binding = null
        fenceOverlays.clear()
        markerToFenceId.clear()
    }
}
