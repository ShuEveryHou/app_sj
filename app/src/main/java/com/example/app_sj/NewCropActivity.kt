package com.example.app_sj

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

class NewCropActivity : AppCompatActivity() {

    // ========== 视图组件声明 ==========
    private lateinit var cropImageView: ZoomableImageView
    private lateinit var cropOverlayView: CropOverlayView
    private lateinit var btnCancel: TextView
    private lateinit var btnReset: TextView
    private lateinit var btnDone: TextView
    private lateinit var tabCrop: TextView
    private lateinit var tabRotate: TextView
    private lateinit var ratioContainer: LinearLayout

    // ========== 比例按钮声明 ==========
    private lateinit var btnFree: Button
    private lateinit var btn1_1: Button
    private lateinit var btn4_3: Button
    private lateinit var btn3_4: Button
    private lateinit var btn16_9: Button
    private lateinit var btn9_16: Button

    // ========== 图片数据 ==========
    private var originalBitmap: Bitmap? = null
    private var imagePath: String? = null
    private var resourceId: Int = 0
    private var isFromCamera: Boolean = false

    // ========== 裁剪相关状态 ==========
    private var currentRatio: Float = 0f  // 0表示自由比例

    companion object {
        const val REQUEST_NEW_CROP = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_new)

        initViews()
        loadImageData()
        setupListeners()

