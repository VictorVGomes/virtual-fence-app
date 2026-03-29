package com.victorgomes.geofenceapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.victorgomes.geofenceapp.databinding.ActivityMainBinding
import com.victorgomes.geofenceapp.utils.NotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        if (startTimeMs == 0L) startTimeMs = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createNotificationChannel(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)

        // Push the bottom nav down so it sits above the system navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationView) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBar.bottom)
            insets
        }

        // Defer to the next frame so the NavController's start-destination transaction
        // is committed before we navigate away to the log. Calling navigate() directly
        // in onCreate() races with the initial fragment setup and can leave mapFragment
        // in a partially-initialised state when the user later opens the map tab.
        binding.root.post { handleNotificationIntent(intent) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Activity is already running — NavController is stable, navigate immediately.
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_NAVIGATE_TO_LOG, false) == true) {
            navController.navigate(R.id.logFragment)
        }
    }

    companion object {
        /** Millisecond timestamp of when the app process was first launched. */
        var startTimeMs = 0L
    }
}
