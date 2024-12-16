package com.raytechinnovators.bijlionn

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.raytechinnovators.bijlionn.databinding.ActivityNoInternetBinding

class NoInternetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoInternetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoInternetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        changeNavigationBarColor()
        binding.retryButton.setOnClickListener {
            if (NetworkUtils.isNetworkAvailable(this)) {
                // Reload the activity
                val intent = Intent(this@NoInternetActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // Show a toast message if the internet is still not available
                Toast.makeText(this, "No internet connection. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigation_bar_color)
    }
}
