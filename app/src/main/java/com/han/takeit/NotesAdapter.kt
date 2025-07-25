package com.han.takeit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.han.takeit.databinding.ItemNoteBinding

class NotesAdapter(
    private val notes: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit,
    private val onSelectionChanged: (Int) -> Unit,
    private var maxLines: Int = 6
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {
    
    private var isSelectionMode = false
    private val selectedNotes = mutableSetOf<Long>()
    
    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) {
            selectedNotes.clear()
            onSelectionChanged(0)
        }
        notifyDataSetChanged()
    }
    
    fun getSelectedNotes(): List<Note> {
        return notes.filter { selectedNotes.contains(it.id) }
    }
    
    fun hasSelectedNotes(): Boolean {
        return selectedNotes.isNotEmpty()
    }
    
    fun clearSelection() {
        selectedNotes.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }
    
    fun updateMaxLines(newMaxLines: Int) {
        maxLines = newMaxLines
        notifyDataSetChanged()
    }

    class NoteViewHolder(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            note: Note, 
            isSelectionMode: Boolean,
            isSelected: Boolean,
            maxLines: Int,
            onNoteClick: (Note) -> Unit,
            onNoteLongClick: (Note) -> Unit,
            onToggleSelection: (Note) -> Unit
        ) {
            // 根据设置的最大行数显示内容预览
            val lines = note.content.split("\n")
            val preview = if (lines.size > maxLines) {
                lines.take(maxLines).joinToString("\n")
            } else {
                note.content
            }
            
            // 设置TextView的最大行数
            binding.textTitle.maxLines = maxLines
            binding.textTitle.text = preview
            
            // 显示标签
            if (note.tags.isNotEmpty()) {
                val tagsText = note.tags.joinToString(" ") { "#$it" }
                binding.textContent.text = tagsText
            } else {
                binding.textContent.text = ""
            }
            
            binding.textTime.text = note.getFormattedTime()
            
            // 设置选择状态的视觉效果
            binding.root.isActivated = isSelected
            binding.linearLayout.isActivated = isSelected
            binding.root.alpha = if (isSelectionMode && !isSelected) 0.7f else 1.0f
            
            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    onToggleSelection(note)
                } else {
                    onNoteClick(note)
                }
            }
            
            binding.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    onNoteLongClick(note)
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        val isSelected = selectedNotes.contains(note.id)
        
        holder.bind(
            note = note,
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            maxLines = maxLines,
            onNoteClick = onNoteClick,
            onNoteLongClick = onNoteLongClick,
            onToggleSelection = { toggleNoteSelection(it) }
        )
    }
    
    fun toggleSelection(note: Note) {
        toggleNoteSelection(note)
    }
    
    private fun toggleNoteSelection(note: Note) {
        if (selectedNotes.contains(note.id)) {
            selectedNotes.remove(note.id)
        } else {
            selectedNotes.add(note.id)
        }
        notifyItemChanged(notes.indexOf(note))
        onSelectionChanged(selectedNotes.size)
    }

    override fun getItemCount(): Int = notes.size
}