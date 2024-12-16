package com.raytechinnovators.bijlionn

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.raytechinnovators.bijlionn.R

class web : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        changeNavigationBarColor()
    }

    fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigation_bar_color)
    }
}