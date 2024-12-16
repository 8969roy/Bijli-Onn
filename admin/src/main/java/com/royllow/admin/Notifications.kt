package com.royllow.admin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView


class Notifications : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notfication)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
    bottomNavigation.selectedItemId = R.id.notfication
    bottomNavigation.setOnNavigationItemSelectedListener { item ->
        val itemId = item.itemId
        if (itemId == NOTIFICATION_ID) {
        } else if (itemId == CHAT_ID) {
            startActivity(Intent(applicationContext, Chats::class.java))
        } else if (itemId == HOME_ID) {
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }
        overridePendingTransition(0, 0)
        true
    }


    }


    companion object {
        val HOME_ID = R.id.home
        val CHAT_ID = R.id.chat
        val NOTIFICATION_ID = R.id.notfication

    }

}