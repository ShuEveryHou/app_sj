package com.example.app_sj

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File

class ImageDetailActivity: AppCompatActivity() {
    private lateinit var  ivDetail: ImageView
    private lateinit var layoutTopBar: LinearLayout
    private lateinit var layoutBottomBar: LinearLayout
    private lateinit var tvPhotoInfo: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnSend: Button
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button
    private lateinit var btnText: Button


    private var isUIVisible = false //显示操作栏状态,初始不可见


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detail)


        initViews()
        setupCLickListeners()
        loadImageData()
    }

    private fun initViews(){
        ivDetail = findViewById(R.id.ivDetail)
        layoutTopBar = findViewById(R.id.layoutTopBar)
        layoutBottomBar = findViewById(R.id.layoutBottomBar)
        tvPhotoInfo = findViewById(R.id.tvPhotoInfo)
        btnBack = findViewById(R.id.btnBack)
        btnSend = findViewById(R.id.btnSend)
        btnEdit = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
        btnText = findViewById(R.id.btnText)
    }

    private fun setupCLickListeners(){
         //返回按钮
        btnBack.setOnClickListener {
            finish()
        }

        //切换图片细节UI
        ivDetail.setOnClickListener {
            toggleUI()
        }

        //发送按钮
        btnSend.setOnClickListener {

        }
        //编辑按钮
        btnEdit.setOnClickListener {

        }
        //删除按钮
        btnDelete.setOnClickListener {

        }
        //文字按钮
        btnText.setOnClickListener {

        }
    }

    private fun loadImageData(){
        val photoId = intent.getIntExtra("photo_id",0)
        val resourceId = intent.getIntExtra("photo_resource_id",0)
        val photoTitle = intent.getStringExtra("photo_title")?:"未命名图片"

        tvPhotoInfo.text = "图片ID: $photoId - $photoTitle"

        loadImage(resourceId)
    }

    private fun loadImage(resourceId: Int){
        try {
            if (resourceId != 0) {
                // 使用Glide加载资源图片
                Glide.with(this)
                    .load(resourceId)
                    .fitCenter()  // 保持比例，完整显示
                    .into(ivDetail)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleUI(){
        if(isUIVisible){
            //隐藏UI元素
            layoutTopBar.visibility = View.GONE
            layoutBottomBar.visibility = View.GONE
        }else{
            layoutTopBar.visibility = View.VISIBLE
            layoutBottomBar.visibility = View.VISIBLE
        }
        isUIVisible =!isUIVisible
    }

    private fun loadImage(photo: Photo){
        try {
            if (photo.isFromCamera && photo.filePath.isNotEmpty()) {
                // 加载相机拍摄的图片
                Glide.with(this)
                    .load(File(photo.filePath))
                    .fitCenter()
                    .into(ivDetail)
            } else if (photo.resourceId != 0) {
                // 加载资源图片
                Glide.with(this)
                    .load(photo.resourceId)
                    .fitCenter()
                    .into(ivDetail)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}