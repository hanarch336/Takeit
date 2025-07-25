package com.han.takeit

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.*

class TakeItApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        applyLanguageSettings()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyLanguageSettings()
    }
    
    private fun applyLanguageSettings() {
        val savedLanguage = SettingsActivity.getSavedLanguage(this)
        if (savedLanguage == SettingsActivity.LANGUAGE_SYSTEM) {
            // 跟随系统语言，获取真正的系统首选语言
            val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList.getDefault().get(0)
            } else {
                Locale.getDefault()
            }
            val config = Configuration(resources.configuration)
            config.setLocale(systemLocale)
            resources.updateConfiguration(config, resources.displayMetrics)
        } else {
            SettingsActivity.updateLanguage(this, savedLanguage)
        }
    }
    
    override fun attachBaseContext(base: Context?) {
        val savedLanguage = base?.let { SettingsActivity.getSavedLanguage(it) } ?: SettingsActivity.LANGUAGE_SYSTEM
        val context = if (base != null) {
            if (savedLanguage == SettingsActivity.LANGUAGE_SYSTEM) {
                // 跟随系统语言，使用原始context
                base
            } else {
                updateContextLanguage(base, savedLanguage)
            }
        } else {
            base
        }
        super.attachBaseContext(context)
    }
    
    private fun updateContextLanguage(context: Context, language: String): Context {
        val locale = when (language) {
            SettingsActivity.LANGUAGE_CHINESE -> Locale("zh", "CN")
            SettingsActivity.LANGUAGE_ENGLISH -> Locale("en", "US")
            SettingsActivity.LANGUAGE_SYSTEM -> {
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
        return context.createConfigurationContext(config)
    }
}