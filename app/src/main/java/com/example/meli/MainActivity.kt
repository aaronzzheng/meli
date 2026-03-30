package com.example.meli

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.meli.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "MeliLifecycle"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate")

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
}
