package com.han.takeit.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DatabaseBackupManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DatabaseBackupManager"
        private const val BACKUP_DIR = "database_backups"
        private const val BACKUP_PREFIX = "notes_backup_"
        private const val BACKUP_EXTENSION = ".db"
    }
    
    private val backupDirectory: File by lazy {
        File(context.filesDir, BACKUP_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * 创建数据库备份
     */
    fun createBackup(isAutoBackup: Boolean = false): Boolean {
        return try {
            val currentDbPath = context.getDatabasePath(NoteDatabase.DATABASE_NAME)
            if (!currentDbPath.exists()) {
                Log.e(TAG, "Database file does not exist: ${currentDbPath.absolutePath}")
                return false
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = if (isAutoBackup) {
                "${BACKUP_PREFIX}autobackup_$timestamp$BACKUP_EXTENSION"
            } else {
                "$BACKUP_PREFIX$timestamp$BACKUP_EXTENSION"
            }
            val backupFile = File(backupDirectory, backupFileName)
            
            // 复制数据库文件
            FileInputStream(currentDbPath).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.i(TAG, "Database backup created: ${backupFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create database backup", e)
            false
        }
    }
    
    /**
     * 获取所有备份文件
     */
    fun getAllBackups(): List<BackupInfo> {
        return try {
            backupDirectory.listFiles { file ->
                file.isFile && file.name.startsWith(BACKUP_PREFIX) && file.name.endsWith(BACKUP_EXTENSION)
            }?.map { file ->
                BackupInfo(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    createdTime = file.lastModified()
                )
            }?.sortedByDescending { it.createdTime } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get backup list", e)
            emptyList()
        }
    }
    
    /**
     * 删除备份文件
     */
    fun deleteBackup(backupInfo: BackupInfo): Boolean {
        return try {
            val file = File(backupInfo.filePath)
            val result = file.delete()
            if (result) {
                Log.i(TAG, "Backup deleted: ${backupInfo.fileName}")
            } else {
                Log.e(TAG, "Failed to delete backup: ${backupInfo.fileName}")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete backup: ${backupInfo.fileName}", e)
            false
        }
    }
    
    /**
     * 恢复数据库从备份
     */
    fun restoreFromBackup(backupInfo: BackupInfo): Boolean {
        return try {
            val backupFile = File(backupInfo.filePath)
            if (!backupFile.exists()) {
                Log.e(TAG, "Backup file does not exist: ${backupInfo.filePath}")
                return false
            }
            
            // 先创建当前数据库的备份
            createBackup(true)
            
            val currentDbPath = context.getDatabasePath(NoteDatabase.DATABASE_NAME)
            
            // 关闭当前数据库连接
            // 注意：这里需要确保所有数据库连接都已关闭
            
            // 复制备份文件到当前数据库位置
            FileInputStream(backupFile).use { input ->
                FileOutputStream(currentDbPath).use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.i(TAG, "Database restored from backup: ${backupInfo.fileName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore from backup: ${backupInfo.fileName}", e)
            false
        }
    }
    
    /**
     * 迁移旧版本数据库
     */
    fun migrateOldDatabase(): Boolean {
        return try {
            // 首先创建当前数据库的备份
            createBackup(true)
            
            val currentDbPath = context.getDatabasePath(NoteDatabase.DATABASE_NAME)
            val db = SQLiteDatabase.openDatabase(currentDbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            
            // 检查数据库版本并执行迁移
            val currentVersion = db.version
            Log.i(TAG, "Current database version: $currentVersion")
            
            if (currentVersion < NoteDatabase.DATABASE_VERSION) {
                // 执行数据库升级
                val noteDatabase = NoteDatabase(context)
                noteDatabase.onUpgrade(db, currentVersion, NoteDatabase.DATABASE_VERSION)
                db.version = NoteDatabase.DATABASE_VERSION
                Log.i(TAG, "Database migrated from version $currentVersion to ${NoteDatabase.DATABASE_VERSION}")
            }
            
            db.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate old database", e)
            false
        }
    }
    
    /**
     * 获取备份目录大小
     */
    fun getBackupDirectorySize(): Long {
        return try {
            backupDirectory.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate backup directory size", e)
            0L
        }
    }
    
    /**
     * 清理所有备份
     */
    fun clearAllBackups(): Boolean {
        return try {
            val backups = getAllBackups()
            var allDeleted = true
            backups.forEach { backup ->
                if (!deleteBackup(backup)) {
                    allDeleted = false
                }
            }
            allDeleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all backups", e)
            false
        }
    }
    
    /**
     * 智能合并数据库
     * 支持列名变更、新增列、删除列等情况
     */
    fun mergeDatabase(backupInfo: BackupInfo, conflictStrategy: ConflictStrategy = ConflictStrategy.KEEP_NEWER): MergeResult {
        return try {
            val backupFile = File(backupInfo.filePath)
            if (!backupFile.exists()) {
                Log.e(TAG, "Backup file does not exist: ${backupInfo.filePath}")
                return MergeResult(false, "备份文件不存在")
            }
            
            // 先创建当前数据库的备份
            createBackup(true)
            
            val currentDbPath = context.getDatabasePath(NoteDatabase.DATABASE_NAME)
            val backupDb = SQLiteDatabase.openDatabase(backupFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val currentDb = SQLiteDatabase.openDatabase(currentDbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            
            val mergeResult = performDatabaseMerge(backupDb, currentDb, conflictStrategy)
            
            backupDb.close()
            currentDb.close()
            
            Log.i(TAG, "Database merge completed: ${mergeResult.message}")
            mergeResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to merge database: ${backupInfo.fileName}", e)
            MergeResult(false, "合并失败: ${e.message}")
        }
    }
    
    /**
     * 执行数据库合并的核心逻辑
     */
    private fun performDatabaseMerge(backupDb: SQLiteDatabase, currentDb: SQLiteDatabase, conflictStrategy: ConflictStrategy): MergeResult {
        var mergedNotes = 0
        var mergedTags = 0
        var conflicts = 0
        
        try {
            // 获取备份数据库的表结构信息
            val backupSchema = getDatabaseSchema(backupDb)
            val currentSchema = getDatabaseSchema(currentDb)
            
            // 合并标签数据
            if (backupSchema.containsKey("tags") && currentSchema.containsKey("tags")) {
                val tagResult = mergeTags(backupDb, currentDb, conflictStrategy)
                mergedTags = tagResult.first
                conflicts += tagResult.second
            }
            
            // 合并笔记数据
            if (backupSchema.containsKey("notes") && currentSchema.containsKey("notes")) {
                val noteResult = mergeNotes(backupDb, currentDb, conflictStrategy)
                mergedNotes = noteResult.first
                conflicts += noteResult.second
            }
            
            // 合并笔记标签关联数据
            if (backupSchema.containsKey("note_tags") && currentSchema.containsKey("note_tags")) {
                mergeNoteTags(backupDb, currentDb)
            }
            
            return MergeResult(
                true, 
                "合并完成: 笔记 $mergedNotes 条, 标签 $mergedTags 个, 冲突 $conflicts 个",
                mergedNotes,
                mergedTags,
                conflicts
            )
        } catch (e: Exception) {
             Log.e(TAG, "Error during database merge", e)
             return MergeResult(false, "合并过程中出错: ${e.message}")
         }
     }
     
     /**
      * 获取数据库表结构信息
      */
     private fun getDatabaseSchema(db: SQLiteDatabase): Map<String, List<String>> {
         val schema = mutableMapOf<String, List<String>>()
         
         // 获取所有表名
         val tablesCursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
         while (tablesCursor.moveToNext()) {
             val tableName = tablesCursor.getString(0)
             if (!tableName.startsWith("sqlite_")) {
                 // 获取表的列信息
                 val columnsCursor = db.rawQuery("PRAGMA table_info($tableName)", null)
                 val columns = mutableListOf<String>()
                 while (columnsCursor.moveToNext()) {
                     columns.add(columnsCursor.getString(1)) // 列名在索引1
                 }
                 columnsCursor.close()
                 schema[tableName] = columns
             }
         }
         tablesCursor.close()
         
         return schema
     }
     
     /**
      * 合并标签数据
      */
     private fun mergeTags(backupDb: SQLiteDatabase, currentDb: SQLiteDatabase, conflictStrategy: ConflictStrategy): Pair<Int, Int> {
         var merged = 0
         var conflicts = 0
         
         val backupCursor = backupDb.rawQuery("SELECT * FROM tags", null)
         while (backupCursor.moveToNext()) {
             val tagName = backupCursor.getString(backupCursor.getColumnIndexOrThrow("tag_name"))
             val tagColor = backupCursor.getString(backupCursor.getColumnIndexOrThrow("tag_color"))
             
             // 检查当前数据库中是否已存在该标签
             val existsCursor = currentDb.rawQuery("SELECT tag_color FROM tags WHERE tag_name = ?", arrayOf(tagName))
             if (existsCursor.moveToFirst()) {
                 // 标签已存在，根据冲突策略处理
                 val existingColor = existsCursor.getString(0)
                 if (existingColor != tagColor) {
                     conflicts++
                     when (conflictStrategy) {
                         ConflictStrategy.KEEP_BACKUP -> {
                             currentDb.execSQL("UPDATE tags SET tag_color = ? WHERE tag_name = ?", arrayOf(tagColor, tagName))
                             merged++
                         }
                         ConflictStrategy.KEEP_CURRENT -> {
                             // 保持当前数据，不做更改
                         }
                         ConflictStrategy.KEEP_NEWER -> {
                             // 这里简单地保持备份数据，实际应用中可以比较时间戳
                             currentDb.execSQL("UPDATE tags SET tag_color = ? WHERE tag_name = ?", arrayOf(tagColor, tagName))
                             merged++
                         }
                     }
                 }
             } else {
                 // 标签不存在，直接插入
                 currentDb.execSQL("INSERT INTO tags (tag_name, tag_color) VALUES (?, ?)", arrayOf(tagName, tagColor))
                 merged++
             }
             existsCursor.close()
         }
         backupCursor.close()
         
         return Pair(merged, conflicts)
     }
     
     /**
      * 合并笔记数据
      */
     private fun mergeNotes(backupDb: SQLiteDatabase, currentDb: SQLiteDatabase, conflictStrategy: ConflictStrategy): Pair<Int, Int> {
         var merged = 0
         var conflicts = 0
         
         val backupCursor = backupDb.rawQuery("SELECT * FROM notes", null)
         while (backupCursor.moveToNext()) {
             val noteId = backupCursor.getLong(backupCursor.getColumnIndexOrThrow("id"))
             val content = backupCursor.getString(backupCursor.getColumnIndexOrThrow("content"))
             val timestamp = backupCursor.getLong(backupCursor.getColumnIndexOrThrow("timestamp"))
             
             // 处理可能不存在的列
             val createdTime = try {
                 backupCursor.getLong(backupCursor.getColumnIndexOrThrow("created_time"))
             } catch (e: Exception) {
                 timestamp // 如果没有created_time列，使用timestamp
             }
             
             val modifiedTime = try {
                 backupCursor.getLong(backupCursor.getColumnIndexOrThrow("modified_time"))
             } catch (e: Exception) {
                 timestamp // 如果没有modified_time列，使用timestamp
             }
             
             val customProperties = try {
                 backupCursor.getString(backupCursor.getColumnIndexOrThrow("custom_properties")) ?: "{}"
             } catch (e: Exception) {
                 "{}" // 如果没有custom_properties列，使用空JSON
             }
             
             val deleted = try {
                 backupCursor.getInt(backupCursor.getColumnIndexOrThrow("deleted"))
             } catch (e: Exception) {
                 0 // 如果没有deleted列，默认为0
             }
             
             // 检查当前数据库中是否已存在该笔记
             val existsCursor = currentDb.rawQuery("SELECT modified_time FROM notes WHERE id = ?", arrayOf(noteId.toString()))
             if (existsCursor.moveToFirst()) {
                 // 笔记已存在，根据冲突策略处理
                 val existingModifiedTime = existsCursor.getLong(0)
                 conflicts++
                 when (conflictStrategy) {
                     ConflictStrategy.KEEP_BACKUP -> {
                         updateNote(currentDb, noteId, content, timestamp, createdTime, modifiedTime, customProperties, deleted)
                         merged++
                     }
                     ConflictStrategy.KEEP_CURRENT -> {
                         // 保持当前数据，不做更改
                     }
                     ConflictStrategy.KEEP_NEWER -> {
                         if (modifiedTime > existingModifiedTime) {
                             updateNote(currentDb, noteId, content, timestamp, createdTime, modifiedTime, customProperties, deleted)
                             merged++
                         }
                     }
                 }
             } else {
                 // 笔记不存在，直接插入
                 insertNote(currentDb, noteId, content, timestamp, createdTime, modifiedTime, customProperties, deleted)
                 merged++
             }
             existsCursor.close()
         }
         backupCursor.close()
         
         return Pair(merged, conflicts)
      }
      
      /**
       * 合并笔记标签关联数据
       */
      private fun mergeNoteTags(backupDb: SQLiteDatabase, currentDb: SQLiteDatabase) {
          val backupCursor = backupDb.rawQuery("SELECT * FROM note_tags", null)
          while (backupCursor.moveToNext()) {
              val noteId = backupCursor.getLong(backupCursor.getColumnIndexOrThrow("note_id"))
              val tagId = backupCursor.getLong(backupCursor.getColumnIndexOrThrow("tag_id"))
              
              // 检查关联是否已存在
              val existsCursor = currentDb.rawQuery(
                  "SELECT 1 FROM note_tags WHERE note_id = ? AND tag_id = ?", 
                  arrayOf(noteId.toString(), tagId.toString())
              )
              if (!existsCursor.moveToFirst()) {
                  // 关联不存在，插入
                  currentDb.execSQL(
                      "INSERT OR IGNORE INTO note_tags (note_id, tag_id) VALUES (?, ?)", 
                      arrayOf(noteId.toString(), tagId.toString())
                  )
              }
              existsCursor.close()
          }
          backupCursor.close()
      }
      
      /**
       * 更新笔记
       */
      private fun updateNote(db: SQLiteDatabase, noteId: Long, content: String, timestamp: Long, 
                            createdTime: Long, modifiedTime: Long, customProperties: String, deleted: Int) {
          db.execSQL(
              "UPDATE notes SET content = ?, timestamp = ?, created_time = ?, modified_time = ?, custom_properties = ?, deleted = ? WHERE id = ?",
              arrayOf(content, timestamp.toString(), createdTime.toString(), modifiedTime.toString(), customProperties, deleted.toString(), noteId.toString())
          )
      }
      
      /**
       * 插入笔记
       */
      private fun insertNote(db: SQLiteDatabase, noteId: Long, content: String, timestamp: Long, 
                            createdTime: Long, modifiedTime: Long, customProperties: String, deleted: Int) {
          db.execSQL(
              "INSERT INTO notes (id, content, timestamp, created_time, modified_time, custom_properties, deleted) VALUES (?, ?, ?, ?, ?, ?, ?)",
              arrayOf(noteId.toString(), content, timestamp.toString(), createdTime.toString(), modifiedTime.toString(), customProperties, deleted.toString())
          )
      }
  }
 
 /**
  * 冲突解决策略
  */
 enum class ConflictStrategy {
     KEEP_CURRENT,    // 保持当前数据
     KEEP_BACKUP,     // 使用备份数据
     KEEP_NEWER       // 保持较新的数据
 }
 
 /**
  * 合并结果
  */
 data class MergeResult(
     val success: Boolean,
     val message: String,
     val mergedNotes: Int = 0,
     val mergedTags: Int = 0,
     val conflicts: Int = 0
 )
 
 /**
 * 备份信息数据类
 */
data class BackupInfo(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val createdTime: Long
) {
    fun getFormattedSize(): String {
        val kb = fileSize / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$fileSize B"
        }
    }
    
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(createdTime))
    }
}