package com.han.takeit

import java.text.SimpleDateFormat
import java.util.*

data class Note(
    val id: Long,
    val content: String,
    val createdTime: Long,
    val modifiedTime: Long,
    val tags: List<String> = emptyList(),
    val customProperties: Map<String, String> = emptyMap()
) {
    // 为了兼容性，保留timestamp属性，返回创建时间
    val timestamp: Long get() = createdTime
    
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        return sdf.format(Date(createdTime))
    }
    
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(createdTime))
    }
    
    fun getFormattedModifiedDate(): String {
        val sdf = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        return sdf.format(Date(modifiedTime))
    }
    
    fun getFormattedModifiedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(modifiedTime))
    }
}