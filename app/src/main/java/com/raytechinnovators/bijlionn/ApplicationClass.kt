package com.raytechinnovators.bijlionn

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.database
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.raytechinnovators.AppOpenAdManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApplicationClass : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()


        // Initialize Firebase
        FirebaseApp.initializeApp(this@ApplicationClass)
        // Initialize Mobile Ads SDK
        MobileAds.initialize(this)

        // Initialize Google Mobile Ads SDK and AppOpenAdManager in the background
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {

            // Initialize the Google Mobile Ads SDK on the main thread
            withContext(Dispatchers.Main) {
                MobileAds.initialize(this@ApplicationClass) {}
            }

            // Initialize App Open Ad Manager on the main thread
            withContext(Dispatchers.Main) {
                appOpenAdManager = AppOpenAdManager(this@ApplicationClass)
            }
        }
    }
}
