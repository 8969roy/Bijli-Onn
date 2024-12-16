package com.raytechinnovators.bijlionn

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.Manifest


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.Firebase
import com.raytechinnovators.bijlionn.Account.Companion.ACCOUNT_ID
import com.raytechinnovators.bijlionn.Account.Companion.BILL_ID
import com.raytechinnovators.bijlionn.Account.Companion.CHAT_ID
import com.raytechinnovators.bijlionn.Account.Companion.HOME_ID
import com.raytechinnovators.bijlionn.Account.Companion.QUIZ_ID
import com.raytechinnovators.bijlionn.adapters.NewsAdapter
import com.raytechinnovators.bijlionn.adapters.SpecialNewsViewHolder
import com.raytechinnovators.bijlionn.models.AdsModel
import com.raytechinnovators.bijlionn.models.LocationStatus
import com.raytechinnovators.bijlionn.models.NewsItem

private var appOpenAd: AppOpenAd? = null
private var loadTime: Long = 0

class MainActivity : AppCompatActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var shimmer: ShimmerFrameLayout
    private lateinit var shimmero: ShimmerFrameLayout
    private var nativeAd: NativeAd? = null
    private lateinit var adManager: AdManager
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewHorizontal: RecyclerView
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var specialNewsViewHolder: SpecialNewsViewHolder
    private lateinit var newsList: MutableList<NewsItem>
    private lateinit var videoList: MutableList<NewsItem>
    private lateinit var setFeeder: TextView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var badge: BadgeDrawable
    private lateinit var newsBadge: BadgeDrawable

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var chargingSourceTextView: TextView
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private var batteryReceiver: BroadcastReceiver? = null
    private lateinit var appUpdateManager: AppUpdateManager
    private val TAG = "MainActivity"


    private var shouldShowBottomSheet = true


    // Handle back button press using onBackPressedDispatcher
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        networkChangeReceiver = NetworkChangeReceiver()
        val isCharging = intent?.getBooleanExtra("isCharging", false) ?: false
        Log.d("MainActivity", "Charging state received: $isCharging")
        updateFirebaseChargingStatus(isCharging)

        MobileAds.initialize(this) { }
        changeNavigationBarColor()


        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_500)


        initializeViews()
        setupBottomNavigation()
        setupRecyclerViews()

        setupCurrentUser()
        checkForAppUpdate()


        // Notification permission check
        checkNotificationPermission()

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Create badge and attach it to the chat menu item
        badge = bottomNavigation.getOrCreateBadge(R.id.chat)
        badge.isVisible = false // Ensure badge is visible

        newsBadge = bottomNavigation.getOrCreateBadge(R.id.home)
        newsBadge.isVisible = false // Ensure badge is initially hidden

        // Call function to fetch users with unread messages and calculate the count
        fetchTotalUserCount()
        fetchTotalNewsCount()


        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "SwipeRefreshLayout triggered")
            setupFirebase()
        }


        FirebaseMessaging.getInstance().subscribeToTopic("news").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM", "Subscribed to news topic")
            } else {
                Log.e("FCM", "Subscription to news topic failed", task.exception)
            }
        }

        chargingSourceTextView = findViewById(R.id.chargingSourceTextView)

        // Register receiver for battery status changes
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let { updateChargingMetrics(it) }
            }
        }
        val batteryStatusFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, batteryStatusFilter)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (shouldShowBottomSheet) {
                    // Show the BottomSheetDialog instead of calling super.onBackPressed()
                    showExitBottomSheet()

                    // Listen for ad configuration changes from Firebase Database
                    val database = Firebase.database
                    val adsRef = database.getReference("ads")
                    adsRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            // Fetch AdsModel and ad configuration
                            val adsModel = dataSnapshot.getValue(AdsModel::class.java)
                            val showExitNativeAd = adsModel?.showExitNativeAd ?: false
                            val EXIT_NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"
                            val adUnitId = dataSnapshot.child("adIdExitNative").value?.toString() ?:EXIT_NATIVE_AD_ID

                            // Load native ad if enabled
                            if (showExitNativeAd) {
                                loadNativeAd(adUnitId)
                            } else {
                                Log.d("AdConfig", "Ads are disabled, skipping ad load")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }


                    })

                } else {
                    // Call the default behavior of onBackPressed()
                    finish() // or super.onBackPressed() if needed
                }
            }
        })
        //////token move fro chat activity
        database = FirebaseDatabase.getInstance()
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener { token ->
            val map = HashMap<String, Any>()
            map["token"] = token
            FirebaseAuth.getInstance().uid?.let {
                database.getReference().child("users").child(it).updateChildren(map)
            }
            //Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();
        }

    }

    fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigation_bar_color)
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 (API level 33) and above
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission already granted, proceed with notification setup
            } else {
                // Request permission
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 (API level 32)
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission already granted, proceed with notification setup
            } else {
                // Request permission
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        } else {
            // For Android 11 (API level 31) and below
            // No permission required, proceed with notification setup
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with notification setup
                } else {
                    // Permission denied, handle accordingly
                }
            }
        }
    }


    private lateinit var bottomSheetView: View

    private fun showExitBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)

        // Inflate the custom bottom sheet layout
        bottomSheetView = layoutInflater.inflate(R.layout.exit_bottom_sheet, null)
        bottomSheetDialog.window?.navigationBarColor =
            ContextCompat.getColor(this, R.color.dialog_white)

        // Set the content view for the bottom sheet dialog
        bottomSheetDialog.setContentView(bottomSheetView)

        // Dismiss bottom sheet on clicking the "Exit" button
        bottomSheetView.findViewById<Button>(R.id.exit_button).setOnClickListener {
            bottomSheetDialog.dismiss()
            finishAffinity() // Closes the app
        }

        // Dismiss bottom sheet on clicking the "Cancel" button
        bottomSheetView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            bottomSheetDialog.dismiss()
        }


        // Optional: Add a dimming effect for smoother UX
        bottomSheetDialog.dismissWithAnimation = true

        // Show the bottom sheet
        bottomSheetDialog.show()
        changeNavigationBarColor()
    }



    private fun loadNativeAd(adUnitId: Any) {
        val adLoader = AdLoader.Builder(this, adUnitId.toString())
            .forNativeAd { ad: NativeAd ->
            // If an existing native ad exists, destroy it before loading a new one
            nativeAd?.destroy()
            nativeAd = ad
            val adView = layoutInflater.inflate(R.layout.admob_native_medium, null) as NativeAdView
            populateNativeAdView(ad, adView)
            val adContainer = bottomSheetView.findViewById<FrameLayout>(R.id.native_ad_container)
            adContainer.removeAllViews()
            adContainer.addView(adView)
        }.withAdListener(object : com.google.android.gms.ads.AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Toast.makeText(
                    this@MainActivity, "Ad failed to load: ${adError.message}", Toast.LENGTH_SHORT
                ).show()
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Headline
        adView.findViewById<TextView>(R.id.ad_headline).text = nativeAd.headline
        adView.headlineView = adView.findViewById(R.id.ad_headline)

        // App Icon
        val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
        nativeAd.icon?.let {
            iconView.visibility = VISIBLE
            iconView.setImageDrawable(it.drawable)
        } ?: run { iconView.visibility = GONE }
        adView.iconView = iconView

        // Body
        val bodyView = adView.findViewById<TextView>(R.id.ad_body)
        bodyView.text = nativeAd.body
        adView.bodyView = bodyView

        // Advertiser
        val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
        nativeAd.advertiser?.let {
            advertiserView.text = it
            advertiserView.visibility = VISIBLE
        } ?: run { advertiserView.visibility = GONE }
        adView.advertiserView = advertiserView

        // Price
        val priceView = adView.findViewById<TextView>(R.id.ad_price)
        nativeAd.price?.let {
            priceView.text = it
            priceView.visibility = VISIBLE
        } ?: run { priceView.visibility = GONE }
        adView.priceView = priceView

        // Store
        val storeView = adView.findViewById<TextView>(R.id.ad_store)
        nativeAd.store?.let {
            storeView.text = it
            storeView.visibility = VISIBLE
        } ?: run { storeView.visibility = GONE }
        adView.storeView = storeView

        // Rating
        val ratingBar = adView.findViewById<RatingBar>(R.id.ad_stars)
        nativeAd.starRating?.let {
            ratingBar.rating = it.toFloat()
            ratingBar.visibility = VISIBLE
        } ?: run { ratingBar.visibility = GONE }
        adView.starRatingView = ratingBar

        // MediaView (for video or images)
        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        adView.mediaView = mediaView

        // Call to Action Button
        adView.findViewById<Button>(R.id.ad_call_to_action).apply {
            text = nativeAd.callToAction
            visibility = if (nativeAd.callToAction != null) VISIBLE else INVISIBLE
        }
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

        // Set the native ad to the view
        adView.setNativeAd(nativeAd)
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryReceiver?.let {
            unregisterReceiver(it)
        }
        nativeAd?.destroy()
    }


    // Function to update Firebase Charging Status
    private fun updateFirebaseChargingStatus(isCharging: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().reference
        val userRef = databaseRef.child("users").child(userId)

        // Fetch the location of the current user
        userRef.child("location").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val location = dataSnapshot.getValue(String::class.java) ?: return
                updateChargingStatusForLocation(isCharging, userId, location)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Failed to read user's location: $databaseError")
            }
        })
    }

    // Function to update charging status for a location
    private fun updateChargingStatusForLocation(
        isCharging: Boolean, userId: String, location: String
    ) {
        val databaseRef = FirebaseDatabase.getInstance().reference

        // Update Device Status for the current user
        val deviceStatusRef =
            databaseRef.child("ChargingStatus").child("DeviceStatus").child(userId)
        deviceStatusRef.setValue(isCharging).addOnSuccessListener {
            Log.d(TAG, "Device charging status updated successfully for userId: $userId")
            // Now update the location status after successfully updating device status
            updateLocationStatus(databaseRef, location)
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error updating device charging status for userId: $userId", e)
        }
    }


    // Function to update location status based on device statuses
    private fun updateLocationStatus(databaseRef: DatabaseReference, location: String) {
        val locationRef = databaseRef.child("ChargingStatus").child(location)
        val deviceStatusRef = databaseRef.child("ChargingStatus").child("DeviceStatus")

        deviceStatusRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val chargingDevicesCount =
                    dataSnapshot.children.count { it.getValue(Boolean::class.java) == true }

                locationRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(mutableData: MutableData): Transaction.Result {
                        val locationStatus =
                            mutableData.getValue(LocationStatus::class.java) ?: LocationStatus()

                        locationStatus.current = chargingDevicesCount
                        locationStatus.maximum =
                            maxOf(locationStatus.maximum, locationStatus.current)
                        locationStatus.minimum =
                            minOf(locationStatus.minimum, locationStatus.current)

                        mutableData.value = locationStatus
                        return Transaction.success(mutableData)
                    }

                    override fun onComplete(
                        databaseError: DatabaseError?,
                        committed: Boolean,
                        dataSnapshot: DataSnapshot?
                    ) {
                        if (committed) {
                            Log.d(
                                TAG,
                                "Firebase transaction completed successfully for location: $location"
                            )
                        } else {
                            databaseError?.let {
                                Log.e(
                                    TAG,
                                    "Firebase transaction failed for location: $location, error: $it"
                                )
                            }
                        }
                    }
                })
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Error reading device statuses: $databaseError")
            }
        })
    }

    // Function to handle charging metrics update
    private fun updateChargingMetrics(intent: Intent) {
        val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging: Boolean =
            (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)

        val chargePlug: Int = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        var chargingSource: String = "False"

        if (isCharging) {
            chargingSource = when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_AC -> "True"
                else -> "False"
            }
            updateFirebaseChargingStatus(true)
        } else {
            updateFirebaseChargingStatus(false)
        }

        runOnUiThread {
            chargingSourceTextView.text = chargingSource
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        setFeeder = findViewById(R.id.SetFeeder)
        shimmer = findViewById(R.id.shimmer)
        shimmero = findViewById(R.id.shimmer_two)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        recyclerView = findViewById(R.id.recycler_view_news)
        recyclerViewHorizontal = findViewById(R.id.Horizontal_view)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setBackgroundColor(ContextCompat.getColor(this, R.color.light_green))
        bottomNavigation.selectedItemId = R.id.home
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                HOME_ID -> { /* Handle home click */
                }

                BILL_ID -> startActivity(Intent(applicationContext, Bill::class.java))
                CHAT_ID -> startActivity(Intent(applicationContext, Chat::class.java))
                QUIZ_ID -> startActivity(Intent(applicationContext, Game::class.java))
                ACCOUNT_ID -> startActivity(Intent(applicationContext, Account::class.java))
            }
            overridePendingTransition(0, 0)
            true
        }
    }

    private fun setupRecyclerViews() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager

        val layoutManagerHorizontal =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewHorizontal.layoutManager = layoutManagerHorizontal

        shimmer.startShimmer()
        shimmero.startShimmer()

        newsList = mutableListOf()
        videoList = mutableListOf()

        specialNewsViewHolder = SpecialNewsViewHolder(videoList, true)
        recyclerViewHorizontal.adapter = specialNewsViewHolder

        newsAdapter = NewsAdapter(newsList, this)
        recyclerView.adapter = newsAdapter
    }

    private fun setupFirebase() {
        val database = FirebaseDatabase.getInstance()
        val newsRef = database.getReference("news")

        shimmer.startShimmer()
        recyclerView.visibility = View.GONE
        shimmer.visibility = View.VISIBLE

        shimmero.startShimmer()
        recyclerViewHorizontal.visibility = View.GONE
        shimmero.visibility = View.VISIBLE

        newsRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                newsList.clear()
                videoList.clear()

                for (data in snapshot.children) {
                    val newsItem = data.getValue(NewsItem::class.java)
                    if (!newsItem?.imageUrl.isNullOrEmpty()) {
                        if (newsItem != null) {
                            newsList.add(newsItem)
                        }
                    }
                    if (!newsItem?.videoUrl.isNullOrEmpty()) {
                        if (newsItem != null) {
                            videoList.add(newsItem)
                        }
                    }
                }

                newsAdapter.notifyDataSetChanged()
                specialNewsViewHolder.notifyDataSetChanged()
                stopShimmerAndRefresh()

                newsList.reverse()
                videoList.reverse()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read news data", error.toException())
            }
        })
    }

    private fun stopShimmerAndRefresh() {
        shimmer.stopShimmer()
        shimmer.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        shimmero.stopShimmer()
        shimmero.visibility = View.GONE
        recyclerViewHorizontal.visibility = View.VISIBLE

        swipeRefreshLayout.isRefreshing = false
    }

    private fun setupCurrentUser() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val databaseRef = FirebaseDatabase.getInstance().reference
            databaseRef.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val location = snapshot.child("location").getValue(String::class.java)
                        location?.let {
                            val mySharedPreferences = MyAccountSharedPreferences(this@MainActivity)
                            mySharedPreferences.saveUserData(it)
                            setFeeder.text = it
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Failed to read user data", error.toException())
                    }
                })
        }
    }

    private fun checkForAppUpdate() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(
                    AppUpdateType.IMMEDIATE
                )
            ) {
                requestAppUpdate(appUpdateInfo)
            }
        }
    }

    private fun requestAppUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo, AppUpdateType.IMMEDIATE, this, REQUEST_UPDATE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_UPDATE && resultCode != RESULT_OK) {
            checkForAppUpdate()
        }
    }

    override fun onResume() {
        super.onResume()
        UserStatusManager.setIsOnline(true)
        setupFirebase()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filter)


    }

    override fun onPause() {
        super.onPause()
        UserStatusManager.setIsOnline(false)
        unregisterReceiver(networkChangeReceiver)

    }

    private fun fetchTotalNewsCount() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        currentUserUid?.let {
            val userReference = FirebaseDatabase.getInstance().getReference("users").child(it)
            userReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val newsCount = dataSnapshot.child("newsCount").getValue(Int::class.java) ?: 0
                    updateNewsBadge(newsCount)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Error fetching news count", databaseError.toException())
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
        currentUserUid?.let {
            val userChatsRef = FirebaseDatabase.getInstance().getReference("chats")
            userChatsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var totalUserCount = 0
                    for (roomSnapshot in dataSnapshot.children) {
                        if (roomSnapshot.child(currentUserUid).exists()) {
                            val userCount = roomSnapshot.child(currentUserUid).child("UserCount")
                                .getValue(Int::class.java) ?: 0
                            totalUserCount += userCount
                        }
                    }
                    updateBadgeCount(totalUserCount)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Error fetching user count", databaseError.toException())
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
            }, 2000) // Delay hiding badge number for 2 seconds
        }
    }

    object UserStatusManager {
        private var isOnline = false
        private val handler = Handler(Looper.getMainLooper())

        fun setIsOnline(online: Boolean) {
            isOnline = online
            if (!online) {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({ updateUserStatusInFirebase() }, 5000) // Delay for 5 seconds
            } else {
                updateUserStatusInFirebase()
            }
        }

        private fun updateUserStatusInFirebase() {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            userId?.let {
                val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                userRef.child("status").setValue(isOnline)
                if (!isOnline) {
                    userRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)
                }
            }
        }
    }

    companion object {

        private const val REQUEST_UPDATE = 101
    }

}
