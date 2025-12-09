// StickerManager.kt
package com.example.app_sj

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * 贴纸资源管理器
 */
object StickerManager {

    // 贴纸类型枚举
    enum class StickerType {
        BLACK_CAT,          // 黑猫
        CAT_1,              // 黄猫
        CLOUD_,             // 云
        CUP_,               // 杯子
        CUTE_TEXT,          // 可爱文字
        LUCKY_TEXT,         // 幸运文字
        MOON_,              // 月亮
        SNACK_,             // 小蛇
        START_CUTE,         // 星星
        TEXT_2,             // 棒棒文字
        TEXT_3,             // 开心文字
        TEXT_4,             // 加油文字
        WOLF_,              // 灰狼
    }

    /**
     * 获取贴纸资源ID
     */
    fun getStickerResource(type: StickerType): Int {
        return when (type) {
            StickerType.BLACK_CAT -> R.drawable.black_cat
            StickerType.CAT_1 -> R.drawable.cat_1
            StickerType.CLOUD_ -> R.drawable.cloud_
            StickerType.CUP_ -> R.drawable.cup_
            StickerType.CUTE_TEXT -> R.drawable.cute_text
            StickerType.LUCKY_TEXT -> R.drawable.lucky_text
            StickerType.MOON_ -> R.drawable.moon_
            StickerType.SNACK_ -> R.drawable.snack_
            StickerType.START_CUTE -> R.drawable.start_cute
            StickerType.TEXT_2 -> R.drawable.text_bb
            StickerType.TEXT_3 -> R.drawable.text_kx
            StickerType.TEXT_4 -> R.drawable.text_jy
            StickerType.WOLF_ -> R.drawable.wolf_
        }
    }

    /**
     * 获取贴纸名称
     */
    fun getStickerName(type: StickerType): String {
        return when (type) {
            StickerType.BLACK_CAT -> "黑猫"
            StickerType.CAT_1 -> "黄猫"
            StickerType.CLOUD_ -> "云"
            StickerType.CUP_ -> "杯子"
            StickerType.CUTE_TEXT -> "可爱文字"
            StickerType.LUCKY_TEXT -> "幸运文字"
            StickerType.MOON_ -> "月亮"
            StickerType.SNACK_ -> "小蛇"
            StickerType.START_CUTE -> "星星"
            StickerType.TEXT_2 -> "文字2"
            StickerType.TEXT_3 -> "文字3"
            StickerType.TEXT_4 -> "文字4"
            StickerType.WOLF_ -> "灰狼"
        }
    }

    /**
     * 获取所有贴纸类型
     */
    fun getAllStickerTypes(): List<StickerType> {
        return StickerType.values().toList()
    }

    /**
     * 加载贴纸Bitmap（带缩放）
     */
    fun loadStickerBitmap(context: Context, type: StickerType, maxSize: Int = 200): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val resId = getStickerResource(type)
            BitmapFactory.decodeResource(context.resources, resId, options)

            // 计算合适的采样率
            val scale = calculateInSampleSize(options, maxSize, maxSize)
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
                inPreferredConfig = Bitmap.Config.ARGB_8888  // 贴纸需要透明通道
            }

            BitmapFactory.decodeResource(context.resources, resId, loadOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
}