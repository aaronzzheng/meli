package com.example.meli.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meli.databinding.NoteItemBinding
import com.example.meli.model.Note

class NoteAdapter(
    private val onEdit: (Note) -> Unit,
    private val onDelete: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.ViewHolder>(NoteDiffCallback()) {

    class ViewHolder(val binding: NoteItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = NoteItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = getItem(position)
        holder.binding.noteTitle.text = note.title
        holder.binding.editBtn.setOnClickListener { onEdit(note) }
        holder.binding.deleteBtn.setOnClickListener { onDelete(note) }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
    }
}