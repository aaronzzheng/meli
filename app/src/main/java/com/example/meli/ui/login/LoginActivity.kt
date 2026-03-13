package com.example.meli.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.meli.databinding.ActivityLoginBinding
import com.example.meli.ui.auth.AuthViewModel
import com.example.meli.ui.notes.NotesActivity

class LoginActivity : AppCompatActivity() {
    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Immediate redirect if logged in
        if (viewModel.currentUser != null) {
            navigateToNotes()
            return
        }

        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prevent double clicks and show progress
        viewModel.isLoading.observe(this) { loading ->
            binding.loginBtn.isEnabled = !loading
            binding.registerBtn.isEnabled = !loading
            binding.loginBtn.text = if (loading) "Processing..." else "Login"
        }

        viewModel.status.observe(this) { result ->
            result?.onSuccess { 
                navigateToNotes() 
            }?.onFailure { error ->
                Log.e("AUTH_ERROR", "Login/Signup failed", error)
                Toast.makeText(this, "Error: ${error.localizedMessage}", Toast.LENGTH_LONG).show() 
            }
            viewModel.clearStatus()
        }

        binding.loginBtn.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val pass = binding.passwordInput.text.toString()
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                viewModel.login(email, pass)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.registerBtn.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val pass = binding.passwordInput.text.toString()
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                viewModel.register(email, pass)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToNotes() {
        startActivity(Intent(this, NotesActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null // Clear binding to prevent potential memory leaks
    }
}