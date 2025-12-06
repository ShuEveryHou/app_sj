// Photo.kt
package com.example.app_sj

data class Photo(
    val id: Int,
    val resourceId: Int = 0,
    val filePath: String = "",
    val title: String = "",
    val isFromCamera: Boolean = false,
    val isUserCreated: Boolean = false // 新增：标记是否为用户创建的图片
) {
    fun getDisplayPath(): Any {
        return if (filePath.isNotEmpty()) {
            filePath
        } else {
            resourceId
        }
    }

    fun getDisplayName(): String {
        return if (title.isNotEmpty()) title else "图片_$id"
    }
}