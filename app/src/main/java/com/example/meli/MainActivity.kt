package com.example.meli

import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.meli.databinding.ActivityMainBinding

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
        
        navView.setupWithNavController(navController)

        // Hide bottom navigation on the login screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // List of destinations where the bottom nav should be visible
                R.id.navigation_home,
                R.id.navigation_list,
                R.id.navigation_search,
                R.id.navigation_profile -> {
                    navView.visibility = View.VISIBLE
                }
                // For all other destinations, hide it
                else -> {
                    navView.visibility = View.GONE
                }
            }
        }
    }
}