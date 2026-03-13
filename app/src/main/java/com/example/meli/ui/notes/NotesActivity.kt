package com.example.meli.ui.notes

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meli.databinding.ActivityNotesBinding
import com.example.meli.ui.auth.AuthViewModel
import com.example.meli.ui.login.LoginActivity

class NotesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotesBinding
    private val viewModel: NotesViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val adapter by lazy {
        NoteAdapter(
            onEdit = { note -> showNoteDialog(note.id, note.title) },
            onDelete = { note -> viewModel.deleteNote(note.id) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.notesRv.layoutManager = LinearLayoutManager(this)
        binding.notesRv.adapter = adapter

        viewModel.notes.observe(this) { notes ->
            adapter.submitList(notes)
        }

        binding.addNoteFab.setOnClickListener { showNoteDialog() }
        binding.logoutBtn.setOnClickListener {
            authViewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showNoteDialog(id: String? = null, currentTitle: String = "") {
        val input = EditText(this).apply { setText(currentTitle) }
        AlertDialog.Builder(this)
            .setTitle(if (id == null) "New Note" else "Edit Note")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val title = input.text.toString()
                if (title.isNotBlank()) {
                    if (id == null) viewModel.addNote(title) else viewModel.updateNote(id, title)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}