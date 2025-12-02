package com.example.app_sj

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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


    // 当前图片数据
    private var currentPhotoId = 0
    private var currentResourceId = 0
    private var currentFilePath = ""
    private var currentPhotoTitle = ""
    private var isFromCamera = false

    // 裁剪后的图片路径（新添加的变量）
    private var croppedImagePath: String? = null

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
        //剪裁按钮
        btnEdit.setOnClickListener {
            showCropOptionsDialog()
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

        loadImageFast(resourceId, filePath, isFromCamera)
    }

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


    private fun showCropOptionsDialog() {
        val cropOptions = arrayOf(
            "自由裁剪",
            "1:1 (正方形)",
            "4:3 (横屏)",
            "16:9 (宽屏)",
            "3:4 (竖屏)",
            "9:16 (手机屏幕)"
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择裁剪比例")
            .setItems(cropOptions) { _, which ->
                // 这里我们不再使用旧的CropRatio枚举，直接传递比例值
                startImageCrop(which)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun startImageCrop(optionIndex: Int) {
        try {
            // 启动我们手写的裁剪Activity
            ImageCropActivity.startForResult(
                activity = this,
                imagePath = if (isFromCamera && currentFilePath.isNotEmpty()) currentFilePath else null,
                resourceId = if (!isFromCamera) currentResourceId else 0,
                isFromCamera = isFromCamera
            )

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "无法启动裁剪: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 处理裁剪结果（新增这部分）
        if (requestCode == ImageCropActivity.REQUEST_CROP) {
            if (resultCode == RESULT_OK && data != null) {
                val croppedPath = data.getStringExtra(ImageCropActivity.EXTRA_CROPPED_PATH)
                if (croppedPath != null) {
                    // 显示裁剪后的图片
                    Glide.with(this)
                        .load(File(croppedPath))
                        .into(ivDetail)

                    // 更新UI
                    tvPhotoInfo.text = "${currentPhotoTitle} (已裁剪)"
                    Toast.makeText(this, "裁剪完成", Toast.LENGTH_SHORT).show()

                    // 保存裁剪后的路径，用于后续保存到相册
                    croppedImagePath = croppedPath
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "裁剪已取消", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

}