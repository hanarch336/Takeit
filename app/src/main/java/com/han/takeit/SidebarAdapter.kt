package com.han.takeit

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.han.takeit.databinding.ItemSidebarDateGroupBinding
import com.han.takeit.databinding.ItemSidebarNoteBinding
import java.text.SimpleDateFormat
import java.util.*

class SidebarAdapter(
    private val onNoteClick: (Note) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<SidebarDateGroup>()
    private val expandedItems = mutableSetOf<String>()

    companion object {
        private const val TYPE_DATE_GROUP = 0
        private const val TYPE_NOTE = 1
    }

    fun updateData(notes: List<Note>) {
        val groupedNotes = groupNotesByDate(notes)
        items.clear()
        items.addAll(groupedNotes)
        notifyDataSetChanged()
    }

    private fun groupNotesByDate(notes: List<Note>): List<SidebarDateGroup> {
        val dateFormat = SimpleDateFormat("yyyy.M.d", Locale.getDefault())
        val displayFormat = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
        
        return notes
            .groupBy { note ->
                dateFormat.format(Date(note.timestamp))
            }
            .map { (dateKey, notesList) ->
                val displayDate = displayFormat.format(Date(notesList.first().timestamp))
                SidebarDateGroup(
                    date = dateKey,
                    formattedDate = displayDate,
                    notes = notesList.sortedByDescending { it.timestamp },
                    isExpanded = expandedItems.contains(dateKey)
                )
            }
            .sortedByDescending { group ->
                group.notes.maxOfOrNull { it.timestamp } ?: 0L
            }
    }

    private fun getFlattenedItems(): List<Any> {
        val flatItems = mutableListOf<Any>()
        items.forEach { group ->
            flatItems.add(group)
            if (group.isExpanded) {
                flatItems.addAll(group.notes)
            }
        }
        return flatItems
    }

    override fun getItemCount(): Int = getFlattenedItems().size

    override fun getItemViewType(position: Int): Int {
        return when (getFlattenedItems()[position]) {
            is SidebarDateGroup -> TYPE_DATE_GROUP
            is Note -> TYPE_NOTE
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE_GROUP -> {
                val binding = ItemSidebarDateGroupBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                DateGroupViewHolder(binding)
            }
            TYPE_NOTE -> {
                val binding = ItemSidebarNoteBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                NoteViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DateGroupViewHolder -> {
                val group = getFlattenedItems()[position] as SidebarDateGroup
                holder.bind(group)
            }
            is NoteViewHolder -> {
                val note = getFlattenedItems()[position] as Note
                holder.bind(note)
            }
        }
    }

    inner class DateGroupViewHolder(
        private val binding: ItemSidebarDateGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(group: SidebarDateGroup) {
            binding.textDateGroup.text = group.formattedDate
            binding.textNoteCount.text = group.notes.size.toString()
            
            // 设置展开图标的旋转状态
            val rotation = if (group.isExpanded) 180f else 0f
            binding.iconExpand.rotation = rotation
            
            binding.layoutGroupHeader.setOnClickListener {
                toggleGroup(group)
            }
        }
        
        private fun toggleGroup(group: SidebarDateGroup) {
            group.isExpanded = !group.isExpanded
            
            if (group.isExpanded) {
                expandedItems.add(group.date)
            } else {
                expandedItems.remove(group.date)
            }
            
            // 动画旋转图标
            val targetRotation = if (group.isExpanded) 180f else 0f
            ObjectAnimator.ofFloat(binding.iconExpand, "rotation", targetRotation)
                .setDuration(200)
                .start()
            
            notifyDataSetChanged()
        }
    }

    inner class NoteViewHolder(
        private val binding: ItemSidebarNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            // 显示笔记预览（最多2行）
            val preview = note.content.replace("\n", " ").trim()
            binding.textNotePreview.text = if (preview.length > 50) {
                preview.take(50) + "..."
            } else {
                preview
            }
            
            binding.textNoteTime.text = note.getFormattedTime()
            
            binding.root.setOnClickListener {
                onNoteClick(note)
            }
        }
    }
}