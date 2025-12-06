package com.example.app_sj

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private lateinit var btnPublicAlbum: MaterialButton
    private lateinit var btnPrivateAlbum: MaterialButton

    private lateinit var btnCameraTest: MaterialButton  // 测试摄像头的按钮

    private val DEFAULT_PASSWORD="123456"

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
    }

    // 可以添加一个菜单或按钮来清理用户图片
    fun clearUserImages() {
        AlertDialog.Builder(this)
            .setTitle("清理用户图片")
            .setMessage("确定要删除所有用户创建的图片吗？")
            .setPositiveButton("确定") { _, _ ->
                ImageManager.clearAllUserImages(this)
                Toast.makeText(this, "用户图片已清理", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
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