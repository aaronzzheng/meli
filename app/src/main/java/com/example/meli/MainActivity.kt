package com.example.meli

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.meli.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomnavigation.BottomNavigationView

private const val TAG = "MeliLifecycle"
private const val DARK_MODE_LIGHT_THRESHOLD_LUX = 10f

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private val lightSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val lightLevel = event.values.firstOrNull() ?: return
            val desiredNightMode = if (lightLevel < DARK_MODE_LIGHT_THRESHOLD_LUX) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }

            if (AppCompatDelegate.getDefaultNightMode() != desiredNightMode) {
                Log.d(TAG, "Ambient light changed to $lightLevel lux, applying mode=$desiredNightMode")
                AppCompatDelegate.setDefaultNightMode(desiredNightMode)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate")

        sensorManager = getSystemService(SensorManager::class.java)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        var shouldShowBottomNav = false
        var imeVisible = false
        val navHost = findViewById<View>(R.id.nav_host_fragment_activity_main)

        fun updateBottomNavVisibility() {
            val showBottomNav = shouldShowBottomNav && !imeVisible
            navView.visibility = if (showBottomNav) View.VISIBLE else View.GONE

            val params = navHost.layoutParams as ConstraintLayout.LayoutParams
            if (showBottomNav) {
                params.bottomToTop = R.id.nav_view
                params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            } else {
                params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
            navHost.layoutParams = params
        }

        navView.setupWithNavController(navController)
        ViewCompat.setOnApplyWindowInsetsListener(binding.container) { _, insets ->
            imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            updateBottomNavVisibility()
            insets
        }

        if (savedInstanceState == null &&
            FirebaseAuth.getInstance().currentUser != null &&
            navController.currentDestination?.id == R.id.navigation_login
        ) {
            navController.navigate(R.id.action_navigation_login_to_navigation_home)
        }

        // Hide bottom navigation on the login screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            shouldShowBottomNav = when (destination.id) {
                // List of destinations where the bottom nav should be visible
                R.id.navigation_home,
                R.id.navigation_list,
                R.id.navigation_search,
                R.id.navigation_profile,
                R.id.userProfileFragment,
                R.id.settingsFragment,
                R.id.settingsDetailFragment,
                R.id.friendsFragment,
                R.id.accountSettingsFragment -> true
                // For all other destinations, hide it
                else -> false
            }
            updateBottomNavVisibility()
        }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let { sensor ->
            sensorManager.registerListener(
                lightSensorListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(lightSensorListener)
        super.onPause()
    }
}
