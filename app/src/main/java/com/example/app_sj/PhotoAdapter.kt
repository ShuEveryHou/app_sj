package com.example.app_sj

import android.net.wifi.p2p.WifiP2pManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

//显示图片列表
class PhotoAdapter(
    private var photos: List<Photo> = emptyList(),
    private val onItemClick: (Photo)->Unit ={ }

): RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>(){

    // ViewHolder 类
    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    // 绑定数据
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]

        //直接使用资源ID加载图片资源
        Glide.with(holder.itemView.context)
            .load(photo.resourceId)
            .centerCrop()
            .into(holder.ivPhoto)

        //设置点击事件
        holder.ivPhoto.setOnClickListener {
            onItemClick(photo)
        }
    }

    // 返回项目数量
    override fun getItemCount(): Int = photos.size


    //加载图片
    /*private fun loadImage(photo: Photo,imageView: ImageView){
        try{
            if(photo.imagePath.startsWith("R.drawable.")){
                //加载图片
                val resourceName = photo.imagePath.substringAfter("R.drawable.")
                val resourceId = imageView.context.resources.getIdentifier(
                    resourceName,
                    "drawable",
                    imageView.context.packageName
                )
                if(resourceId!=0){
                    Glide.with(imageView.context)
                        .load(resourceId)
                        .centerCrop()
                        .into(imageView)
                }else{
                    imageView.setBackgroundColor(0xFFF0F0F.toInt())
                }
            }else{
                Glide.with(imageView.context)
                    .load(photo.imagePath)
                    .centerCrop()
                    .into(imageView)
            }
        }catch (e: Exception){
            e.printStackTrace()
            imageView.setBackgroundColor(0xFFF0F0F0.toInt())
        }
    }*/


    // 添加新图片
    fun addPhoto(newPhoto: Photo) {
        val newList = photos.toMutableList()
        newList.add(newPhoto)
        photos = newList
        notifyItemInserted(photos.size - 1)
    }

    // 更新图片列表
    fun updatePhotos(newPhotos: List<Photo>) {
        photos = newPhotos
        notifyDataSetChanged()
    }
}