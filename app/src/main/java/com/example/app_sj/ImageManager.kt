// ImageManager.kt
package com.example.app_sj

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * 图片管理器 - 管理用户创建的图片
 */
object ImageManager {

    private const val USER_IMAGES_DIR = "user_images"
    private const val INFO_FILE = "images_info.txt"

    /**
     * 保存用户创建的图片
     */
    fun saveUserImage(context: Context, bitmap: Bitmap, title: String = ""): String? {
        return try {
            // 创建时间戳文件名
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "user_${timeStamp}.jpg"


            // 保存到应用私有目录
            val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), USER_IMAGES_DIR)
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val imageFile = File(storageDir, fileName)

            // 保存图片
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
            }

            // 保存图片信息
            saveImageInfo(context, fileName, title)

            notifyImageUpdated(context)

            imageFile.absolutePath



        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ImageManager", "保存图片失败: ${e.message}")
            null
        }
    }

        /**
     * 保存图片信息
     */
    private fun saveImageInfo(context: Context, fileName: String, title: String) {
        try {
            val infoFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "$USER_IMAGES_DIR/$INFO_FILE")

            val infoLine = "$fileName|${if (title.isNotEmpty()) title else "用户图片"}|${System.currentTimeMillis()}\n"

            if (infoFile.exists()) {
                // 追加信息
                infoFile.appendText(infoLine)
            } else {
                // 创建新文件
                infoFile.writeText(infoLine)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notifyImageUpdated(context: Context) {
        try {
            // 发送自定义广播通知图片更新
            val updateIntent = Intent("IMAGE_UPDATED")

            // 使用兼容性方式发送广播
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Android 13+
                context.sendBroadcast(updateIntent, android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.sendBroadcast(updateIntent)
            }

            Log.d("ImageManager", "发送图片更新广播")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ImageManager", "发送广播失败: ${e.message}")
        }
    }

    /**
     * 获取所有用户创建的图片
     */
    fun getUserImages(context: Context): List<Photo> {
        val userPhotos = mutableListOf<Photo>()

        try {
            val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), USER_IMAGES_DIR)
            if (!storageDir.exists()) {
                return emptyList()
            }

            // 读取图片信息文件
            val infoFile = File(storageDir, INFO_FILE)
            if (infoFile.exists()) {
                val lines = infoFile.readLines()

                for ((index, line) in lines.withIndex()) {
                    val parts = line.split("|")
                    if (parts.size >= 2) {
                        val fileName = parts[0]
                        val title = parts[1]
                        val imageFile = File(storageDir, fileName)

                        if (imageFile.exists()) {
                            // 使用负数的ID，避免与系统图片冲突
                            val photoId = -(index + 1)
                            userPhotos.add(Photo(
                                id = photoId,
                                resourceId = 0,
                                filePath = imageFile.absolutePath,
                                title = title,
                                isFromCamera = false,
                                isUserCreated = true
                            ))
                        }
                    }
                }
            } else {
                // 如果没有信息文件，扫描目录中的所有图片文件
                val imageFiles = storageDir.listFiles { file ->
                    file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".png") || file.name.endsWith(".jpeg"))
                }

                imageFiles?.forEachIndexed { index, file ->
                    userPhotos.add(Photo(
                        id = -(index + 1),
                        resourceId = 0,
                        filePath = file.absolutePath,
                        title = "用户图片_${index + 1}",
                        isFromCamera = false,
                        isUserCreated = true
                    ))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ImageManager", "获取用户图片失败: ${e.message}")
        }

        return userPhotos
    }

    /**
     * 删除用户图片
     */
    fun deleteUserImage(context: Context, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清空所有用户图片
     */
    fun clearAllUserImages(context: Context) {
        try {
            val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), USER_IMAGES_DIR)
            if (storageDir.exists() && storageDir.isDirectory) {
                storageDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}