package com.example.app_sj

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    // 缩放相关
    private var scaleFactor = 1.0f
    private val minScale = 0.5f
    private val maxScale = 2.0f

    // 矩阵和点
    private val matrix = Matrix()
    private val savedMatrix = Matrix()

    // 手势检测器
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    // 触摸状态
    private var mode = NONE
    private val start = PointF()
    private val lastEvent = FloatArray(9)

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    init {
        // 初始化设置
        scaleType = ScaleType.MATRIX
        setImageMatrix(matrix)

        // 初始化手势检测器
        initGestureDetectors()
    }

    private fun initGestureDetectors() {
        // 缩放手势检测器
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                mode = ZOOM
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                var scale = scaleFactor * detector.scaleFactor
                scale = max(minScale, min(scale, maxScale))

                if (scaleFactor != scale) {
                    scaleFactor = scale

                    // 计算缩放中心点
                    val focusX = detector.focusX
                    val focusY = detector.focusY

                    // 应用缩放
                    matrix.set(savedMatrix)
                    matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                    setImageMatrix(matrix)
                }
                return true
            }
        })

        // 手势检测器（用于双击）
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // 双击恢复原始大小
                resetZoom()
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                // 必须返回true，否则其他手势无法检测
                return true
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 传递事件给手势检测器
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        val currentMatrix = FloatArray(9)
        matrix.getValues(currentMatrix)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = DRAG

                // 保存当前矩阵值
                matrix.getValues(lastEvent)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                savedMatrix.set(matrix)
                mode = ZOOM
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    // 平移图像
                    val dx = event.x - start.x
                    val dy = event.y - start.y

                    // 计算可移动边界
                    val matrixValues = FloatArray(9)
                    matrix.getValues(matrixValues)
                    val currentX = matrixValues[Matrix.MTRANS_X]
                    val currentY = matrixValues[Matrix.MTRANS_Y]

                    // 应用平移
                    matrix.set(savedMatrix)
                    matrix.postTranslate(dx, dy)

                    // 检查边界
                    matrix.getValues(matrixValues)
                    val newX = matrixValues[Matrix.MTRANS_X]
                    val newY = matrixValues[Matrix.MTRANS_Y]

                    // 如果超出边界，限制位置
                    if (!canScrollHorizontally(newX)) {
                        matrix.postTranslate(currentX - newX, 0f)
                    }
                    if (!canScrollVertically(newY)) {
                        matrix.postTranslate(0f, currentY - newY)
                    }

                    setImageMatrix(matrix)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }

        // 保存当前矩阵状态
        matrix.getValues(lastEvent)
        return true
    }

    /**
     * 检查是否可以水平滚动
     */
    private fun canScrollHorizontally(transX: Float): Boolean {
        if (drawable == null) return true

        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val viewWidth = width.toFloat()
        val scaledWidth = drawableWidth * scaleFactor

        if (scaledWidth <= viewWidth) {
            // 图片宽度小于视图宽度，不需要滚动
            return false
        }

        // 检查是否在边界内
        val maxTransX = (scaledWidth - viewWidth) / 2
        val minTransX = -maxTransX

        return transX in minTransX..maxTransX
    }

    /**
     * 检查是否可以垂直滚动
     */
    private fun canScrollVertically(transY: Float): Boolean {
        if (drawable == null) return true

        val drawableHeight = drawable.intrinsicHeight.toFloat()
        val viewHeight = height.toFloat()
        val scaledHeight = drawableHeight * scaleFactor

        if (scaledHeight <= viewHeight) {
            // 图片高度小于视图高度，不需要滚动
            return false
        }

        // 检查是否在边界内
        val maxTransY = (scaledHeight - viewHeight) / 2
        val minTransY = -maxTransY

        return transY in minTransY..maxTransY
    }

    /**
     * 重置缩放和平移
     */
    fun resetZoom() {
        scaleFactor = 1.0f
        matrix.reset()

        // 居中显示
        val drawable = drawable ?: return
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val scaleX = viewWidth / drawableWidth
        val scaleY = viewHeight / drawableHeight
        val scale = min(scaleX, scaleY)

        matrix.setScale(scale, scale)

        // 居中
        val dx = (viewWidth - drawableWidth * scale) / 2
        val dy = (viewHeight - drawableHeight * scale) / 2
        matrix.postTranslate(dx, dy)

        setImageMatrix(matrix)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 视图大小变化时重新居中
        resetZoom()
    }
}