package com.example.app_sj

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * 裁剪框视图 - 用于显示裁剪区域并处理用户交互
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 裁剪框的Paint（白色边框）
    private val cropBorderPaint = Paint().apply {
        color = Color.WHITE  // 边框颜色：白色
        style = Paint.Style.STROKE  // 描边样式
        strokeWidth = 2f  // 边框宽度：2像素
        isAntiAlias = true  // 开启抗锯齿，使边框更平滑
    }

    // 网格线的Paint（白色虚线）
    private val gridLinePaint = Paint().apply {
        color = Color.WHITE  // 网格线颜色：白色
        style = Paint.Style.STROKE  // 描边样式
        strokeWidth = 1f  // 线宽：1像素
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)  // 虚线效果，10像素实线+10像素间隔
        isAntiAlias = true
    }

    // 遮罩层的Paint（半透明黑色）
    private val maskPaint = Paint().apply {
        color = Color.parseColor("#80000000")  // 半透明黑色（ARGB：0x80000000）
        style = Paint.Style.FILL  // 填充样式
        isAntiAlias = true
    }

    // 裁剪框的矩形区域
    private val cropRect = RectF()

    // 触摸点相关变量
    private var lastTouchX = 0f  // 上一次触摸的X坐标
    private var lastTouchY = 0f  // 上一次触摸的Y坐标
    private var isDragging = false  // 是否正在拖拽
    private var dragMode = DRAG_NONE  // 拖拽模式

    // 裁剪框的最小尺寸（避免太小）
    private val minCropSize = 100f  // 最小100像素

    // 拖拽模式常量
    companion object {
        private const val DRAG_NONE = 0  // 无拖拽
        private const val DRAG_MOVE = 1  // 移动整个裁剪框
        private const val DRAG_LEFT = 2  // 调整左边框
        private const val DRAG_TOP = 3   // 调整上边框
        private const val DRAG_RIGHT = 4  // 调整右边框
        private const val DRAG_BOTTOM = 5  // 调整下边框
        private const val DRAG_TOP_LEFT = 6  // 调整左上角
        private const val DRAG_TOP_RIGHT = 7  // 调整右上角
        private const val DRAG_BOTTOM_LEFT = 8  // 调整左下角
        private const val DRAG_BOTTOM_RIGHT = 9  // 调整右下角

        // 触摸点的容差范围（像素）
        private const val TOUCH_TOLERANCE = 30f
    }

    // 裁剪比例（0表示自由比例）
    var cropRatio: Float = 0f
        set(value) {
            field = value
            if (value > 0) {
                // 如果设置了固定比例，调整裁剪框保持比例
                adjustCropRectToRatio()
            }
        }

    init {
        // 初始化裁剪框为视图中心，大小为视图的80%
        val padding = 50f
        cropRect.set(
            padding,  // left
            padding,  // top
            width.toFloat() - padding,  // right
            height.toFloat() - padding  // bottom
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 视图大小改变时，重新设置裁剪框
        if (w > 0 && h > 0) {
            val padding = 50f
            val centerX = w / 2f
            val centerY = h / 2f
            val cropWidth = (w - 2 * padding).coerceAtLeast(minCropSize)
            val cropHeight = (h - 2 * padding).coerceAtLeast(minCropSize)

            cropRect.set(
                centerX - cropWidth / 2,
                centerY - cropHeight / 2,
                centerX + cropWidth / 2,
                centerY + cropHeight / 2
            )

            // 如果设置了比例，调整裁剪框
            if (cropRatio > 0) {
                adjustCropRectToRatio()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. 绘制半透明遮罩层（裁剪框外部）
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, maskPaint)  // 上部分
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), maskPaint)  // 下部分
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, maskPaint)  // 左部分
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, maskPaint)  // 右部分

        // 2. 绘制裁剪框边框
        canvas.drawRect(cropRect, cropBorderPaint)

        // 3. 绘制九宫格网格线（帮助用户构图）
        drawGridLines(canvas)

        // 4. 绘制角落的调整手柄（小白方块）
        drawCornerHandles(canvas)
    }

    /**
     * 绘制九宫格网格线
     */
    private fun drawGridLines(canvas: Canvas) {
        // 计算网格线的位置（将裁剪框分为3x3的网格）
        val thirdWidth = cropRect.width() / 3
        val thirdHeight = cropRect.height() / 3

        // 绘制两条竖线
        canvas.drawLine(
            cropRect.left + thirdWidth, cropRect.top,
            cropRect.left + thirdWidth, cropRect.bottom,
            gridLinePaint
        )
        canvas.drawLine(
            cropRect.left + thirdWidth * 2, cropRect.top,
            cropRect.left + thirdWidth * 2, cropRect.bottom,
            gridLinePaint
        )

        // 绘制两条横线
        canvas.drawLine(
            cropRect.left, cropRect.top + thirdHeight,
            cropRect.right, cropRect.top + thirdHeight,
            gridLinePaint
        )
        canvas.drawLine(
            cropRect.left, cropRect.top + thirdHeight * 2,
            cropRect.right, cropRect.top + thirdHeight * 2,
            gridLinePaint
        )
    }

    /**
     * 绘制角落的调整手柄
     */
    private fun drawCornerHandles(canvas: Canvas) {
        val handleSize = 20f  // 手柄大小
        val handlePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // 左上角手柄
        canvas.drawRect(
            cropRect.left - handleSize / 2,
            cropRect.top - handleSize / 2,
            cropRect.left + handleSize / 2,
            cropRect.top + handleSize / 2,
            handlePaint
        )

        // 右上角手柄
        canvas.drawRect(
            cropRect.right - handleSize / 2,
            cropRect.top - handleSize / 2,
            cropRect.right + handleSize / 2,
            cropRect.top + handleSize / 2,
            handlePaint
        )

        // 左下角手柄
        canvas.drawRect(
            cropRect.left - handleSize / 2,
            cropRect.bottom - handleSize / 2,
            cropRect.left + handleSize / 2,
            cropRect.bottom + handleSize / 2,
            handlePaint
        )

        // 右下角手柄
        canvas.drawRect(
            cropRect.right - handleSize / 2,
            cropRect.bottom - handleSize / 2,
            cropRect.right + handleSize / 2,
            cropRect.bottom + handleSize / 2,
            handlePaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 手指按下，判断触摸位置并设置拖拽模式
                dragMode = getDragMode(x, y)
                lastTouchX = x
                lastTouchY = y
                isDragging = dragMode != DRAG_NONE
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    // 计算移动距离
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY

                    // 根据拖拽模式调整裁剪框
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

                    // 如果设置了固定比例，调整另一边以保持比例
                    if (cropRatio > 0 && dragMode != DRAG_MOVE) {
                        adjustToKeepRatio()
                    }

                    // 更新上一次触摸位置
                    lastTouchX = x
                    lastTouchY = y

                    // 重绘视图
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 手指抬起，结束拖拽
                isDragging = false
                dragMode = DRAG_NONE
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * 获取拖拽模式（判断触摸点位置）
     */
    private fun getDragMode(x: Float, y: Float): Int {
        // 检查是否触摸到角落手柄
        if (isPointNear(x, y, cropRect.left, cropRect.top)) return DRAG_TOP_LEFT
        if (isPointNear(x, y, cropRect.right, cropRect.top)) return DRAG_TOP_RIGHT
        if (isPointNear(x, y, cropRect.left, cropRect.bottom)) return DRAG_BOTTOM_LEFT
        if (isPointNear(x, y, cropRect.right, cropRect.bottom)) return DRAG_BOTTOM_RIGHT

        // 检查是否触摸到边框
        if (isPointNear(x, y, (cropRect.left + cropRect.right) / 2, cropRect.top)) return DRAG_TOP
        if (isPointNear(x, y, (cropRect.left + cropRect.right) / 2, cropRect.bottom)) return DRAG_BOTTOM
        if (isPointNear(x, y, cropRect.left, (cropRect.top + cropRect.bottom) / 2)) return DRAG_LEFT
        if (isPointNear(x, y, cropRect.right, (cropRect.top + cropRect.bottom) / 2)) return DRAG_RIGHT

        // 检查是否触摸到裁剪框内部（移动整个框）
        if (cropRect.contains(x, y)) return DRAG_MOVE

        return DRAG_NONE
    }

    /**
     * 判断点是否在指定点附近（用于检测触摸到边框或角落）
     */
    private fun isPointNear(x: Float, y: Float, pointX: Float, pointY: Float): Boolean {
        return Math.abs(x - pointX) <= TOUCH_TOLERANCE && Math.abs(y - pointY) <= TOUCH_TOLERANCE
    }

    /**
     * 移动整个裁剪框
     */
    private fun moveCropRect(dx: Float, dy: Float) {
        // 计算新的位置
        val newLeft = cropRect.left + dx
        val newTop = cropRect.top + dy
        val newRight = cropRect.right + dx
        val newBottom = cropRect.bottom + dy

        // 检查边界（确保裁剪框不超出视图范围）
        if (newLeft >= 0 && newRight <= width && newTop >= 0 && newBottom <= height) {
            cropRect.set(newLeft, newTop, newRight, newBottom)
        }
    }

    /**
     * 调整左边框
     */
    private fun resizeLeft(dx: Float) {
        val newLeft = cropRect.left + dx
        // 确保左边框不超出视图且裁剪框宽度不小于最小值
        if (newLeft >= 0 && newLeft < cropRect.right - minCropSize) {
            cropRect.left = newLeft
        }
    }

    /**
     * 调整上边框
     */
    private fun resizeTop(dy: Float) {
        val newTop = cropRect.top + dy
        if (newTop >= 0 && newTop < cropRect.bottom - minCropSize) {
            cropRect.top = newTop
        }
    }

    /**
     * 调整右边框
     */
    private fun resizeRight(dx: Float) {
        val newRight = cropRect.right + dx
        if (newRight <= width && newRight > cropRect.left + minCropSize) {
            cropRect.right = newRight
        }
    }

    /**
     * 调整下边框
     */
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
        if (cropRatio <= 0) return  // 自由比例，不需要调整

        val currentWidth = cropRect.width()
        val currentHeight = cropRect.height()
        val targetHeight = currentWidth / cropRatio
        val targetWidth = currentHeight * cropRatio

        when (dragMode) {
            DRAG_LEFT, DRAG_RIGHT -> {
                // 调整宽度时，根据比例计算新高度
                cropRect.bottom = cropRect.top + currentWidth / cropRatio
            }
            DRAG_TOP, DRAG_BOTTOM -> {
                // 调整高度时，根据比例计算新宽度
                cropRect.right = cropRect.left + currentHeight * cropRatio
            }
            DRAG_TOP_LEFT, DRAG_TOP_RIGHT, DRAG_BOTTOM_LEFT, DRAG_BOTTOM_RIGHT -> {
                // 调整角落时，保持对角线方向的比例
                val newWidth = cropRect.width()
                val newHeight = cropRect.height()
                val currentRatio = newWidth / newHeight

                if (currentRatio > cropRatio) {
                    // 太宽了，调整高度
                    cropRect.bottom = cropRect.top + newWidth / cropRatio
                } else {
                    // 太高了，调整宽度
                    cropRect.right = cropRect.left + newHeight * cropRatio
                }
            }
        }
    }

    /**
     * 根据当前比例调整裁剪框
     */
    private fun adjustCropRectToRatio() {
        if (cropRatio <= 0) return

        val centerX = cropRect.centerX()
        val centerY = cropRect.centerY()
        val currentWidth = cropRect.width()
        val currentHeight = cropRect.height()
        val currentRatio = currentWidth / currentHeight

        var newWidth = currentWidth
        var newHeight = currentHeight

        if (currentRatio > cropRatio) {
            // 当前框太宽，调整高度
            newHeight = newWidth / cropRatio
        } else {
            // 当前框太高，调整宽度
            newWidth = newHeight * cropRatio
        }

        // 设置新的裁剪框（保持中心点不变）
        cropRect.set(
            centerX - newWidth / 2,
            centerY - newHeight / 2,
            centerX + newWidth / 2,
            centerY + newHeight / 2
        )

        // 确保不超出边界
        ensureCropRectInBounds()
    }

    /**
     * 确保裁剪框在视图边界内
     */
    private fun ensureCropRectInBounds() {
        if (cropRect.left < 0) {
            val offset = -cropRect.left
            cropRect.left = 0f
            cropRect.right += offset
        }

        if (cropRect.top < 0) {
            val offset = -cropRect.top
            cropRect.top = 0f
            cropRect.bottom += offset
        }

        if (cropRect.right > width) {
            val offset = cropRect.right - width
            cropRect.right = width.toFloat()
            cropRect.left -= offset
        }

        if (cropRect.bottom > height) {
            val offset = cropRect.bottom - height
            cropRect.bottom = height.toFloat()
            cropRect.top -= offset
        }
    }

    /**
     * 获取裁剪区域（相对于视图的坐标）
     */
    fun getCropRect(): RectF {
        return RectF(cropRect)
    }

    /**
     * 设置裁剪区域
     */
    fun setCropRect(rect: RectF) {
        cropRect.set(rect)
        ensureCropRectInBounds()
        invalidate()
    }
}