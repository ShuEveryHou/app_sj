package com.example.app_sj

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var btnPublicAlbum: MaterialButton
    private lateinit var btnPrivateAlbum: MaterialButton

    private lateinit var btnCameraTest: MaterialButton  // 测试摄像头的按钮
    private lateinit var btnTakePhoto: MaterialButton  // 拍照按钮

    // 用于存储当前照片路径
    private var currentPhotoPath: String? = null

    // 拍照请求的launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // 拍照成功，处理照片
            handleCapturedPhoto()
        } else {
            // 用户取消拍照
            Toast.makeText(this, "拍照取消", Toast.LENGTH_SHORT).show()
        }
    }
    // 权限请求的launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 权限授予，启动相机
            launchCamera()
        } else {
            // 权限被拒绝
            Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }


        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews();
        setupClickListeners();

    }
    private fun initializeViews(){
        //初始化按钮
        btnPublicAlbum=findViewById(R.id.btnPublicAlbum)
        btnPrivateAlbum=findViewById(R.id.btnPrivateAlbum)

        btnCameraTest = findViewById(R.id.btnCameraTest)  // 初始化测试摄像头按钮
        btnTakePhoto = findViewById(R.id.btnTakePhoto)  // 初始化拍照按钮
    }

    private fun setupClickListeners(){
        //公开相册
        btnPublicAlbum.setOnClickListener {
            navigateToPublicAlbum()
        }
        //私有相册
        btnPrivateAlbum.setOnClickListener {
            navigatetoPrivateAlbum()
        }

        // 摄像头测试按钮
        btnCameraTest.setOnClickListener {
            navigateToCameraTest()
        }

        // 拍照按钮
        btnTakePhoto.setOnClickListener {
            checkAndRequestCameraPermission()
        }
    }

    /**
     * 检查并请求相机权限
     */
    private fun checkAndRequestCameraPermission() {
        // 检查设备是否有相机硬件
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            showNoCameraDialog()
            return
        }

        // 检查权限
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 已有权限，启动相机
                launchCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // 需要解释为什么需要权限
                showPermissionRationaleDialog()
            }

            else -> {
                // 请求权限
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    /**
     * 显示无相机对话框
     */
    private fun showNoCameraDialog() {
        AlertDialog.Builder(this)
            .setTitle("无法拍照")
            .setMessage("您的设备没有摄像头或摄像头不可用。\n\n如果您在使用模拟器，请确保已配置虚拟摄像头。")
            .setPositiveButton("确定", null)
            .setNegativeButton("打开相机测试") { _, _ ->
                navigateToCameraTest()
            }
            .show()
    }
    /**
     * 显示权限解释对话框
     */
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要相机权限")
            .setMessage("此功能需要使用您的相机拍照。请授予相机权限以继续。")
            .setPositiveButton("确定") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    /**
     * 启动相机
     */
    private fun launchCamera() {
        try {
            // 创建图片文件
            val photoFile = createImageFile()

            // 创建URI（使用FileProvider安全共享文件）
            val photoUri: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )

            // 创建相机Intent
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                // 授予临时权限给相机应用
                val resInfoList = packageManager.queryIntentActivities(
                    this, PackageManager.MATCH_DEFAULT_ONLY
                )
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    grantUriPermission(
                        packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }

            // 启动相机
            takePictureLauncher.launch(takePictureIntent)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "启动相机失败: ${e.message}")
            Toast.makeText(this, "启动相机失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    /**
     * 创建图片文件
     */
    @Throws(Exception::class)
    private fun createImageFile(): File {
        // 创建时间戳文件名
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        // 获取存储目录
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storageDir?.mkdirs()  // 确保目录存在

        // 创建临时文件
        val imageFile = File.createTempFile(
            imageFileName,  /* 前缀 */
            ".jpg",         /* 后缀 */
            storageDir      /* 目录 */
        )

        // 保存文件路径
        currentPhotoPath = imageFile.absolutePath
        Log.d("MainActivity", "创建图片文件: $currentPhotoPath")

        return imageFile
    }

    /**
     * 处理拍摄的照片
     */
    private fun handleCapturedPhoto() {
        if (currentPhotoPath.isNullOrEmpty()) {
            Toast.makeText(this, "照片保存失败", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFile = File(currentPhotoPath!!)
        if (!photoFile.exists()) {
            Toast.makeText(this, "照片文件不存在", Toast.LENGTH_SHORT).show()
            return
        }

        // 显示确认对话框
        showPhotoConfirmationDialog(photoFile)
    }

    /**
     * 显示照片确认对话框
     */
    private fun showPhotoConfirmationDialog(photoFile: File) {
        AlertDialog.Builder(this)
            .setTitle("拍照成功")
            .setMessage("照片已保存，是否要编辑此照片？")
            .setPositiveButton("编辑") { _, _ ->
                // 保存到用户图片目录
                savePhotoToUserAlbum(photoFile)
            }
            .setNegativeButton("取消") { _, _ ->
                // 删除临时文件
                photoFile.delete()
                Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("仅保存") { _, _ ->
                // 仅保存不编辑
                savePhotoToUserAlbum(photoFile)
                Toast.makeText(this, "照片已保存", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    /**
     * 保存照片到用户相册
     */
    private fun savePhotoToUserAlbum(photoFile: File) {
        try {
            // 读取图片文件
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath, options)
            if (bitmap == null) {
                Toast.makeText(this, "无法读取照片", Toast.LENGTH_SHORT).show()
                return
            }

            // 使用ImageManager保存到用户图片目录
            val savedPath = ImageManager.saveUserImage(this, bitmap, "相机照片")

            if (savedPath != null) {
                // 发送广播通知图片更新
                notifyImageUpdated()

                // 导航到图片详情页
                navigateToImageDetail(savedPath)

                // 清理临时文件
                photoFile.delete()
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }

            bitmap.recycle()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 通知图片更新
     */
    private fun notifyImageUpdated() {
        val updateIntent = Intent("IMAGE_UPDATED")
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .sendBroadcast(updateIntent)
    }

    /**
     * 导航到图片详情页
     */
    private fun navigateToImageDetail(imagePath: String) {
        // 创建Photo对象
        val photo = Photo(
            id = -1,  // 临时ID，在详情页中会重新计算
            resourceId = 0,
            filePath = imagePath,
            title = "相机照片",
            isFromCamera = true,
            isUserCreated = true
        )

        // 跳转到详情页
        val intent = Intent(this, ImageDetailActivity::class.java).apply {
            putExtra("photo_id", photo.id)
            putExtra("photo_resource_id", photo.resourceId)
            putExtra("photo_file_path", photo.filePath)
            putExtra("photo_title", photo.title)
            putExtra("is_from_camera", photo.isFromCamera)
            putExtra("is_user_created", photo.isUserCreated)
        }

        startActivity(intent)
    }


    //摄像头测试函数
    private fun navigateToCameraTest() {
        val intent = Intent(this, CameraTestActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToPublicAlbum(){
        //显示跳转intent
        val intent=Intent(this, PublicAlbumActivity::class.java)

        startActivity(intent)
    }

    private fun navigatetoPrivateAlbum(){

    }

}