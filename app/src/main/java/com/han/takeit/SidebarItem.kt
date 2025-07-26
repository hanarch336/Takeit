package com.han.takeit

sealed class SidebarItem {
    data class DateGroup(
        val date: String,
        val notes: List<Note>,
        var isExpanded: Boolean = false
    ) : SidebarItem()
    
    data class NoteItem(
        val note: Note,
        val parentDate: String
    ) : SidebarItem()
}

data class SidebarDateGroup(
    val date: String,
    val formattedDate: String,
    val notes: List<Note>,
    var isExpanded: Boolean = false
)