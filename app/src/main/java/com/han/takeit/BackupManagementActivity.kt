package com.han.takeit

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.han.takeit.databinding.ActivityBackupManagementBinding
import com.han.takeit.db.DatabaseBackupManager
import com.han.takeit.db.BackupInfo
import com.han.takeit.db.ConflictStrategy
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class BackupManagementActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBackupManagementBinding
    private lateinit var backupManager: DatabaseBackupManager
    private lateinit var backupAdapter: BackupAdapter
    private val backupList = mutableListOf<BackupInfo>()
    private var currentBackupToSave: BackupInfo? = null
    
    private val saveBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { saveBackupToUri(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示，让内容延伸到状态栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 设置状态栏为完全透明
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        binding = ActivityBackupManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        
        backupManager = DatabaseBackupManager(this)
        loadBackups()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.backup_management)
        }
    }
    
    private fun setupRecyclerView() {
        backupAdapter = BackupAdapter(
            backups = backupList,
            onDeleteClick = { backup ->
                showDeleteConfirmDialog(backup)
            },
            onRestoreClick = { backup ->
                showRestoreConfirmDialog(backup)
            },
            onMergeClick = { backup ->
                showMergeConfirmDialog(backup)
            },
            onSaveClick = { backup ->
                saveBackupToLocal(backup)
            }
        )
        
        binding.recyclerViewBackups.apply {
            layoutManager = LinearLayoutManager(this@BackupManagementActivity)
            adapter = backupAdapter
        }
    }
    
    private fun setupButtons() {
        binding.btnCreateBackup.setOnClickListener {
            createBackup()
        }
        
        binding.btnClearAllBackups.setOnClickListener {
            showClearAllConfirmDialog()
        }
    }
    
    private fun loadBackups() {
        backupList.clear()
        backupList.addAll(backupManager.getAllBackups())
        backupAdapter.notifyDataSetChanged()
        
        updateUI()
    }
    
    private fun updateUI() {
        val totalSize = backupManager.getBackupDirectorySize()
        val sizeText = formatFileSize(totalSize)
        binding.tvBackupInfo.text = getString(R.string.backup_info, backupList.size, sizeText)
        
        binding.btnClearAllBackups.isEnabled = backupList.isNotEmpty()
    }
    
    private fun createBackup() {
        binding.btnCreateBackup.isEnabled = false
        
        if (backupManager.createBackup()) {
            Toast.makeText(this, R.string.backup_created_success, Toast.LENGTH_SHORT).show()
            loadBackups()
        } else {
            Toast.makeText(this, R.string.backup_created_failed, Toast.LENGTH_SHORT).show()
        }
        
        binding.btnCreateBackup.isEnabled = true
    }
    
    private fun showDeleteConfirmDialog(backup: BackupInfo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_backup_title)
            .setMessage(getString(R.string.delete_backup_message, backup.fileName))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deleteBackup(backup)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun deleteBackup(backup: BackupInfo) {
        if (backupManager.deleteBackup(backup)) {
            Toast.makeText(this, R.string.backup_deleted_success, Toast.LENGTH_SHORT).show()
            loadBackups()
        } else {
            Toast.makeText(this, R.string.backup_deleted_failed, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showRestoreConfirmDialog(backup: BackupInfo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.restore_backup_title)
            .setMessage(getString(R.string.restore_backup_message, backup.fileName))
            .setPositiveButton(R.string.action_restore) { _, _ ->
                restoreBackup(backup)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun restoreBackup(backup: BackupInfo) {
        if (backupManager.restoreFromBackup(backup)) {
            Toast.makeText(this, R.string.backup_restored_success, Toast.LENGTH_SHORT).show()
            // 重启应用以加载恢复的数据
            AlertDialog.Builder(this)
                .setTitle(R.string.restart_required_title)
                .setMessage(R.string.restart_required_message)
                .setPositiveButton(R.string.action_restart) { _, _ ->
                    restartApp()
                }
                .setCancelable(false)
                .show()
        } else {
            Toast.makeText(this, R.string.backup_restored_failed, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showMergeConfirmDialog(backup: BackupInfo) {
        val conflictOptions = arrayOf(
            getString(R.string.conflict_strategy_keep_newer),
            getString(R.string.conflict_strategy_keep_backup),
            getString(R.string.conflict_strategy_keep_current)
        )
        var selectedStrategy = 0 // 默认选择保持较新数据
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.merge_backup_title))
            .setMessage(getString(R.string.merge_backup_message, backup.fileName))
            .setSingleChoiceItems(conflictOptions, selectedStrategy) { _, which ->
                selectedStrategy = which
            }
            .setPositiveButton(getString(R.string.action_merge)) { _, _ ->
                val strategy = when (selectedStrategy) {
                    0 -> ConflictStrategy.KEEP_NEWER
                    1 -> ConflictStrategy.KEEP_BACKUP
                    2 -> ConflictStrategy.KEEP_CURRENT
                    else -> ConflictStrategy.KEEP_NEWER
                }
                mergeBackup(backup, strategy)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }
    
    private fun mergeBackup(backup: BackupInfo, strategy: ConflictStrategy) {
        try {
            val result = backupManager.mergeDatabase(backup, strategy)
            if (result.success) {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                // 刷新界面数据
                loadBackups()
            } else {
                Toast.makeText(this, getString(R.string.merge_failed, result.message), Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.merge_error, e.message), Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showClearAllConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_all_backups_title)
            .setMessage(R.string.clear_all_backups_message)
            .setPositiveButton(R.string.action_clear_all) { _, _ ->
                clearAllBackups()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun clearAllBackups() {
        if (backupManager.clearAllBackups()) {
            Toast.makeText(this, R.string.backups_cleared_success, Toast.LENGTH_SHORT).show()
            loadBackups()
        } else {
            Toast.makeText(this, R.string.backups_cleared_failed, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }
    
    private fun saveBackupToLocal(backup: BackupInfo) {
        currentBackupToSave = backup
        saveBackupLauncher.launch(backup.fileName)
    }
    
    private fun saveBackupToUri(uri: Uri) {
        val backup = currentBackupToSave ?: return
        
        try {
            val sourceFile = File(backup.filePath)
            if (!sourceFile.exists()) {
                Toast.makeText(this, "备份文件不存在", Toast.LENGTH_SHORT).show()
                return
            }
            
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            Toast.makeText(this, "备份文件已保存成功", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            currentBackupToSave = null
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
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