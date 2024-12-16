package com.raytechinnovators

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.raytechinnovators.bijlionn.ApplicationClass
import com.raytechinnovators.bijlionn.Splash
import com.raytechinnovators.bijlionn.models.AdsModel
import java.util.*

class AppOpenAdManager(private val application: ApplicationClass) : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isAdLoading = false
    private var loadTime: Long = 0
    private var showAppOpenAd = false // Cache the ad display condition
    private var adUnitId: String = "" // Store the ad unit ID

    init {
        application.registerActivityLifecycleCallbacks(this)

        val database = Firebase.database
        val adsRef = database.getReference("ads")

        // Listen for ad configuration changes from Firebase Database
        adsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val adsModel = dataSnapshot.getValue(AdsModel::class.java)
                showAppOpenAd = adsModel?. showAppOpenAd ?: false // Update showAppOpenAd

                val SHOW_APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"
                adUnitId = dataSnapshot.child("adIdShowAppOpen").value?.toString() ?: SHOW_APP_OPEN_AD_ID

                // Only load the ad if enabled in Firebase
                if (showAppOpenAd) {
                    loadAppOpenAd(adUnitId)
                } else {
                    Log.d("AdConfig", "Ads are disabled, skipping ad load")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("AdConfig", "Failed to load ads config", error.toException())
            }
        })
    }

    private fun loadAppOpenAd(adUnitId: String) {
        if (!showAppOpenAd || isAdAvailable()) {
            return // Stop if ads are disabled or already available
        }

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            adUnitId,
            adRequest,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTime = Date().time
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isAdLoading = false
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && Date().time - loadTime < 4 * 60 * 60 * 1000 // 4 hours validity
    }

    private fun showAdIfAvailable(activity: Activity) {
        // Check if ads should be displayed based on the Firebase flag
        if (showAppOpenAd && isAdAvailable()) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    loadAppOpenAd(adUnitId) // Load a new ad after dismissal if enabled
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    loadAppOpenAd(adUnitId) // Retry loading if it fails
                }

                override fun onAdShowedFullScreenContent() {
                    appOpenAd = null // Clear the reference after showing
                }
            }
            appOpenAd?.show(activity)
        } else {
            Log.d("AdConfig", "Ad display condition not met or ad unavailable")
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity is Splash) { // Show the ad after the splash screen is done
            showAdIfAvailable(activity)
        }
    }

    // Other lifecycle methods are required but not used
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
