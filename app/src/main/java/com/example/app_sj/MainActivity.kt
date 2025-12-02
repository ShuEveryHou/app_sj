package com.example.app_sj

import android.os.Bundle
import android.content.Intent
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
        /*enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/
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