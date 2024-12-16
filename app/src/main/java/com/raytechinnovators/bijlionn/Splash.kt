package com.raytechinnovators.bijlionn

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.raytechinnovators.bijlionn.R


class Splash : AppCompatActivity() {

    private val splashTimer = 3000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        window.statusBarColor = ContextCompat.getColor(this, R.color.transparent)
        changeNavigationBarColor()

        Handler().postDelayed({
            val intent = Intent(this@Splash, PhoneActivity::class.java)
            startActivity(intent)
            finish()
        }, splashTimer)
    }

    fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.splash_navigation_bar_color)
    }
}
