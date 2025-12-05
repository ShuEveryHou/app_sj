package com.example.app_sj

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max

/**
 * 裁剪框视图 - 用于显示裁剪区域并处理用户交互
 * 这是一个独立的裁剪框覆盖层，不包含图片显示功能
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ========== 接口定义 ==========
    interface OnCropChangeListener {
        fun onCropChanged(cropRect: RectF)
    }

    // ========== 绘制相关 ==========
    // 裁剪框的Paint（白色边框）
    private val cropBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    // 网格线的Paint（白色虚线）
    private val gridLinePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        isAntiAlias = true
    }

    // 遮罩层的Paint（半透明黑色）
    private val maskPaint = Paint().apply {
        color = Color.parseColor("#80000000")  // 半透明黑色
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // 手柄的Paint（白色方块）
    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // 手柄边框的Paint
    private val handleBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    // ========== 状态变量 ==========
    // 裁剪框的矩形区域
    private val cropRect = RectF()

    // 触摸点相关变量
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var dragMode = DRAG_NONE

    // 裁剪比例（0表示自由比例）
    private var currentRatio: Float = 0f

    // 最小裁剪尺寸
    private val minCropSize = 100f

    // 监听器
    private var cropChangeListener: OnCropChangeListener? = null

    // 是否显示裁剪框
    private var showCropRect: Boolean = false

    // ========== 常量定义 ==========
    companion object {
        // 拖拽模式
        private const val DRAG_NONE = 0
        private const val DRAG_MOVE = 1
        private const val DRAG_LEFT = 2
        private const val DRAG_TOP = 3
        private const val DRAG_RIGHT = 4
        private const val DRAG_BOTTOM = 5
        private const val DRAG_TOP_LEFT = 6
        private const val DRAG_TOP_RIGHT = 7
        private const val DRAG_BOTTOM_LEFT = 8
        private const val DRAG_BOTTOM_RIGHT = 9

        // 触摸容差和手柄大小
        private const val TOUCH_TOLERANCE = 30f
        private const val HANDLE_SIZE = 24f
    }

    // ========== 公开方法 ==========

    /**
     * 设置裁剪比例
     * @param ratio 比例值，0表示自由比例
     */
    fun setCropRatio(ratio: Float) {
        currentRatio = ratio
        if (width > 0 && height > 0) {
            createInitialCropRect()
            invalidate()
            notifyCropChanged()
        }
    }

    /**
     * 重置裁剪框到默认位置和大小
     */
    fun resetCropRect() {
        if (width > 0 && height > 0) {
            createDefaultCropRect()
            invalidate()
            notifyCropChanged()
        }
    }

    /**
     * 设置裁剪框变化监听器
     */
    fun setOnCropChangeListener(listener: OnCropChangeListener) {
        this.cropChangeListener = listener
    }

    /**
     * 获取当前裁剪框
     */
    fun getCropRect(): RectF {
        return RectF(cropRect)
    }

    /**
     * 设置裁剪框（用于从外部恢复状态）
     */
    fun setCropRect(rect: RectF) {
        cropRect.set(rect)
        ensureCropRectInBounds()
        invalidate()
        notifyCropChanged()
    }

    /**
     * 显示/隐藏裁剪框
     */
    fun setShowCropRect(show: Boolean) {
        showCropRect = show
        if (show && width > 0 && height > 0) {
            createDefaultCropRect()
        }
        invalidate()
    }

    // ========== 初始化方法 ==========

    init {
        // 初始不显示裁剪框
        showCropRect = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 视图尺寸变化时，重新创建裁剪框
        if (showCropRect && w > 0 && h > 0) {
            if (currentRatio > 0) {
                createInitialCropRect()
            } else {
                createDefaultCropRect()
            }
        }
    }

    // ========== 绘制方法 ==========

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!showCropRect || cropRect.isEmpty) return

        // 1. 绘制遮罩层（裁剪框外部）
        drawMask(canvas)

        // 2. 绘制裁剪框边框
        canvas.drawRect(cropRect, cropBorderPaint)

        // 3. 绘制网格线（九宫格）
        drawGridLines(canvas)

        // 4. 绘制角落手柄
        drawCornerHandles(canvas)
    }

    /**
     * 绘制遮罩层
     */
    private fun drawMask(canvas: Canvas) {
        // 上部分
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, maskPaint)
        // 下部分
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), maskPaint)
        // 左部分
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, maskPaint)
        // 右部分
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, maskPaint)
    }

    /**
     * 绘制九宫格网格线
     */
    private fun drawGridLines(canvas: Canvas) {
        val thirdWidth = cropRect.width() / 3
        val thirdHeight = cropRect.height() / 3

        // 竖线
        for (i in 1..2) {
            val x = cropRect.left + thirdWidth * i
            canvas.drawLine(x, cropRect.top, x, cropRect.bottom, gridLinePaint)
        }

        // 横线
        for (i in 1..2) {
            val y = cropRect.top + thirdHeight * i
            canvas.drawLine(cropRect.left, y, cropRect.right, y, gridLinePaint)
        }
    }

    /**
     * 绘制角落手柄
     */
    private fun drawCornerHandles(canvas: Canvas) {
        val halfHandle = HANDLE_SIZE / 2

        // 四个角落
        val corners = listOf(
            PointF(cropRect.left, cropRect.top),        // 左上
            PointF(cropRect.right, cropRect.top),       // 右上
            PointF(cropRect.left, cropRect.bottom),     // 左下
            PointF(cropRect.right, cropRect.bottom)     // 右下
        )

        corners.forEach { point ->
            // 绘制白色方块
            canvas.drawRect(
                point.x - halfHandle,
                point.y - halfHandle,
                point.x + halfHandle,
                point.y + halfHandle,
                handlePaint
            )

            // 绘制黑色边框
            canvas.drawRect(
                point.x - halfHandle,
                point.y - halfHandle,
                point.x + halfHandle,
                point.y + halfHandle,
                handleBorderPaint
            )
        }
    }

    // ========== 触摸事件处理 ==========

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!showCropRect) return false

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragMode = getDragMode(x, y)
                if (dragMode != DRAG_NONE) {
                    lastTouchX = x
                    lastTouchY = y
                    isDragging = true
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && dragMode != DRAG_NONE) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY

                    // 根据拖拽模式调整裁剪框
                    adjustCropRect(dx, dy)

                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    notifyCropChanged()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                dragMode = DRAG_NONE
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * 根据拖拽模式调整裁剪框
     */
    private fun adjustCropRect(dx: Float, dy: Float) {
        when (dragMode) {
            DRAG_MOVE -> moveCropRect(dx, dy)
            DRAG_LEFT -> resizeLeft(dx)
            DRAG_TOP -> resizeTop(dy)
            DRAG_RIGHT -> resizeRight(dx)
            DRAG_BOTTOM -> resizeBottom(dy)
            DRAG_TOP_LEFT -> {
                resizeLeft(dx)
                resizeTop(dy)
            }
            DRAG_TOP_RIGHT -> {
                resizeRight(dx)
                resizeTop(dy)
            }
            DRAG_BOTTOM_LEFT -> {
                resizeLeft(dx)
                resizeBottom(dy)
            }
            DRAG_BOTTOM_RIGHT -> {
                resizeRight(dx)
                resizeBottom(dy)
            }
        }

        // 如果设置了固定比例，保持比例
        if (currentRatio > 0 && dragMode != DRAG_MOVE) {
            adjustToKeepRatio()
        }

        // 确保裁剪框在边界内
        ensureCropRectInBounds()
    }

    /**
     * 获取拖拽模式
     */
    private fun getDragMode(x: Float, y: Float): Int {
        // 检查角落
        if (isPointNear(x, y, cropRect.left, cropRect.top)) return DRAG_TOP_LEFT
        if (isPointNear(x, y, cropRect.right, cropRect.top)) return DRAG_TOP_RIGHT
        if (isPointNear(x, y, cropRect.left, cropRect.bottom)) return DRAG_BOTTOM_LEFT
        if (isPointNear(x, y, cropRect.right, cropRect.bottom)) return DRAG_BOTTOM_RIGHT

        // 检查边缘（添加一些容差）
        val edgeTolerance = TOUCH_TOLERANCE * 2

        if (abs(x - cropRect.left) < edgeTolerance && y >= cropRect.top && y <= cropRect.bottom)
            return DRAG_LEFT
        if (abs(x - cropRect.right) < edgeTolerance && y >= cropRect.top && y <= cropRect.bottom)
            return DRAG_RIGHT
        if (abs(y - cropRect.top) < edgeTolerance && x >= cropRect.left && x <= cropRect.right)
            return DRAG_TOP
        if (abs(y - cropRect.bottom) < edgeTolerance && x >= cropRect.left && x <= cropRect.right)
            return DRAG_BOTTOM

        // 检查内部
        if (cropRect.contains(x, y)) return DRAG_MOVE

        return DRAG_NONE
    }

    /**
     * 判断点是否在指定点附近
     */
    private fun isPointNear(x: Float, y: Float, pointX: Float, pointY: Float): Boolean {
        return abs(x - pointX) <= TOUCH_TOLERANCE && abs(y - pointY) <= TOUCH_TOLERANCE
    }

    // ========== 裁剪框调整方法 ==========

    private fun moveCropRect(dx: Float, dy: Float) {
        val newLeft = cropRect.left + dx
        val newTop = cropRect.top + dy
        val newRight = cropRect.right + dx
        val newBottom = cropRect.bottom + dy

        // 检查边界
        if (newLeft >= 0 && newRight <= width && newTop >= 0 && newBottom <= height) {
            cropRect.set(newLeft, newTop, newRight, newBottom)
        }
    }

    private fun resizeLeft(dx: Float) {
        val newLeft = cropRect.left + dx
        if (newLeft >= 0 && newLeft < cropRect.right - minCropSize) {
            cropRect.left = newLeft
        }
    }

    private fun resizeTop(dy: Float) {
        val newTop = cropRect.top + dy
        if (newTop >= 0 && newTop < cropRect.bottom - minCropSize) {
            cropRect.top = newTop
        }
    }

    private fun resizeRight(dx: Float) {
        val newRight = cropRect.right + dx
        if (newRight <= width && newRight > cropRect.left + minCropSize) {
            cropRect.right = newRight
        }
    }

    private fun resizeBottom(dy: Float) {
        val newBottom = cropRect.bottom + dy
        if (newBottom <= height && newBottom > cropRect.top + minCropSize) {
            cropRect.bottom = newBottom
        }
    }

    /**
     * 调整裁剪框以保持固定比例
     */
    private fun adjustToKeepRatio() {
        if (currentRatio <= 0) return

        val currentWidth = cropRect.width()
        val currentHeight = cropRect.height()
        val currentRatioValue = currentWidth / currentHeight

        if (abs(currentRatioValue - currentRatio) > 0.01f) {
            // 比例不一致，需要调整
            when (dragMode) {
                DRAG_LEFT, DRAG_RIGHT -> {
                    // 调整宽度时，根据比例设置高度
                    val targetHeight = currentWidth / currentRatio
                    cropRect.bottom = cropRect.top + targetHeight
                }
                DRAG_TOP, DRAG_BOTTOM -> {
                    // 调整高度时，根据比例设置宽度
                    val targetWidth = currentHeight * currentRatio
                    cropRect.right = cropRect.left + targetWidth
                }
                DRAG_TOP_LEFT, DRAG_TOP_RIGHT, DRAG_BOTTOM_LEFT, DRAG_BOTTOM_RIGHT -> {
                    // 对角线调整，保持中心点
                    val centerX = cropRect.centerX()
                    val centerY = cropRect.centerY()

                    val newWidth: Float
                    val newHeight: Float

                    if (currentRatioValue > currentRatio) {
                        // 太宽了，调整高度
                        newHeight = currentWidth / currentRatio
                        newWidth = currentWidth
                    } else {
                        // 太高了，调整宽度
                        newWidth = currentHeight * currentRatio
                        newHeight = currentHeight
                    }

                    cropRect.set(
                        centerX - newWidth / 2,
                        centerY - newHeight / 2,
                        centerX + newWidth / 2,
                        centerY + newHeight / 2
                    )
                }
            }
        }
    }

    /**
     * 确保裁剪框在视图边界内
     */
    private fun ensureCropRectInBounds() {
        // 左边界
        if (cropRect.left < 0) {
            val offset = -cropRect.left
            cropRect.left = 0f
            if (dragMode == DRAG_MOVE) {
                cropRect.right += offset
            }
        }

        // 上边界
        if (cropRect.top < 0) {
            val offset = -cropRect.top
            cropRect.top = 0f
            if (dragMode == DRAG_MOVE) {
                cropRect.bottom += offset
            }
        }

        // 右边界
        if (cropRect.right > width) {
            val offset = cropRect.right - width
            cropRect.right = width.toFloat()
            if (dragMode == DRAG_MOVE) {
                cropRect.left -= offset
            }
        }

        // 下边界
        if (cropRect.bottom > height) {
            val offset = cropRect.bottom - height
            cropRect.bottom = height.toFloat()
            if (dragMode == DRAG_MOVE) {
                cropRect.top -= offset
            }
        }

        // 确保最小尺寸
        if (cropRect.width() < minCropSize) {
            val centerX = cropRect.centerX()
            cropRect.left = centerX - minCropSize / 2
            cropRect.right = centerX + minCropSize / 2
        }

        if (cropRect.height() < minCropSize) {
            val centerY = cropRect.centerY()
            cropRect.top = centerY - minCropSize / 2
            cropRect.bottom = centerY + minCropSize / 2
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建初始裁剪框（根据比例）
     */
    private fun createInitialCropRect() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val centerX = viewWidth / 2
        val centerY = viewHeight / 2

        // 裁剪框大小（占据视图的80%）
        val maxSize = min(viewWidth, viewHeight) * 0.8f

        var cropWidth: Float
        var cropHeight: Float

        if (currentRatio > 0) {
            // 固定比例
            if (currentRatio >= 1) {
                // 宽度 >= 高度
                cropWidth = min(maxSize, maxSize * currentRatio)
                cropHeight = cropWidth / currentRatio
            } else {
                // 高度 > 宽度
                cropHeight = min(maxSize, maxSize / currentRatio)
                cropWidth = cropHeight * currentRatio
            }
        } else {
            // 自由比例，使用正方形
            cropWidth = maxSize
            cropHeight = maxSize
        }

        cropRect.set(
            centerX - cropWidth / 2,
            centerY - cropHeight / 2,
            centerX + cropWidth / 2,
            centerY + cropHeight / 2
        )

        showCropRect = true
    }

    /**
     * 创建默认裁剪框（自由比例）
     */
    private fun createDefaultCropRect() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val centerX = viewWidth / 2
        val centerY = viewHeight / 2

        // 默认使用正方形，占据视图的70%
        val cropSize = min(viewWidth, viewHeight) * 0.7f

        cropRect.set(
            centerX - cropSize / 2,
            centerY - cropSize / 2,
            centerX + cropSize / 2,
            centerY + cropSize / 2
        )

        showCropRect = true
    }

    /**
     * 通知裁剪框变化
     */
    private fun notifyCropChanged() {
        cropChangeListener?.onCropChanged(RectF(cropRect))
    }
}