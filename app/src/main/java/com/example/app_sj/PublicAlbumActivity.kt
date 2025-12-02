package com.example.app_sj

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PublicAlbumActivity : AppCompatActivity() {

    private lateinit var rvPhotos: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter
    //支持相机功能
    private lateinit var fabCamera: FloatingActionButton
    //照片ID计数器
    private var photoIdCounter = 50//从50开始，避免和本地图片冲突


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pubilc_album)

        setupToolbar()
        setupRecyclerView()
        loadSamplePhotos()  // 加载示例图片
    }


    // 设置顶部工具栏
    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 设置返回按钮点击事件
        toolbar.setNavigationOnClickListener {
            returnToMainActivity()
        }
    }


    //设置RecyclerView和图片网格
    private fun setupRecyclerView() {
        rvPhotos = findViewById(R.id.rvPhotos)

        // 创建网格布局管理器 - 一行3列
        val layoutManager = GridLayoutManager(this, 3)
        rvPhotos.layoutManager = layoutManager

        // 创建适配器,点击回调
        photoAdapter = PhotoAdapter(emptyList()){
            photo ->
            openImageDetail(photo)
        }
        rvPhotos.adapter = photoAdapter

        // 设置图片间距 - 4dp
        val spacingInPixels = 4.dpToPx()
        rvPhotos.addItemDecoration(GridSpacingItemDecoration(3, spacingInPixels, true))
    }

    //加载图片数据
    private fun loadSamplePhotos() {  // 添加这个方法定义

        val samplePhotos = mutableListOf<Photo>()
        // 获取资源ID的函数
        fun getResourceId(name: String): Int {
            return resources.getIdentifier(name, "drawable", packageName)
        }

        // 本地图片数据
        val imageData  = listOf(
            Pair("bao_1","图1"),
            Pair("bao_2","图2"),
            Pair("bao_3","图3"),
            Pair("bao_4","图4"),
            Pair("bao_5","图5"),
            Pair("bao_6","图6"),
            Pair("bao_7","图7"),
            Pair("bao_8","图8"),
            Pair("bao_9","图9"),
            Pair("bao_10","图10"),
            Pair("bao_11","图11"),
            Pair("bao_12","图12"),
            Pair("bao_13","图13"),
            Pair("bao_14","图14"),
            Pair("pic_1","图15"),
            Pair("pic_2","图16"),
            Pair("pic_3","图17"),
            Pair("pic_4","图18"),
            Pair("pic_5","图19"),
            Pair("pic_6","图20"),
            Pair("pic_7","图21"),
            Pair("pic_8","图22"),
            Pair("pic_9","图23"),
            Pair("pic_10","图24"),
            Pair("pic_11","图25"),
            Pair("pic_12","图26"),
            Pair("pic_13","图27"),
            Pair("pic_14","图28"),
            Pair("pic_15","图29"),
            Pair("pic_16","图30"),
        )

        // 创建Photo对象列表
        for ((index, data) in imageData.withIndex()) {
            val (imageName, title) = data
            val resourceId = getResourceId(imageName)

            if (resourceId != 0) {
                // 资源存在，添加到列表
                samplePhotos.add(Photo(index + 1, resourceId, title))
            } else {
                // 资源不存在，使用默认图标
                samplePhotos.add(Photo(index + 1, android.R.drawable.ic_menu_camera, "$title (默认)"))
            }
        }

        // 更新适配器数据
        photoAdapter.updatePhotos(samplePhotos)
    }

    /*private fun openImageDetail(photo: Photo) {
        // 创建Intent跳转到详情页面
        val intent = Intent(this, ImageDetailActivity::class.java).apply {
            putExtra("photo_id", photo.id)
            putExtra("photo_resource_id", photo.resourceId)
            putExtra("photo_title", photo.title)
        }

        // 启动详情页面
        startActivity(intent)

    }*/
    private fun openImageDetail(photo: Photo) {
        // 预加载图片到缓存
        ImageDetailActivity.preloadImage(this, photo.resourceId)

        val intent = Intent(this, ImageDetailActivity::class.java).apply {
            putExtra("photo_id", photo.id)
            putExtra("photo_resource_id", photo.resourceId)
            putExtra("photo_title", photo.title)
            putExtra("is_from_camera", photo.isFromCamera)
        }
        // 启动详情页面
        startActivity(intent)
    }

    //返回主界面
    private fun returnToMainActivity() {
        finish()
    }

    fun onBackPressedDispatcher() {
        returnToMainActivity()
    }


    //扩展函数：dp转px
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

}