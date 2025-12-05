package com.example.app_sj

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 图片裁剪Activity - 手写裁剪功能
 */
class ImageCropActivity : AppCompatActivity() {

    // 视图组件
    private lateinit var ivCropImage: ZoomableImageView
    private lateinit var cropOverlay: CropOverlayView
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button

    // 比例按钮
    private lateinit var btnRatioFree: Button
    private lateinit var btnRatio1_1: Button
    private lateinit var btnRatio4_3: Button
    private lateinit var btnRatio16_9: Button
    private lateinit var btnRatio3_4: Button
    private lateinit var btnRatio9_16: Button

    // 图片数据
    private var imageBitmap: Bitmap? = null
    private var imagePath: String? = null
    private var resourceId: Int = 0
    private var isFromCamera: Boolean = false

    // 裁剪结果保存路径
    private var croppedImagePath: String? = null

    companion object {
        const val REQUEST_CROP = 100
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_RESOURCE_ID = "resource_id"
        const val EXTRA_IS_FROM_CAMERA = "is_from_camera"
        const val EXTRA_CROPPED_PATH = "cropped_path"

        /**
         * 启动裁剪Activity
         */
        fun startForResult(
            activity: Activity,
            imagePath: String? = null,
            resourceId: Int = 0,
            isFromCamera: Boolean = false
        ) {
            val intent = Intent(activity, ImageCropActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_PATH, imagePath)
                putExtra(EXTRA_RESOURCE_ID, resourceId)
                putExtra(EXTRA_IS_FROM_CAMERA, isFromCamera)
            }
            activity.startActivityForResult(intent, REQUEST_CROP)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_new)

        // 初始化视图
        initViews()

        /*// 加载图片数据
        loadImageData()*/

        // 设置事件监听
        setupListeners()

