package com.example.app_sj

import android.os.Bundle
import android.content.Intent
import android.text.InputType
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    private lateinit var btnPublicAlbum: MaterialButton
    private lateinit var btnPrivateAlbum: MaterialButton

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
    }

    private fun navigateToPublicAlbum(){
        //显示跳转intent
        val intent=Intent(this, PublicAlbumActivity::class.java)

        startActivity(intent)
    }

    private fun navigatetoPrivateAlbum(){

    }

}