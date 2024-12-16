package com.raytechinnovators.bijlionn

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowMetrics
import android.widget.LinearLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError


class AdManager(private val context: Context, private val adContainer: LinearLayout) {
    private lateinit var adView: AdView

    // Get the ad size with screen width.
    private val adSize: AdSize
        get() {
            val displayMetrics = context.resources.displayMetrics
            val adWidthPixels =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val windowMetrics: WindowMetrics =
                        (context as? Activity)?.windowManager?.currentWindowMetrics
                            ?: return AdSize.BANNER
                    windowMetrics.bounds.width()
                } else {
                    displayMetrics.widthPixels
                }
            val density = displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
        }


    fun showBannerAd(adUnitId: String) {
        // Create a new ad view.
        val adView = AdView(context)
        adView.adUnitId = adUnitId // Correctly set the ad unit ID here
        adView.setAdSize(adSize)
        this.adView = adView

        // Replace ad container with new ad view.
        adContainer.removeAllViews()
        adContainer.addView(adView)

        // Start loading the ad in the background.
        val extras = Bundle()
        extras.putString("collapsible", "bottom")

        val adRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            .build()

        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Handle the error here if needed
            }

            override fun onAdLoaded() {
                // Perform any actions on ad load success here if needed
            }
        }

        // Load the ad after setting ad size and ad unit ID
        adView.loadAd(adRequest)
    }

}