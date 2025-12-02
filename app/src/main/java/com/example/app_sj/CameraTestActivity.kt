package com.example.app_sj

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CameraTestActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 200
        private const val REQUEST_CAMERA_CAPTURE = 201
    }

    private lateinit var tvStatus: TextView
    private lateinit var btnTest: Button
    private lateinit var btnSimple: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_test)

        tvStatus = findViewById(R.id.tvStatus)
        btnTest = findViewById(R.id.btnTest)
        btnSimple = findViewById(R.id.btnSimple)

        setupUI()
    }

    private fun setupUI() {
        // 测试按钮 - 完整流程
        btnTest.setOnClickListener {
            testCameraFull()
        }

        // 简单按钮 - 最简测试
        btnSimple.setOnClickListener {
            testCameraSimple()
        }

        updateStatus()
    }

    private fun updateStatus() {
        val sb = StringBuilder()

        // 1. 检查硬件
        val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        sb.append("1. 设备有相机: ${if (hasCamera) "是" else "否"}\n")

        // 2. 检查权限
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        sb.append("2. 相机权限: ${if (hasPermission) "已授予" else "未授予"}\n")

        // 3. 检查是否有应用能处理相机Intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val activities = packageManager.queryIntentActivities(cameraIntent, 0)
        sb.append("3. 可用的相机应用: ${activities.size} 个\n")

        // 列出所有应用
        activities.forEachIndexed { index, resolveInfo ->
            sb.append("   ${index + 1}. ${resolveInfo.activityInfo.packageName}\n")
        }

        tvStatus.text = sb.toString()
        Log.d("CameraTest", sb.toString())
    }

    /**
     * 完整测试流程
     */
    private fun testCameraFull() {
        Log.d("CameraTest", "开始完整测试")

        // 1. 检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            return
        }

        // 2. 检查是否有相机应用
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "没有可用的相机应用", Toast.LENGTH_LONG).show()
            return
        }

        // 3. 启动相机
        try {
            startActivityForResult(cameraIntent, REQUEST_CAMERA_CAPTURE)
            Toast.makeText(this, "正在启动相机...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("CameraTest", "启动相机失败: ${e.message}")
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 最简单测试 - 直接启动系统相机
     */
    private fun testCameraSimple() {
        Log.d("CameraTest", "开始简单测试")

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // 尝试启动
        try {
            startActivity(intent)
            Toast.makeText(this, "已尝试启动系统相机", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("CameraTest", "简单测试失败: ${e.message}")

            // 提供解决方案
            AlertDialog.Builder(this)
                .setTitle("相机启动失败")
                .setMessage("错误: ${e.message}\n\n可能原因:\n" +
                        "1. 模拟器未配置摄像头\n" +
                        "2. 系统相机应用缺失\n" +
                        "3. 权限问题")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                testCameraFull()
            } else {
                Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show()
            }
        }

        updateStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CAMERA_CAPTURE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "拍照成功!", Toast.LENGTH_SHORT).show()
                Log.d("CameraTest", "相机返回成功，数据: ${data?.extras?.keySet()}")
            } else {
                Toast.makeText(this, "拍照取消", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}