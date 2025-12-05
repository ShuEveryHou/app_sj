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
    private lateinit var cropPreview: ImageView
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
    private var currentBitmap: Bitmap? = null
    private var imagePath: String? = null
    private var resourceId: Int = 0
    private var isFromCamera: Boolean = false

    // ========== 裁剪相关状态 ==========
    private var currentRatio: Float = 0f  // 0表示自由比例
    private var originalMatrix: Matrix = Matrix()
    private var originalCropRect: RectF = RectF()

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
        cropPreview = findViewById(R.id.cropPreview)
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
     * 加载图片数据
     */
    private fun loadImageData() {
        // 从Intent获取图片数据
        imagePath = intent.getStringExtra("photo_file_path")
        resourceId = intent.getIntExtra("photo_resource_id", 0)
        isFromCamera = intent.getBooleanExtra("is_from_camera", false)

        // 加载图片
        if (isFromCamera && !imagePath.isNullOrEmpty()) {
            // 从文件加载
            loadBitmapFromFile(imagePath!!)
        } else if (resourceId != 0) {
            // 从资源加载
            loadBitmapFromResource(resourceId)
        } else {
            Toast.makeText(this, "图片数据错误", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 从文件加载Bitmap
     */
    private fun loadBitmapFromFile(filePath: String) {
        try {
            // 获取图片尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            // 计算合适的采样率（限制最大尺寸为2048）
            val maxSize = 2048
            val width = options.outWidth
            val height = options.outHeight
            var scale = 1

            if (width > maxSize || height > maxSize) {
                val scaleX = width.toFloat() / maxSize
                val scaleY = height.toFloat() / maxSize
                scale = max(scaleX, scaleY).toInt()
            }

            // 加载Bitmap
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            originalBitmap = BitmapFactory.decodeFile(filePath, loadOptions)
            currentBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)

            // 显示图片
            cropImageView.setImageBitmap(currentBitmap)

            // 保存原始状态
            originalMatrix.set(cropImageView.imageMatrix)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 从资源加载Bitmap
     */
    private fun loadBitmapFromResource(resId: Int) {
        try {
            // 从资源加载
            originalBitmap = BitmapFactory.decodeResource(resources, resId)
            currentBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)

            // 显示图片
            cropImageView.setImageBitmap(currentBitmap)

            // 保存原始状态
            originalMatrix.set(cropImageView.imageMatrix)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
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
        // 这里只定义一个ratioButtons变量
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
        cropOverlayView.setOnCropChangeListener(object : CropOverlayView.OnCropChangeListener {
            override fun onCropChanged(cropRect: RectF) {
                updateCropPreview()
            }
        })

        // ========== 标签点击监听器 ==========
        tabCrop.setOnClickListener {
            // 切换到裁剪标签
            selectTab(true)
        }

        tabRotate.setOnClickListener {
            // 切换到旋转标签（暂未实现）
            selectTab(false)
            Toast.makeText(this, "旋转功能待实现", Toast.LENGTH_SHORT).show()
        }
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

        // 更新预览
        updateCropPreview()
    }

    /**
     * 选择标签（裁剪/旋转）
     * @param isCropTab true表示裁剪标签，false表示旋转标签
     */
    private fun selectTab(isCropTab: Boolean) {
        if (isCropTab) {
            // 选中裁剪标签
            tabCrop.setTextColor(Color.WHITE)
            tabCrop.setBackgroundColor(Color.parseColor("#40000000"))
            tabRotate.setTextColor(Color.parseColor("#80FFFFFF"))
            tabRotate.setBackgroundColor(Color.TRANSPARENT)
            ratioContainer.visibility = View.VISIBLE
        } else {
            // 选中旋转标签
            tabRotate.setTextColor(Color.WHITE)
            tabRotate.setBackgroundColor(Color.parseColor("#40000000"))
            tabCrop.setTextColor(Color.parseColor("#80FFFFFF"))
            tabCrop.setBackgroundColor(Color.TRANSPARENT)
            ratioContainer.visibility = View.GONE
        }
    }

    /**
     * 重置到原始状态
     */
    private fun resetToOriginal() {
        // 重置图片
        currentBitmap?.recycle()
        currentBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
        cropImageView.setImageBitmap(currentBitmap)
        cropImageView.imageMatrix = originalMatrix

        // 重置裁剪框
        cropOverlayView.resetCropRect()

        // 隐藏预览
        cropPreview.visibility = View.GONE

        // 重置比例选择
        selectRatio(0f)

        // 隐藏还原按钮
        btnReset.visibility = View.GONE

        Toast.makeText(this, "已还原到原始状态", Toast.LENGTH_SHORT).show()
    }

    /**
     * 更新裁剪预览
     */
    private fun updateCropPreview() {
        if (currentBitmap == null) return

        val cropRect = cropOverlayView.getCropRect()

        // 转换裁剪框坐标到图片坐标
        val imageRect = getImageDisplayRect()
        if (imageRect.width() <= 0 || imageRect.height() <= 0) return

        val scaleX = currentBitmap!!.width.toFloat() / imageRect.width()
        val scaleY = currentBitmap!!.height.toFloat() / imageRect.height()

        val cropRectInImage = RectF(
            (cropRect.left - imageRect.left) * scaleX,
            (cropRect.top - imageRect.top) * scaleY,
            (cropRect.right - imageRect.left) * scaleX,
            (cropRect.bottom - imageRect.top) * scaleY
        )

        // 确保裁剪区域在图片范围内
        cropRectInImage.left = max(0f, cropRectInImage.left)
        cropRectInImage.top = max(0f, cropRectInImage.top)
        cropRectInImage.right = min(currentBitmap!!.width.toFloat(), cropRectInImage.right)
        cropRectInImage.bottom = min(currentBitmap!!.height.toFloat(), cropRectInImage.bottom)

        // 创建裁剪预览
        if (cropRectInImage.width() > 0 && cropRectInImage.height() > 0) {
            try {
                val previewBitmap = Bitmap.createBitmap(
                    currentBitmap!!,
                    cropRectInImage.left.toInt(),
                    cropRectInImage.top.toInt(),
                    cropRectInImage.width().toInt(),
                    cropRectInImage.height().toInt()
                )

                // 显示预览
                cropPreview.setImageBitmap(previewBitmap)
                cropPreview.visibility = View.VISIBLE

            } catch (e: Exception) {
                e.printStackTrace()
                cropPreview.visibility = View.GONE
            }
        } else {
            cropPreview.visibility = View.GONE
        }
    }

    /**
     * 获取图片在ImageView中的显示区域
     */
    private fun getImageDisplayRect(): RectF {
        val drawable = cropImageView.drawable ?: return RectF()
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
     * 执行裁剪操作
     */
    private fun performCrop() {
        if (currentBitmap == null) {
            Toast.makeText(this, "图片未加载", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 获取裁剪区域
            val cropRect = cropOverlayView.getCropRect()
            val imageRect = getImageDisplayRect()

            if (imageRect.width() <= 0 || imageRect.height() <= 0) {
                Toast.makeText(this, "无法获取图片显示区域", Toast.LENGTH_SHORT).show()
                return
            }

            // 转换坐标
            val scaleX = currentBitmap!!.width.toFloat() / imageRect.width()
            val scaleY = currentBitmap!!.height.toFloat() / imageRect.height()

            val cropRectInImage = RectF(
                (cropRect.left - imageRect.left) * scaleX,
                (cropRect.top - imageRect.top) * scaleY,
                (cropRect.right - imageRect.left) * scaleX,
                (cropRect.bottom - imageRect.top) * scaleY
            )

            // 边界检查
            cropRectInImage.left = max(0f, cropRectInImage.left)
            cropRectInImage.top = max(0f, cropRectInImage.top)
            cropRectInImage.right = min(currentBitmap!!.width.toFloat(), cropRectInImage.right)
            cropRectInImage.bottom = min(currentBitmap!!.height.toFloat(), cropRectInImage.bottom)

            val width = cropRectInImage.width().toInt()
            val height = cropRectInImage.height().toInt()

            if (width <= 0 || height <= 0) {
                Toast.makeText(this, "裁剪区域无效", Toast.LENGTH_SHORT).show()
                return
            }

            // 执行裁剪
            val croppedBitmap = Bitmap.createBitmap(
                currentBitmap!!,
                cropRectInImage.left.toInt(),
                cropRectInImage.top.toInt(),
                width,
                height
            )

            // 保存裁剪后的图片
            val savedPath = saveCroppedImage(croppedBitmap)

            if (savedPath != null) {
                // 返回结果
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

            // 高质量保存（100%质量）
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
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
        // 释放内存
        originalBitmap?.recycle()
        currentBitmap?.recycle()
        originalBitmap = null
        currentBitmap = null
    }
}