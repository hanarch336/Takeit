package com.han.takeit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.han.takeit.databinding.ItemNoteBinding
import com.google.android.flexbox.FlexboxLayout
import com.han.takeit.db.Tag
import com.han.takeit.db.NoteRepository
import android.graphics.Color

class NotesAdapter(
    private val notes: List<Note>,
    private val noteRepository: NoteRepository,
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
            noteRepository: NoteRepository,
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
            updateTagsDisplay(note.tags, binding.tagsContainer, noteRepository)
            
            // 如果没有标签，隐藏标签容器
            binding.tagsContainer.visibility = if (note.tags.isNotEmpty()) View.VISIBLE else View.GONE
            
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
        
        private fun updateTagsDisplay(tags: List<String>, tagsContainer: FlexboxLayout, noteRepository: NoteRepository) {
            tagsContainer.removeAllViews()
            
            // 获取所有标签对象
            val allTagObjects = noteRepository.getAllTagObjects()
            
            tags.forEach { tagName ->
                // 查找对应的Tag对象
                val tagObject = allTagObjects.find { it.name == tagName }
                val tagColor = tagObject?.color ?: "#6200EE" // 默认颜色
                
                val tagView = TextView(tagsContainer.context).apply {
                    text = tagName
                    
                    // 根据标签颜色设置背景和文本颜色
                    val tagDrawable = ContextCompat.getDrawable(context, R.drawable.tag_background)?.mutate()
                    tagDrawable?.setTint(Color.parseColor(tagColor))
                    background = tagDrawable
                    
                    // 根据背景色计算文本颜色
                    val textColorString = Tag.getTextColor(tagColor)
                    val textColor = Color.parseColor(textColorString)
                    setTextColor(textColor)
                    
                    textSize = 14f
                    setPadding(24, 14, 24, 14) // 为首页卡片标签设置额外内边距
                }
                
                val layoutParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 16, 8) // 右边距16dp，下边距8dp
                }
                
                tagView.layoutParams = layoutParams
                tagsContainer.addView(tagView)
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
            noteRepository = noteRepository,
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