package com.example.app_sj

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class TextOverlayView(context: Context) : View(context) {

    interface OnTextSelectedListener {
        fun onTextSelected(textOverlay: TextOverlayView)
        fun onTextDeleted(textOverlay: TextOverlayView)
    }

    // 重命名以避免与View父类方法冲突
    private var overlayText: String = "点击输入文字"
    private var textPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 24f * resources.displayMetrics.scaledDensity
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var fontType: String = "默认字体"
    private var textColor: Int = Color.BLACK
    private var textAlpha: Int = 255

    private var posX: Float = 0f
    private var posY: Float = 0f
    private var scale: Float = 1.0f
    private var rotation: Float = 0f
    private var width: Float = 200f
    private var height: Float = 100f

    private var isOverlaySelected: Boolean = false
    private var isDragging: Boolean = false
    private var isScaling: Boolean = false
    private var isRotating: Boolean = false

    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var lastTouchDistance: Float = 0f
    private var lastTouchAngle: Float = 0f

    private val deleteButtonRect = RectF()
    private val addButtonRect = RectF()
    private val buttonSize = 36f
    private val buttonPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }

    private val selectedBorderPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private var textSelectedListener: OnTextSelectedListener? = null
    private var textLayout: StaticLayout? = null

    init {
        val metrics = resources.displayMetrics
        posX = metrics.widthPixels / 2f - width / 2
        posY = metrics.heightPixels / 2f - height / 2

        isClickable = true
        updateTextLayout()
    }

    fun setOnTextSelectedListener(listener: OnTextSelectedListener) {
        this.textSelectedListener = listener
    }

    // 重命名方法以避免冲突
    fun setOverlayText(text: String) {
        this.overlayText = text
        updateTextLayout()
        invalidate()
    }

    fun getOverlayText(): String = overlayText

    fun setFontType(fontType: String) {
        this.fontType = fontType
        updateFont()
        updateTextLayout()
        invalidate()
    }

    fun getFontType(): String = fontType

    fun setTextSize(spSize: Float) {
        val pxSize = spSize * resources.displayMetrics.scaledDensity
        textPaint.textSize = pxSize
        updateTextLayout()
        invalidate()
    }

    fun getTextSize(): Float = textPaint.textSize / resources.displayMetrics.scaledDensity

    fun setTextColor(color: Int) {
        this.textColor = color
        textPaint.color = color
        invalidate()
    }

    fun getTextColor(): Int = textColor

    // 重命名以避免与View.getAlpha()冲突
    fun setTextAlpha(alpha: Int) {
        this.textAlpha = alpha.coerceIn(0, 255)
        textPaint.alpha = textAlpha
        invalidate()
    }

    // 重命名以避免与View.getAlpha()冲突
    fun getTextAlpha(): Int = textAlpha

    fun setPosition(x: Float, y: Float) {
        this.posX = x
        this.posY = y
        invalidate()
    }

    // 添加override修饰符
    override fun setSelected(selected: Boolean) {
        this.isOverlaySelected = selected
        super.setSelected(selected)
        invalidate()
    }

    private fun updateFont() {
        when (fontType) {
            "粗体" -> {
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.isFakeBoldText = true
            }
            "斜体" -> {
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            "等宽字体" -> {
                textPaint.typeface = Typeface.MONOSPACE
            }
            "手写体" -> {
                textPaint.typeface = Typeface.create("cursive", Typeface.NORMAL)
            }
            "艺术体" -> {
                textPaint.typeface = Typeface.create("serif", Typeface.NORMAL)
            }
            else -> {
                textPaint.typeface = Typeface.DEFAULT
                textPaint.isFakeBoldText = false
            }
        }
    }

    private fun updateTextLayout() {
        val textBounds = Rect()
        textPaint.getTextBounds(overlayText, 0, overlayText.length, textBounds)

        width = textBounds.width() + 40f
        height = textBounds.height() + 40f

        val availableWidth = (width - 20).toInt()
        textLayout = StaticLayout(
            overlayText, textPaint, availableWidth,
            Layout.Alignment.ALIGN_CENTER,
            1.0f, 0.0f, false
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.translate(posX, posY)
        canvas.scale(scale, scale, width / 2, height / 2)
        canvas.rotate(rotation, width / 2, height / 2)

        if (isOverlaySelected) {
            canvas.drawRoundRect(
                0f, 0f, width, height,
                8f, 8f, Paint().apply {
                    color = Color.parseColor("#40FFFFFF")
                    style = Paint.Style.FILL
                }
            )
        }

        if (isOverlaySelected) {
            canvas.drawRoundRect(
                0f, 0f, width, height,
                8f, 8f, selectedBorderPaint
            )

            drawControlButtons(canvas)
        } else {
            canvas.drawRoundRect(
                0f, 0f, width, height,
                8f, 8f, borderPaint
            )
        }

        drawText(canvas)
        canvas.restore()
    }

    private fun drawControlButtons(canvas: Canvas) {
        deleteButtonRect.set(
            -buttonSize / 2,
            -buttonSize / 2,
            buttonSize / 2,
            buttonSize / 2
        )

        addButtonRect.set(
            width - buttonSize / 2,
            -buttonSize / 2,
            width + buttonSize / 2,
            buttonSize / 2
        )

        canvas.drawRoundRect(deleteButtonRect, 4f, 4f, buttonPaint)
        canvas.drawText("×",
            deleteButtonRect.centerX() - 6f,
            deleteButtonRect.centerY() + 10f,
            Paint().apply {
                color = Color.BLACK
                textSize = 24f
                textAlign = Paint.Align.CENTER
            })

        canvas.drawRoundRect(addButtonRect, 4f, 4f, buttonPaint)
        canvas.drawText("+",
            addButtonRect.centerX() - 6f,
            addButtonRect.centerY() + 10f,
            Paint().apply {
                color = Color.BLACK
                textSize = 24f
                textAlign = Paint.Align.CENTER
            })
    }

    private fun drawText(canvas: Canvas) {
        canvas.save()

        val textX = (width - textPaint.measureText(overlayText)) / 2

        if (textLayout != null) {
            canvas.translate(10f, 20f)
            textLayout!!.draw(canvas)
        } else {
            canvas.drawText(overlayText, textX, height / 2 + 10f, textPaint)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        val localPoint = convertToLocalCoordinates(x, y)
        val localX = localPoint[0]
        val localY = localPoint[1]

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (isOverlaySelected) {
                    if (deleteButtonRect.contains(localX, localY)) {
                        textSelectedListener?.onTextDeleted(this)
                        return true
                    }

                    if (addButtonRect.contains(localX, localY)) {
                        performClick()
                        return true
                    }
                }

                if (!isOverlaySelected) {
                    isOverlaySelected = true
                    textSelectedListener?.onTextSelected(this)
                    invalidate()
                }

                isDragging = true
                lastTouchX = x
                lastTouchY = y
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isDragging = false
                    isScaling = true
                    isRotating = true

                    val x1 = event.getX(0)
                    val y1 = event.getY(0)
                    val x2 = event.getX(1)
                    val y2 = event.getY(1)

                    lastTouchDistance = calculateDistance(x1, y1, x2, y2)
                    lastTouchAngle = calculateAngle(x1, y1, x2, y2)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && event.pointerCount == 1) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY

                    posX += dx
                    posY += dy

                    lastTouchX = x
                    lastTouchY = y

                    invalidate()
                    return true
                }

                if ((isScaling || isRotating) && event.pointerCount == 2) {
                    val x1 = event.getX(0)
                    val y1 = event.getY(0)
                    val x2 = event.getX(1)
                    val y2 = event.getY(1)

                    val currentDistance = calculateDistance(x1, y1, x2, y2)
                    val currentAngle = calculateAngle(x1, y1, x2, y2)

                    if (isScaling) {
                        val scaleFactor = currentDistance / lastTouchDistance
                        scale *= scaleFactor
                        scale = scale.coerceIn(0.5f, 5.0f)
                        lastTouchDistance = currentDistance
                    }

                    if (isRotating) {
                        val angleDelta = currentAngle - lastTouchAngle
                        rotation += angleDelta
                        rotation %= 360f
                        lastTouchAngle = currentAngle
                    }

                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                isDragging = false
                isScaling = false
                isRotating = false
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun convertToLocalCoordinates(screenX: Float, screenY: Float): FloatArray {
        val matrix = Matrix()
        matrix.postTranslate(-posX, -posY)
        matrix.postRotate(-rotation, width / 2, height / 2)
        matrix.postScale(1/scale, 1/scale, width / 2, height / 2)

        val points = floatArrayOf(screenX, screenY)
        matrix.mapPoints(points)

        return points
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateAngle(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return atan2(dy, dx)
    }

    fun isTouchInView(x: Float, y: Float): Boolean {
        val localPoint = convertToLocalCoordinates(x, y)
        return localPoint[0] in 0f..width && localPoint[1] in 0f..height
    }

    fun drawTextOnCanvas(canvas: Canvas, imageWidth: Int, imageHeight: Int) {
        val paint = Paint(textPaint).apply {
            alpha = this@TextOverlayView.textAlpha
            color = this@TextOverlayView.textColor
            textSize = this@TextOverlayView.textPaint.textSize
            typeface = this@TextOverlayView.textPaint.typeface
            isAntiAlias = true
        }

        // 坐标转换
        val screenX = posX * imageWidth / resources.displayMetrics.widthPixels
        val screenY = posY * imageHeight / resources.displayMetrics.heightPixels

        Log.d("TextOverlay", "转换后位置: screenX=$screenX, screenY=$screenY")

        canvas.save()
        canvas.translate(screenX, screenY)
        canvas.scale(scale, scale)
        canvas.rotate(rotation)

        // 绘制文本 - 使用正确的基线
        val textBounds = Rect()
        paint.getTextBounds(overlayText, 0, overlayText.length, textBounds)
        val textY = -textBounds.top  // 让文本在原点正确显示

        canvas.drawText(overlayText, 0f, textY.toFloat(), paint)
        canvas.restore()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}