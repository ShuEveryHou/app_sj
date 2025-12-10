package com.example.app_sj

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlin.math.*

//贴纸覆盖视图，能够拖动、缩放、旋转、删除
class StickerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnStickerActionListener {
        fun onStickerSelected(sticker: StickerOverlayView)
        fun onStickerDeleted(sticker: StickerOverlayView)
        fun onStickerBringForward(sticker: StickerOverlayView)
        fun onStickerSendBackward(sticker: StickerOverlayView)
    }

    // 贴纸位图
    private var stickerBitmap: Bitmap? = null

    // 变换参数
    private var currentScale: Float = 1.0f
    private var currentRotation: Float = 0f
    private var posX: Float = 0f
    private var posY: Float = 0f
    private var centerX: Float = 0f
    private var centerY: Float = 0f

    // 控制框
    private val borderRect = RectF()
    private val deleteButtonRect = RectF()  // 右上角删除按钮
    private val rotateButtonRect = RectF()  // 左上角旋转按钮
    private val scaleButtonRect = RectF()   // 右下角缩放按钮

    // 触摸状态
    private var isSelected: Boolean = false
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var lastTouchDistance: Float = 0f
    private var lastTouchAngle: Float = 0f

    private var currentAction: Action = Action.NONE

    // 按钮大小
    private val buttonSize = 36f

    // 监听器
    private var actionListener: OnStickerActionListener? = null

    enum class Action {
        NONE, DRAG, ROTATE, SCALE, DELETE
    }

    init {
        isClickable = true
    }

    fun setOnStickerActionListener(listener: OnStickerActionListener) {
        this.actionListener = listener
    }

    fun setStickerBitmap(bitmap: Bitmap) {
        stickerBitmap = bitmap
        updateCenter()
        updateControlRects()
        invalidate()
    }

    fun setPosition(x: Float, y: Float) {
        posX = x
        posY = y
        updateCenter()
        updateControlRects()
        invalidate()
    }


    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        this.isSelected = selected
        invalidate()
    }

    fun bringForward() {
        parent?.let {
            val parentView = parent as ViewGroup
            val currentIndex = parentView.indexOfChild(this)
            if (currentIndex < parentView.childCount - 1) {
                parentView.removeView(this)
                parentView.addView(this, currentIndex + 1)
            }
        }
    }

    fun sendBackward() {
        parent?.let {
            val parentView = parent as ViewGroup
            val currentIndex = parentView.indexOfChild(this)
            if (currentIndex > 0) {
                parentView.removeView(this)
                parentView.addView(this, currentIndex - 1)
            }
        }
    }

    private fun updateCenter() {
        stickerBitmap?.let { bitmap ->
            val scaledWidth = bitmap.width * currentScale
            val scaledHeight = bitmap.height * currentScale
            centerX = posX + scaledWidth / 2
            centerY = posY + scaledHeight / 2
        }
    }

    private fun updateControlRects() {
        stickerBitmap?.let { bitmap ->
            val scaledWidth = bitmap.width * currentScale
            val scaledHeight = bitmap.height * currentScale

            // 边框矩形
            borderRect.set(
                posX - 10f,
                posY - 10f,
                posX + scaledWidth + 10f,
                posY + scaledHeight + 10f
            )

            // 控制按钮
            val halfButton = buttonSize / 2

            // 删除按钮（右上角）
            deleteButtonRect.set(
                borderRect.right - buttonSize,
                borderRect.top - buttonSize,
                borderRect.right,
                borderRect.top
            )

            // 旋转按钮（左上角）
            rotateButtonRect.set(
                borderRect.left,
                borderRect.top - buttonSize,
                borderRect.left + buttonSize,
                borderRect.top
            )

            // 缩放按钮（右下角）
            scaleButtonRect.set(
                borderRect.right - buttonSize,
                borderRect.bottom - buttonSize,
                borderRect.right,
                borderRect.bottom
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        stickerBitmap?.let { bitmap ->
            // 保存画布状态
            canvas.save()

            // 应用变换：移动到中心点 → 缩放 → 旋转 → 移动到位置
            canvas.translate(centerX, centerY)
            canvas.scale(currentScale, currentScale)
            canvas.rotate(currentRotation)

            // 绘制贴纸（从中心点绘制）
            val left = -bitmap.width / 2f
            val top = -bitmap.height / 2f
            canvas.drawBitmap(bitmap, left, top, null)

            // 恢复画布状态
            canvas.restore()

            // 如果选中，绘制控制框和按钮
            if (isSelected) {
                drawControlBorder(canvas)
                drawControlButtons(canvas)
            }
        }
    }

    private fun drawControlBorder(canvas: Canvas) {
        val borderPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            isAntiAlias = true
        }

        // 绘制旋转后的边框
        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(currentRotation)

        val borderWidth = borderRect.width() - 20f
        val borderHeight = borderRect.height() - 20f
        canvas.drawRect(-borderWidth/2, -borderHeight/2, borderWidth/2, borderHeight/2, borderPaint)

        canvas.restore()
    }

    private fun drawControlButtons(canvas: Canvas) {
        // 删除按钮
        val deletePaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(deleteButtonRect.centerX(), deleteButtonRect.centerY(),
            buttonSize/2, deletePaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("×", deleteButtonRect.centerX(),
            deleteButtonRect.centerY() + 7, textPaint)

        // 旋转按钮
        val rotatePaint = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(rotateButtonRect.centerX(), rotateButtonRect.centerY(),
            buttonSize/2, rotatePaint)
        canvas.drawText("↻", rotateButtonRect.centerX(),
            rotateButtonRect.centerY() + 8, textPaint)

        // 缩放按钮
        val scalePaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(scaleButtonRect.centerX(), scaleButtonRect.centerY(),
            buttonSize/2, scalePaint)
        canvas.drawText("□", scaleButtonRect.centerX(),
            scaleButtonRect.centerY() + 8, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isSelected) {
            // 未选中的贴纸，点击时选中
            if (event.action == MotionEvent.ACTION_DOWN) {
                isSelected = true
                actionListener?.onStickerSelected(this)
                invalidate()
                return true
            }
            return false
        }

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 检查点击位置
                currentAction = getActionAtPoint(x, y)

                when (currentAction) {
                    Action.DELETE -> {
                        actionListener?.onStickerDeleted(this)
                        return true
                    }
                    Action.DRAG, Action.ROTATE, Action.SCALE -> {
                        lastTouchX = x
                        lastTouchY = y
                        return true
                    }
                    else -> {
                        // 点击贴纸内部，开始拖动
                        if (isPointInSticker(x, y)) {
                            currentAction = Action.DRAG
                            lastTouchX = x
                            lastTouchY = y
                            return true
                        }
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {

                when (currentAction) {
                    Action.DRAG -> {
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        posX += dx
                        posY += dy
                        updateCenter()
                        updateControlRects()
                        lastTouchX = x
                        lastTouchY = y
                        invalidate()
                    }
                    Action.ROTATE -> {
                        val angle = atan2(y - centerY, x - centerX) * 180 / PI.toFloat()
                        currentRotation = angle
                        invalidate()
                    }
                    Action.SCALE -> {
                        val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
                        val originalDistance = sqrt(
                            (stickerBitmap?.width ?: 1).toFloat().pow(2) +
                                    (stickerBitmap?.height ?: 1).toFloat().pow(2)
                        ) / 2
                        currentScale = (distance / originalDistance).coerceIn(0.2f, 5f)
                        updateControlRects()
                        invalidate()
                    }
                    Action.DELETE -> {
                        // 删除操作不需要处理MOVE事件
                        return true
                    }
                    Action.NONE -> {
                        // 无操作
                        return true
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                currentAction = Action.NONE
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun getActionAtPoint(x: Float, y: Float): Action {
        return when {
            deleteButtonRect.contains(x, y) -> Action.DELETE
            rotateButtonRect.contains(x, y) -> Action.ROTATE
            scaleButtonRect.contains(x, y) -> Action.SCALE
            isPointInSticker(x, y) -> Action.DRAG
            else -> Action.NONE
        }
    }

    private fun isPointInSticker(x: Float, y: Float): Boolean {
        // 将点转换到贴纸的局部坐标系
        val matrix = Matrix()
        matrix.postRotate(-currentRotation, centerX, centerY)
        val points = floatArrayOf(x, y)
        matrix.mapPoints(points)

        val transformedX = points[0]
        val transformedY = points[1]

        return transformedX >= borderRect.left && transformedX <= borderRect.right &&
                transformedY >= borderRect.top && transformedY <= borderRect.bottom
    }

    //获取贴纸的绘制数据，用于保存图片
    fun getDrawData(): StickerDrawData {
        return StickerDrawData(
            bitmap = stickerBitmap,
            x = centerX,
            y = centerY,
            scale = currentScale,
            rotation = currentRotation
        )
    }

    data class StickerDrawData(
        val bitmap: Bitmap?,
        val x: Float,
        val y: Float,
        val scale: Float,
        val rotation: Float
    )
}