// PublicAlbumActivity.kt - 完整版本
package com.example.app_sj

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
        // 清空资源ID列表
        photoResourceIds.clear()

        // 本地图片数据（匹配您drawable目录中的文件名）
        val imageData = listOf(
            "bao_1" to "图1",
            "bao_2" to "图2",
            "bao_3" to "图3",
            "bao_4" to "图4",
            "bao_5" to "图5",
            "bao_6" to "图6",
            "bao_7" to "图7",
            "bao_8" to "图8",
            "bao_9" to "图9",
            "bao_10" to "图10",
            "bao_11" to "图11",
            "bao_12" to "图12",
            "bao_13" to "图13",
            "bao_14" to "图14",
            "pic_1" to "图15",
            "pic_2" to "图16",
            "pic_3" to "图17",
            "pic_4" to "图18",
            "pic_5" to "图19",
            "pic_6" to "图20",
            "pic_7" to "图21",
            "pic_8" to "图22",
            "pic_9" to "图23",
            "pic_10" to "图24",
            "pic_11" to "图25",
            "pic_12" to "图26",
            "pic_13" to "图27",
            "pic_14" to "图28",
            "pic_15" to "图29",
            "pic_16" to "图30"
        )

        for ((index, data) in imageData.withIndex()) {
            val (imageName, title) = data

            // 获取资源ID
            val resourceId = getResourceId(imageName)

            if (resourceId != 0) {
                // 资源存在，添加到列表
                val photo = Photo(index + 1, resourceId, "", title, false)
                samplePhotos.add(photo)
                photoResourceIds.add(resourceId)

                // 打印调试信息
                Log.d("PublicAlbum", "找到图片: $imageName -> $resourceId")
            } else {
                // 资源不存在，使用默认图标
                Log.w("PublicAlbum", "未找到图片: $imageName，使用默认图标")
                samplePhotos.add(Photo(index + 1, android.R.drawable.ic_menu_camera, "", "$title (默认)", false))
            }
        }

        // 更新适配器数据
        photoAdapter.updatePhotos(samplePhotos)

        // 打印统计信息
        Log.i("PublicAlbum", "成功加载 ${samplePhotos.size} 张图片，其中 ${photoResourceIds.size} 张是本地图片")
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

        val intent = Intent(this, ImageDetailActivity::class.java).apply {
            putExtra("photo_id", photo.id)
            putExtra("photo_resource_id", photo.resourceId)
            putExtra("photo_title", photo.title)
            putExtra("is_from_camera", false)
        }
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