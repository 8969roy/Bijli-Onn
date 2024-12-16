package com.raytechinnovators.bijlionn

import android.content.Intent
import android.os.Bundle
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import com.raytechinnovators.bijlionn.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the binding instance
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animation for image moving from bottom to top
        val animation = TranslateAnimation(0f, 0f, 1000f, 0f).apply {
            duration = 5000
            repeatCount = TranslateAnimation.INFINITE
            repeatMode = TranslateAnimation.RESTART
        }
        binding.movingImage.startAnimation(animation)

        // Continue button click listener
        binding.continueButton.setOnClickListener {
            val intent = Intent(this, PhoneActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
