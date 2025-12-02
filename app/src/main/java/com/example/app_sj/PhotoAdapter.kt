package com.example.app_sj

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

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
            .load(photo.getDisplayPath())
            .centerCrop()
            .placeholder(R.drawable.placeholder_image)
            .into(holder.ivPhoto)

        //设置点击事件
        holder.ivPhoto.setOnClickListener {
            onItemClick(photo)
        }
    }

    // 返回项目数量
    override fun getItemCount(): Int = photos.size

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