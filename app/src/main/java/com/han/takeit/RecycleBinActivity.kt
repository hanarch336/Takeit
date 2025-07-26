package com.han.takeit

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.han.takeit.databinding.ActivityRecycleBinBinding
import com.han.takeit.db.NoteRepository

class RecycleBinActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRecycleBinBinding
    private lateinit var noteRepository: NoteRepository
    private lateinit var notesAdapter: RecycleBinAdapter
    private val deletedNotes = mutableListOf<Note>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityRecycleBinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        noteRepository = NoteRepository(this)
        
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        
        loadDeletedNotes()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.recycle_bin)
        }
    }
    
    private fun setupRecyclerView() {
        notesAdapter = RecycleBinAdapter(
            notes = deletedNotes,
            noteRepository = noteRepository,
            onNoteClick = { note ->
                showRestoreDialog(note)
            },
            onNoteLongClick = { note ->
                showPermanentDeleteDialog(note)
            },
            onSelectionChanged = { _ -> }
        )
        
        binding.recyclerViewNotes.apply {
            layoutManager = LinearLayoutManager(this@RecycleBinActivity)
            adapter = notesAdapter
        }
    }
    
    private fun setupButtons() {
        binding.btnRestoreAll.setOnClickListener {
            if (deletedNotes.isNotEmpty()) {
                showRestoreAllDialog()
            }
        }
        
        binding.btnClearAll.setOnClickListener {
            if (deletedNotes.isNotEmpty()) {
                showClearAllDialog()
            }
        }
    }
    
    private fun loadDeletedNotes() {
        // 自动清理30天以上的已删除笔记
        noteRepository.autoCleanOldDeletedNotes()
        
        deletedNotes.clear()
        deletedNotes.addAll(noteRepository.getDeletedNotes())
        notesAdapter.notifyDataSetChanged()
        
        updateUI()
    }
    
    private fun updateUI() {
        binding.btnRestoreAll.isEnabled = deletedNotes.isNotEmpty()
        binding.btnClearAll.isEnabled = deletedNotes.isNotEmpty()
        
        if (deletedNotes.isEmpty()) {
            binding.tvEmptyMessage.visibility = android.view.View.VISIBLE
            binding.recyclerViewNotes.visibility = android.view.View.GONE
        } else {
            binding.tvEmptyMessage.visibility = android.view.View.GONE
            binding.recyclerViewNotes.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun showRestoreDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("恢复笔记")
            .setMessage("确定要恢复这条笔记吗？")
            .setPositiveButton("恢复") { _, _ ->
                restoreNote(note)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showPermanentDeleteDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("永久删除")
            .setMessage("确定要永久删除这条笔记吗？此操作不可撤销！")
            .setPositiveButton("永久删除") { _, _ ->
                permanentDeleteNote(note)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showRestoreAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("恢复所有笔记")
            .setMessage("确定要恢复所有已删除的笔记吗？")
            .setPositiveButton("恢复所有") { _, _ ->
                restoreAllNotes()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showClearAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("清空回收站")
            .setMessage("确定要永久删除所有笔记吗？此操作不可撤销！")
            .setPositiveButton("清空所有") { _, _ ->
                clearAllNotes()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun restoreNote(note: Note) {
        noteRepository.restoreNote(note.id)
        Toast.makeText(this, "笔记已恢复", Toast.LENGTH_SHORT).show()
        loadDeletedNotes()
    }
    
    private fun permanentDeleteNote(note: Note) {
        noteRepository.permanentDeleteNote(note.id)
        Toast.makeText(this, "笔记已永久删除", Toast.LENGTH_SHORT).show()
        loadDeletedNotes()
    }
    
    private fun restoreAllNotes() {
        val noteIds = deletedNotes.map { it.id }
        noteRepository.restoreNotes(noteIds)
        Toast.makeText(this, "所有笔记已恢复", Toast.LENGTH_SHORT).show()
        loadDeletedNotes()
    }
    
    private fun clearAllNotes() {
        val noteIds = deletedNotes.map { it.id }
        noteRepository.permanentDeleteNotes(noteIds)
        Toast.makeText(this, "回收站已清空", Toast.LENGTH_SHORT).show()
        loadDeletedNotes()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}