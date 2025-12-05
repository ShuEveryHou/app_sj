// Photo.kt
package com.example.app_sj

data class Photo(
    val id: Int,
    val resourceId: Int = 0,
    val filePath: String = "",
    val title: String = "",
    val isFromCamera: Boolean = false
) {
    fun getDisplayPath(): Any {
        // 如果有文件路径，优先使用文件路径
        // 否则使用资源ID
        return if (filePath.isNotEmpty()) {
            filePath
        } else {
            resourceId  // 直接返回资源ID，Glide可以处理
        }
    }
}