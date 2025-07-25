package com.han.takeit

import java.text.SimpleDateFormat
import java.util.*

data class Note(
    val id: Long,
    val content: String,
    val timestamp: Long,
    val tags: List<String> = emptyList()
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}