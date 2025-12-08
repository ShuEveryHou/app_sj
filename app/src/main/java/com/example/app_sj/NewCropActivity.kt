package com.example.app_sj

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
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
    // 图片显示相关
    private lateinit var cropImageView: ZoomableImageView
    private lateinit var cropOverlayView: CropOverlayView
    private lateinit var tvTitle: TextView
    private lateinit var tvRotateStatus: TextView

    // 顶部工具栏
    private lateinit var btnCancel: TextView
    private lateinit var btnReset: TextView
    private lateinit var btnDone: TextView

    // 功能标签
    private lateinit var tabCrop: TextView
    private lateinit var tabRotate: TextView

    // 布局容器
    private lateinit var layoutCrop: LinearLayout
    private lateinit var layoutRotate: LinearLayout
    private lateinit var ratioContainer: LinearLayout

    // 裁剪相关按钮
    private lateinit var btnFree: Button
    private lateinit var btn1_1: Button
    private lateinit var btn4_3: Button
    private lateinit var btn3_4: Button
    private lateinit var btn16_9: Button
    private lateinit var btn9_16: Button

    // 旋转相关按钮
    private lateinit var btnRotateClockwise: Button      // 顺时针
    private lateinit var btnRotateCounterClockwise: Button // 逆时针
    private lateinit var btnRotate180: Button            // 180°
    private lateinit var btnFlipHorizontal: Button       // 水平翻转
    private lateinit var btnFlipVertical: Button         // 垂直翻转

    // ========== 数据状态 ==========
    private var imagePath: String? = null
    private var resourceId: Int = 0
    private var isFromCamera: Boolean = false

    private var isUserCreated: Boolean = false

    // 当前功能模式：true=裁剪，false=旋转
    private var isCropMode: Boolean = true

    // 旋转相关状态
    private var currentRotation: Float = 0f          // 当前旋转角度（0, 90, 180, 270）
    private var isHorizontalFlipped: Boolean = false // 是否水平翻转
    private var isVerticalFlipped: Boolean = false   // 是否垂直翻转

    // 变换矩阵
    private val transformMatrix: Matrix = Matrix()

    // Bitmap
    private var originalBitmap: Bitmap? = null
    private var transformedBitmap: Bitmap? = null

    // 防止频繁操作的标志
    private var isTransforming: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_new)

        initViews()
        loadImageData()
        setupListeners()

        // 默认进入裁剪模式
        switchToCropMode()
        selectRatio(0f)
    }

    /**
     * 初始化所有视图组件
     */
    private fun initViews() {
        // 图片显示相关
        cropImageView = findViewById(R.id.cropImageView)
        cropOverlayView = findViewById(R.id.cropOverlayView)
        tvTitle = findViewById(R.id.tvTitle)
        tvRotateStatus = findViewById(R.id.tvRotateStatus)

        // 顶部工具栏
        btnCancel = findViewById(R.id.btnCancel)
        btnReset = findViewById(R.id.btnReset)
        btnDone = findViewById(R.id.btnDone)

        // 功能标签
        tabCrop = findViewById(R.id.tabCrop)
        tabRotate = findViewById(R.id.tabRotate)

        // 布局容器
        layoutCrop = findViewById(R.id.layoutCrop)
        layoutRotate = findViewById(R.id.layoutRotate)
        ratioContainer = findViewById(R.id.ratioContainer)

        // 裁剪相关按钮
        btnFree = findViewById(R.id.btnFree)
        btn1_1 = findViewById(R.id.btn1_1)
        btn4_3 = findViewById(R.id.btn4_3)
        btn3_4 = findViewById(R.id.btn3_4)
        btn16_9 = findViewById(R.id.btn16_9)
        btn9_16 = findViewById(R.id.btn9_16)

        // 旋转相关按钮
        btnRotateClockwise = findViewById(R.id.btnRotateClockwise)
        btnRotateCounterClockwise = findViewById(R.id.btnRotateCounterClockwise)
        btnRotate180 = findViewById(R.id.btnRotate180)
        btnFlipHorizontal = findViewById(R.id.btnFlipHorizontal)
        btnFlipVertical = findViewById(R.id.btnFlipVertical)
    }

    /**
     * 加载图片数据
     */
    private fun loadImageData() {
        // 从Intent获取图片数据
        imagePath = intent.getStringExtra("photo_file_path")
        resourceId = intent.getIntExtra("photo_resource_id", 0)
        isFromCamera = intent.getBooleanExtra("is_from_camera", false)
        isUserCreated = intent.getBooleanExtra("is_user_created", false)


        // 加载原始Bitmap（使用较小尺寸，避免内存问题）
        try {
            if ((isFromCamera || isUserCreated) && !imagePath.isNullOrEmpty()) {
                // 从文件加载，使用采样
                originalBitmap = decodeSampledBitmapFromFile(imagePath!!, 1024, 1024)
            } else if (resourceId != 0) {
                // 从资源加载，使用采样
                originalBitmap = decodeSampledBitmapFromResource(resourceId, 1024, 1024)
            } else {
                Toast.makeText(this, "图片数据错误", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 初始化变换后的Bitmap
        transformedBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)

        // 显示图片
        updateImageDisplay()
        updateStatusText()
    }

    /**
     * 从文件解码Bitmap，使用采样避免内存过大
     */
    private fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(filePath, options)
    }

    /**
     * 从资源解码Bitmap，使用采样避免内存过大
     */
    private fun decodeSampledBitmapFromResource(resId: Int, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(resources, resId, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeResource(resources, resId, options)
    }

    /**
     * 计算采样大小
     */
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

    /**
     * 设置所有事件监听器
     */
    private fun setupListeners() {
        // 取消按钮
        btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // 还原按钮
        btnReset.setOnClickListener {
            resetToOriginal()
        }

        // 完成按钮
        btnDone.setOnClickListener {
            performCrop()
        }

        // 功能标签点击监听
        tabCrop.setOnClickListener {
            switchToCropMode()
        }

        tabRotate.setOnClickListener {
            switchToRotateMode()
        }

        // 裁剪比例按钮监听
        setupCropListeners()

        // 旋转按钮监听（使用防抖处理）
        setupRotateListeners()
    }

    /**
     * 设置裁剪比例按钮监听器
     */
    private fun setupCropListeners() {
        val ratioButtons = listOf(
            Pair(btnFree, 0f),
            Pair(btn1_1, 1f),
            Pair(btn4_3, 4f/3f),
            Pair(btn3_4, 3f/4f),
            Pair(btn16_9, 16f/9f),
            Pair(btn9_16, 9f/16f)
        )

        ratioButtons.forEach { (button, ratio) ->
            button.setOnClickListener {
                selectRatio(ratio)
            }
        }
    }

    /**
     * 设置旋转按钮监听器（添加防抖处理）
     */
    private fun setupRotateListeners() {
        // 顺时针旋转（防抖处理）
        btnRotateClockwise.setOnClickListener {
            if (!isTransforming) {
                rotateClockwise90()
            }
        }

        // 逆时针旋转（防抖处理）
        btnRotateCounterClockwise.setOnClickListener {
            if (!isTransforming) {
                rotateCounterClockwise90()
            }
        }

        // 180°旋转（防抖处理）
        btnRotate180.setOnClickListener {
            if (!isTransforming) {
                rotate180()
            }
        }

        // 水平翻转（防抖处理）
        btnFlipHorizontal.setOnClickListener {
            if (!isTransforming) {
                flipHorizontal()
            }
        }

        // 垂直翻转（防抖处理）
        btnFlipVertical.setOnClickListener {
            if (!isTransforming) {
                flipVertical()
            }
        }
    }

    /**
     * 切换到裁剪模式
     */
    private fun switchToCropMode() {
        isCropMode = true
        tvTitle.text = "编辑图片 - 裁剪"

        // 更新标签状态
        tabCrop.isSelected = true
        tabRotate.isSelected = false
        updateTabColors()

        // 显示裁剪界面，隐藏旋转界面
        layoutCrop.visibility = View.VISIBLE
        layoutRotate.visibility = View.GONE

        // 显示裁剪框
        cropOverlayView.setShowCropRect(true)

        // 显示还原按钮
        btnReset.visibility = View.VISIBLE
    }

    /**
     * 切换到旋转模式
     */
    private fun switchToRotateMode() {
        isCropMode = false
        tvTitle.text = "编辑图片 - 旋转"

        // 更新标签状态
        tabCrop.isSelected = false
        tabRotate.isSelected = true
        updateTabColors()

        // 显示旋转界面，隐藏裁剪界面
        layoutCrop.visibility = View.GONE
        layoutRotate.visibility = View.VISIBLE

        // 隐藏裁剪框
        cropOverlayView.setShowCropRect(false)

        // 显示还原按钮
        btnReset.visibility = View.VISIBLE

        // 更新状态文本
        updateStatusText()
    }

    /**
     * 更新标签颜色
     */
    private fun updateTabColors() {
        if (tabCrop.isSelected) {
            tabCrop.setTextColor(getColor(android.R.color.white))
            tabRotate.setTextColor(getColor(android.R.color.darker_gray))
        } else {
            tabCrop.setTextColor(getColor(android.R.color.darker_gray))
            tabRotate.setTextColor(getColor(android.R.color.white))
        }
    }

    /**
     * 选择裁剪比例
     */
    private fun selectRatio(ratio: Float) {
        val buttons = listOf(
            Pair(btnFree, 0f),
            Pair(btn1_1, 1f),
            Pair(btn4_3, 4f/3f),
            Pair(btn3_4, 3f/4f),
            Pair(btn16_9, 16f/9f),
            Pair(btn9_16, 9f/16f)
        )

        buttons.forEach { (button, buttonRatio) ->
            button.isSelected = (buttonRatio == ratio)
        }

        cropOverlayView.setCropRatio(ratio)
    }

    /**
     * 顺时针旋转90°（安全版本）
     */
    private fun rotateClockwise90() {
        if (isTransforming) return

        isTransforming = true
        try {
            currentRotation = (currentRotation - 90) % 360
            if (currentRotation < 0) currentRotation += 360

            safeApplyTransformations()
            updateStatusText()

            Toast.makeText(this, "顺时针旋转90°", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "旋转失败", Toast.LENGTH_SHORT).show()
        } finally {
            isTransforming = false
        }
    }

    /**
     * 逆时针旋转90°（安全版本）
     */
    private fun rotateCounterClockwise90() {
        if (isTransforming) return

        isTransforming = true
        try {
            currentRotation = (currentRotation + 90) % 360

            safeApplyTransformations()
            updateStatusText()

            Toast.makeText(this, "逆时针旋转90°", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "旋转失败", Toast.LENGTH_SHORT).show()
        } finally {
            isTransforming = false
        }
    }

    /**
     * 旋转180°（安全版本）
     */
    private fun rotate180() {
        if (isTransforming) return

        isTransforming = true
        try {
            currentRotation = (currentRotation + 180) % 360

            safeApplyTransformations()
            updateStatusText()

            Toast.makeText(this, "旋转180°", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "旋转失败", Toast.LENGTH_SHORT).show()
        } finally {
            isTransforming = false
        }
    }

    /**
     * 水平翻转（安全版本）
     */
    private fun flipHorizontal() {
        if (isTransforming) return

        isTransforming = true
        try {
            isHorizontalFlipped = !isHorizontalFlipped

            safeApplyTransformations()
            updateStatusText()

            val status = if (isHorizontalFlipped) "水平翻转" else "取消水平翻转"
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "翻转失败", Toast.LENGTH_SHORT).show()
        } finally {
            isTransforming = false
        }
    }

    /**
     * 垂直翻转（安全版本）
     */
    private fun flipVertical() {
        if (isTransforming) return

        isTransforming = true
        try {
            isVerticalFlipped = !isVerticalFlipped

            safeApplyTransformations()
            updateStatusText()

            val status = if (isVerticalFlipped) "垂直翻转" else "取消垂直翻转"
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "翻转失败", Toast.LENGTH_SHORT).show()
        } finally {
            isTransforming = false
        }
    }

    /**
     * 重置所有变换
     */
    private fun resetAllTransformations() {
        if (isTransforming) return

        isTransforming = true
        try {
            currentRotation = 0f
            isHorizontalFlipped = false
            isVerticalFlipped = false

            safeApplyTransformations()
            updateStatusText()

            Toast.makeText(this, "已重置所有变换", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "重置失败", Toast.LENGTH_SHORT).show()
        } finally {
            isTransforming = false
        }
    }

    /**
     * 重置到原始状态
     */
    private fun resetToOriginal() {
        if (isCropMode) {
            // 裁剪模式：重置裁剪框
            cropOverlayView.resetCropRect()
            selectRatio(0f)
            Toast.makeText(this, "裁剪框已重置", Toast.LENGTH_SHORT).show()
        } else {
            // 旋转模式：重置所有变换
            resetAllTransformations()
        }
    }

    /**
     * 更新状态文本
     */
    private fun updateStatusText() {
        val rotationText = when (currentRotation) {
            0f -> "0°"
            90f -> "90°"
            180f -> "180°"
            270f -> "270°"
            else -> "${currentRotation.toInt()}°"
        }

        val horizontalText = if (isHorizontalFlipped) "水平: 镜像" else "水平: 正常"
        val verticalText = if (isVerticalFlipped) "垂直: 镜像" else "垂直: 正常"

        tvRotateStatus.text = "状态: 旋转$rotationText | $horizontalText | $verticalText"
    }

    /**
     * 安全的应用变换（避免内存问题和并发问题）
     */
    private fun safeApplyTransformations() {
        if (originalBitmap == null) return

        try {
            // 重置变换矩阵
            transformMatrix.reset()

            // 计算图片中心点
            val centerX = originalBitmap!!.width / 2f
            val centerY = originalBitmap!!.height / 2f

            // 1. 平移至中心点
            transformMatrix.postTranslate(-centerX, -centerY)

            // 2. 应用旋转
            if (currentRotation != 0f) {
                transformMatrix.postRotate(currentRotation)
            }

            // 3. 应用翻转（镜像）
            var scaleX = 1f
            var scaleY = 1f

            if (isHorizontalFlipped) {
                scaleX = -1f  // 水平镜像
            }

            if (isVerticalFlipped) {
                scaleY = -1f  // 垂直镜像
            }

            // 注意：翻转需要在旋转之后应用，以达到正确的镜像效果
            if (scaleX != 1f || scaleY != 1f) {
                transformMatrix.postScale(scaleX, scaleY)
            }

            // 4. 平移回原位置
            transformMatrix.postTranslate(centerX, centerY)

            // 回收旧的Bitmap
            transformedBitmap?.recycle()

            // 创建变换后的Bitmap
            transformedBitmap = Bitmap.createBitmap(
                originalBitmap!!,
                0, 0,
                originalBitmap!!.width, originalBitmap!!.height,
                transformMatrix,
                true
            )

            // 显示变换后的图片
            runOnUiThread {
                updateImageDisplay()
            }

        } catch (e: OutOfMemoryError) {
            // 内存不足，尝试回收内存
            System.gc()
            Toast.makeText(this, "内存不足，请稍后再试", Toast.LENGTH_SHORT).show()
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * 更新图片显示
     */
    private fun updateImageDisplay() {
        if (transformedBitmap != null && !transformedBitmap!!.isRecycled) {
            cropImageView.setImageBitmap(transformedBitmap)
            cropImageView.resetZoom()
        }
    }

    /**
     * 执行裁剪操作
     */
    private fun performCrop() {
        if (transformedBitmap == null || transformedBitmap!!.isRecycled) {
            Toast.makeText(this, "图片未加载", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            var resultBitmap = transformedBitmap!!

            // 如果是在裁剪模式，应用裁剪
            if (isCropMode) {
                resultBitmap = applyCrop(resultBitmap)
            }

            // 使用ImageManager保存图片到用户图片目录
            val savedPath = ImageManager.saveUserImage(this, resultBitmap, "编辑后的图片")

            if (savedPath != null) {
                // 显示保存位置信息
                val file = File(savedPath)
                val fileSize = String.format("%.1f", file.length() / 1024.0 / 1024.0)

                val result = Intent().apply {
                    putExtra("cropped_image_path", savedPath)
                    putExtra("is_user_created", true) // 标记为用户创建的图片
                }
                setResult(RESULT_OK, result)

                Toast.makeText(this,
                    "保存成功！\n位置: ${file.parentFile?.name}\n大小: ${fileSize}MB",
                    Toast.LENGTH_LONG).show()

                finish()
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "处理失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 应用裁剪到Bitmap
     */
    private fun applyCrop(bitmap: Bitmap): Bitmap {
        val cropRect = cropOverlayView.getCropRect()
        val drawable = cropImageView.drawable ?: return bitmap

        val imageRect = getImageDisplayRect(drawable)
        if (imageRect.width() <= 0 || imageRect.height() <= 0) {
            return bitmap
        }

        val cropRectInImage = convertCropRectToImageCoordinates(cropRect, imageRect, bitmap)

        return Bitmap.createBitmap(
            bitmap,
            cropRectInImage.left.toInt(),
            cropRectInImage.top.toInt(),
            cropRectInImage.width().toInt(),
            cropRectInImage.height().toInt()
        )
    }

    /**
     * 获取图片显示区域
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
     * 转换裁剪框坐标
     */
    private fun convertCropRectToImageCoordinates(
        cropRectInOverlay: RectF,
        imageRect: RectF,
        bitmap: Bitmap
    ): RectF {
        val scaleX = bitmap.width.toFloat() / imageRect.width()
        val scaleY = bitmap.height.toFloat() / imageRect.height()

        val cropRectInImage = RectF(
            (cropRectInOverlay.left - imageRect.left) * scaleX,
            (cropRectInOverlay.top - imageRect.top) * scaleY,
            (cropRectInOverlay.right - imageRect.left) * scaleX,
            (cropRectInOverlay.bottom - imageRect.top) * scaleY
        )

        cropRectInImage.left = max(0f, cropRectInImage.left)
        cropRectInImage.top = max(0f, cropRectInImage.top)
        cropRectInImage.right = min(bitmap.width.toFloat(), cropRectInImage.right)
        cropRectInImage.bottom = min(bitmap.height.toFloat(), cropRectInImage.bottom)

        return cropRectInImage
    }

    /**
     * 保存图片到文件
     */
    private fun saveCroppedImage(bitmap: Bitmap): String? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "edited_${timeStamp}.jpg"

            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            storageDir?.mkdirs()

            val imageFile = File(storageDir, fileName)

            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
            }

            imageFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        /**
         * 启动NewCropActivity的静态方法
         */
        fun startForResult(activity: AppCompatActivity, photo: Photo) {
            val intent = Intent(activity, NewCropActivity::class.java).apply {
                // 传递图片数据
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

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        originalBitmap?.recycle()
        transformedBitmap?.recycle()
        originalBitmap = null
        transformedBitmap = null
    }
}