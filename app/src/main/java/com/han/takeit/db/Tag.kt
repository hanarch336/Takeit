package com.han.takeit.db

data class Tag(
    val id: Long = 0,
    val name: String,
    val color: String = "#6200EE"
) {
    companion object {
        // 预定义的颜色选项
        val PRESET_COLORS = listOf(
            "#6200EE", // 紫色
            "#03DAC6", // 青色
            "#FF6200", // 橙色
            "#FF5722", // 深橙色
            "#4CAF50", // 绿色
            "#2196F3", // 蓝色
            "#9C27B0", // 紫红色
            "#F44336", // 红色
            "#795548", // 棕色
            "#607D8B"  // 蓝灰色
        )
        
        // 生成随机颜色
        fun getRandomColor(): String {
            return PRESET_COLORS.random()
        }
        
        // 根据背景色计算文本颜色（黑色或白色）
        fun getTextColor(backgroundColor: String): String {
            val color = backgroundColor.removePrefix("#")
            val r = color.substring(0, 2).toInt(16)
            val g = color.substring(2, 4).toInt(16)
            val b = color.substring(4, 6).toInt(16)
            
            // 计算亮度 (0.299*R + 0.587*G + 0.114*B)
            val brightness = (0.299 * r + 0.587 * g + 0.114 * b)
            
            // 如果亮度大于128，使用黑色文字，否则使用白色文字
            return if (brightness > 128) "#000000" else "#FFFFFF"
        }
    }
}