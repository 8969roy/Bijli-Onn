package com.raytechinnovators.bijlionn
import android.app.Application
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Remote Config
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        // Set default values for Remote Config
        val defaults = mapOf(
            "show_app_open_ad" to true,
            "show_banner_ad" to true,
            "show_collapsible_banner_ad" to true,
            "show_interstitial_ad" to true,
            "show_exit_native_ad" to true,
            "show_item_native_ad" to true,
            "show_bill_native_ad" to true,
            "show_success_native_ad" to true
        )

        remoteConfig.setDefaultsAsync(defaults)

        // Configure Remote Config settings
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Fetch and activate Remote Config values on app startup
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("RemoteConfig", "Config fetched and activated")

                    // Optionally log the fetched values
                    Log.d(
                        "RemoteConfig",
                        "show_app_open_ad = ${remoteConfig.getBoolean("show_app_open_ad")}"
                    )
                    Log.d(
                        "RemoteConfig",
                        "show_banner_ad= ${remoteConfig.getBoolean("show_banner_ad")}"
                    )
                    Log.d(
                        "RemoteConfig",
                        "show_collapsible_banner_ad = ${remoteConfig.getBoolean("show_collapsible_banner_ad")}"
                    )
                    Log.d(
                        "RemoteConfig",
                        "show_interstitial_ad= ${remoteConfig.getBoolean("show_interstitial_ad")}"
                    )
                    Log.d(
                        "RemoteConfig",
                        "show_exit_native_ad = ${remoteConfig.getBoolean("show_exit_native_ad")}"
                    )
                    Log.d(
                        "RemoteConfig",
                        "show_item_native_ad = ${remoteConfig.getBoolean("show_item_native_ad")}"
                    )
                    Log.d(
                        "RemoteConfig",
                        "show_bill_native_ad = ${remoteConfig.getBoolean("show_bill_native_ad")}"
                    )
                    Log.d(
                        "RemoteConfig",
                        "show_success_native_ad = ${remoteConfig.getBoolean("show_success_native_ad")}"
                    )

                } else {
                    Log.e("RemoteConfig", "Config fetch failed")
                }
            }
    }
}


