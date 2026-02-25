package com.example.meli

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
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
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_list,
                R.id.navigation_search,
                R.id.navigation_profile
            )
        )
        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
    override fun onStart() { super.onStart(); Log.d(TAG, "MainActivity onStart") }
    override fun onResume() { super.onResume(); Log.d(TAG, "MainActivity onResume") }
    override fun onPause() { Log.d(TAG, "MainActivity onPause"); super.onPause() }
    override fun onStop() { Log.d(TAG, "MainActivity onStop"); super.onStop() }
    override fun onDestroy() { Log.d(TAG, "MainActivity onDestroy"); super.onDestroy() }
}
