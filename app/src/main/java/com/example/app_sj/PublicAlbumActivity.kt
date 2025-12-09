// PublicAlbumActivity.kt - 完整版本
package com.example.app_sj

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import kotlin.math.max
import kotlin.math.min

class PublicAlbumActivity : AppCompatActivity() {

    private lateinit var rvPhotos: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter

    private lateinit var imageUpdateReceiver: BroadcastReceiver

    // 存储所有图片资源ID的列表
    private val photoResourceIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pubilc_album)

        setupToolbar()
        setupRecyclerView()
        loadSamplePhotos()

        // 延迟预加载图片，确保UI先渲染完成
        rvPhotos.postDelayed({
            preloadVisibleImages()
        }, 50)

        setupBroadcastReceiver()
    }

    private fun setupBroadcastReceiver() {
        // 创建广播接收器
        imageUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "IMAGE_UPDATED") {
                    runOnUiThread {
                        refreshPhotos()
                        Toast.makeText(this@PublicAlbumActivity, "图片已更新", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 使用LocalBroadcastManager（需要添加依赖）
        val filter = IntentFilter("IMAGE_UPDATED")
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .registerReceiver(imageUpdateReceiver, filter)
    }

    private fun refreshPhotos() {
        loadSamplePhotos()
    }
    override fun onDestroy() {
        super.onDestroy()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(imageUpdateReceiver)
    }
    private fun notifyImageUpdated(context: Context) {
        val updateIntent = Intent("IMAGE_UPDATED")
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
            .sendBroadcast(updateIntent)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            returnToMainActivity()
        }
    }

    private fun setupRecyclerView() {
        rvPhotos = findViewById(R.id.rvPhotos)
        val layoutManager = GridLayoutManager(this, 3)
        rvPhotos.layoutManager = layoutManager

        photoAdapter = PhotoAdapter(emptyList()) { photo ->
            openImageDetail(photo)
        }
        rvPhotos.adapter = photoAdapter

        val spacingInPixels = 4.dpToPx()
        rvPhotos.addItemDecoration(GridSpacingItemDecoration(3, spacingInPixels, true))

        // 添加滑动监听，预加载可见项周围的图片
        rvPhotos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                preloadVisibleImages()
            }
        })
    }

    /**
     * 加载示例图片
     */
    private fun loadSamplePhotos() {
        val samplePhotos = mutableListOf<Photo>()

        // 1. 首先加载系统图片（原有的本地图片）
        fun getResourceId(name: String): Int {
            return resources.getIdentifier(name, "drawable", packageName)
        }

        val imageData = listOf(
            Pair("pic_1","图1"),
            Pair("pic_2","图2"),
            Pair("pic_3","图3"),
            Pair("pic_4","图4"),
            Pair("pic_5","图5"),
            Pair("pic_6","图6"),
            Pair("pic_7","图7"),
            Pair("pic_8","图8"),
            Pair("pic_9","图9"),
            Pair("pic_10","图10"),
            Pair("pic_11","图11"),
            Pair("pic_12","图12"),
            Pair("pic_13","图13"),
            Pair("pic_14","图14"),
            Pair("pic_15","图15"),
        )

        for ((index, data) in imageData.withIndex()) {
            val (imageName, title) = data
            val resourceId = getResourceId(imageName)

            if (resourceId != 0) {
                samplePhotos.add(Photo(
                    id = index + 1,
                    resourceId = resourceId,
                    title = title
                ))
            }
        }

        // 2. 加载用户创建的图片（放在系统图片之后）
        val userPhotos = ImageManager.getUserImages(this)
        val userStartId = samplePhotos.size + 1

        userPhotos.forEachIndexed { index, photo ->
            // 重要：检查文件是否存在
            val file = if (photo.filePath.isNotEmpty()) {
                java.io.File(photo.filePath)
            } else {
                null
            }

            if (file?.exists() == true) {
                val newPhoto = Photo(
                    id = userStartId + index,
                    resourceId = 0,
                    filePath = photo.filePath,
                    title = photo.title,
                    isFromCamera = false,
                    isUserCreated = true
                )
                samplePhotos.add(newPhoto)
            }
        }

        // 3. 更新适配器数据
        photoAdapter.updatePhotos(samplePhotos)

        // 4. 显示统计信息
        val systemCount = imageData.size
        val userCount = userPhotos.size
        val validUserCount = samplePhotos.size - systemCount

        if (validUserCount > 0) {
            Toast.makeText(this,
                "加载完成: ${systemCount}张系统图片 + ${validUserCount}张用户图片",
                Toast.LENGTH_SHORT).show()
        }
    }
    /**
     * 获取drawable资源ID
     */
    private fun getResourceId(imageName: String): Int {
        return resources.getIdentifier(imageName, "drawable", packageName)
    }

    /**
     * 预加载当前可见和即将可见的图片
     */
    private fun preloadVisibleImages() {
        // 确保photoResourceIds不为空
        if (photoResourceIds.isEmpty()) {
            return
        }

        try {
            val layoutManager = rvPhotos.layoutManager as GridLayoutManager
            val firstVisible = layoutManager.findFirstVisibleItemPosition()
            val lastVisible = layoutManager.findLastVisibleItemPosition()

            if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
                return
            }

            // 扩展预加载范围（当前可见的前后各3个）
            val start = max(0, firstVisible - 3)
            val end = min(photoResourceIds.size - 1, lastVisible + 3)

            // 使用单独的线程进行预加载，避免阻塞UI
            Thread {
                for (i in start..end) {
                    try {
                        // 预加载图片到Glide缓存
                        val resourceId = photoResourceIds[i]
                        Glide.with(this@PublicAlbumActivity)
                            .load(resourceId)
                            .preload()

                        // 每预加载3张图片休息一下，避免过度占用资源
                        if ((i - start) % 3 == 0) {
                            Thread.sleep(10)
                        }
                    } catch (e: Exception) {
                        // 忽略预加载错误
                        Log.w("PublicAlbum", "预加载图片失败: ${e.message}")
                    }
                }
            }.start()
        } catch (e: Exception) {
            Log.e("PublicAlbum", "预加载失败: ${e.message}")
        }
    }

    /**
     * 打开图片详情页面
     */
    private fun openImageDetail(photo: Photo) {
        // 立即预加载当前点击的图片（确保详情页快速显示）
        if (photo.resourceId != 0) {
            Glide.with(this)
                .load(photo.resourceId)
                .preload()
        }

        // 预加载相邻图片（为滑动查看做准备）
        preloadNeighborImages(photo.id - 1) // photo.id是从1开始的，所以要减1

        Log.d("PublicAlbum", "打开图片详情: ID=${photo.id}, 类型=${if(photo.isUserCreated)"用户图片" else "系统图片"}")

        val intent = Intent(this, ImageDetailActivity::class.java).apply {
            putExtra("photo_id", photo.id)
            putExtra("photo_resource_id", photo.resourceId)
            putExtra("photo_title", photo.title)
            putExtra("is_from_camera", false)
            putExtra("is_user_created", photo.isUserCreated)
            putExtra("photo_file_path", photo.filePath)
        }

        // 调试信息
        Log.d("PublicAlbum", "传递数据:")
        Log.d("PublicAlbum", "  filePath=${photo.filePath}")
        Log.d("PublicAlbum", "  resourceId=${photo.resourceId}")
        Log.d("PublicAlbum", "  isUserCreated=${photo.isUserCreated}")

        startActivity(intent)
    }

    /**
     * 预加载相邻的图片
     */
    private fun preloadNeighborImages(currentIndex: Int) {
        if (photoResourceIds.isEmpty()) return

        val start = max(0, currentIndex - 2)
        val end = min(photoResourceIds.size - 1, currentIndex + 2)

        Thread {
            for (i in start..end) {
                if (i == currentIndex) continue // 跳过当前图片

                try {
                    Glide.with(this@PublicAlbumActivity)
                        .load(photoResourceIds[i])
                        .preload()

                    Thread.sleep(5)
                } catch (e: Exception) {
                    // 忽略错误
                }
            }
        }.start()
    }

    private fun returnToMainActivity() {
        finish()
    }

    /**
     * dp转px的扩展函数
     */
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}