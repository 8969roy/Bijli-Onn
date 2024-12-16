package com.raytechinnovators.bijlionn

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.raytechinnovators.bijlionn.Account.Companion.ACCOUNT_ID
import com.raytechinnovators.bijlionn.Account.Companion.BILL_ID
import com.raytechinnovators.bijlionn.Account.Companion.CHAT_ID
import com.raytechinnovators.bijlionn.Account.Companion.HOME_ID
import com.raytechinnovators.bijlionn.Account.Companion.QUIZ_ID
import com.raytechinnovators.bijlionn.models.AdsModel

class Bill : AppCompatActivity() {
    private var bottomNavigation: BottomNavigationView? = null
    private lateinit var checkLayout: LinearLayout
    private lateinit var payLayout: LinearLayout
    private lateinit var earnEzBtn: Button
    private lateinit var install:  Button
    private lateinit var badge: BadgeDrawable
    private lateinit var clickCountTextView: TextView
    private lateinit var newsBadge: BadgeDrawable
    private lateinit var networkChangeReceiver: NetworkChangeReceiver

    // Default Ad Unit ID
    private val BILL_NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"
    private var nativeAd: NativeAd? = null
    private lateinit var nativeAdContainer: FrameLayout

    private val links = listOf(
        "http://www.youtube.com/@entrepreneurXadventurer",
        "http://www.youtube.com/@entrepreneurXadventurer",
        "http://www.youtube.com/@entrepreneur.Bikram.roy",

    )
    private var linkIndex = 0
    private var clickCount = 0


    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill)
        networkChangeReceiver = NetworkChangeReceiver()
        initViews()
        setupBottomNavigation()
        setupButtons()
        setupFirebaseListeners()
        changeNavigationBarColor()

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this)
        val database = Firebase.database
        val adsRef = database.getReference("ads")
        // Initialize native ad container view
        nativeAdContainer = findViewById(R.id.native_ad_container)

        // Listen for ad configuration changes from Firebase Database
        adsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Fetch AdsModel and ad configuration
                val adsModel = dataSnapshot.getValue(AdsModel::class.java)
                val showBillNativeAd = adsModel?.showBillNativeAd ?: false
                val adUnitId = dataSnapshot.child("adIdBillNative").value?.toString() ?:BILL_NATIVE_AD_ID

                // Load native ad if enabled
                if (showBillNativeAd) {
                    loadNativeAd(adUnitId)
                } else {
                    Log.d("AdConfig", "Ads are disabled, skipping ad load")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("AdConfig", "Failed to load ads config", error.toException())
            }
        })

        // Optional: change navigation bar color
        changeNavigationBarColor()
    }

    // Change navigation bar color
    private fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigation_bar_color)
    }


    // Load native ad with specific Ad Unit ID
    private fun loadNativeAd(adUnitId: String) {
        val adLoader = AdLoader.Builder(this, adUnitId)
            .forNativeAd { ad: NativeAd ->
                // Destroy the previous ad, if any
                nativeAd?.destroy()
                nativeAd = ad

                // Inflate and populate the ad view layout
                val adView =
                    layoutInflater.inflate(R.layout.admob_native_medium, null) as NativeAdView
                populateNativeAdView(ad, adView)

                // Update UI by adding the populated ad view
                nativeAdContainer.removeAllViews()
                nativeAdContainer.addView(adView)
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("AdLoader", "Failed to load native ad: ${adError.message}")
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        // Load the ad
        adLoader.loadAd(AdRequest.Builder().build())
    }

    // Populate native ad view with ad data
    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.apply {
            // Headline
            findViewById<TextView>(R.id.ad_headline).apply {
                text = nativeAd.headline
                headlineView = this
            }

            // Icon
            findViewById<ImageView>(R.id.ad_app_icon).apply {
                visibility = if (nativeAd.icon != null) VISIBLE else GONE
                nativeAd.icon?.let { setImageDrawable(it.drawable) }
                iconView = this
            }

            // Body
            findViewById<TextView>(R.id.ad_body).apply {
                text = nativeAd.body
                bodyView = this
            }

            // Advertiser
            findViewById<TextView>(R.id.ad_advertiser).apply {
                visibility = if (nativeAd.advertiser != null) VISIBLE else GONE
                text = nativeAd.advertiser
                advertiserView = this
            }

            // Price
            findViewById<TextView>(R.id.ad_price).apply {
                visibility = if (nativeAd.price != null) VISIBLE else GONE
                text = nativeAd.price
                priceView = this
            }

            // Store
            findViewById<TextView>(R.id.ad_store).apply {
                visibility = if (nativeAd.store != null) VISIBLE else GONE
                text = nativeAd.store
                storeView = this
            }

            // Rating
            findViewById<RatingBar>(R.id.ad_stars).apply {
                visibility = if (nativeAd.starRating != null) VISIBLE else GONE
                rating = nativeAd.starRating?.toFloat() ?: 0f
                starRatingView = this
            }

            // MediaView
            mediaView = findViewById(R.id.ad_media)

            // Call to Action
            findViewById<Button>(R.id.ad_call_to_action).apply {
                text = nativeAd.callToAction
                visibility = if (nativeAd.callToAction != null) VISIBLE else INVISIBLE
                callToActionView = this
            }

            // Set the native ad to the view
            setNativeAd(nativeAd)
        }
    }

    // Clean up the native ad on activity destruction
    override fun onDestroy() {
        super.onDestroy()
        nativeAd?.destroy()
    }


    private fun initViews() {
        checkLayout = findViewById(R.id.checkLayout)
        payLayout = findViewById(R.id.payLayout)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        earnEzBtn = findViewById(R.id.earn_btn)
       install = findViewById(R.id.install)
        clickCountTextView = findViewById(R.id.click_count)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_500)
    }

    private fun setupBottomNavigation() {
        bottomNavigation?.apply {
            setBackgroundColor(resources.getColor(R.color.light_green))
            selectedItemId = R.id.bill
            setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    BILL_ID -> { /* Already on this screen */
                    }

                    CHAT_ID -> startActivity(Intent(applicationContext, Chat::class.java))
                    QUIZ_ID -> startActivity(Intent(applicationContext, Game::class.java))
                    HOME_ID -> startActivity(
                        Intent(
                            applicationContext, MainActivity::class.java
                        )
                    )

                    ACCOUNT_ID -> startActivity(Intent(applicationContext, Account::class.java))
                }
                overridePendingTransition(0, 0)
                true
            }
        }

        badge = bottomNavigation!!.getOrCreateBadge(R.id.chat)
        badge.isVisible = false

        newsBadge = bottomNavigation!!.getOrCreateBadge(R.id.home)
        newsBadge.isVisible = false


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Switch to home tab
                bottomNavigation!!.selectedItemId = Chat.HOME_ID
                // Call the default behavior of onBackPressed()
                finish()
            }
        })
    }


    private fun setupButtons() {
        earnEzBtn.setOnClickListener {
            openCustomTab("https://www.youtube.com/channel/UCtOkrq-48rP9YTpCrCVN4vA")
        }

        checkLayout.setOnClickListener {
            openCustomTab("https://www.mobikwik.com/jbvnl-jharkhand-electricity-bill-payment/")
        }

        payLayout.setOnClickListener {
            openCustomTab("https://suvidha.jbvnl.co.in/epi/login.aspx")
        }

        install.setOnClickListener {
            openCustomTab("https://linktr.ee/ChatWise")
        }
    }


    private fun setupFirebaseListeners() {
        val database = FirebaseDatabase.getInstance()
        val clickRef = database.getReference("clickCount")

        clickRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                clickCount = snapshot.getValue(Int::class.java) ?: 0
                updateTextView()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        earnEzBtn.setOnClickListener {
            openNextLink()
        }

        fetchTotalUserCount()
        fetchTotalNewsCount()
    }

    private fun openNextLink() {
        if (linkIndex < links.size) {
            openCustomTab(links[linkIndex])
            linkIndex++
            incrementClickCount()
        }
    }

    private fun openCustomTab(url: String) {
        val builder = CustomTabsIntent.Builder()
        builder.setToolbarColor(ContextCompat.getColor(this, R.color.purple_500))
        builder.addDefaultShareMenuItem()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
    }

    private fun incrementClickCount() {
        val database = FirebaseDatabase.getInstance()
        val clickRef = database.getReference("clickCount")

        clickCount++
        clickRef.setValue(clickCount).addOnCompleteListener {
            if (it.isSuccessful) {
                updateTextView()
            }
        }
    }


    private fun formatNumber(number: Int): String {
        return when {
            number >= 1_00_00_000 -> String.format("%.1f Cr", number / 1_00_00_000.0)
            number >= 1_00_000 -> String.format("%.1f L", number / 1_00_000.0)
            number >= 1_000 -> String.format("%.1f K", number / 1_000.0)
            else -> number.toString()
        }
    }


    private fun updateTextView() {
        // Calculate animation start and end positions
        val startY = 0f
        val endY = -clickCountTextView.height.toFloat()

        // Create translate animation from startY to endY
        val animation = TranslateAnimation(0f, 0f, startY, endY)
        animation.duration = 500
        animation.fillAfter = true

        // Set animation listener to update TextView after animation ends
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                // Reset translation to original position
                clickCountTextView.translationY = 0f

                // Update TextView with new value
                clickCountTextView.text = clickCount.toString()
                // Update TextView with new value
                clickCountTextView.text = formatNumber(clickCount)

                // Animate new value coming in from bottom
                val newAnimation =
                    TranslateAnimation(0f, 0f, clickCountTextView.height.toFloat(), 0f)
                newAnimation.duration = 500
                clickCountTextView.startAnimation(newAnimation)
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // Start the animation on the clickCountTextView
        clickCountTextView.startAnimation(animation)
    }


    private fun fetchTotalNewsCount() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserUid != null) {
            val userReference =
                FirebaseDatabase.getInstance().getReference("users").child(currentUserUid)
            userReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val newsCount =
                        dataSnapshot.child("newsCount").getValue(Int::class.java) ?: 0
                    updateNewsBadge(newsCount)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors
                }
            })
        }
    }

    private fun updateNewsBadge(count: Int) {
        if (count > 0) {
            newsBadge.isVisible = true
            newsBadge.number = count
        } else {
            newsBadge.isVisible = false
        }
    }

    private fun fetchTotalUserCount() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserUid != null) {
            val userChatsRef = FirebaseDatabase.getInstance().getReference("chats")
            userChatsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var totalUserCount = 0
                    for (roomSnapshot in dataSnapshot.children) {
                        if (roomSnapshot.child(currentUserUid).exists()) {
                            val userCount =
                                roomSnapshot.child(currentUserUid).child("UserCount")
                                    .getValue(Int::class.java) ?: 0
                            totalUserCount += userCount
                        }
                    }
                    updateBadgeCount(totalUserCount)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors
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
            }, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkChangeReceiver)
    }
}
