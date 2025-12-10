package com.example.app_sj

import android.graphics.*

//滤镜工具类 - 手动实现6种基础滤镜
object FilterUtils {

    // 滤镜类型枚举
    enum class FilterType {
        ORIGINAL,     // 原图
        BLACK_WHITE,  // 黑白
        RETRO,        // 复古
        FRESH,        // 清新
        WARM,         // 暖色调
        COLD          // 冷色调
    }

    //应用滤镜效果
    fun applyFilter(src: Bitmap, filterType: FilterType): Bitmap {
        // 确保使用合适的配置，避免内存问题
        val safeConfig = src.config ?: Bitmap.Config.ARGB_8888

        return when (filterType) {
            FilterType.ORIGINAL -> applyOriginalFilter(src, safeConfig)
            FilterType.BLACK_WHITE -> applyBlackWhiteFilter(src)
            FilterType.RETRO -> applyRetroFilter(src)
            FilterType.FRESH -> applyFreshFilter(src)
            FilterType.WARM -> applyWarmFilter(src)
            FilterType.COLD -> applyColdFilter(src)
        }
    }


    //原图滤镜（无效果）
    private fun applyOriginalFilter(src: Bitmap, config: Bitmap.Config): Bitmap {
        return src.copy(config, true)
    }


    //黑白滤镜 - 灰度化处理
    private fun applyBlackWhiteFilter(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height

        // 使用RGB_565配置减少内存使用
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        // 获取像素数组
        val srcPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val dstPixels = IntArray(width * height)

        for (i in srcPixels.indices) {
            val pixel = srcPixels[i]

            // 提取ARGB分量
            val alpha = pixel shr 24 and 0xFF
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF

            // 灰度化公式：Gray = 0.299 * R + 0.587 * G + 0.114 * B
            val gray = (red * 0.299 + green * 0.587 + blue * 0.114).toInt()

            // 将灰度值赋给RGB
            dstPixels[i] = (alpha shl 24) or (gray shl 16) or (gray shl 8) or gray
        }

        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }


    //复古滤镜 - 黄褐色调
    private fun applyRetroFilter(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val srcPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val dstPixels = IntArray(width * height)

        for (i in srcPixels.indices) {
            val pixel = srcPixels[i]

            val alpha = pixel shr 24 and 0xFF
            var red = pixel shr 16 and 0xFF
            var green = pixel shr 8 and 0xFF
            var blue = pixel and 0xFF

            // 复古效果：增加红色和黄色调，减少蓝色
            red = (red * 1.2).toInt().coerceIn(0, 255)
            green = (green * 1.1).toInt().coerceIn(0, 255)
            blue = (blue * 0.8).toInt().coerceIn(0, 255)

            // 添加一些棕色色调
            val sepiaRed = (red * 0.393 + green * 0.769 + blue * 0.189).toInt()
            val sepiaGreen = (red * 0.349 + green * 0.686 + blue * 0.168).toInt()
            val sepiaBlue = (red * 0.272 + green * 0.534 + blue * 0.131).toInt()

            dstPixels[i] = (alpha shl 24) or
                    (sepiaRed.coerceIn(0, 255) shl 16) or
                    (sepiaGreen.coerceIn(0, 255) shl 8) or
                    sepiaBlue.coerceIn(0, 255)
        }

        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }


    //清新滤镜 - 增强绿色和蓝色，提高亮度
    private fun applyFreshFilter(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val srcPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val dstPixels = IntArray(width * height)

        for (i in srcPixels.indices) {
            val pixel = srcPixels[i]

            val alpha = pixel shr 24 and 0xFF
            var red = pixel shr 16 and 0xFF
            var green = pixel shr 8 and 0xFF
            var blue = pixel and 0xFF

            // 清新效果：增强绿色和蓝色，整体提亮
            red = (red * 1.1).toInt().coerceIn(0, 255)
            green = (green * 1.3).toInt().coerceIn(0, 255)
            blue = (blue * 1.2).toInt().coerceIn(0, 255)

            // 提高亮度
            val brightness = 20
            red = (red + brightness).coerceIn(0, 255)
            green = (green + brightness).coerceIn(0, 255)
            blue = (blue + brightness).coerceIn(0, 255)

            // 增加饱和度
            val avg = (red + green + blue) / 3
            red = (avg + (red - avg) * 1.5).toInt().coerceIn(0, 255)
            green = (avg + (green - avg) * 1.5).toInt().coerceIn(0, 255)
            blue = (avg + (blue - avg) * 1.5).toInt().coerceIn(0, 255)

            dstPixels[i] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }

        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }


