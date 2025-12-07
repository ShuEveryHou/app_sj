package com.example.app_sj

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import java.io.File

class TextEditActivity : AppCompatActivity() {

    // ========== 视图组件 ==========
    private lateinit var ivBackground: ImageView
    private lateinit var textOverlayContainer: FrameLayout
    private lateinit var etTextInput: EditText//文字输入框
    private lateinit var btnFont: Button //字体按钮
    private lateinit var btnSize: Button//字号按钮
    private lateinit var btnColor: Button//颜色按钮
    private lateinit var btnAlpha: Button//透明度按钮
    private lateinit var btnAddText: Button//添加文字框
    private lateinit var btnSave: Button//文字保存按钮
    private lateinit var btnCancel: Button//文字取消按钮
    private lateinit var tvHint: TextView

    // 控制面板
    private lateinit var panelAlpha: LinearLayout
    private lateinit var panelFont: LinearLayout
    private lateinit var panelSize: LinearLayout//字号控制
    private lateinit var panelColor: LinearLayout//颜色控制面板

    // 透明度面板
    private lateinit var seekBarAlpha: SeekBar
    private lateinit var tvAlphaValue: TextView

    // 字号面板
    private lateinit var seekBarSize: SeekBar
    private lateinit var tvSizeValue: TextView

    // 字体面板
    private lateinit var btnFont1: Button
    private lateinit var btnFont2: Button
    private lateinit var btnFont3: Button

    // 颜色面板
    private lateinit var seekBarRed: SeekBar
    private lateinit var seekBarGreen: SeekBar
    private lateinit var seekBarBlue: SeekBar
    private lateinit var tvRedValue: TextView
    private lateinit var tvGreenValue: TextView
    private lateinit var tvBlueValue: TextView
    private lateinit var colorPreview: View
    private lateinit var tvHexColor: TextView

    // ========== 数据状态 ==========
    private var imagePath: String? = null
    private var resourceId: Int = 0
    private var isFromCamera: Boolean = false
    private var originalBitmap: Bitmap? = null

    private var currentTextOverlay: TextOverlayView? = null
    private val textOverlays = mutableListOf<TextOverlayView>()

    // 当前设置
    private var currentFont = "默认字体"
    private var currentSize = 24f
    private var currentColor = Color.BLACK
    private var currentAlpha = 255  // 50%-100% (128-255)

