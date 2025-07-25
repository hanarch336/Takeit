package com.han.takeit

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.slider.Slider
import java.util.*

class SettingsActivity : AppCompatActivity() {
    
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
        setContentView(R.layout.activity_settings)
        
        setupToolbar()
        setupLanguageSettings()
        setupCardPreviewSettings()
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
}