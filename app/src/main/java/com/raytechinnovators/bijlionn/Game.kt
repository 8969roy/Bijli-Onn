package com.raytechinnovators.bijlionn

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.raytechinnovators.bijlionn.models.AdsModel

class Game : AppCompatActivity() {
    private var bottomNavigation: BottomNavigationView? = null
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private var loading: LottieAnimationView? = null
    private lateinit var badge: BadgeDrawable
    private lateinit var newsBadge: BadgeDrawable
    private var interstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_PROGRESS)
        setContentView(R.layout.activity_game)

        val toolbar: Toolbar = findViewById(R.id.toolbar5)
        networkChangeReceiver = NetworkChangeReceiver()
        loading = findViewById(R.id.loading)

        // Initialize AdMob
        MobileAds.initialize(this) {}
        // Call functions to fetch user-related counts
        fetchTotalUserCount()
        fetchTotalNewsCount()

        val database = FirebaseDatabase.getInstance()
        val adsRef = database.getReference("ads")
        adsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val adsModel = dataSnapshot.getValue(AdsModel::class.java)
                val INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
                val adUnitId = dataSnapshot.child("adIdInterstitial").value?.toString() ?: INTERSTITIAL_AD_ID
                val showInterstitialAd = adsModel?.showInterstitialAd ?: false

                if (showInterstitialAd) {
                    loadInterstitialAd(adUnitId)
                } else {
                    Log.d("AdConfig", "Ads are disabled, skipping ad load")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
            }
        })

        changeNavigationBarColor()

        // Set colors for status bar and bottom navigation
        window.statusBarColor = ContextCompat.getColor(this, R.color.game_status_bar)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation?.setBackgroundColor(ContextCompat.getColor(this, R.color.light_green))
        bottomNavigation?.selectedItemId = R.id.quiz

        bottomNavigation?.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                Chat.QUIZ_ID -> {}
                Chat.BILL_ID -> startActivity(Intent(applicationContext, Bill::class.java))
                Chat.CHAT_ID -> startActivity(Intent(applicationContext, Chat::class.java))
                Chat.HOME_ID -> startActivity(Intent(applicationContext, MainActivity::class.java))
                Chat.ACCOUNT_ID -> startActivity(Intent(applicationContext, Account::class.java))
            }
            overridePendingTransition(0, 0)
            true
        }

        badge = bottomNavigation!!.getOrCreateBadge(R.id.chat)
        badge.isVisible = false
        newsBadge = bottomNavigation!!.getOrCreateBadge(R.id.home)
        newsBadge.isVisible = false

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                bottomNavigation?.selectedItemId = Chat.HOME_ID
                finish()
            }
        })
    }

    private fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigation_bar_color)
    }

    private fun loadInterstitialAd(adUnitId: String) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, adUnitId, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
            })
    }

    private fun fetchTotalNewsCount() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        currentUserUid?.let { uid ->
            val userReference = FirebaseDatabase.getInstance().getReference("users").child(uid)
            userReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val newsCount = dataSnapshot.child("newsCount").getValue(Int::class.java) ?: 0
                    updateNewsBadge(newsCount)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Database error: ${databaseError.message}")
                }
            })
        }
    }

    private fun updateNewsBadge(count: Int) {
        newsBadge.isVisible = count > 0
        if (count > 0) {
            newsBadge.number = count
        } else {
            newsBadge.clearNumber()
        }
    }

    private fun fetchTotalUserCount() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        currentUserUid?.let { uid ->
            val userChatsRef = FirebaseDatabase.getInstance().getReference("chats")
            userChatsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var totalUserCount = 0
                    for (roomSnapshot in dataSnapshot.children) {
                        if (roomSnapshot.child(uid).exists()) {
                            val userCount = roomSnapshot.child(uid).child("UserCount").getValue(Int::class.java) ?: 0
                            totalUserCount += userCount
                        }
                    }
                    updateBadgeCount(totalUserCount)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Database error: ${databaseError.message}")
                }
            })
        }
    }

    private fun updateBadgeCount(count: Int) {
        if (count > 0) {
            badge.number = count
            badge.isVisible = true
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                badge.clearNumber()
                badge.isVisible = false
            }, 2000)
        }
    }

    companion object {
        val HOME_ID = R.id.home
        val CHAT_ID = R.id.chat
        val BILL_ID = R.id.bill
        val QUIZ_ID = R.id.quiz
        val ACCOUNT_ID = R.id.account
    }

    fun onImageClick(view: View) {
        loading?.visibility = View.VISIBLE
        loading?.playAnimation()

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null // Release ad reference
                launchUrl()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                launchUrl()
            }
        }

        interstitialAd?.show(this) ?: launchUrl()
    }

    private fun launchUrl() {
        val url = "https://www.gamezop.com/?id=4286"
        CustomTabsIntent.Builder()
            .setToolbarColor(ContextCompat.getColor(this, R.color.ColorPrimary))
            .build()
            .launchUrl(this, Uri.parse(url))
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(networkChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkChangeReceiver)
    }
}
