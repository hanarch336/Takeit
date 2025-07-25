package com.han.takeit.db

import android.content.Context
import com.han.takeit.Note

class NoteRepository(context: Context) {
    private val database = NoteDatabase(context)
    
    // 获取所有笔记
    fun getAllNotes(): List<Note> {
        return database.getAllNotes()
    }
    
    // 根据ID获取笔记
    fun getNoteById(noteId: Long): Note? {
        return database.getNoteById(noteId)
    }
    
    // 保存笔记（插入或更新）
    fun saveNote(note: Note): Long {
        val existingNote = database.getNoteById(note.id)
        return if (existingNote == null) {
            // 新笔记，插入
            database.insertNote(note)
        } else {
            // 已存在，更新
            database.updateNote(note)
            note.id
        }
    }
    
    // 删除笔记
    fun deleteNote(noteId: Long): Boolean {
        return database.deleteNote(noteId) > 0
    }
    
    // 批量删除笔记
    fun deleteNotes(noteIds: List<Long>): Int {
        return database.deleteNotes(noteIds)
    }
    
    // 生成新的笔记ID
    fun generateNoteId(): Long {
        return System.currentTimeMillis()
    }
    
    // 标签相关操作
    fun getAllTags(): List<String> {
        return database.getAllTags()
    }
    
    fun getAllTagObjects(): List<Tag> {
        return database.getAllTagObjects()
    }
    
    fun insertTag(tagName: String, tagColor: String = Tag.getRandomColor()): Long {
        return database.insertTag(tagName, tagColor)
    }
    
    fun insertTagWithColor(tagName: String, tagColor: String): Long {
        return database.insertTag(tagName, tagColor)
    }
    
    fun updateTagColor(tagName: String, newColor: String) {
        database.updateTagColor(tagName, newColor)
    }
    
    fun addTagToNote(noteId: Long, tagName: String) {
        database.addTagToNote(noteId, tagName)
    }
    
    fun removeTagFromNote(noteId: Long, tagName: String) {
        database.removeTagFromNote(noteId, tagName)
    }
    
    fun searchTags(query: String): List<String> {
        return getAllTags().filter { it.contains(query, ignoreCase = true) }
    }
    
    fun getNotesByTag(tagName: String): List<Note> {
        return database.getNotesByTag(tagName)
    }
    
    // 创建示例笔记
    fun createSampleNotes() {
        val sampleNotes = listOf(
            Note(generateNoteId(), "完成项目开发\n准备会议材料", System.currentTimeMillis(), listOf("工作", "项目")),
            Note(generateNoteId(), "牛奶\n面包\n鸡蛋\n水果", System.currentTimeMillis() - 86400000, listOf("购物")),
            Note(generateNoteId(), "《Android开发艺术探索》\n第一章：Activity的生命周期", System.currentTimeMillis() - 172800000, listOf("学习", "Android"))
        )
        
        for (note in sampleNotes) {
            saveNote(note)
        }
    }
}