package com.han.takeit

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import com.google.android.material.slider.Slider
import com.han.takeit.db.DatabaseBackupManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    
    private val saveBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { saveBackupToUri(it) }
    }
    
    companion object {
        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_CHINESE = "zh"
        const val LANGUAGE_ENGLISH = "en"
        const val PREF_LANGUAGE = "language_preference"
        const val PREF_MAX_LINES = "max_lines_preference"
        const val DEFAULT_MAX_LINES = 6
        
        fun updateLanguage(context: Context, language: String) {
            val locale = when (language) {
                LANGUAGE_CHINESE -> Locale("zh", "CN")
                LANGUAGE_ENGLISH -> Locale("en", "US")
                LANGUAGE_SYSTEM -> {
                    // 获取真正的系统首选语言
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        LocaleList.getDefault().get(0)
                    } else {
                        Locale.getDefault()
                    }
                }
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        LocaleList.getDefault().get(0)
                    } else {
                        Locale.getDefault()
                    }
                }
            }
            
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
        
        fun getSavedLanguage(context: Context): String {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            return prefs.getString(PREF_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
        }
        
        fun saveLanguage(context: Context, language: String) {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_LANGUAGE, language).apply()
        }
        
        fun getMaxLines(context: Context): Int {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            return prefs.getInt(PREF_MAX_LINES, DEFAULT_MAX_LINES)
        }
        
        fun saveMaxLines(context: Context, maxLines: Int) {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putInt(PREF_MAX_LINES, maxLines).apply()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示，让内容延伸到状态栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 设置状态栏为完全透明
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        setContentView(R.layout.activity_settings)
        
        setupToolbar()
        setupLanguageSettings()
        setupCardPreviewSettings()
        setupBackupManagement()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupLanguageSettings() {
        val radioGroup = findViewById<RadioGroup>(R.id.radio_group_language)
        val currentLanguage = getSavedLanguage(this)
        
        // 设置当前选中的语言
        when (currentLanguage) {
            LANGUAGE_SYSTEM -> radioGroup.check(R.id.radio_system)
            LANGUAGE_CHINESE -> radioGroup.check(R.id.radio_chinese)
            LANGUAGE_ENGLISH -> radioGroup.check(R.id.radio_english)
        }
        
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedLanguage = when (checkedId) {
                R.id.radio_chinese -> LANGUAGE_CHINESE
                R.id.radio_english -> LANGUAGE_ENGLISH
                else -> LANGUAGE_SYSTEM
            }
            
            if (selectedLanguage != currentLanguage) {
                saveLanguage(this, selectedLanguage)
                
                // 重启应用以应用新语言
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
    
    private fun setupCardPreviewSettings() {
        val slider = findViewById<Slider>(R.id.slider_max_lines)
        val valueText = findViewById<TextView>(R.id.text_max_lines_value)
        
        // 设置当前值
        val currentMaxLines = getMaxLines(this)
        slider.value = currentMaxLines.toFloat()
        valueText.text = currentMaxLines.toString()
        
        // 设置滑块变化监听器
        slider.addOnChangeListener { _, value, _ ->
            val maxLines = value.toInt()
            valueText.text = maxLines.toString()
            saveMaxLines(this, maxLines)
        }
    }
    
    private fun setupBackupManagement() {
        val btnBackupManagement = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_backup_management)
        btnBackupManagement.setOnClickListener {
            val intent = Intent(this, BackupManagementActivity::class.java)
            startActivity(intent)
        }
        
        val btnSaveCurrentDatabase = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save_current_database)
        btnSaveCurrentDatabase.setOnClickListener {
            saveCurrentDatabaseToLocal()
        }
    }
    
    private fun saveCurrentDatabaseToLocal() {
        try {
            val backupManager = DatabaseBackupManager(this)
            
            // 创建备份文件
            if (backupManager.createBackup()) {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "takeit_database_$timestamp.db"
                saveBackupLauncher.launch(fileName)
            } else {
                Toast.makeText(this, "创建备份失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveBackupToUri(uri: Uri) {
        try {
            val backupManager = DatabaseBackupManager(this)
            val backups = backupManager.getAllBackups()
            if (backups.isNotEmpty()) {
                val latestBackup = backups.first()
                val sourceFile = File(latestBackup.filePath)
                
                if (!sourceFile.exists()) {
                    Toast.makeText(this, "备份文件不存在", Toast.LENGTH_SHORT).show()
                    return
                }
                
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                Toast.makeText(this, "数据库已保存成功", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "没有可用的备份文件", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}