        // 延迟加载图片，确保视图已布局完成
        ivCropImage.post {
            loadImageData()
            // 默认选择自由比例
            setCropRatio(0f)
        }
    }

    /**
     * 初始化视图组件
     */
    private fun initViews() {
        ivCropImage = findViewById(R.id.tabCrop)//剪裁
        cropOverlay = findViewById(R.id.tabRotate)//旋转
        btnCancel = findViewById(R.id.btnCancel)//取消
        btnConfirm = findViewById(R.id.btnDone)//确认

        // 比例按钮
        btnRatioFree = findViewById(R.id.btnFree)//自由比例
        btnRatio1_1 = findViewById(R.id.btn1_1)//1：1
        btnRatio4_3 = findViewById(R.id.btn4_3)//4:3
        btnRatio16_9 = findViewById(R.id.btn3_4)//3:4
        btnRatio3_4 = findViewById(R.id.btn16_9)//16:9
        btnRatio9_16 = findViewById(R.id.btn9_16)//9:16
    }

    /**
     * 加载图片数据
     */
    private fun loadImageData() {
        // 从Intent获取图片数据
        imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        resourceId = intent.getIntExtra(EXTRA_RESOURCE_ID, 0)
        isFromCamera = intent.getBooleanExtra(EXTRA_IS_FROM_CAMERA, false)

        // 加载图片到ImageView
        if (isFromCamera && !imagePath.isNullOrEmpty()) {
            // 从文件路径加载
            Glide.with(this)
                .load(File(imagePath))
                .into(ivCropImage)

            // 同时加载Bitmap用于裁剪
            loadBitmapFromFile(imagePath!!)
        } else if (resourceId != 0) {
            // 从资源ID加载
            Glide.with(this)
                .load(resourceId)
                .into(ivCropImage)

            // 从资源加载Bitmap
            loadBitmapFromResource(resourceId)
        } else {
            Toast.makeText(this, "图片数据错误", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 从文件加载Bitmap
     */
    /*private fun loadBitmapFromFile(filePath: String) {
        try {
            val options = BitmapFactory.Options().apply {
                // 先只获取图片尺寸，不加载像素数据
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeFile(filePath, options)

            // 计算合适的采样率，避免内存溢出
            val imageWidth = options.outWidth
            val imageHeight = options.outHeight
            val scale = calculateInSampleSize(options, 1024, 1024)

            // 使用采样率加载Bitmap
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
                inPreferredConfig = Bitmap.Config.RGB_565  // 使用更少内存的配置
            }

            imageBitmap = BitmapFactory.decodeFile(filePath, loadOptions)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
            finish()
        }
    }*/
    private fun loadBitmapFromResource(resourceId: Int) {
        try {
            // 使用更安全的方式加载资源Bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeResource(resources, resourceId, options)

            // 计算采样率
            val imageWidth = options.outWidth
            val imageHeight = options.outHeight
            val scale = calculateInSampleSize(options, 1920, 1080)

            // 使用采样率加载Bitmap
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            imageBitmap = BitmapFactory.decodeResource(resources, resourceId, loadOptions)

            // 如果还是null，使用最简单的方式
            if (imageBitmap == null) {
                imageBitmap = BitmapFactory.decodeResource(resources, resourceId)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // 最后尝试：直接加载
            imageBitmap = BitmapFactory.decodeResource(resources, resourceId)
        }
    }

    private fun loadBitmapFromFile(filePath: String) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeFile(filePath, options)

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                throw Exception("图片文件损坏")
            }

            // 计算采样率
            val scale = calculateInSampleSize(options, 1920, 1080)

            // 使用采样率加载Bitmap
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            imageBitmap = BitmapFactory.decodeFile(filePath, loadOptions)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "图片加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 计算Bitmap采样率（避免内存溢出）
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val width = options.outWidth
        val height = options.outHeight
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // 计算最大的inSampleSize值，保持图片尺寸大于等于要求尺寸
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 设置事件监听器
     */
    private fun setupListeners() {
        // 取消按钮
        btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // 确定按钮（执行裁剪）
        btnConfirm.setOnClickListener {
            performCrop()
        }

        // 比例选择按钮
        btnRatioFree.setOnClickListener { setCropRatio(0f) }
        btnRatio1_1.setOnClickListener { setCropRatio(1f) }
        btnRatio4_3.setOnClickListener { setCropRatio(4f / 3f) }
        btnRatio16_9.setOnClickListener { setCropRatio(16f / 9f) }
        btnRatio3_4.setOnClickListener { setCropRatio(3f / 4f) }
        btnRatio9_16.setOnClickListener { setCropRatio(9f / 16f) }
    }

    /**
     * 设置裁剪比例
     */
    private fun setCropRatio(ratio: Float) {
        // 使用公开方法设置比例
        cropOverlay.setCropRatio(ratio)

        // 显示裁剪框（使用公开方法）
        cropOverlay.setShowCropRect(true)

        // 高亮当前选中的比例按钮
        updateRatioButtons(ratio)
    }

    /**
     * 更新比例按钮的高亮状态
     */
    private fun updateRatioButtons(selectedRatio: Float) {
        // 所有按钮默认状态
        val buttons = listOf(
            btnRatioFree to 0f,
            btnRatio1_1 to 1f,
            btnRatio4_3 to (4f / 3f),
            btnRatio16_9 to (16f / 9f),
            btnRatio3_4 to (3f / 4f),
            btnRatio9_16 to (9f / 16f)
        )


        buttons.forEach { (button, ratio) ->
            if (ratio == selectedRatio) {
                // 选中状态：白色背景，黑色文字
                button.setBackgroundColor(Color.WHITE)
                button.setTextColor(Color.BLACK)
            } else {
                // 未选中状态：透明背景，白色文字
                button.setBackgroundColor(Color.TRANSPARENT)
                button.setTextColor(Color.WHITE)
            }
        }
    }

    /**
     * 执行裁剪操作
     */
    private fun performCrop() {
        if (imageBitmap == null) {
            Toast.makeText(this, "图片未加载", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 1. 获取裁剪区域（相对于裁剪框视图的坐标）
            val cropRectInOverlay = cropOverlay.getCropRect()

            // 2. 将裁剪区域坐标转换为图片坐标
            val cropRectInImage = convertCropRectToImageCoordinates(cropRectInOverlay)

            // 3. 创建裁剪后的Bitmap
            val croppedBitmap = Bitmap.createBitmap(
                imageBitmap!!,
                cropRectInImage.left.toInt(),
                cropRectInImage.top.toInt(),
                cropRectInImage.width().toInt(),
                cropRectInImage.height().toInt()
            )

            // 4. 保存裁剪后的图片
            croppedImagePath = saveCroppedBitmap(croppedBitmap)

            if (croppedImagePath != null) {
                // 5. 返回结果
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_CROPPED_PATH, croppedImagePath)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "保存裁剪图片失败", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "裁剪失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 将裁剪框坐标转换为图片坐标
     */
    private fun convertCropRectToImageCoordinates(cropRectInOverlay: RectF): RectF {
        // 获取图片在ImageView中的显示信息
        val imageMatrix = ivCropImage.imageMatrix
        val imageRect = RectF(
            0f, 0f,
            imageBitmap!!.width.toFloat(),
            imageBitmap!!.height.toFloat()
        )
        imageMatrix.mapRect(imageRect)

        // 计算转换比例
        val scaleX = imageBitmap!!.width.toFloat() / imageRect.width()
        val scaleY = imageBitmap!!.height.toFloat() / imageRect.height()

        // 转换坐标
        return RectF(
            (cropRectInOverlay.left - imageRect.left) * scaleX,
            (cropRectInOverlay.top - imageRect.top) * scaleY,
            (cropRectInOverlay.right - imageRect.left) * scaleX,
            (cropRectInOverlay.bottom - imageRect.top) * scaleY
        )
    }

    /**
     * 保存裁剪后的Bitmap到文件
     */
    private fun saveCroppedBitmap(bitmap: Bitmap): String? {
        return try {
            // 创建时间戳文件名
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "CROPPED_${timeStamp}.jpg"

            // 保存到应用私有目录
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File(storageDir, fileName)

            // 压缩并保存Bitmap
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
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
        // 释放Bitmap内存
        imageBitmap?.recycle()
        imageBitmap = null
    }
}