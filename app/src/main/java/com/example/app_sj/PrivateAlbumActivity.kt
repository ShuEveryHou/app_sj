package com.example.app_sj

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PrivateAlbumActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        TextView(this).apply {
            text="私有相册\n验证通过！"
            textSize=24f
            setTextColor(getColor(android.R.color.holo_red_dark))
        }.also { setContentView(it) }
    }
}