        // 默认选中自由比例
        selectRatio(0f)
    }

    /**
     * 初始化所有视图组件
     */
    private fun initViews() {
        cropImageView = findViewById(R.id.cropImageView)
        cropOverlayView = findViewById(R.id.cropOverlayView)
        btnCancel = findViewById(R.id.btnCancel)
        btnReset = findViewById(R.id.btnReset)
        btnDone = findViewById(R.id.btnDone)
        tabCrop = findViewById(R.id.tabCrop)
        tabRotate = findViewById(R.id.tabRotate)
        ratioContainer = findViewById(R.id.ratioContainer)

        // 初始化比例按钮
        btnFree = findViewById(R.id.btnFree)
        btn1_1 = findViewById(R.id.btn1_1)
        btn4_3 = findViewById(R.id.btn4_3)
        btn3_4 = findViewById(R.id.btn3_4)
        btn16_9 = findViewById(R.id.btn16_9)
        btn9_16 = findViewById(R.id.btn9_16)

        // 隐藏旋转标签（暂未实现）
        tabRotate.visibility = View.GONE
        ratioContainer.visibility = View.VISIBLE
    }

    /**
     * 加载图片数据 - 简化版本，只用Glide加载显示，不保存Bitmap
     */
    private fun loadImageData() {
        // 从Intent获取图片数据
        imagePath = intent.getStringExtra("photo_file_path")
        resourceId = intent.getIntExtra("photo_resource_id", 0)
        isFromCamera = intent.getBooleanExtra("is_from_camera", false)

        // 直接使用Glide加载图片显示，不保存Bitmap到内存
        if (isFromCamera && !imagePath.isNullOrEmpty()) {
            // 从文件加载
            Glide.with(this)
                .load(File(imagePath))
                .into(cropImageView)

        } else if (resourceId != 0) {
            // 从资源加载
            Glide.with(this)
                .load(resourceId)
                .into(cropImageView)

        } else {
            Toast.makeText(this, "图片数据错误", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 设置所有事件监听器
     */
    private fun setupListeners() {
        // 取消按钮 - 取消裁剪并返回
        btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // 还原按钮 - 重置到原始状态
        btnReset.setOnClickListener {
            resetToOriginal()
        }

        // 完成按钮 - 执行裁剪并返回结果
        btnDone.setOnClickListener {
            performCrop()
        }

        // ========== 比例按钮监听器设置 ==========
        val ratioButtons = listOf(
            Pair(btnFree, 0f),      // 自由比例
            Pair(btn1_1, 1f),       // 1:1 正方形
            Pair(btn4_3, 4f/3f),    // 4:3 横屏
            Pair(btn3_4, 3f/4f),    // 3:4 竖屏
            Pair(btn16_9, 16f/9f),  // 16:9 宽屏
            Pair(btn9_16, 9f/16f)   // 9:16 手机竖屏
        )

        // 为每个按钮设置点击监听器
        ratioButtons.forEach { (button, ratio) ->
            button.setOnClickListener {
                selectRatio(ratio)
            }
        }

        // ========== 裁剪框变化监听器 ==========
        // 注意：这里移除了updateCropPreview的调用，因为不需要实时预览
        cropOverlayView.setOnCropChangeListener(object : CropOverlayView.OnCropChangeListener {
            override fun onCropChanged(cropRect: RectF) {
                // 不需要实时预览，所以这里什么都不做
                // 或者可以添加一些简单的日志
                // Log.d("Crop", "裁剪框变化: $cropRect")
            }
        })
    }

    /**
     * 选择裁剪比例
     * @param ratio 比例值，0表示自由比例
     */
    private fun selectRatio(ratio: Float) {
        currentRatio = ratio

        // 定义所有比例按钮及其对应的比例值
        val buttons = listOf(
            Pair(btnFree, 0f),
            Pair(btn1_1, 1f),
            Pair(btn4_3, 4f/3f),
            Pair(btn3_4, 3f/4f),
            Pair(btn16_9, 16f/9f),
            Pair(btn9_16, 9f/16f)
        )

        // 更新按钮选中状态
        buttons.forEach { (button, buttonRatio) ->
            button.isSelected = (buttonRatio == ratio)
        }

        // 设置裁剪框比例
        cropOverlayView.setCropRatio(ratio)

        // 显示还原按钮
        btnReset.visibility = View.VISIBLE
    }

    /**
     * 重置到原始状态
     */
    private fun resetToOriginal() {
        // 重置裁剪框
        cropOverlayView.resetCropRect()

        // 重置比例选择
        selectRatio(0f)

        // 隐藏还原按钮
        btnReset.visibility = View.GONE

        Toast.makeText(this, "已还原到原始状态", Toast.LENGTH_SHORT).show()
    }

    /**
     * 执行裁剪操作 - 简化版本
     */
    private fun performCrop() {
        // 直接使用Glide加载的Bitmap进行裁剪
        val drawable = cropImageView.drawable ?: run {
            Toast.makeText(this, "图片未加载", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 1. 获取裁剪区域（相对于裁剪框视图的坐标）
            val cropRect = cropOverlayView.getCropRect()

            // 2. 获取图片显示区域
            val imageRect = getImageDisplayRect(drawable)
            if (imageRect.width() <= 0 || imageRect.height() <= 0) {
                Toast.makeText(this, "无法获取图片显示区域", Toast.LENGTH_SHORT).show()
                return
            }

            // 3. 获取ImageView中的Bitmap
            val bitmap = getBitmapFromDrawable(drawable)
            if (bitmap == null) {
                Toast.makeText(this, "无法获取图片数据", Toast.LENGTH_SHORT).show()
                return
            }

            // 4. 将裁剪框坐标转换为图片坐标
            val cropRectInImage = convertCropRectToImageCoordinates(cropRect, imageRect, bitmap)

            // 5. 创建裁剪后的Bitmap
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                cropRectInImage.left.toInt(),
                cropRectInImage.top.toInt(),
                cropRectInImage.width().toInt(),
                cropRectInImage.height().toInt()
            )

            // 6. 保存裁剪后的图片
            val savedPath = saveCroppedImage(croppedBitmap)

            if (savedPath != null) {
                // 7. 返回结果
                val result = Intent().apply {
                    putExtra("cropped_image_path", savedPath)
                }
                setResult(RESULT_OK, result)
                finish()
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "裁剪失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 从Drawable获取Bitmap
     */
    private fun getBitmapFromDrawable(drawable: android.graphics.drawable.Drawable): Bitmap? {
        return if (drawable is android.graphics.drawable.BitmapDrawable) {
            drawable.bitmap
        } else {
            // 如果不是BitmapDrawable，创建一个新的Bitmap
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    /**
     * 获取图片在ImageView中的显示区域
     */
    private fun getImageDisplayRect(drawable: android.graphics.drawable.Drawable): RectF {
        val matrix = cropImageView.imageMatrix

        val bounds = RectF(
            0f, 0f,
            drawable.intrinsicWidth.toFloat(),
            drawable.intrinsicHeight.toFloat()
        )

        matrix.mapRect(bounds)
        return bounds
    }

    /**
     * 将裁剪框坐标转换为图片坐标
     */
    private fun convertCropRectToImageCoordinates(
        cropRectInOverlay: RectF,
        imageRect: RectF,
        bitmap: Bitmap
    ): RectF {
        // 计算转换比例
        val scaleX = bitmap.width.toFloat() / imageRect.width()
        val scaleY = bitmap.height.toFloat() / imageRect.height()

        // 转换坐标
        val cropRectInImage = RectF(
            (cropRectInOverlay.left - imageRect.left) * scaleX,
            (cropRectInOverlay.top - imageRect.top) * scaleY,
            (cropRectInOverlay.right - imageRect.left) * scaleX,
            (cropRectInOverlay.bottom - imageRect.top) * scaleY
        )

        // 边界检查
        cropRectInImage.left = max(0f, cropRectInImage.left)
        cropRectInImage.top = max(0f, cropRectInImage.top)
        cropRectInImage.right = min(bitmap.width.toFloat(), cropRectInImage.right)
        cropRectInImage.bottom = min(bitmap.height.toFloat(), cropRectInImage.bottom)

        return cropRectInImage
    }

    /**
     * 保存裁剪后的Bitmap到文件
     */
    private fun saveCroppedImage(bitmap: Bitmap): String? {
        return try {
            // 创建时间戳文件名
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "cropped_${timeStamp}.jpg"

            // 保存到应用私有目录
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir == null || !storageDir.exists()) {
                storageDir?.mkdirs()
            }

            val imageFile = File(storageDir, fileName)

            // 保存Bitmap（使用90%质量，兼顾质量和文件大小）
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
            }

            // 释放Bitmap内存
            bitmap.recycle()

            imageFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        originalBitmap?.recycle()
        originalBitmap = null
    }
}