package com.example.app_sj

import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import java.io.File
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.target.Target


class ImageDetailActivity: AppCompatActivity() {
    private lateinit var ivDetail: ZoomableImageView
    private lateinit var layoutTopBar: LinearLayout
    private lateinit var layoutBottomBar: LinearLayout
    private lateinit var tvPhotoInfo: TextView
    private lateinit var tvZoomHint: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnResetZoom: ImageView
    private lateinit var btnSend: Button
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button
    private lateinit var btnText: Button


    private var isUIVisible = false //显示操作栏状态,初始不可见
    private val  handler = android.os.Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detail)


        initViews()
        setupCLickListeners()
        loadImageData()
        showZoomHintTemporarily()
    }

    private fun initViews(){
        ivDetail = findViewById(R.id.ivDetail)  // 现在使用ZoomableImageView
        layoutTopBar = findViewById(R.id.layoutTopBar)
        layoutBottomBar = findViewById(R.id.layoutBottomBar)
        tvPhotoInfo = findViewById(R.id.tvPhotoInfo)
        tvZoomHint = findViewById(R.id.tvZoomHint)
        btnBack = findViewById(R.id.btnBack)
        btnResetZoom = findViewById(R.id.btnResetZoom)
        btnSend = findViewById(R.id.btnSend)
        btnEdit = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
        btnText = findViewById(R.id.btnText)

        // 预加载时先设置一个透明占位符，避免空白
        ivDetail.setBackgroundColor(0x00000000)
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

        // 重置缩放按钮
        btnResetZoom.setOnClickListener {
            ivDetail.resetZoom()
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

        val photoId = intent.getIntExtra("photo_id", 0)
        val resourceId = intent.getIntExtra("photo_resource_id", 0)
        val filePath = intent.getStringExtra("photo_file_path") ?: ""
        val photoTitle = intent.getStringExtra("photo_title") ?: "未命名图片"
        val isFromCamera = intent.getBooleanExtra("is_from_camera", false)

        // 设置照片信息文本
        tvPhotoInfo.text = "${photoTitle} (ID: ${photoId})"

        // 加载图片
        //loadImage(resourceId, filePath, isFromCamera)

        loadImageFast(resourceId, filePath, isFromCamera)
    }

    /*private fun loadImage(resourceId: Int, filePath: String, isFromCamera: Boolean) {
        try {
            if (isFromCamera && filePath.isNotEmpty()) {
                // 加载相机拍摄的图片
                Glide.with(this)
                    .load(filePath)
                    .fitCenter()
                    .into(ivDetail)
            } else if (resourceId != 0) {
                // 加载资源图片
                Glide.with(this)
                    .load(resourceId)
                    .fitCenter()
                    .into(ivDetail)
            }

            // 图片加载完成后，重置缩放状态
            ivDetail.postDelayed({
                ivDetail.resetZoom()
            }, 100)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }*/

    private fun loadImageFast(resourceId: Int, filePath: String, isFromCamera: Boolean) {
        // 方案1：使用Glide的thumbnail先显示缩略图
        if (isFromCamera && filePath.isNotEmpty()) {
            // 加载相机图片
            Glide.with(this)
                .load(File(filePath))
                .sizeMultiplier(0.5f)  // 先显示50%质量的缩略图
                .listener(createGlideListener())
                .into(ivDetail)
        } else {
            // 加载资源图片
            Glide.with(this)
                .load(resourceId)
                .sizeMultiplier(0.5f)  // 先显示50%质量的缩略图
                .listener(createGlideListener())
                .into(ivDetail)
        }
    }

    private fun createGlideListener(): com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
        return object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
            override fun onLoadFailed(
                e: com.bumptech.glide.load.engine.GlideException?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                // 加载失败，也尝试居中
                ivDetail.post { ivDetail.resetZoom() }
                return false
            }

            override fun onResourceReady(
                resource: android.graphics.drawable.Drawable,
                model: Any,
                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                dataSource: com.bumptech.glide.load.DataSource,
                isFirstResource: Boolean
            ): Boolean {
                // 图片加载完成后，立即居中
                ivDetail.post { ivDetail.resetZoom() }
                return false
            }
        }
    }
    companion object {
        fun preloadImage(context: android.content.Context, resourceId: Int) {
            // 在打开详情页前预加载
            Glide.with(context)
                .load(resourceId)
                .preload()  // 预加载到缓存
        }
    }

    private fun toggleUI(){
        if(isUIVisible){
            //隐藏UI元素
            layoutTopBar.visibility = View.GONE
            layoutBottomBar.visibility = View.GONE
            btnResetZoom.visibility = View.GONE
        }else{
            layoutTopBar.visibility = View.VISIBLE
            layoutBottomBar.visibility = View.VISIBLE
            btnResetZoom.visibility = View.VISIBLE
        }
        isUIVisible =!isUIVisible
    }

    //缩放提示
    private fun showZoomHintTemporarily() {
        tvZoomHint.visibility = View.VISIBLE
        tvZoomHint.alpha = 1.0f

        // 3秒后淡出
        handler.postDelayed({
            tvZoomHint.animate()
                .alpha(0f)
                .setDuration(1000)
                .withEndAction {
                    tvZoomHint.visibility = View.GONE
                }
                .start()
        }, 3000)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

}