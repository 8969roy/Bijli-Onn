package com.raytechinnovators.bijlionn

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.raytechinnovators.bijlionn.R

class FullImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_image)

        val imageUrl = intent.getStringExtra("imageUrl")

        val photoView = findViewById<PhotoView>(R.id.photoView)

        Glide.with(this)
            .load(imageUrl)
            .into(photoView)
        changeNavigationBarColor()


    }

    fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigation_bar_color)
    }
}
