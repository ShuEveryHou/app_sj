package com.example.app_sj

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import java.io.File

class PhotoAdapter(
    private var photos: List<Photo> = emptyList(),
    private val onItemClick: (Photo) -> Unit = {}
): RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]

        val requestOptions = RequestOptions()
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder_image) // 确保有正确的占位图
            .error(android.R.drawable.ic_menu_camera)

        // 根据图片来源选择加载方式
        if (photo.isUserCreated && photo.filePath.isNotEmpty()) {
            // 用户创建的图片：检查文件是否存在
            val file = File(photo.filePath)
            if (file.exists()) {
                Glide.with(holder.itemView.context)
                    .load(file)
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade()) // 添加过渡动画
                    .into(holder.ivPhoto)
            } else {
                // 文件不存在，显示错误图标
                holder.ivPhoto.setImageResource(android.R.drawable.ic_menu_camera)
            }
        } else if (photo.resourceId != 0) {
            // 系统图片：从资源ID加载
            Glide.with(holder.itemView.context)
                .load(photo.resourceId)
                .apply(requestOptions)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.ivPhoto)
        } else {
            // 默认图标
            holder.ivPhoto.setImageResource(android.R.drawable.ic_menu_camera)
        }

        // 设置点击事件
        holder.ivPhoto.setOnClickListener {
            onItemClick(photo)
        }
    }

    override fun getItemCount(): Int = photos.size

    fun addPhoto(newPhoto: Photo) {
        val newList = photos.toMutableList()
        newList.add(newPhoto)
        photos = newList
        notifyItemInserted(photos.size - 1)
    }

    fun updatePhotos(newPhotos: List<Photo>) {
        photos = newPhotos
        notifyDataSetChanged()
    }
}