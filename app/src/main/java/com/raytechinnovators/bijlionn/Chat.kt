package com.raytechinnovators.bijlionn

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.raytechinnovators.bijlionn.adapters.UserAdapter
import com.raytechinnovators.bijlionn.models.AdsModel
import com.raytechinnovators.bijlionn.models.User


class Chat : AppCompatActivity() {
    private lateinit var database: FirebaseDatabase
    var bottom_navigation: BottomNavigationView? = null
    var button: Button? = null
    private lateinit var badge: BadgeDrawable

    private lateinit var currentUser: FirebaseUser
    private lateinit var userAdapter: UserAdapter
    private lateinit var userList: MutableList<User>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var databaseRef: DatabaseReference
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var newsBadge: BadgeDrawable
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private lateinit var adManager: AdManager



    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        Log.d(TAG, "onCreate called")
        networkChangeReceiver = NetworkChangeReceiver()
        // Initialize views
        userRecyclerView = findViewById(R.id.userRecyclerView)
        shimmerFrameLayout = findViewById(R.id.shimmer_view_container)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        bottom_navigation = findViewById(R.id.bottom_navigation)

        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_500)
        // Change the background color
        bottom_navigation!!.setBackgroundColor(resources.getColor(R.color.light_green))
        bottom_navigation!!.selectedItemId = R.id.chat

        // Create badge and attach it to the chat menu item
        badge = bottom_navigation!!.getOrCreateBadge(R.id.chat)
        badge.isVisible = false
        // Initialize the news badge
        newsBadge = bottom_navigation!!.getOrCreateBadge(R.id.home)
        newsBadge.isVisible = false // Ensure badge is initially hidden

        bottom_navigation!!.setOnNavigationItemSelectedListener { item ->
            val itemId = item.itemId
            Log.d(TAG, "BottomNavigation item selected: $itemId")
            when (itemId) {
                CHAT_ID -> {}
                BILL_ID -> startActivity(Intent(applicationContext, Bill::class.java))
                QUIZ_ID -> startActivity(Intent(applicationContext, Game::class.java))
                HOME_ID -> startActivity(Intent(applicationContext, MainActivity::class.java))
                ACCOUNT_ID -> startActivity(Intent(applicationContext, Account::class.java))
            }
            overridePendingTransition(0, 0)

            true

        }

        adManager = AdManager(this, findViewById(R.id.adContainer))

        val database = Firebase.database
        val adsRef = database.getReference("ads")
        adsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Fetch AdsModel and ad configuration
                val adsModel = dataSnapshot.getValue(AdsModel::class.java)
                val COLLAPSESIBLE_BANNER_AD_ID = "ca-app-pub-3940256099942544/2014213617"
                val adUnitId = dataSnapshot.child("adIdCollapsibleBanner").value?.toString() ?: COLLAPSESIBLE_BANNER_AD_ID
                val showCollapsibleBannerAd = adsModel?.showCollapsibleBannerAd ?: false
                // Load native ad if enabled
                if (showCollapsibleBannerAd) {
                    // Load the native ad if ads are enabled
                    adManager.showBannerAd(adUnitId)
                } else {
                    Log.d("AdConfig", "Ads are disabled, skipping ad load")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })


        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userList = mutableListOf()
        userAdapter = UserAdapter(this, userList)
        userRecyclerView.adapter = userAdapter
        currentUser = FirebaseAuth.getInstance().currentUser!!
        databaseRef = FirebaseDatabase.getInstance().getReference("users")


        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "SwipeRefreshLayout triggered")
            fetchUsers()
        }

        fetchTotalUserCount()
        fetchTotalNewsCount()
        changeNavigationBarColor()

        /// token moved to mainActivity from here
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Switch to home tab
                bottom_navigation!!.selectedItemId = HOME_ID
                // Call the default behavior of onBackPressed()
                finish()
            }
        })


    }

    fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigation_bar_color)
    }


    private fun fetchTotalNewsCount() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserUid != null) {
            val userReference =
                FirebaseDatabase.getInstance().getReference("users").child(currentUserUid)

            userReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val newsCount = dataSnapshot.child("newsCount").getValue(Int::class.java) ?: 0
                    // Update the news badge count
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
            // Show the badge with the news count
            newsBadge.isVisible = true
            newsBadge.number = count
        } else {
            // Hide the badge if there are no s items
            newsBadge.isVisible = false
        }
    }


    private fun fetchTotalUserCount() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d(TAG, "fetchTotalUserCount called")



        if (currentUserUid != null) {
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

                    Log.d(TAG, "Total user count: $totalUserCount")
                    updateBadgeCount(totalUserCount)


                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "fetchTotalUserCount onCancelled: ${databaseError.message}")
                }
            })
        }
    }

    private fun updateBadgeCount(count: Int) {
        Log.d(TAG, "updateBadgeCount called with count: $count")
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

    private fun fetchUsers() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d(TAG, "fetchUsers called")

        if (currentUserUid != null) {
            shimmerFrameLayout.startShimmer()
            userRecyclerView.visibility = View.GONE
            shimmerFrameLayout.visibility = View.VISIBLE

            databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    userList.clear()
                    val isAdmin = isAdmin(currentUserUid, dataSnapshot)
                    val userWithLastMessageList = mutableListOf<Pair<User, Long>>()
                    val childrenCount = dataSnapshot.childrenCount.toInt()
                    var processedCount = 0
                    val processedUserUids = HashSet<String>()

                    for (snapshot in dataSnapshot.children) {
                        val user = snapshot.getValue(User::class.java)
                        val userUid = user?.uid ?: continue
                        if (processedUserUids.contains(userUid)) continue
                        processedUserUids.add(userUid)
                        val isUserAdmin = isAdmin(userUid, dataSnapshot)

                        if (!isAdmin && isUserAdmin) {
                            user.let { userList.add(it) }
                            processedCount++
                            if (processedCount == childrenCount) {
                                stopShimmerAndRefresh()
                            }
                            continue
                        }

                        if (isAdmin && !isUserAdmin) {
                            val roomId = getRoomId(currentUserUid, userUid)
                            val chatRef = FirebaseDatabase.getInstance()
                                .getReference("chats/$roomId/$currentUserUid/lastMessageTime")
                            chatRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(chatSnapshot: DataSnapshot) {
                                    val lastMessageTime = chatSnapshot.getValue(Long::class.java)
                                    if (lastMessageTime != null) {
                                        userWithLastMessageList.add(Pair(user, lastMessageTime))
                                    }
                                    processedCount++
                                    if (processedCount == childrenCount) {
                                        userWithLastMessageList.sortByDescending { it.second }
                                        userList.addAll(userWithLastMessageList.map { it.first })
                                        stopShimmerAndRefresh()
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    Log.e(
                                        TAG,
                                        "fetchUsers chatRef onCancelled: ${databaseError.message}"
                                    )
                                    processedCount++
                                    if (processedCount == childrenCount) {
                                        stopShimmerAndRefresh()
                                    }
                                }
                            })
                        } else {
                            processedCount++
                            if (processedCount == childrenCount) {
                                stopShimmerAndRefresh()
                            }
                        }
                    }

                    if (processedCount == childrenCount) {
                        stopShimmerAndRefresh()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "fetchUsers databaseRef onCancelled: ${databaseError.message}")
                    stopShimmerAndRefresh()
                }
            })
        }
    }

    private fun stopShimmerAndRefresh() {
        Log.d(TAG, "stopShimmerAndRefresh called")
        shimmerFrameLayout.stopShimmer()
        shimmerFrameLayout.visibility = View.GONE
        userRecyclerView.visibility = View.VISIBLE
        swipeRefreshLayout.isRefreshing = false
        userAdapter.notifyDataSetChanged()
    }

    fun getRoomId(senderId: String, receiverId: String): String {
        return if (senderId < receiverId) {
            senderId + receiverId
        } else {
            receiverId + senderId
        }
    }

    private fun isAdmin(userUid: String, dataSnapshot: DataSnapshot): Boolean {
        val userSnapshot = dataSnapshot.child(userUid)
        val isAdmin = userSnapshot.child("admin").getValue(Boolean::class.java)
        return isAdmin ?: false
    }

    companion object {
        val HOME_ID = R.id.home
        val CHAT_ID = R.id.chat
        val BILL_ID = R.id.bill
        val QUIZ_ID = R.id.quiz
        val ACCOUNT_ID = R.id.account

        private const val CHAT_NODE = "chats"
        private const val MESSAGES_NODE = "messages"
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        MainActivity.UserStatusManager.setIsOnline(true)
        fetchUsers()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        MainActivity.UserStatusManager.setIsOnline(false)
        unregisterReceiver(networkChangeReceiver)
    }

}
