package com.example.app_sj

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.File

class ImageDetailActivity : AppCompatActivity() {
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
    private var isUserCreated = false  // 添加这个字段

    // 裁剪后的图片路径
    private var croppedImagePath: String? = null

    // ========== Activity Result Launchers ==========
    private val newCropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val croppedPath = data?.getStringExtra("cropped_image_path")
            if (!croppedPath.isNullOrEmpty()) {
                // 显示裁剪后的图片
                updateImageAfterEdit(croppedPath)
                Toast.makeText(this, "裁剪完成", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val textEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val savedPath = data?.getStringExtra("saved_image_path")
            if (!savedPath.isNullOrEmpty()) {
                updateImageAfterEdit(savedPath)
                Toast.makeText(this, "文字添加完成", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val enhanceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val savedPath = data?.getStringExtra("saved_image_path")
            if (!savedPath.isNullOrEmpty()) {
                updateImageAfterEdit(savedPath)
                Toast.makeText(this, "编辑完成", Toast.LENGTH_SHORT).show()
            }
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

        // 裁剪按钮
        btnEdit.setOnClickListener {
            startCropActivity()
        }

        // 综合按钮（原来的删除按钮）
        btnDelete.setOnClickListener {
            startEnhanceActivity()
        }

        // 文字按钮
        btnText.setOnClickListener {
            startTextEditActivity()
        }
    }

    /**
     * 加载图片数据
     */
    @SuppressLint("SetTextI18n")
    private fun loadImageData() {
        // 从Intent获取所有数据
        currentPhotoId = intent.getIntExtra("photo_id", 0)
        currentResourceId = intent.getIntExtra("photo_resource_id", 0)
        currentFilePath = intent.getStringExtra("photo_file_path") ?: ""
        currentPhotoTitle = intent.getStringExtra("photo_title") ?: "未命名图片"
        isFromCamera = intent.getBooleanExtra("is_from_camera", false)
        isUserCreated = intent.getBooleanExtra("is_user_created", false) // 获取这个字段

        Log.d("ImageDetail", "=== 加载图片数据 ===")
        Log.d("ImageDetail", "ID: $currentPhotoId")
        Log.d("ImageDetail", "资源ID: $currentResourceId")
        Log.d("ImageDetail", "文件路径: $currentFilePath")
        Log.d("ImageDetail", "标题: $currentPhotoTitle")
        Log.d("ImageDetail", "来自相机: $isFromCamera")
        Log.d("ImageDetail", "用户创建: $isUserCreated")

        // 验证数据
        if (isUserCreated && currentFilePath.isEmpty()) {
            Log.e("ImageDetail", "用户图片但没有文件路径!")
            Toast.makeText(this, "图片数据错误: 缺少文件路径", Toast.LENGTH_SHORT).show()
        }

        // 设置照片信息
        val sourceText = if (isUserCreated) "用户图片" else "系统图片"
        tvPhotoInfo.text = "${currentPhotoTitle} (ID: ${currentPhotoId}, 来源: ${sourceText})"

        // 加载图片
        loadImage()
    }

    /**
     * 加载图片
     */
    private fun loadImage() {
        Log.d("ImageDetail", "开始加载图片...")

        val requestOptions = com.bumptech.glide.request.RequestOptions()
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)

        if (isUserCreated && currentFilePath.isNotEmpty()) {
            // 用户创建的图片：从文件路径加载
            val file = File(currentFilePath)
            if (file.exists()) {
                Log.d("ImageDetail", "从文件加载: ${file.absolutePath}, 大小: ${file.length()/1024}KB")

                Glide.with(this)
                    .load(file)
                    .apply(requestOptions)
                    .listener(createGlideListener())
                    .into(ivDetail)
            } else {
                Log.e("ImageDetail", "文件不存在: $currentFilePath")
                ivDetail.setImageResource(R.drawable.error_image)
                Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show()
            }
        } else if (currentResourceId != 0) {
            // 系统图片：从资源ID加载
            Log.d("ImageDetail", "从资源加载: $currentResourceId")
            Glide.with(this)
                .load(currentResourceId)
                .apply(requestOptions)
                .listener(createGlideListener())
                .into(ivDetail)
        } else {
            // 默认图标
            Log.e("ImageDetail", "没有可用的图片数据")
            ivDetail.setImageResource(R.drawable.error_image)
            Toast.makeText(this, "图片数据错误", Toast.LENGTH_SHORT).show()
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
                Log.e("ImageDetail", "Glide加载失败: ${e?.message}")
                ivDetail.post {
                    ivDetail.setImageResource(R.drawable.error_image)
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
                Log.d("ImageDetail", "图片加载完成")
                ivDetail.post {
                    ivDetail.resetZoom()
                    ivDetail.animate().alpha(1f).setDuration(200).start()
                }
                return false
            }
        }
    }

    /**
     * 启动裁剪活动
     */
    private fun startCropActivity() {
        Log.d("ImageDetail", "启动裁剪活动")

        val photo = createCurrentPhoto()
        if (photo.filePath.isEmpty() && photo.resourceId == 0) {
            Toast.makeText(this, "图片数据错误，无法裁剪", Toast.LENGTH_SHORT).show()
            return
        }

        // 使用launcher启动活动
        val intent = Intent(this, NewCropActivity::class.java).apply {
            putExtra("photo_id", photo.id)
            putExtra("photo_resource_id", photo.resourceId)
            putExtra("photo_file_path", photo.filePath)
            putExtra("photo_title", photo.title)
            putExtra("is_from_camera", photo.isFromCamera)
            putExtra("is_user_created", photo.isUserCreated)
        }

        newCropLauncher.launch(intent)
    }

    /**
     * 启动综合编辑活动
     */
    private fun startEnhanceActivity() {
        Log.d("ImageDetail", "启动综合编辑活动")

        val photo = createCurrentPhoto()
        if (photo.filePath.isEmpty() && photo.resourceId == 0) {
            Toast.makeText(this, "图片数据错误，无法编辑", Toast.LENGTH_SHORT).show()
            return
        }

        // 使用launcher启动活动
        val intent = Intent(this, ImageEnhanceActivity::class.java).apply {
            putExtra("photo_id", photo.id)
            putExtra("photo_resource_id", photo.resourceId)
            putExtra("photo_file_path", photo.filePath)
            putExtra("photo_title", photo.title)
            putExtra("is_from_camera", photo.isFromCamera)
            putExtra("is_user_created", photo.isUserCreated)
        }

        enhanceLauncher.launch(intent)
    }

    /**
     * 启动文字编辑活动
     */
    private fun startTextEditActivity() {
        Log.d("ImageDetail", "启动文字编辑活动")

        val photo = createCurrentPhoto()
        if (photo.filePath.isEmpty() && photo.resourceId == 0) {
            Toast.makeText(this, "图片数据错误，无法添加文字", Toast.LENGTH_SHORT).show()
            return
        }

        // 使用launcher启动活动
        val intent = Intent(this, TextEditActivity::class.java).apply {
            putExtra("photo_id", photo.id)
            putExtra("photo_resource_id", photo.resourceId)
            putExtra("photo_file_path", photo.filePath)
            putExtra("photo_title", photo.title)
            putExtra("is_from_camera", photo.isFromCamera)
            putExtra("is_user_created", photo.isUserCreated)
        }

        textEditLauncher.launch(intent)
    }

    /**
     * 创建当前图片的Photo对象
     */
    private fun createCurrentPhoto(): Photo {
        return Photo(
            id = currentPhotoId,
            resourceId = currentResourceId,
            filePath = currentFilePath,
            title = currentPhotoTitle,
            isFromCamera = isFromCamera,
            isUserCreated = isUserCreated
        )
    }

    /**
     * 编辑后更新图片显示
     */
    private fun updateImageAfterEdit(newImagePath: String) {
        Log.d("ImageDetail", "更新图片: $newImagePath")

        // 更新当前图片路径
        currentFilePath = newImagePath
        isFromCamera = true
        isUserCreated = true

        // 重新加载图片
        loadImage()
    }

    private fun toggleUI() {
        if (isUIVisible) {
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

    private fun showZoomHintTemporarily() {
        tvZoomHint.visibility = View.VISIBLE
        tvZoomHint.alpha = 1.0f

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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}