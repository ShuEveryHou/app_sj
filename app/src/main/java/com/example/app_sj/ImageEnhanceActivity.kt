package com.example.app_sj

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ImageEnhanceActivity : AppCompatActivity() {

    // ========== 视图组件 ==========
    private lateinit var ivOriginal: ImageView
    private lateinit var ivProcessed: ImageView
    private lateinit var btnCancel: TextView
    private lateinit var btnSave: TextView

    private lateinit var btnBrightness: Button
    private lateinit var btnContrast: Button
    private lateinit var btnFilter: Button
    private lateinit var btnSticker: Button
    private lateinit var btnMerge: Button

    private lateinit var panelBrightness: LinearLayout
    private lateinit var panelContrast: LinearLayout

    private lateinit var seekBarBrightness: SeekBar
    private lateinit var tvBrightnessValue: TextView
    private lateinit var seekBarContrast: SeekBar
    private lateinit var tvContrastValue: TextView

    // ========== 数据状态 ==========
    private var imagePath: String? = null
    private var resourceId: Int = 0
    private var isFromCamera: Boolean = false

    private var isUserCreated: Boolean = false
    private var originalBitmap: Bitmap? = null
    private var processedBitmap: Bitmap? = null

    // 调整参数
    private var brightnessValue = 0
    private var contrastValue = 100 // 百分比表示，100表示无变化

    // 处理控制
    private var currentPanel: String = "none"
    private val handler = Handler(Looper.getMainLooper())
    private var processingTask: Runnable? = null
    private var isProcessing = false
    private var pendingBrightness = 0
    private var pendingContrast = 100

    companion object {
        fun startForResult(activity: AppCompatActivity, photo: Photo) {
            val intent = Intent(activity, ImageEnhanceActivity::class.java).apply {
                putExtra("photo_id", photo.id)
                putExtra("photo_resource_id", photo.resourceId)
                putExtra("photo_file_path", photo.filePath)
                putExtra("photo_title", photo.title)
                putExtra("is_from_camera", photo.isFromCamera)
                putExtra("is_user_created", photo.isUserCreated)
            }
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_enhance)

        initViews()
        loadImageData()
        setupListeners()
        setupPanels()
    }

    private fun initViews() {
        ivOriginal = findViewById(R.id.ivOriginal)
        ivProcessed = findViewById(R.id.ivProcessed)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)

        btnBrightness = findViewById(R.id.btnBrightness)
        btnContrast = findViewById(R.id.btnContrast)
        btnFilter = findViewById(R.id.btnFilter)
        btnSticker = findViewById(R.id.btnSticker)
        btnMerge = findViewById(R.id.btnMerge)

        panelBrightness = findViewById(R.id.panelBrightness)
        panelContrast = findViewById(R.id.panelContrast)

        seekBarBrightness = findViewById(R.id.seekBarBrightness)
        tvBrightnessValue = findViewById(R.id.tvBrightnessValue)
        seekBarContrast = findViewById(R.id.seekBarContrast)
        tvContrastValue = findViewById(R.id.tvContrastValue)
    }

    private fun setupPanels() {
        seekBarBrightness.progress = 100
        seekBarContrast.progress = 100
        updateDisplayValues()
    }

    private fun loadImageData() {
        imagePath = intent.getStringExtra("photo_file_path")
        resourceId = intent.getIntExtra("photo_resource_id", 0)
        isFromCamera = intent.getBooleanExtra("is_from_camera", false)
        isUserCreated = intent.getBooleanExtra("is_user_created", false)

        // 使用Glide加载图片（Glide会自动处理内存和线程）
        if ((isFromCamera || isUserCreated) && !imagePath.isNullOrEmpty()) {
            Glide.with(this)
                .load(File(imagePath))
                .into(ivProcessed)

            // 后台线程加载原始Bitmap
            Thread {
                try {
                    originalBitmap = loadBitmapFromFile(imagePath!!)
                    if (originalBitmap != null) {
                        processedBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()

        } else if (resourceId != 0) {
            Glide.with(this)
                .load(resourceId)
                .into(ivProcessed)

            Thread {
                try {
                    originalBitmap = loadBitmapFromResource(resourceId)
                    if (originalBitmap != null) {
                        processedBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        } else {
            Toast.makeText(this, "图片数据错误", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadBitmapFromFile(filePath: String): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        val scale = calculateInSampleSize(options, 800, 800) // 限制尺寸减少内存

        val loadOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
            inPreferredConfig = Bitmap.Config.RGB_565 // 使用更少内存的配置
        }

        return BitmapFactory.decodeFile(filePath, loadOptions)
    }

    private fun loadBitmapFromResource(resId: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(resources, resId, options)

        val scale = calculateInSampleSize(options, 800, 800)

        val loadOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        return BitmapFactory.decodeResource(resources, resId, loadOptions)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveEnhancedImage() }

        btnBrightness.setOnClickListener { togglePanel("brightness") }
        btnContrast.setOnClickListener { togglePanel("contrast") }
        btnFilter.setOnClickListener {
            Toast.makeText(this, "滤镜功能待实现", Toast.LENGTH_SHORT).show()
        }
        btnSticker.setOnClickListener {
            Toast.makeText(this, "贴纸功能待实现", Toast.LENGTH_SHORT).show()
        }
        btnMerge.setOnClickListener {
            Toast.makeText(this, "拼接功能待实现", Toast.LENGTH_SHORT).show()
        }

        // 亮度滑块 - 添加防抖处理
        seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // 0-200映射到-100~100
                    val brightness = progress - 100
                    tvBrightnessValue.text = brightness.toString()

                    // 保存当前值并触发延迟处理
                    pendingBrightness = brightness
                    scheduleImageProcessing()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 开始滑动时暂停处理
                cancelPendingProcessing()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 停止滑动时立即处理一次
                brightnessValue = pendingBrightness
                processImageWithDelay(0)
            }
        })

        // 对比度滑块 - 添加防抖处理
        seekBarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // 0-200映射到-50~150，转换为百分比
                    val contrastRaw = progress - 50  // -50~150
                    val contrastPercent = 100 + contrastRaw  // 50~250，100表示无变化
                    tvContrastValue.text = contrastRaw.toString()

                    // 保存当前值并触发延迟处理
                    pendingContrast = contrastPercent
                    scheduleImageProcessing()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                cancelPendingProcessing()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                contrastValue = pendingContrast
                processImageWithDelay(0)
            }
        })
    }

    /**
     * 取消待处理的任务
     */
    private fun cancelPendingProcessing() {
        processingTask?.let { handler.removeCallbacks(it) }
        processingTask = null
    }

    /**
     * 调度图像处理（防抖）
     */
    private fun scheduleImageProcessing() {
        cancelPendingProcessing()

        processingTask = Runnable {
            brightnessValue = pendingBrightness
            contrastValue = pendingContrast
            processImageInBackground()
        }

        // 延迟150ms处理，避免频繁触发
        handler.postDelayed(processingTask!!, 150)
    }

    /**
     * 延迟处理图像
     */
    private fun processImageWithDelay(delay: Long = 150) {
        cancelPendingProcessing()

        processingTask = Runnable {
            processImageInBackground()
        }

        handler.postDelayed(processingTask!!, delay)
    }

    /**
     * 在后台线程处理图像
     */
    private fun processImageInBackground() {
        if (isProcessing || originalBitmap == null) return

        isProcessing = true

        Thread {
            try {
                // 处理图像
                val resultBitmap = adjustBrightnessAndContrast(
                    originalBitmap!!,
                    brightnessValue,
                    contrastValue
                )

                // 更新UI
                runOnUiThread {
                    processedBitmap = resultBitmap
                    ivProcessed.setImageBitmap(resultBitmap)
                    isProcessing = false
                }

            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "内存不足，请选择小一点的图片", Toast.LENGTH_SHORT).show()
                    isProcessing = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isProcessing = false
            }
        }.start()
    }

    /**
     * 优化版的图像处理函数
     */
    private fun adjustBrightnessAndContrast(src: Bitmap, brightness: Int, contrastPercent: Int): Bitmap {
        // 如果没有任何调整，直接返回原图
        if (brightness == 0 && contrastPercent == 100) {
            return src.copy(Bitmap.Config.ARGB_8888, true)
        }

        val width = src.width
        val height = src.height

        // 使用RGB_565配置减少内存使用
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        // 获取像素数据
        val srcPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val dstPixels = IntArray(width * height)

        // 预计算查找表（LUT）来加速处理
        val brightnessLut = IntArray(256)
        val contrastLut = IntArray(256)

        // 亮度查找表
        val brightnessAdjust = (brightness * 255 / 100).coerceIn(-255, 255)
        for (i in 0..255) {
            brightnessLut[i] = (i + brightnessAdjust).coerceIn(0, 255)
        }

        // 对比度查找表
        val contrastFactor = (contrastPercent - 100) / 100.0f
        for (i in 0..255) {
            // 将像素值从0-255转换到0-1
            val normalized = i / 255.0f
            // 应用对比度
            val adjusted = ((normalized - 0.5f) * (1 + contrastFactor)) + 0.5f
            // 转换回0-255并限制范围
            contrastLut[i] = (adjusted * 255).toInt().coerceIn(0, 255)
        }

        // 处理每个像素
        for (i in srcPixels.indices) {
            val pixel = srcPixels[i]

            // 提取ARGB分量
            val alpha = pixel shr 24 and 0xFF
            var red = pixel shr 16 and 0xFF
            var green = pixel shr 8 and 0xFF
            var blue = pixel and 0xFF

            // 应用查找表处理
            red = contrastLut[red]
            green = contrastLut[green]
            blue = contrastLut[blue]

            red = brightnessLut[red].coerceIn(0, 255)
            green = brightnessLut[green].coerceIn(0, 255)
            blue = brightnessLut[blue].coerceIn(0, 255)

            // 重新组合像素
            dstPixels[i] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }

        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 切换面板
     */
    private fun togglePanel(panelName: String) {
        if (currentPanel == panelName) {
            closeAllPanels()
            currentPanel = "none"
            return
        }

        closeAllPanels()
        currentPanel = panelName

        when (panelName) {
            "brightness" -> {
                panelBrightness.visibility = View.VISIBLE
                btnBrightness.isSelected = true
            }
            "contrast" -> {
                panelContrast.visibility = View.VISIBLE
                btnContrast.isSelected = true
            }
        }
    }

    /**
     * 关闭所有面板
     */
    private fun closeAllPanels() {
        panelBrightness.visibility = View.GONE
        panelContrast.visibility = View.GONE

        btnBrightness.isSelected = false
        btnContrast.isSelected = false
        btnFilter.isSelected = false
        btnSticker.isSelected = false
        btnMerge.isSelected = false

        currentPanel = "none"
    }

    /**
     * 更新显示值
     */
    private fun updateDisplayValues() {
        tvBrightnessValue.text = brightnessValue.toString()
        val contrastRaw = (contrastValue - 100)
        tvContrastValue.text = contrastRaw.toString()
    }

    /**
     * 保存增强后的图片
     */
    private fun saveEnhancedImage() {
        if (processedBitmap == null) {
            Toast.makeText(this, "图片未加载", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "正在保存图片...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val savedPath = ImageManager.saveUserImage(
                    this@ImageEnhanceActivity,
                    processedBitmap!!,
                    "增强图片"
                )

                runOnUiThread {
                    if (savedPath != null) {
                        Toast.makeText(this@ImageEnhanceActivity, "保存成功！", Toast.LENGTH_SHORT).show()

                        val result = Intent().apply {
                            putExtra("saved_image_path", savedPath)
                        }
                        setResult(RESULT_OK, result)
                        finish()
                    } else {
                        Toast.makeText(this@ImageEnhanceActivity, "保存失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@ImageEnhanceActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        cancelPendingProcessing()
        handler.removeCallbacksAndMessages(null)

        // 回收Bitmap
        originalBitmap?.recycle()
        processedBitmap?.recycle()
        originalBitmap = null
        processedBitmap = null

        // 提示系统进行垃圾回收
        System.gc()
    }
}