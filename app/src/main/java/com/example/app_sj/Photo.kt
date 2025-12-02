package com.example.app_sj

data class Photo (
    val id: Int,
    val resourceId: Int = 0,
    val filePath: String = "",
    val title: String = "",
    val isFromCamera: Boolean = false //标记是否来自相机
){
    //显示获取图片路径
    fun getDisplayPath(): Any{
        return if(isFromCamera && filePath.isNotBlank()){
            filePath
        }else{
            resourceId
        }
    }
}