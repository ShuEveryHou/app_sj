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
import java.io.File


//这个文件基本上是实现扩展功能，代码量大
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

    private lateinit var panelBrightness: LinearLayout//明亮面板
    private lateinit var panelContrast: LinearLayout//对比度面板
    private lateinit var panelFilter: LinearLayout  //滤镜面板

    private lateinit var seekBarBrightness: SeekBar
    private lateinit var tvBrightnessValue: TextView
    private lateinit var seekBarContrast: SeekBar
    private lateinit var tvContrastValue: TextView

    // ========== 滤镜相关 ==========
    private lateinit var filterContainer: LinearLayout
    private lateinit var filterOriginal: LinearLayout
    private lateinit var filterBlackWhite: LinearLayout
    private lateinit var filterRetro: LinearLayout
    private lateinit var filterFresh: LinearLayout
    private lateinit var filterWarm: LinearLayout
    private lateinit var filterCold: LinearLayout

    // ========== 贴纸相关属性 ==========
    private lateinit var panelSticker: LinearLayout
    private lateinit var stickerContainer: LinearLayout
    private lateinit var btnBringForward: Button
    private lateinit var btnSendBackward: Button
    // 贴纸预览视图
    private lateinit var stickerBlack_cat: LinearLayout
    private lateinit var stickerCat_1: LinearLayout
    private lateinit var stickerCloud_: LinearLayout
    private lateinit var stickerCup_: LinearLayout
    private lateinit var stickerCute_text: LinearLayout
    private lateinit var stickerLucky_text: LinearLayout
    private lateinit var stickerMoon_: LinearLayout
    private lateinit var stickerSnack_: LinearLayout
    private lateinit var stickerStart_cute: LinearLayout
    private lateinit var stickerText_2: LinearLayout
    private lateinit var stickerText_3: LinearLayout
    private lateinit var stickerText_4: LinearLayout
    private lateinit var stickerWolf_: LinearLayout

    private var currentStickerView: StickerOverlayView? = null
    private val stickerViews = mutableListOf<StickerOverlayView>()

    // ========== 数据状态 ==========
    private var imagePath: String? = null
    private var resourceId: Int = 0
    private var isFromCamera: Boolean = false
    private var isUserCreated: Boolean = false
    private var originalBitmap: Bitmap? = null
    private var processedBitmap: Bitmap? = null

    // 调整参数
    private var brightnessValue = 0
    private var contrastValue = 100

    // 滤镜状态
    private var currentFilter: FilterUtils.FilterType = FilterUtils.FilterType.ORIGINAL
    private var filteredBitmap: Bitmap? = null
    private var isFilterMode: Boolean = false

    // 处理控制
    private var currentPanel: String = "none"
    private val handler = Handler(Looper.getMainLooper())
    private var processingTask: Runnable? = null
    private var isProcessing = false
    private var pendingBrightness = 0
    private var pendingContrast = 100

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
        btnCancel = findViewById(R.id.btnCancel)//取消
        btnSave = findViewById(R.id.btnSave)//保存

        btnBrightness = findViewById(R.id.btnBrightness)//明亮
        btnContrast = findViewById(R.id.btnContrast)//对比度
        btnFilter = findViewById(R.id.btnFilter)//滤镜
        btnSticker = findViewById(R.id.btnSticker)//贴纸
        btnMerge = findViewById(R.id.btnMerge)//拼接

        panelBrightness = findViewById(R.id.panelBrightness)//明亮
        panelContrast = findViewById(R.id.panelContrast)//对比度
        panelFilter = findViewById(R.id.panelFilter)//滤镜


        // 初始化滤镜容器和按钮
        filterContainer = findViewById(R.id.filterContainer)
        filterOriginal = findViewById(R.id.filterOriginal)
        filterBlackWhite = findViewById(R.id.filterBlackWhite)
        filterRetro = findViewById(R.id.filterRetro)
        filterFresh = findViewById(R.id.filterFresh)
        filterWarm = findViewById(R.id.filterWarm)
        filterCold = findViewById(R.id.filterCold)

        // 贴纸相关视图
        panelSticker = findViewById(R.id.panelSticker)
        stickerContainer = findViewById(R.id.stickerContainer)
        btnBringForward = findViewById(R.id.btnBringForward)
        btnSendBackward = findViewById(R.id.btnSendBackward)

        stickerBlack_cat  = findViewById(R.id.stickerBlack_cat)
        stickerCat_1    = findViewById(R.id.stickerCat_1)
        stickerCloud_   = findViewById(R.id.stickerCloud_)
        stickerCup_     = findViewById(R.id.stickerCup_)
        stickerCute_text    = findViewById(R.id.stickerCute_text)
        stickerLucky_text   = findViewById(R.id.stickerLucky_text)
        stickerMoon_    = findViewById(R.id.stickerMoon_)
        stickerSnack_   = findViewById(R.id.stickerSnack_)
        stickerStart_cute   = findViewById(R.id.stickerStart_cute)
        stickerText_2   = findViewById(R.id.stickerText_2)
        stickerText_3   = findViewById(R.id.stickerText_3)
        stickerText_4   = findViewById(R.id.stickerText_4)
        stickerWolf_    = findViewById(R.id.stickerWolf_)


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

        // 使用Glide加载图片
        if ((isFromCamera || isUserCreated) && !imagePath.isNullOrEmpty()) {
            Glide.with(this)
                .load(File(imagePath))
                .into(ivProcessed)

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

        val scale = calculateInSampleSize(options, 800, 800)

        val loadOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
            inPreferredConfig = Bitmap.Config.RGB_565
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

        btnCancel.setOnClickListener {//取消
            finish()
        }

        btnSave.setOnClickListener {//保存
            saveEnhancedImage()
        }

        btnBrightness.setOnClickListener {// 亮度按钮
            togglePanel("brightness")
        }

        btnContrast.setOnClickListener { // 对比度按钮
            togglePanel("contrast")
        }

        btnFilter.setOnClickListener { // 滤镜按钮
            togglePanel("filter")
        }

        btnSticker.setOnClickListener {//贴纸按钮
            togglePanel("sticker")
        }

        btnMerge.setOnClickListener {
            Toast.makeText(this, "拼接功能待实现", Toast.LENGTH_SHORT).show()
        }

        // 亮度滑块
        seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val brightness = progress - 100
                    tvBrightnessValue.text = brightness.toString()
                    pendingBrightness = brightness
                    scheduleImageProcessing()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                cancelPendingProcessing()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                brightnessValue = pendingBrightness
                processImageWithDelay(0)
            }
        })

        // 对比度滑块
        seekBarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val contrastRaw = progress - 50
                    val contrastPercent = 100 + contrastRaw
                    tvContrastValue.text = contrastRaw.toString()
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

        // 设置滤镜选择监听器
        setupFilterListeners()

        // 贴纸功能初始化
        setupStickerListeners()
    }

    //设置贴纸相关监听器
    private fun setupStickerListeners() {
        // 贴纸预览点击监听
        stickerBlack_cat.setOnClickListener { addStickerToImage(StickerManager.StickerType.BLACK_CAT) }
        stickerCat_1.setOnClickListener { addStickerToImage(StickerManager.StickerType.CAT_1) }
        stickerCloud_.setOnClickListener { addStickerToImage(StickerManager.StickerType.CLOUD_) }
        stickerCup_.setOnClickListener { addStickerToImage(StickerManager.StickerType.CUP_) }
        stickerCute_text.setOnClickListener { addStickerToImage(StickerManager.StickerType.CUTE_TEXT) }
        stickerLucky_text.setOnClickListener { addStickerToImage(StickerManager.StickerType.LUCKY_TEXT) }
        stickerMoon_.setOnClickListener { addStickerToImage(StickerManager.StickerType.MOON_) }
        stickerSnack_.setOnClickListener { addStickerToImage(StickerManager.StickerType.SNACK_) }
        stickerStart_cute.setOnClickListener { addStickerToImage(StickerManager.StickerType.START_CUTE) }
        stickerText_2.setOnClickListener { addStickerToImage(StickerManager.StickerType.TEXT_2) }
        stickerText_3.setOnClickListener { addStickerToImage(StickerManager.StickerType.TEXT_3) }
        stickerText_4.setOnClickListener { addStickerToImage(StickerManager.StickerType.TEXT_4) }
        stickerWolf_.setOnClickListener { addStickerToImage(StickerManager.StickerType.WOLF_) }

        // 层级按钮监听
        btnBringForward.setOnClickListener {
            currentStickerView?.bringForward()
            updateStickerLayerButtons()
        }

        btnSendBackward.setOnClickListener {
            currentStickerView?.sendBackward()
            updateStickerLayerButtons()
        }

        // 图片容器点击监听（用于取消选择贴纸）
        findViewById<FrameLayout>(R.id.imageContainer).setOnClickListener {
            if (currentPanel == "sticker") {
                currentStickerView?.setSelected(false)
                currentStickerView = null
                updateStickerLayerButtons()
            }
        }
    }

    //添加贴纸到图片
    private fun addStickerToImage(stickerType: StickerManager.StickerType) {
        val imageContainer = findViewById<FrameLayout>(R.id.imageContainer)

        // 加载贴纸位图
        val stickerBitmap = StickerManager.loadStickerBitmap(this, stickerType)
        if (stickerBitmap == null) {
            Toast.makeText(this, "贴纸加载失败", Toast.LENGTH_SHORT).show()
            return
        }

        // 创建贴纸视图
        val stickerView = StickerOverlayView(this).apply {
            setStickerBitmap(stickerBitmap)

            // 计算初始位置（居中）
            val containerWidth = imageContainer.width.toFloat()
            val containerHeight = imageContainer.height.toFloat()
            val stickerWidth = stickerBitmap.width.toFloat() * 0.5f
            val stickerHeight = stickerBitmap.height.toFloat() * 0.5f

            setPosition(
                containerWidth / 2 - stickerWidth / 2,
                containerHeight / 2 - stickerHeight / 2
            )

            setOnStickerActionListener(object : StickerOverlayView.OnStickerActionListener {
                override fun onStickerSelected(sticker: StickerOverlayView) {
                    // 取消之前选中的贴纸
                    currentStickerView?.setSelected(false)
                    currentStickerView = sticker
                    sticker.setSelected(true)
                    updateStickerLayerButtons()
                }

                override fun onStickerDeleted(sticker: StickerOverlayView) {
                    imageContainer.removeView(sticker)
                    stickerViews.remove(sticker)
                    if (currentStickerView == sticker) {
                        currentStickerView = null
                        updateStickerLayerButtons()
                    }
                }

                override fun onStickerBringForward(sticker: StickerOverlayView) {
                    sticker.bringForward()
                }

                override fun onStickerSendBackward(sticker: StickerOverlayView) {
                    sticker.sendBackward()
                }
            })
        }

        // 添加到容器
        imageContainer.addView(stickerView)
        stickerViews.add(stickerView)

        // 选中新添加的贴纸
        currentStickerView?.setSelected(false)
        currentStickerView = stickerView
        stickerView.setSelected(true)
        updateStickerLayerButtons()

        Toast.makeText(this, "已添加${StickerManager.getStickerName(stickerType)}贴纸", Toast.LENGTH_SHORT).show()
    }


    //更新贴纸层级按钮状态
    private fun updateStickerLayerButtons() {
        val hasSelectedSticker = currentStickerView != null
        btnBringForward.isEnabled = hasSelectedSticker
        btnSendBackward.isEnabled = hasSelectedSticker
    }


    //在画布上绘制贴纸（用于保存）
    private fun drawStickerOnCanvas(
        stickerView: StickerOverlayView,
        canvas: Canvas,
        imageWidth: Int,
        imageHeight: Int
    ) {
        val drawData = stickerView.getDrawData()
        drawData.bitmap?.let { bitmap ->
            // 获取图片容器
            val container = findViewById<FrameLayout>(R.id.imageContainer)
            val scaleX = imageWidth.toFloat() / container.width
            val scaleY = imageHeight.toFloat() / container.height

            // 保存画布状态
            canvas.save()

            // 应用变换
            canvas.translate(drawData.x * scaleX, drawData.y * scaleY)
            canvas.scale(drawData.scale, drawData.scale)
            canvas.rotate(drawData.rotation)

            // 从中心点绘制
            val left = -bitmap.width / 2f
            val top = -bitmap.height / 2f
            canvas.drawBitmap(bitmap, left, top, null)

            // 恢复画布状态
            canvas.restore()
        }
    }

    //设置滤镜选择监听器
    private fun setupFilterListeners() {

        filterOriginal.setOnClickListener {//原图
            selectFilter(FilterUtils.FilterType.ORIGINAL)
        }

        filterBlackWhite.setOnClickListener {//黑白滤镜
            selectFilter(FilterUtils.FilterType.BLACK_WHITE)
        }

        filterRetro.setOnClickListener {//复古色调
            selectFilter(FilterUtils.FilterType.RETRO)
        }

        filterFresh.setOnClickListener {//清新色调
            selectFilter(FilterUtils.FilterType.FRESH)
        }

        filterWarm.setOnClickListener {//暖色调
            selectFilter(FilterUtils.FilterType.WARM)
        }

        filterCold.setOnClickListener {//冷色调
            selectFilter(FilterUtils.FilterType.COLD)
        }
    }


    //切换面板显示
    private fun togglePanel(panelName: String) {

        // 如果点击的是当前已展开的面板，则关闭它
        if (currentPanel == panelName) {
            closeAllPanels()
            currentPanel = "none"
            return
        }

        // 先关闭所有面板
        closeAllPanels()

        // 然后展开指定的面板
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
            "filter" -> {
                if (panelFilter == null) {
                    return
                }

                panelFilter.visibility = View.VISIBLE
                btnFilter.isSelected = true


                // 加载滤镜预览
                loadFilterPreviews()
            }
            "sticker" -> {  // 添加贴纸面板处理
                panelSticker.visibility = View.VISIBLE
                btnSticker.isSelected = true

                // 更新层级按钮状态
                updateStickerLayerButtons()
            }
        }
    }


    //关闭所有面板
    private fun closeAllPanels() {

        if (panelBrightness != null) {
            panelBrightness.visibility = View.GONE
        }

        if (panelContrast != null) {
            panelContrast.visibility = View.GONE
        }

        if (panelFilter != null) {
            panelFilter.visibility = View.GONE
        }

        if (::panelSticker.isInitialized) {
            panelSticker.visibility = View.GONE
        }

        // 取消所有按钮的选中状态
        if (btnBrightness != null) btnBrightness.isSelected = false
        if (btnContrast != null) btnContrast.isSelected = false
        if (btnFilter != null) btnFilter.isSelected = false
        if (btnSticker != null) btnSticker.isSelected = false
        if (btnMerge != null) btnMerge.isSelected = false

        // 取消贴纸选中状态
        currentStickerView?.setSelected(false)
        currentStickerView = null
        updateStickerLayerButtons()

        currentPanel = "none"
        Log.d("PanelDebug", "当前面板重置为: $currentPanel")
    }

    private fun cancelPendingProcessing() {
        processingTask?.let { handler.removeCallbacks(it) }
        processingTask = null
    }

    private fun scheduleImageProcessing() {
        cancelPendingProcessing()

        processingTask = Runnable {
            brightnessValue = pendingBrightness
            contrastValue = pendingContrast
            processImageInBackground()
        }

        handler.postDelayed(processingTask!!, 150)
    }

    private fun processImageWithDelay(delay: Long = 150) {
        cancelPendingProcessing()

        processingTask = Runnable {
            processImageInBackground()
        }

        handler.postDelayed(processingTask!!, delay)
    }

    private fun processImageInBackground() {
        if (isProcessing || originalBitmap == null) return

        isProcessing = true

        Thread {
            try {
                val resultBitmap = adjustBrightnessAndContrast(
                    originalBitmap!!,
                    brightnessValue,
                    contrastValue
                )

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

    private fun adjustBrightnessAndContrast(src: Bitmap, brightness: Int, contrastPercent: Int): Bitmap {
        if (brightness == 0 && contrastPercent == 100) {
            return src.copy(Bitmap.Config.ARGB_8888, true)
        }

        val width = src.width
        val height = src.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        val srcPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val dstPixels = IntArray(width * height)

        val brightnessLut = IntArray(256)
        val contrastLut = IntArray(256)

        val brightnessAdjust = (brightness * 255 / 100).coerceIn(-255, 255)
        for (i in 0..255) {
            brightnessLut[i] = (i + brightnessAdjust).coerceIn(0, 255)
        }

        val contrastFactor = (contrastPercent - 100) / 100.0f
        for (i in 0..255) {
            val normalized = i / 255.0f
            val adjusted = ((normalized - 0.5f) * (1 + contrastFactor)) + 0.5f
            contrastLut[i] = (adjusted * 255).toInt().coerceIn(0, 255)
        }

        for (i in srcPixels.indices) {
            val pixel = srcPixels[i]

            val alpha = pixel shr 24 and 0xFF
            var red = pixel shr 16 and 0xFF
            var green = pixel shr 8 and 0xFF
            var blue = pixel and 0xFF

            red = contrastLut[red]
            green = contrastLut[green]
            blue = contrastLut[blue]

            red = brightnessLut[red].coerceIn(0, 255)
            green = brightnessLut[green].coerceIn(0, 255)
            blue = brightnessLut[blue].coerceIn(0, 255)

            dstPixels[i] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }

        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }


    //选择滤镜
    private fun selectFilter(filterType: FilterUtils.FilterType) {

        // 更新当前滤镜
        currentFilter = filterType

        // 更新选中状态
        updateFilterSelection()

        // 应用滤镜效果
        applyFilterToImage(filterType)
    }


    //更新滤镜选中状态
    private fun updateFilterSelection() {
        // 清除所有选中状态
        filterOriginal.isSelected = false
        filterBlackWhite.isSelected = false
        filterRetro.isSelected = false
        filterFresh.isSelected = false
        filterWarm.isSelected = false
        filterCold.isSelected = false

        // 设置当前选中状态
        when (currentFilter) {
            FilterUtils.FilterType.ORIGINAL -> filterOriginal.isSelected = true
            FilterUtils.FilterType.BLACK_WHITE -> filterBlackWhite.isSelected = true
            FilterUtils.FilterType.RETRO -> filterRetro.isSelected = true
            FilterUtils.FilterType.FRESH -> filterFresh.isSelected = true
            FilterUtils.FilterType.WARM -> filterWarm.isSelected = true
            FilterUtils.FilterType.COLD -> filterCold.isSelected = true
        }


    }


    //加载滤镜预览图
    private fun loadFilterPreviews() {
        if (originalBitmap == null) {
            return
        }


        Thread {
            try {
                val previewSize = 60

                val originalPreview = FilterUtils.createFilterPreview(
                    originalBitmap!!,
                    FilterUtils.FilterType.ORIGINAL,
                    previewSize
                )

                val bwPreview = FilterUtils.createFilterPreview(
                    originalBitmap!!,
                    FilterUtils.FilterType.BLACK_WHITE,
                    previewSize
                )

                val retroPreview = FilterUtils.createFilterPreview(
                    originalBitmap!!,
                    FilterUtils.FilterType.RETRO,
                    previewSize
                )

                val freshPreview = FilterUtils.createFilterPreview(
                    originalBitmap!!,
                    FilterUtils.FilterType.FRESH,
                    previewSize
                )

                val warmPreview = FilterUtils.createFilterPreview(
                    originalBitmap!!,
                    FilterUtils.FilterType.WARM,
                    previewSize
                )

                val coldPreview = FilterUtils.createFilterPreview(
                    originalBitmap!!,
                    FilterUtils.FilterType.COLD,
                    previewSize
                )

                runOnUiThread {
                    val originalImageView = filterOriginal.getChildAt(0) as ImageView
                    val bwImageView = filterBlackWhite.getChildAt(0) as ImageView
                    val retroImageView = filterRetro.getChildAt(0) as ImageView
                    val freshImageView = filterFresh.getChildAt(0) as ImageView
                    val warmImageView = filterWarm.getChildAt(0) as ImageView
                    val coldImageView = filterCold.getChildAt(0) as ImageView

                    originalImageView.setImageBitmap(originalPreview)
                    bwImageView.setImageBitmap(bwPreview)
                    retroImageView.setImageBitmap(retroPreview)
                    freshImageView.setImageBitmap(freshPreview)
                    warmImageView.setImageBitmap(warmPreview)
                    coldImageView.setImageBitmap(coldPreview)

                    updateFilterSelection()

                    Log.d("FilterDebug", "滤镜预览加载完成")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Filter", "加载滤镜预览失败: ${e.message}")
            }
        }.start()
    }


    //应用滤镜到图片
    private fun applyFilterToImage(filterType: FilterUtils.FilterType) {
        if (originalBitmap == null) {
            Log.d("FilterDebug", "原始图片为空，无法应用滤镜")
            return
        }

        isFilterMode = (filterType != FilterUtils.FilterType.ORIGINAL)

        Thread {
            try {
                Log.d("FilterDebug", "开始应用滤镜: $filterType")

                val startTime = System.currentTimeMillis()
                filteredBitmap = FilterUtils.applyFilter(originalBitmap!!, filterType)
                val endTime = System.currentTimeMillis()

                Log.d("FilterDebug", "滤镜应用完成，耗时: ${endTime - startTime}ms")

                runOnUiThread {
                    if (filteredBitmap != null) {
                        ivProcessed.setImageBitmap(filteredBitmap)
                        Toast.makeText(
                            this,
                            "已应用${getFilterName(filterType)}滤镜",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "内存不足，无法应用滤镜", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Filter", "应用滤镜失败: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "应用滤镜失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }


    //获取滤镜名称
    private fun getFilterName(filterType: FilterUtils.FilterType): String {
        return when (filterType) {
            FilterUtils.FilterType.ORIGINAL -> "原图"
            FilterUtils.FilterType.BLACK_WHITE -> "黑白"
            FilterUtils.FilterType.RETRO -> "复古"
            FilterUtils.FilterType.FRESH -> "清新"
            FilterUtils.FilterType.WARM -> "暖色调"
            FilterUtils.FilterType.COLD -> "冷色调"
        }
    }

    private fun updateDisplayValues() {
        tvBrightnessValue.text = brightnessValue.toString()
        val contrastRaw = (contrastValue - 100)
        tvContrastValue.text = contrastRaw.toString()
    }


    //保存综合变换后的图片
    private fun saveEnhancedImage() {
        // 获取基础图片
        val baseBitmap = if (isFilterMode && filteredBitmap != null) {
            filteredBitmap
        } else if (processedBitmap != null) {
            processedBitmap
        } else {
            originalBitmap
        }

        if (baseBitmap == null) {
            Toast.makeText(this, "图片未加载", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "正在保存图片...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val resultBitmap: Bitmap

                // 如果有贴纸，需要合成
                if (stickerViews.isNotEmpty()) {
                    resultBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(resultBitmap)

                    // 绘制基础图片
                    canvas.drawBitmap(baseBitmap, 0f, 0f, null)

                    // 绘制所有贴纸
                    for (stickerView in stickerViews) {
                        drawStickerOnCanvas(stickerView, canvas, baseBitmap.width, baseBitmap.height)
                    }
                } else {
                    // 没有贴纸，直接使用基础图片
                    resultBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
                }

                // 使用ImageManager保存
                val savedPath = ImageManager.saveUserImage(
                    this@ImageEnhanceActivity,
                    resultBitmap,
                    if (stickerViews.isNotEmpty()) {
                        if (isFilterMode) "贴纸+滤镜图片" else "贴纸图片"
                    } else {
                        if (isFilterMode) "滤镜图片" else "增强图片"
                    }
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

                // 回收Bitmap
                if (resultBitmap != baseBitmap) {
                    resultBitmap.recycle()
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
        cancelPendingProcessing()
        handler.removeCallbacksAndMessages(null)

        originalBitmap?.recycle()
        processedBitmap?.recycle()
        filteredBitmap?.recycle()
        originalBitmap = null
        processedBitmap = null
        filteredBitmap = null

        // 清理贴纸视图
        stickerViews.forEach {
            it.getDrawData().bitmap?.recycle()
        }
        stickerViews.clear()
        currentStickerView = null

        System.gc()
    }
}