package com.example.app_sj

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewTreeObserver
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

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

    // 布局监听器（确保在正确时机居中）
    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    init {
        // 关键：设置scaleType为MATRIX
        super.setScaleType(ScaleType.MATRIX)

        // 初始化手势检测器
        initGestureDetectors()

        // 添加布局完成监听
        setupLayoutListener()
    }

    private fun setupLayoutListener() {
        layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            // 当布局完成且有图片时，立即居中
            if (drawable != null && measuredWidth > 0 && measuredHeight > 0) {
                centerImageImmediately()
                // 居中后移除监听器，避免重复调用
                viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            }
        }

        viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun initGestureDetectors() {
        // 缩放手势检测器
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                mode = ZOOM
                savedMatrix.set(matrix)
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                var scale = scaleFactor * detector.scaleFactor
                scale = kotlin.math.max(minScale, kotlin.math.min(scale, maxScale))

                if (scaleFactor != scale) {
                    scaleFactor = scale

                    // 计算缩放中心点
                    val focusX = detector.focusX
                    val focusY = detector.focusY

                    // 应用缩放
                    matrix.set(savedMatrix)
                    matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)

                    // 检查边界并限制
                    fixTranslation()
                    imageMatrix = matrix
                }
                return true
            }
        })

        // 手势检测器（用于双击）
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // 双击恢复原始大小并居中
                resetZoom()
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
        })
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)

        // 图片设置后，立即尝试居中（如果视图尺寸已确定）
        if (measuredWidth > 0 && measuredHeight > 0) {
            centerImageImmediately()
        }
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)

        // 图片设置后，立即尝试居中（如果视图尺寸已确定）
        if (measuredWidth > 0 && measuredHeight > 0) {
            centerImageImmediately()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 传递事件给手势检测器
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = DRAG
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = ZOOM
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    // 平移图像
                    matrix.set(savedMatrix)
                    matrix.postTranslate(
                        event.x - start.x,
                        event.y - start.y
                    )

                    // 检查并修正边界
                    fixTranslation()
                    imageMatrix = matrix
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                // 确保在边界内
                fixTranslation()
            }
        }

        return true
    }

    //平移，确保图片不会移出边界
    private fun fixTranslation() {
        val drawable = drawable ?: return

        val drawableRect = RectF(
            0f, 0f,
            drawable.intrinsicWidth.toFloat(),
            drawable.intrinsicHeight.toFloat()
        )
        matrix.mapRect(drawableRect)

        val deltaX = getDelta(drawableRect.width(), measuredWidth.toFloat(), drawableRect.left)
        val deltaY = getDelta(drawableRect.height(), measuredHeight.toFloat(), drawableRect.top)

        if (deltaX != 0f || deltaY != 0f) {
            matrix.postTranslate(deltaX, deltaY)
            imageMatrix = matrix
        }
    }

    private fun getDelta(imageSize: Float, viewSize: Float, imageStart: Float): Float {
        return when {
            imageSize < viewSize -> (viewSize - imageSize) / 2 - imageStart
            imageStart > 0 -> -imageStart
            imageStart + imageSize < viewSize -> viewSize - (imageStart + imageSize)
            else -> 0f
        }
    }

    //立即居中显示图片
    private fun centerImageImmediately() {
        val drawable = drawable ?: return
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = measuredWidth.toFloat()
        val viewHeight = measuredHeight.toFloat()

        if (drawableWidth <= 0 || drawableHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return
        }

        // 计算合适的缩放比例（适应屏幕）
        val scaleX = viewWidth / drawableWidth
        val scaleY = viewHeight / drawableHeight
        val scale = kotlin.math.min(scaleX, scaleY)

        // 重置矩阵
        matrix.reset()

        // 应用缩放
        matrix.setScale(scale, scale)
        scaleFactor = scale

        // 计算居中位置
        val scaledWidth = drawableWidth * scale
        val scaledHeight = drawableHeight * scale
        val translateX = (viewWidth - scaledWidth) / 2
        val translateY = (viewHeight - scaledHeight) / 2

        matrix.postTranslate(translateX, translateY)

        imageMatrix = matrix
    }

    //重置缩放和平移
    fun resetZoom() {
        centerImageImmediately()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理监听器
        layoutListener?.let {
            viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
    }

    // 禁止外部修改scaleType，必须使用MATRIX
    override fun setScaleType(scaleType: ScaleType?) {
        super.setScaleType(ScaleType.MATRIX)
    }
}