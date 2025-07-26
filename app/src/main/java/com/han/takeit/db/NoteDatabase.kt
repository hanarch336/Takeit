package com.han.takeit.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.han.takeit.Note
import org.json.JSONObject

class NoteDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "notes.db"
        const val DATABASE_VERSION = 8

        // 笔记表
        const val TABLE_NOTES = "notes"
        const val COLUMN_ID = "id"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_CREATED_TIME = "created_time"
        const val COLUMN_MODIFIED_TIME = "modified_time"
        const val COLUMN_CUSTOM_PROPERTIES = "custom_properties"
        const val COLUMN_DELETED = "deleted"
        
        // 标签表
        const val TABLE_TAGS = "tags"
        const val COLUMN_TAG_ID = "tag_id"
        const val COLUMN_TAG_NAME = "tag_name"
        const val COLUMN_TAG_COLOR = "tag_color"
        
        // 笔记标签关联表
        const val TABLE_NOTE_TAGS = "note_tags"
        const val COLUMN_NOTE_ID = "note_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建笔记表
        val createNotesTable = "CREATE TABLE $TABLE_NOTES ("
            .plus("$COLUMN_ID INTEGER PRIMARY KEY, ")
            .plus("$COLUMN_CONTENT TEXT, ")
            .plus("$COLUMN_TIMESTAMP INTEGER, ")
            .plus("$COLUMN_CREATED_TIME INTEGER, ")
            .plus("$COLUMN_MODIFIED_TIME INTEGER, ")
            .plus("$COLUMN_CUSTOM_PROPERTIES TEXT DEFAULT '{}', ")
            .plus("$COLUMN_DELETED INTEGER DEFAULT 0)")
        db.execSQL(createNotesTable)
        
        // 创建标签表
        val createTagsTable = "CREATE TABLE $TABLE_TAGS ("
            .plus("$COLUMN_TAG_ID INTEGER PRIMARY KEY AUTOINCREMENT, ")
            .plus("$COLUMN_TAG_NAME TEXT UNIQUE, ")
            .plus("$COLUMN_TAG_COLOR TEXT DEFAULT '#6200EE')")
        db.execSQL(createTagsTable)
        
        // 创建笔记标签关联表
        val createNoteTagsTable = "CREATE TABLE $TABLE_NOTE_TAGS ("
            .plus("$COLUMN_NOTE_ID INTEGER, ")
            .plus("$COLUMN_TAG_ID INTEGER, ")
            .plus("PRIMARY KEY ($COLUMN_NOTE_ID, $COLUMN_TAG_ID), ")
            .plus("FOREIGN KEY ($COLUMN_NOTE_ID) REFERENCES $TABLE_NOTES($COLUMN_ID) ON DELETE CASCADE, ")
            .plus("FOREIGN KEY ($COLUMN_TAG_ID) REFERENCES $TABLE_TAGS($COLUMN_TAG_ID) ON DELETE CASCADE)")
        db.execSQL(createNoteTagsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 在升级前创建备份
        try {
            val context = readableDatabase.path?.let { path ->
                // 从数据库路径获取context可能比较复杂，这里我们在调用时传入context
                // 暂时先记录日志
                android.util.Log.i("NoteDatabase", "Database upgrade from version $oldVersion to $newVersion")
            }
        } catch (e: Exception) {
            android.util.Log.e("NoteDatabase", "Failed to create backup during upgrade", e)
        }
        
        if (oldVersion < 5) {
            // 为标签表添加颜色字段
            db.execSQL("ALTER TABLE $TABLE_TAGS ADD COLUMN $COLUMN_TAG_COLOR TEXT DEFAULT '#6200EE'")
        }
        if (oldVersion < 6) {
            // 为笔记表添加自定义属性字段
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_CUSTOM_PROPERTIES TEXT DEFAULT '{}'")
        }
        if (oldVersion < 7) {
            // 添加创建时间和修改时间字段
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_CREATED_TIME INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_MODIFIED_TIME INTEGER DEFAULT 0")
            // 将现有的timestamp值复制到新字段中
            db.execSQL("UPDATE $TABLE_NOTES SET $COLUMN_CREATED_TIME = $COLUMN_TIMESTAMP, $COLUMN_MODIFIED_TIME = $COLUMN_TIMESTAMP")
        }
        if (oldVersion < 8) {
            // 添加软删除字段
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_DELETED INTEGER DEFAULT 0")
        }
    }

    // 插入笔记
    fun insertNote(note: Note): Long {
        val values = ContentValues().apply {
            put(COLUMN_ID, note.id)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_TIMESTAMP, note.timestamp)
            put(COLUMN_CREATED_TIME, note.createdTime)
            put(COLUMN_MODIFIED_TIME, note.modifiedTime)
            put(COLUMN_CUSTOM_PROPERTIES, JSONObject(note.customProperties).toString())
        }

        return writableDatabase.use { db ->
            val result = db.insert(TABLE_NOTES, null, values)
            // 添加标签，使用原始的note.id而不是数据库返回的行ID
            note.tags.forEach { tagName ->
                addTagToNoteWithDb(db, note.id, tagName)
            }
            result
        }
    }

    // 更新笔记
    fun updateNote(note: Note): Int {
        val values = ContentValues().apply {
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_TIMESTAMP, note.timestamp)
            put(COLUMN_CREATED_TIME, note.createdTime)
            put(COLUMN_MODIFIED_TIME, note.modifiedTime)
            put(COLUMN_CUSTOM_PROPERTIES, JSONObject(note.customProperties).toString())
        }

        return writableDatabase.use { db ->
            val result = db.update(
                TABLE_NOTES,
                values,
                "$COLUMN_ID = ?",
                arrayOf(note.id.toString())
            )
            
            // 更新标签：先删除所有关联，再重新添加
            db.delete(
                TABLE_NOTE_TAGS,
                "$COLUMN_NOTE_ID = ?",
                arrayOf(note.id.toString())
            )
            
            note.tags.forEach { tagName ->
                addTagToNoteWithDb(db, note.id, tagName)
            }
            
            result
        }
    }

    // 软删除笔记
    fun deleteNote(noteId: Long): Int {
        val values = ContentValues().apply {
            put(COLUMN_DELETED, 1)
        }
        return writableDatabase.use { db ->
            db.update(
                TABLE_NOTES,
                values,
                "$COLUMN_ID = ?",
                arrayOf(noteId.toString())
            )
        }
    }

    // 获取所有笔记（未删除的）
    fun getAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()

        val query = "SELECT * FROM $TABLE_NOTES WHERE $COLUMN_DELETED = 0 ORDER BY $COLUMN_CREATED_TIME DESC"

        readableDatabase.use { db ->
            db.rawQuery(query, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                    val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
                    val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    val createdTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME))
                    val modifiedTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MODIFIED_TIME))
                    val customPropertiesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUSTOM_PROPERTIES)) ?: "{}"
                    val customProperties = parseCustomProperties(customPropertiesJson)
                    val tags = getTagsForNote(id)

                    notes.add(Note(id, content, createdTime, modifiedTime, tags, customProperties))
                }
            }
        }

        return notes
    }

    // 根据ID获取笔记
    fun getNoteById(noteId: Long): Note? {
        var note: Note? = null

        readableDatabase.use { db ->
            db.query(
                TABLE_NOTES,
                null,
                "$COLUMN_ID = ?",
                arrayOf(noteId.toString()),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
                    val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    val createdTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME))
                    val modifiedTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MODIFIED_TIME))
                    val customPropertiesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUSTOM_PROPERTIES)) ?: "{}"
                    val customProperties = parseCustomProperties(customPropertiesJson)
                    val tags = getTagsForNote(noteId)

                    note = Note(noteId, content, createdTime, modifiedTime, tags, customProperties)
                }
            }
        }

        return note
    }

    // 批量软删除笔记
    fun deleteNotes(noteIds: List<Long>): Int {
        if (noteIds.isEmpty()) return 0

        val values = ContentValues().apply {
            put(COLUMN_DELETED, 1)
        }
        val placeholders = noteIds.joinToString(", ") { "?" }
        val whereClause = "$COLUMN_ID IN ($placeholders)"
        val whereArgs = noteIds.map { it.toString() }.toTypedArray()

        return writableDatabase.use { db ->
            db.update(TABLE_NOTES, values, whereClause, whereArgs)
        }
    }

    // 插入标签（使用现有数据库连接）
    private fun insertTagWithDb(db: SQLiteDatabase, tagName: String, tagColor: String = com.han.takeit.db.Tag.getRandomColor()): Long {
        val values = ContentValues().apply {
            put(COLUMN_TAG_NAME, tagName)
            put(COLUMN_TAG_COLOR, tagColor)
        }
        return db.insert(TABLE_TAGS, null, values)
    }
    
    // 插入标签
    fun insertTag(tagName: String, tagColor: String = com.han.takeit.db.Tag.getRandomColor()): Long {
        val values = ContentValues().apply {
            put(COLUMN_TAG_NAME, tagName)
            put(COLUMN_TAG_COLOR, tagColor)
        }

        return writableDatabase.use { db ->
            db.insert(TABLE_TAGS, null, values)
        }
    }

    // 获取所有标签名称（兼容性方法）
    fun getAllTags(): List<String> {
        return getAllTagObjects().map { it.name }
    }
    
    // 获取所有标签对象
    fun getAllTagObjects(): List<com.han.takeit.db.Tag> {
        val tags = mutableListOf<com.han.takeit.db.Tag>()

        readableDatabase.use { db ->
            db.query(
                TABLE_TAGS,
                arrayOf(COLUMN_TAG_ID, COLUMN_TAG_NAME, COLUMN_TAG_COLOR),
                null,
                null,
                null,
                null,
                COLUMN_TAG_NAME
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val tagId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TAG_ID))
                    val tagName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_NAME))
                    val tagColor = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_COLOR)) ?: "#6200EE"
                    tags.add(com.han.takeit.db.Tag(tagId, tagName, tagColor))
                }
            }
        }

        return tags
    }
    
    // 更新标签颜色
    fun updateTagColor(tagName: String, newColor: String): Int {
        val values = ContentValues().apply {
            put(COLUMN_TAG_COLOR, newColor)
        }
        
        return writableDatabase.use { db ->
            db.update(
                TABLE_TAGS,
                values,
                "$COLUMN_TAG_NAME = ?",
                arrayOf(tagName)
            )
        }
    }

    // 获取笔记的标签
    fun getTagsForNote(noteId: Long): List<String> {
        val tags = mutableListOf<String>()

        val query = "SELECT t.$COLUMN_TAG_NAME FROM $TABLE_TAGS t " +
                "INNER JOIN $TABLE_NOTE_TAGS nt ON t.$COLUMN_TAG_ID = nt.$COLUMN_TAG_ID " +
                "WHERE nt.$COLUMN_NOTE_ID = ?"

        readableDatabase.use { db ->
            db.rawQuery(query, arrayOf(noteId.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    val tagName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_NAME))
                    tags.add(tagName)
                }
            }
        }

        return tags
    }

    // 为笔记添加标签（使用现有数据库连接）
    private fun addTagToNoteWithDb(db: SQLiteDatabase, noteId: Long, tagName: String) {
        // 首先获取或创建标签
        var tagId = getTagIdWithDb(db, tagName)
        if (tagId == -1L) {
            tagId = insertTagWithDb(db, tagName)
        }

        // 添加笔记标签关联
        val values = ContentValues().apply {
            put(COLUMN_NOTE_ID, noteId)
            put(COLUMN_TAG_ID, tagId)
        }

        db.insertWithOnConflict(TABLE_NOTE_TAGS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }
    
    // 为笔记添加标签
    fun addTagToNote(noteId: Long, tagName: String) {
        writableDatabase.use { db ->
            addTagToNoteWithDb(db, noteId, tagName)
        }
    }

    // 从笔记中移除标签
    fun removeTagFromNote(noteId: Long, tagName: String) {
        val tagId = getTagId(tagName)
        if (tagId != -1L) {
            writableDatabase.use { db ->
                db.delete(
                    TABLE_NOTE_TAGS,
                    "$COLUMN_NOTE_ID = ? AND $COLUMN_TAG_ID = ?",
                    arrayOf(noteId.toString(), tagId.toString())
                )
            }
        }
    }

    // 获取标签ID（使用现有数据库连接）
    private fun getTagIdWithDb(db: SQLiteDatabase, tagName: String): Long {
        db.query(
            TABLE_TAGS,
            arrayOf(COLUMN_TAG_ID),
            "$COLUMN_TAG_NAME = ?",
            arrayOf(tagName),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TAG_ID))
            }
        }
        return -1L
    }
    
    // 获取标签ID
    private fun getTagId(tagName: String): Long {
        readableDatabase.use { db ->
            return getTagIdWithDb(db, tagName)
        }
    }

    // 删除标签
    fun deleteTag(tagName: String): Int {
        return writableDatabase.use { db ->
            db.delete(
                TABLE_TAGS,
                "$COLUMN_TAG_NAME = ?",
                arrayOf(tagName)
            )
        }
    }

    // 根据标签搜索笔记
    fun getNotesByTag(tagName: String): List<Note> {
        val notes = mutableListOf<Note>()

        val query = "SELECT n.* FROM $TABLE_NOTES n " +
                "INNER JOIN $TABLE_NOTE_TAGS nt ON n.$COLUMN_ID = nt.$COLUMN_NOTE_ID " +
                "INNER JOIN $TABLE_TAGS t ON nt.$COLUMN_TAG_ID = t.$COLUMN_TAG_ID " +
                "WHERE t.$COLUMN_TAG_NAME = ? " +
                "ORDER BY n.$COLUMN_CREATED_TIME DESC"

        readableDatabase.use { db ->
            db.rawQuery(query, arrayOf(tagName)).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                    val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
                    val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    val createdTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME))
                    val modifiedTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MODIFIED_TIME))
                    val customPropertiesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUSTOM_PROPERTIES)) ?: "{}"
                    val customProperties = parseCustomProperties(customPropertiesJson)
                    val tags = getTagsForNote(id)

                    notes.add(Note(id, content, createdTime, modifiedTime, tags, customProperties))
                }
            }
        }

        return notes
    }

    // 解析自定义属性JSON字符串
    private fun parseCustomProperties(json: String): Map<String, String> {
        return try {
            val jsonObject = JSONObject(json)
            val map = mutableMapOf<String, String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getString(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    // 回收站相关方法
    
    // 获取已删除的笔记
    fun getDeletedNotes(): List<Note> {
        val notes = mutableListOf<Note>()

        val query = "SELECT * FROM $TABLE_NOTES WHERE $COLUMN_DELETED = 1 ORDER BY $COLUMN_CREATED_TIME DESC"

        readableDatabase.use { db ->
            db.rawQuery(query, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                    val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
                    val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    val createdTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME))
                    val modifiedTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MODIFIED_TIME))
                    val customPropertiesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUSTOM_PROPERTIES)) ?: "{}"
                    val customProperties = parseCustomProperties(customPropertiesJson)
                    val tags = getTagsForNote(id)

                    notes.add(Note(id, content, createdTime, modifiedTime, tags, customProperties))
                }
            }
        }

        return notes
    }
    
    // 恢复笔记
    fun restoreNote(noteId: Long): Int {
        val values = ContentValues().apply {
            put(COLUMN_DELETED, 0)
        }
        return writableDatabase.use { db ->
            db.update(
                TABLE_NOTES,
                values,
                "$COLUMN_ID = ?",
                arrayOf(noteId.toString())
            )
        }
    }
    
    // 批量恢复笔记
    fun restoreNotes(noteIds: List<Long>): Int {
        if (noteIds.isEmpty()) return 0

        val values = ContentValues().apply {
            put(COLUMN_DELETED, 0)
        }
        val placeholders = noteIds.joinToString(", ") { "?" }
        val whereClause = "$COLUMN_ID IN ($placeholders)"
        val whereArgs = noteIds.map { it.toString() }.toTypedArray()

        return writableDatabase.use { db ->
            db.update(TABLE_NOTES, values, whereClause, whereArgs)
        }
    }
    
    // 永久删除笔记
    fun permanentDeleteNote(noteId: Long): Int {
        return writableDatabase.use { db ->
            db.delete(
                TABLE_NOTES,
                "$COLUMN_ID = ?",
                arrayOf(noteId.toString())
            )
        }
    }
    
    // 批量永久删除笔记
    fun permanentDeleteNotes(noteIds: List<Long>): Int {
        if (noteIds.isEmpty()) return 0

        val placeholders = noteIds.joinToString(", ") { "?" }
        val whereClause = "$COLUMN_ID IN ($placeholders)"
        val whereArgs = noteIds.map { it.toString() }.toTypedArray()

        return writableDatabase.use { db ->
            db.delete(TABLE_NOTES, whereClause, whereArgs)
        }
    }
    
    // 自动清理30天以上的已删除笔记
    fun autoCleanOldDeletedNotes(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        
        return writableDatabase.use { db ->
            db.delete(
                TABLE_NOTES,
                "$COLUMN_DELETED = 1 AND $COLUMN_MODIFIED_TIME < ?",
                arrayOf(thirtyDaysAgo.toString())
            )
        }
    }
}