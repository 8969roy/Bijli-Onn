package com.royllow.admin

import android.content.Context
import android.content.SharedPreferences

class MyAccountSharedPreferences(context: Context) {
    private val prefs: SharedPreferences

    init {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val location: String?
        get() = prefs.getString(KEY_LOCATION, "")
    val name: String?
        get() = prefs.getString(KEY_NAME, "")
    val number: String?
        get() = prefs.getString(KEY_NUMBER, "")
    val profileImage: String?
        get() = prefs.getString(KEY_IMAGE_URL, "")

    fun saveUserData(name: String?, ProfileImage: String?, phoneNumber: String?) {
        val editor = prefs.edit()
        editor.putString(KEY_NAME, name)
        editor.putString(KEY_NUMBER, phoneNumber)
        editor.putString(KEY_IMAGE_URL, ProfileImage)
        editor.apply()
    }

    fun saveUserData(location: String?) {
        val editor = prefs.edit()
        editor.putString(KEY_LOCATION, location)
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "my_prefs"
        private const val KEY_NAME = "name"
        private const val KEY_NUMBER = "phoneNumber"
        private const val KEY_IMAGE_URL = "profileImage"
        private const val KEY_LOCATION = "location"
    }
}