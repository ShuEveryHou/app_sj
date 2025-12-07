package com.example.app_sj

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File

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

    private var isUIVisible = false
    private val handler = android.os.Handler(Looper.getMainLooper())

    // 当前图片数据
    private var currentPhotoId = 0
    private var currentResourceId = 0
    private var currentFilePath = ""
    private var currentPhotoTitle = ""
    private var isFromCamera = false

    // 裁剪后的图片路径
    private var croppedImagePath: String? = null

    // ========== 新的Activity Result API ==========
    // 用于启动新裁剪Activity的结果回调
    private val newCropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val croppedPath = data?.getStringExtra("cropped_image_path")
            if (!croppedPath.isNullOrEmpty()) {
                // 显示裁剪后的图片
                Glide.with(this)
                    .load(File(croppedPath))
                    .into(ivDetail)

                // 更新当前图片路径
                currentFilePath = croppedPath
                isFromCamera = true

                Toast.makeText(this, "裁剪完成", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 用于启动旧裁剪Activity的结果回调（保持兼容）
    private val oldCropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val croppedPath = result.data?.getStringExtra(ImageCropActivity.EXTRA_CROPPED_PATH)
            if (croppedPath != null) {
                // 显示裁剪后的图片
                Glide.with(this)
                    .load(File(croppedPath))
                    .into(ivDetail)

                // 更新UI
                tvPhotoInfo.text = "${currentPhotoTitle} (已裁剪)"
                Toast.makeText(this, "裁剪完成", Toast.LENGTH_SHORT).show()

                // 保存裁剪后的路径
                croppedImagePath = croppedPath
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "裁剪已取消", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detail)

        initViews()
        setupClickListeners()
        loadImageData()
        showZoomHintTemporarily()
    }

    private fun initViews() {
        ivDetail = findViewById(R.id.ivDetail)
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

        // 预加载时先设置一个透明占位符
        ivDetail.setBackgroundColor(0x00000000)
    }

    private fun setupClickListeners() {
        // 返回按钮
        btnBack.setOnClickListener {
            finish()
        }

        // 切换图片细节UI
        ivDetail.setOnClickListener {
            toggleUI()
        }

        // 重置缩放按钮
        btnResetZoom.setOnClickListener {
            ivDetail.resetZoom()
        }

        // 发送按钮（暂不实现）
        btnSend.setOnClickListener {
            Toast.makeText(this, "发送功能待实现", Toast.LENGTH_SHORT).show()
        }

        // 裁剪按钮 - 使用新的方式启动
        btnEdit.setOnClickListener {
            startNewCropActivity()
        }

        // 删除按钮（暂不实现）
        btnDelete.setOnClickListener {
            Toast.makeText(this, "删除功能待实现", Toast.LENGTH_SHORT).show()
        }

        // 文字按钮
        btnText.setOnClickListener {

            Log.d("TextEdit", "点击文字按钮，当前图片ID: $currentPhotoId")
            val photo = Photo(
                id = currentPhotoId,
                resourceId = currentResourceId,
                filePath = currentFilePath,
                title = currentPhotoTitle,
                isFromCamera = isFromCamera,
                isUserCreated = true // 假设是用户创建的图片
            )

            try {
                TextEditActivity.startForResult(this@ImageDetailActivity, photo)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            // 启动文字编辑Activity
            //TextEditActivity.startForResult(this, photo)
        }
    }

    // 添加处理文字功能返回结果的方法
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val savedPath = data.getStringExtra("saved_image_path")
            if (!savedPath.isNullOrEmpty()) {
                // 重新加载图片显示
                Glide.with(this)
                    .load(File(savedPath))
                    .into(ivDetail)

                // 更新当前图片路径
                currentFilePath = savedPath
                isFromCamera = true

                Toast.makeText(this, "文字添加完成", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ========== 裁剪功能相关 ==========

    /**
     * 启动新的裁剪Activity（不使用白色选择框）
     */
    private fun startNewCropActivity() {
        val intent = Intent(this, NewCropActivity::class.java).apply {
            // 传递图片数据
            putExtra("photo_id", currentPhotoId)
            putExtra("photo_resource_id", currentResourceId)
            putExtra("photo_file_path", currentFilePath)
            putExtra("photo_title", currentPhotoTitle)
            putExtra("is_from_camera", isFromCamera)
        }

        // 使用新的Activity Result API启动
        newCropLauncher.launch(intent)
    }

    /**
     * 旧的裁剪方法（保留兼容性）
     */
    @SuppressLint("Deprecated")
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
                startOldImageCrop(which)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 启动旧的裁剪Activity
     */
    @SuppressLint("Deprecated")
    private fun startOldImageCrop(optionIndex: Int) {
        try {
            // 启动旧的裁剪Activity
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
    

    private fun loadImageData() {
        currentPhotoId = intent.getIntExtra("photo_id", 0)
        currentResourceId = intent.getIntExtra("photo_resource_id", 0)
        currentFilePath = intent.getStringExtra("photo_file_path") ?: ""
        currentPhotoTitle = intent.getStringExtra("photo_title") ?: "未命名图片"
        isFromCamera = intent.getBooleanExtra("is_from_camera", false)

        // 检查是否为用户创建的图片
        val isUserCreated = intent.getBooleanExtra("is_user_created", false)

        // 设置照片信息文本
        val sourceText = if (isUserCreated) "用户图片" else "系统图片"
        tvPhotoInfo.text = "${currentPhotoTitle} (ID: ${currentPhotoId}, 来源: ${sourceText})"

        // 加载图片
        loadImageFast(currentResourceId, currentFilePath, isFromCamera || isUserCreated)
    }

    private fun loadImageFast(resourceId: Int, filePath: String, isFromCamera: Boolean) {
        val requestOptions = com.bumptech.glide.request.RequestOptions()
            .placeholder(android.R.color.transparent)  // 透明占位符
            .error(R.drawable.error_image)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)

        if (isFromCamera && filePath.isNotEmpty()) {
            // 加载相机图片
            Glide.with(this)
                .load(File(filePath))
                .apply(requestOptions)
                .thumbnail(0.5f)  // 先加载10%质量的缩略图
                .listener(createGlideListener())
                .into(ivDetail)
        } else {
            // 加载资源图片
            Glide.with(this)
                .load(resourceId)
                .apply(requestOptions)
                .thumbnail(0.5f)
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
                // 加载失败，显示错误图片
                ivDetail.post {
                    ivDetail.setImageResource(android.R.drawable.ic_menu_camera)
                    ivDetail.resetZoom()
                }
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
                ivDetail.post {
                    ivDetail.resetZoom()
                    // 可选：淡入动画
                    ivDetail.animate().alpha(1f).setDuration(200).start()
                }
                return false
            }
        }
    }

    private fun toggleUI() {
        if (isUIVisible) {
            // 隐藏UI元素
            layoutTopBar.visibility = View.GONE
            layoutBottomBar.visibility = View.GONE
            btnResetZoom.visibility = View.GONE
        } else {
            layoutTopBar.visibility = View.VISIBLE
            layoutBottomBar.visibility = View.VISIBLE
            btnResetZoom.visibility = View.VISIBLE
        }
        isUIVisible = !isUIVisible
    }

    // 缩放提示
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

    // ========== 处理返回按钮 ==========

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        fun preloadImage(context: android.content.Context, resourceId: Int) {
            Glide.with(context)
                .load(resourceId)
                .preload()
        }
    }
}