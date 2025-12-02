package com.example.app_sj

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object CameraUtils {
    // 请求码
    const val REQUEST_CAMERA_PERMISSION = 100
    const val REQUEST_CAMERA_CAPTURE = 101
    const val REQUEST_STORAGE_PERMISSION = 102

    // 当前照片路径
    var currentPhotoPath: String = ""

    //检查相机权限
    fun checkCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    //检查存储权限
    fun checkStoragePermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用 READ_MEDIA_IMAGES
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12及以下
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    //请求获取相机权限
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    //请求获取存储权限
    fun requestStoragePermission(activity: Activity) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        ActivityCompat.requestPermissions(
            activity,
            permissions,
            REQUEST_STORAGE_PERMISSION
        )
    }

    //创建图片
    @Throws(IOException::class)
    fun createImageFile(activity: Activity): File {
        // 创建时间戳文件名
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        // 获取存储目录
        val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // 创建文件
        val imageFile = File.createTempFile(
            imageFileName,  /* 前缀 */
            ".jpg",         /* 后缀 */
            storageDir      /* 目录 */
        )

        // 保存文件路径
        currentPhotoPath = imageFile.absolutePath
        return imageFile
    }

    //启动相机
    fun dispatchTakePictureIntent(activity: Activity): Boolean {
        return try {
            // 创建图片文件
            val photoFile = createImageFile(activity)

            // 创建URI
            val photoURI: Uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                photoFile
            )

            // 创建相机Intent
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

            // 启动相机
            activity.startActivityForResult(takePictureIntent, REQUEST_CAMERA_CAPTURE)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    //检查设备是否存在相机
    fun hasCamera(activity: Activity): Boolean {
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

}