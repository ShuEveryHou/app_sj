package com.example.app_sj

import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar

class PublicAlbumActivity : AppCompatActivity() {

    private lateinit var rvPhotos: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter

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
            photo, imageView ->
            showImageDialog(photo,imageView)
        }
        rvPhotos.adapter = photoAdapter

        // 设置图片间距 - 4dp
        val spacingInPixels = 4.dpToPx()
        rvPhotos.addItemDecoration(GridSpacingItemDecoration(3, spacingInPixels, true))
    }


    //加载图片数据
    private fun loadSamplePhotos() {  // 添加这个方法定义
        // 模拟本地图片数据
        val samplePhotos = listOf(
            Photo(1,"R.drawable.bao_1"),
            Photo(2,"R.drawable.bao_2"),
            Photo(3,"R.drawable.bao_3"),
            Photo(4,"R.drawable.bao_4"),
            Photo(5,"R.drawable.bao_5"),
            Photo(6,"R.drawable.bao_6"),
            Photo(7,"R.drawable.bao_7"),
            Photo(8,"R.drawable.bao_8"),
            Photo(9,"R.drawable.bao_9"),
            Photo(10,"R.drawable.bao_10"),
            Photo(11,"R.drawable.bao_11"),
            Photo(12,"R.drawable.bao_12"),
            Photo(13,"R.drawable.bao_13"),
            Photo(14,"R.drawable.bao_14")
        )

        // 更新适配器数据
        photoAdapter.updatePhotos(samplePhotos)
    }


    //显示图片放大
    private fun showImageDialog(photo: Photo,sourceImageView: ImageView){

        //图片操作框
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_image_preview)

        val ivPreview = dialog.findViewById<ImageView>(R.id.ivPreview)

        //加载放大图
        loadPreviewImage(photo,ivPreview)

        //关闭放大图
        dialog.window?.decorView?.setOnClickListener {
            dialog.dismiss()
        }

        //显示操作框
        dialog.show()
    }

    //图片预加载
    private fun loadPreviewImage(photo: Photo,imageView: ImageView){
        try{
            if(photo.imagePath.startsWith("R.drawable.")){
                //加载图片
                val resourceName = photo.imagePath.substringAfter("R.drawable.")
                val resourceId = resources.getIdentifier(
                    resourceName,
                    "drawable",
                    packageName
                )
                if(resourceId!=0){
                    Glide.with(this)
                        .load(resourceId)
                        .fitCenter()
                        .into(imageView)
                }
            }else{
                Glide.with(this)
                    .load(photo.imagePath)
                    .fitCenter()
                    .into(imageView)
            }
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    //添加新图片到相册末尾
    fun addNewPhoto(imagePath: String) {  // 修正参数名
        // 创建新的图片ID（当前数量 + 1）
        val newId = photoAdapter.itemCount + 1
        val newPhoto = Photo(newId, imagePath)  // 修正参数名

        // 添加到适配器
        photoAdapter.addPhoto(newPhoto)

        // 可选：滚动到最后位置
        rvPhotos.smoothScrollToPosition(photoAdapter.itemCount - 1)
    }


    //返回主界面
    private fun returnToMainActivity() {
        finish()
    }

    fun onBackPressedDispatcher() {
        returnToMainActivity()
    }

    /**
     * 扩展函数：dp转px
     */
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}