    //暖色调滤镜 - 增加红色和黄色
    private fun applyWarmFilter(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val srcPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val dstPixels = IntArray(width * height)

        for (i in srcPixels.indices) {
            val pixel = srcPixels[i]

            val alpha = pixel shr 24 and 0xFF
            var red = pixel shr 16 and 0xFF
            var green = pixel shr 8 and 0xFF
            var blue = pixel and 0xFF

            // 暖色调：增强红色和黄色，减弱蓝色
            red = (red * 1.4).toInt().coerceIn(0, 255)
            green = (green * 1.2).toInt().coerceIn(0, 255)
            blue = (blue * 0.8).toInt().coerceIn(0, 255)

            // 添加橙色色调
            val orangeRed = (red * 1.1 + green * 0.3).toInt().coerceIn(0, 255)
            val orangeGreen = (green * 0.9).toInt().coerceIn(0, 255)

            dstPixels[i] = (alpha shl 24) or
                    (orangeRed shl 16) or
                    (orangeGreen shl 8) or
                    blue
        }

        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }


    //冷色调滤镜 - 增加蓝色和青色
    private fun applyColdFilter(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val srcPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val dstPixels = IntArray(width * height)

        for (i in srcPixels.indices) {
            val pixel = srcPixels[i]

            val alpha = pixel shr 24 and 0xFF
            var red = pixel shr 16 and 0xFF
            var green = pixel shr 8 and 0xFF
            var blue = pixel and 0xFF

            // 冷色调：增强蓝色和青色，减弱红色
            red = (red * 0.7).toInt().coerceIn(0, 255)
            green = (green * 1.1).toInt().coerceIn(0, 255)
            blue = (blue * 1.4).toInt().coerceIn(0, 255)

            // 添加蓝色调
            val coldRed = (red * 0.8).toInt().coerceIn(0, 255)
            val coldGreen = (green * 1.1).toInt().coerceIn(0, 255)
            val coldBlue = (blue * 1.3).toInt().coerceIn(0, 255)

            dstPixels[i] = (alpha shl 24) or
                    (coldRed shl 16) or
                    (coldGreen shl 8) or
                    coldBlue
        }

        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }


    //创建滤镜预览图（小尺寸，用于显示）
    fun createFilterPreview(src: Bitmap, filterType: FilterType, previewSize: Int): Bitmap {
        try {
            // 先缩小原始图片（使用ARGB_8888确保质量）
            val scaledSrc = Bitmap.createScaledBitmap(
                src,
                previewSize,
                previewSize,
                true
            )

            // 如果已经是原图滤镜，直接返回
            if (filterType == FilterType.ORIGINAL) {
                return scaledSrc
            }

            // 应用滤镜
            val filtered = applyFilter(scaledSrc, filterType)

            // 创建带边框的预览图
            val result = Bitmap.createBitmap(previewSize, previewSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            // 绘制滤镜图片
            canvas.drawBitmap(filtered, 0f, 0f, null)

            // 添加边框
            val borderPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            canvas.drawRect(0f, 0f, previewSize.toFloat(), previewSize.toFloat(), borderPaint)

            // 回收临时Bitmap
            if (filtered != scaledSrc) {
                filtered.recycle()
            }

            return result

        } catch (e: Exception) {
            e.printStackTrace()
            // 如果出错，返回一个简单的颜色块
            return createColorPreview(previewSize, filterType)
        }
    }


    //创建颜色预览（备用方案）
    private fun createColorPreview(size: Int, filterType: FilterType): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 根据滤镜类型使用不同的颜色
        val color = when (filterType) {
            FilterType.ORIGINAL -> Color.LTGRAY
            FilterType.BLACK_WHITE -> Color.GRAY
            FilterType.RETRO -> Color.rgb(210, 180, 140)  // 棕色
            FilterType.FRESH -> Color.rgb(173, 216, 230)  // 淡蓝色
            FilterType.WARM -> Color.rgb(255, 200, 120)   // 橙色
            FilterType.COLD -> Color.rgb(135, 206, 250)   // 浅蓝色
        }

        canvas.drawColor(color)

        // 添加边框
        val borderPaint = Paint().apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), borderPaint)

        return bitmap
    }
}