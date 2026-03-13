package com.example.meli.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.meli.databinding.ActivityLoginBinding
import com.example.meli.ui.auth.AuthViewModel
import com.example.meli.ui.notes.NotesActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (viewModel.currentUser != null) navigateToNotes()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.status.observe(this) { result ->
            result?.onSuccess { navigateToNotes() }
                ?.onFailure { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
            viewModel.clearStatus()
        }

        binding.loginBtn.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val pass = binding.passwordInput.text.toString()
            if (email.isNotEmpty() && pass.isNotEmpty()) viewModel.login(email, pass)
        }

        binding.registerBtn.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val pass = binding.passwordInput.text.toString()
            if (email.isNotEmpty() && pass.isNotEmpty()) viewModel.register(email, pass)
        }
    }

    private fun navigateToNotes() {
        startActivity(Intent(this, NotesActivity::class.java))
        finish()
    }
}