    private var currentPanel: String = "none" // 当前展开的面板

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        fun startForResult(activity: AppCompatActivity, photo: Photo) {
            val intent = Intent(activity, TextEditActivity::class.java).apply {
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

        try {
            Log.d("TextEdit", "开始创建Activity")

            // 1. 检查布局文件是否存在
            val layoutId = R.layout.activity_text_edit
            Log.d("TextEdit", "布局ID: $layoutId")

            // 2. 设置布局
            setContentView(layoutId)
            Log.d("TextEdit", "布局设置成功")

            // 3. 分步骤初始化
            initViews()
            Log.d("TextEdit", "视图初始化成功")

            // 4. 分步骤设置监听器
            setupListeners()
            Log.d("TextEdit", "监听器设置成功")

            // 5. 加载数据
            loadImageData()
            Log.d("TextEdit", "数据加载成功")

            // 6. 添加延迟操作
            ivBackground.postDelayed({
                addDefaultTextOverlay()
                Log.d("TextEdit", "默认文本框添加成功")
            }, 300)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TextEdit", "详细错误: ${e.message}")
            Log.e("TextEdit", "错误堆栈: ${e.stackTraceToString()}")
            Toast.makeText(this, "创建界面失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initViews() {
        ivBackground = findViewById(R.id.ivBackground)
        textOverlayContainer = findViewById(R.id.textOverlayContainer)
        etTextInput = findViewById(R.id.etTextInput)
        btnFont = findViewById(R.id.btnFont)
        btnSize = findViewById(R.id.btnSize)
        btnColor = findViewById(R.id.btnColor)
        btnAlpha = findViewById(R.id.btnAlpha)
        btnAddText = findViewById(R.id.btnAddText)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        tvHint = findViewById(R.id.tvHint)

        // 面板
        panelAlpha = findViewById(R.id.panelAlpha)
        panelFont = findViewById(R.id.panelFont)
        panelSize = findViewById(R.id.panelSize)
        panelColor = findViewById(R.id.panelColor)

        // 透明度面板
        seekBarAlpha = findViewById(R.id.seekBarAlpha)
        tvAlphaValue = findViewById(R.id.tvAlphaValue)

        // 字号面板
        seekBarSize = findViewById(R.id.seekBarSize)
        tvSizeValue = findViewById(R.id.tvSizeValue)

        // 字体面板
        btnFont1 = findViewById(R.id.btnFont1)
        btnFont2 = findViewById(R.id.btnFont2)
        btnFont3 = findViewById(R.id.btnFont3)

        // 颜色面板
        seekBarRed = findViewById(R.id.seekBarRed)
        seekBarGreen = findViewById(R.id.seekBarGreen)
        seekBarBlue = findViewById(R.id.seekBarBlue)
        tvRedValue = findViewById(R.id.tvRedValue)
        tvGreenValue = findViewById(R.id.tvGreenValue)
        tvBlueValue = findViewById(R.id.tvBlueValue)
        colorPreview = findViewById(R.id.colorPreview)
        tvHexColor = findViewById(R.id.tvHexColor)
    }

    private fun setupPanels() {
        // 初始化滑块值
        seekBarAlpha.progress = 50  // 100%
        seekBarSize.progress = 12   // 24sp

        // 初始化颜色值
        updateColorFromCurrent()

        // 设置字体按钮初始状态
        updateFontButtons()
    }

    private fun loadImageData() {
        imagePath = intent.getStringExtra("photo_file_path")
        resourceId = intent.getIntExtra("photo_resource_id", 0)
        isFromCamera = intent.getBooleanExtra("is_from_camera", false)

        if (isFromCamera && !imagePath.isNullOrEmpty()) {
            Glide.with(this)
                .load(File(imagePath))
                .into(ivBackground)

            scope.launch(Dispatchers.IO) {
                loadBitmapFromFile(imagePath!!)
            }
        } else if (resourceId != 0) {
            Glide.with(this)
                .load(resourceId)
                .into(ivBackground)

            scope.launch(Dispatchers.IO) {
                loadBitmapFromResource(resourceId)
            }
        } else {
            Toast.makeText(this, "图片数据错误", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadBitmapFromFile(filePath: String) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            val scale = calculateInSampleSize(options, 1920, 1080)

            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            originalBitmap = BitmapFactory.decodeFile(filePath, loadOptions)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadBitmapFromResource(resId: Int) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(resources, resId, options)

            val scale = calculateInSampleSize(options, 1920, 1080)

            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            originalBitmap = BitmapFactory.decodeResource(resources, resId, loadOptions)

        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    private fun setupFunctionButtonListeners() {
        // 透明度按钮
        btnAlpha.setOnClickListener {
            if (currentTextOverlay == null) {
                Toast.makeText(this, "请先选择一个文本框", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            togglePanel("alpha")
        }

        // 字体按钮
        btnFont.setOnClickListener {
            if (currentTextOverlay == null) {
                Toast.makeText(this, "请先选择一个文本框", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            togglePanel("font")
        }

        // 字号按钮
        btnSize.setOnClickListener {
            if (currentTextOverlay == null) {
                Toast.makeText(this, "请先选择一个文本框", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            togglePanel("size")
        }

        // 颜色按钮
        btnColor.setOnClickListener {
            if (currentTextOverlay == null) {
                Toast.makeText(this, "请先选择一个文本框", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            togglePanel("color")
        }
    }

    private fun setupListeners() {
        // 取消按钮
        btnCancel.setOnClickListener {
            finish()
        }

        // 保存按钮
        btnSave.setOnClickListener {
            saveImageWithText()
        }

        // 添加文本框按钮
        btnAddText.setOnClickListener {
            addTextOverlay()
        }

        // 设置功能按钮监听
        setupFunctionButtonListeners()

        // 文字输入监听
        etTextInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                if (currentTextOverlay != null) {
                    currentTextOverlay?.setOverlayText(text)
                    currentTextOverlay?.invalidate()
                }
            }
        })

        // 设置面板内控件的监听器
        setupPanelControls()
    }

    /**
     * 设置面板内控件的监听器
     */
    private fun setupPanelControls() {
        // 透明度滑块
        seekBarAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && currentTextOverlay != null) {
                    val alphaPercent = progress + 50  // 50-100
                    currentAlpha = (alphaPercent * 255 / 100).coerceIn(128, 255)
                    tvAlphaValue.text = "${alphaPercent}%"
                    currentTextOverlay?.setTextAlpha(currentAlpha)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 字号滑块
        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && currentTextOverlay != null) {
                    currentSize = (progress + 12).toFloat()
                    tvSizeValue.text = "${currentSize.toInt()}sp"
                    currentTextOverlay?.setTextSize(currentSize)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 字体按钮
        btnFont1.setOnClickListener {
            if (currentTextOverlay != null) {
                selectFont("默认字体")
            }
        }
        btnFont2.setOnClickListener {
            if (currentTextOverlay != null) {
                selectFont("粗体")
            }
        }
        btnFont3.setOnClickListener {
            if (currentTextOverlay != null) {
                selectFont("艺术体")
            }
        }

        // 颜色滑块
        val colorSeekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && currentTextOverlay != null) {
                    updateColorFromSliders()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekBarRed.setOnSeekBarChangeListener(colorSeekBarListener)
        seekBarGreen.setOnSeekBarChangeListener(colorSeekBarListener)
        seekBarBlue.setOnSeekBarChangeListener(colorSeekBarListener)
    }
    /**
     * 切换面板显示
     */
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
            "alpha" -> {
                panelAlpha.visibility = View.VISIBLE
                btnAlpha.isSelected = true
            }
            "font" -> {
                panelFont.visibility = View.VISIBLE
                btnFont.isSelected = true
            }
            "size" -> {
                panelSize.visibility = View.VISIBLE
                btnSize.isSelected = true
            }
            "color" -> {
                panelColor.visibility = View.VISIBLE
                btnColor.isSelected = true
            }
        }
    }
    /**
     * 关闭所有面板
     */
    private fun closeAllPanels() {
        // 隐藏所有面板
        panelAlpha.visibility = View.GONE
        panelFont.visibility = View.GONE
        panelSize.visibility = View.GONE
        panelColor.visibility = View.GONE

        // 取消所有按钮的选中状态
        btnAlpha.isSelected = false
        btnFont.isSelected = false
        btnSize.isSelected = false
        btnColor.isSelected = false

        currentPanel = "none"
    }

    /**
     * 选择字体
     */
    private fun selectFont(fontName: String) {
        currentFont = fontName
        updateFontButtons()
        currentTextOverlay?.setFontType(currentFont)
    }

    /**
     * 更新字体按钮选中状态
     */
    private fun updateFontButtons() {
        btnFont1.isSelected = (currentFont == "默认字体")
        btnFont2.isSelected = (currentFont == "粗体")
        btnFont3.isSelected = (currentFont == "艺术体")
    }

    /**
     * 从滑块更新颜色
     */
    private fun updateColorFromSliders() {
        val red = seekBarRed.progress
        val green = seekBarGreen.progress
        val blue = seekBarBlue.progress

        tvRedValue.text = red.toString()
        tvGreenValue.text = green.toString()
        tvBlueValue.text = blue.toString()

        currentColor = Color.rgb(red, green, blue)
        updateColorPreview()

        currentTextOverlay?.setTextColor(currentColor)
    }

    /**
     * 从当前颜色更新滑块
     */
    private fun updateColorFromCurrent() {
        val red = Color.red(currentColor)
        val green = Color.green(currentColor)
        val blue = Color.blue(currentColor)

        seekBarRed.progress = red
        seekBarGreen.progress = green
        seekBarBlue.progress = blue

        tvRedValue.text = red.toString()
        tvGreenValue.text = green.toString()
        tvBlueValue.text = blue.toString()

        updateColorPreview()
    }

    /**
     * 更新颜色预览
     */
    private fun updateColorPreview() {
        colorPreview.setBackgroundColor(currentColor)
        tvHexColor.text = String.format("#%08X", 0xFF000000.toInt() or currentColor)
    }

    private fun addDefaultTextOverlay() {
        val centerX = ivBackground.width / 2f - 100
        val centerY = ivBackground.height / 2f - 50
        addTextOverlay("点击输入文字", centerX, centerY)
    }

    private fun addTextOverlay(text: String = "新文字", x: Float = 0f, y: Float = 0f) {
        val textOverlay = TextOverlayView(this).apply {
            setOverlayText(text)
            setTextSize(currentSize)
            setTextColor(currentColor)
            setTextAlpha(currentAlpha)
            setFontType(currentFont)

            if (x != 0f && y != 0f) {
                setPosition(x, y)
            } else {
                // 随机位置
                val randomX = (ivBackground.width * 0.2).toFloat() +
                        (Math.random() * ivBackground.width * 0.6).toFloat()
                val randomY = (ivBackground.height * 0.2).toFloat() +
                        (Math.random() * ivBackground.height * 0.6).toFloat()
                setPosition(randomX, randomY)
            }

            setOnTextSelectedListener(object : TextOverlayView.OnTextSelectedListener {
                override fun onTextSelected(textOverlay: TextOverlayView) {
                    selectTextOverlay(textOverlay)
                }

                override fun onTextDeleted(textOverlay: TextOverlayView) {
                    deleteTextOverlay(textOverlay)
                }
            })
        }

        textOverlayContainer.addView(textOverlay)
        textOverlays.add(textOverlay)
        selectTextOverlay(textOverlay)
    }

    private fun selectTextOverlay(textOverlay: TextOverlayView) {
        // 取消之前选中状态
        currentTextOverlay?.setSelected(false)

        // 设置新的选中
        currentTextOverlay = textOverlay
        textOverlay.setSelected(true)

        // 更新输入框显示当前文字
        etTextInput.setText(textOverlay.getOverlayText())

        // 同步当前设置
        updateCurrentSettingsFromOverlay(textOverlay)

        // 关闭所有面板（重新选中时清空面板）
        closeAllPanels()
    }

    /**
     * 从文本框更新当前设置
     */
    private fun updateCurrentSettingsFromOverlay(textOverlay: TextOverlayView) {
        currentFont = textOverlay.getFontType()
        currentSize = textOverlay.getTextSize()
        currentColor = textOverlay.getTextColor()
        currentAlpha = textOverlay.getTextAlpha()

        // 更新UI控件
        updateFontButtons()
        updateColorFromCurrent()

        // 更新滑块位置
        updateSlidersFromCurrent()
    }
    /**
     * 根据当前设置更新滑块
     */
    private fun updateSlidersFromCurrent() {
        // 透明度：50%-100%映射到128-255
        val alphaPercent = (currentAlpha * 100 / 255).coerceIn(50, 100)
        seekBarAlpha.progress = alphaPercent - 50
        tvAlphaValue.text = "${alphaPercent}%"

        // 字号：12-36sp
        seekBarSize.progress = (currentSize - 12).toInt()
        tvSizeValue.text = "${currentSize.toInt()}sp"
    }

    private fun deleteTextOverlay(textOverlay: TextOverlayView) {
        textOverlayContainer.removeView(textOverlay)
        textOverlays.remove(textOverlay)

        if (currentTextOverlay == textOverlay) {
            currentTextOverlay = null
            etTextInput.setText("")
            closeAllPanels()
        }

        if (textOverlays.isEmpty()) {
            addDefaultTextOverlay()
        }
    }

    private fun showHintTemporarily() {
        tvHint.visibility = View.VISIBLE
        tvHint.alpha = 1.0f

        tvHint.postDelayed({
            tvHint.animate()
                .alpha(0f)
                .setDuration(1000)
                .withEndAction {
                    tvHint.visibility = View.GONE
                }
                .start()
        }, 3000)
    }

    private fun saveImageWithText() {
        if (originalBitmap == null) {
            Toast.makeText(this, "图片未加载完成", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "正在生成图片...", Toast.LENGTH_SHORT).show()

        scope.launch(Dispatchers.IO) {
            try {
                val resultBitmap = Bitmap.createBitmap(
                    originalBitmap!!.width, originalBitmap!!.height,
                    Bitmap.Config.ARGB_8888
                )

                val canvas = Canvas(resultBitmap)
                canvas.drawBitmap(originalBitmap!!, 0f, 0f, null)

                for (textOverlay in textOverlays) {
                    textOverlay.drawTextOnCanvas(canvas, originalBitmap!!.width, originalBitmap!!.height)
                }

                val savedPath = ImageManager.saveUserImage(
                    this@TextEditActivity,
                    resultBitmap,
                    "带文字图片"
                )

                withContext(Dispatchers.Main) {
                    if (savedPath != null) {
                        Toast.makeText(this@TextEditActivity, "保存成功！", Toast.LENGTH_SHORT).show()

                        val result = Intent().apply {
                            putExtra("saved_image_path", savedPath)
                        }
                        setResult(RESULT_OK, result)
                        finish()
                    } else {
                        Toast.makeText(this@TextEditActivity, "保存失败", Toast.LENGTH_SHORT).show()
                    }
                }

                resultBitmap.recycle()

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TextEditActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val x = ev.x.toInt()
            val y = ev.y.toInt()

            // 检查是否点击在控制面板上
            val controlPanel = findViewById<LinearLayout>(R.id.controlPanel)
            val panelRect = Rect()
            controlPanel.getHitRect(panelRect)

            val isInControlPanel = panelRect.contains(x, y)

            // 检查是否点击在文字覆盖层上
            var hitTextOverlay = false
            for (textOverlay in textOverlays) {
                val overlayRect = Rect()
                textOverlay.getHitRect(overlayRect)

                if (overlayRect.contains(x, y)) {
                    hitTextOverlay = true
                    break
                }
            }

            // 如果既没有点击控制面板也没有点击文字覆盖层
            if (!isInControlPanel && !hitTextOverlay) {
                // 取消选中文本框
                currentTextOverlay?.setSelected(false)
                currentTextOverlay = null
                etTextInput.setText("")

                // 关闭所有面板
                closeAllPanels()
            }
        }

        return super.dispatchTouchEvent(ev)
    }
    override fun onDestroy() {
        super.onDestroy()
        originalBitmap?.recycle()
        scope.cancel()
    }
}