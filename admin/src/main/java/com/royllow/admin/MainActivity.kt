package com.royllow.admin

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.royllow.admin.Adapters.NewsAdapter
import com.royllow.admin.Adapters.SpecialNewsViewHolder
import com.royllow.admin.Models.NewsItem





class MainActivity : AppCompatActivity() {

    private lateinit var shimmer: ShimmerFrameLayout
    private lateinit var shimmero: ShimmerFrameLayout
    private var toolbar: Toolbar? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewHorizontal: RecyclerView
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var specialNewsViewHolder: SpecialNewsViewHolder
    private lateinit var neuzList: MutableList<NewsItem>
    private lateinit var vdoList: MutableList<NewsItem>
    private lateinit var setFeeder: TextView
    private lateinit var bottomNavigation: BottomNavigationView



    @SuppressLint("CutPasteId", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupBottomNavigation()
        setupRecyclerViews()
        setupFirebase()
        setupCurrentUser()
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
        bottomNavigation.selectedItemId = R.id.home
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                HOME_ID -> { /* Handle home click */ }

                CHAT_ID -> startActivity(Intent(applicationContext, Chats::class.java))
                NOTIFICATION_ID -> startActivity(Intent(applicationContext, Notifications::class.java))

            }
            overridePendingTransition(0, 0)
            true
        }
    }

    private fun setupRecyclerViews() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager

        val layoutManagerHorizontal = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewHorizontal.layoutManager = layoutManagerHorizontal

        shimmer.startShimmer()
        shimmero.startShimmer()

        neuzList = mutableListOf()
        vdoList = mutableListOf()

        specialNewsViewHolder = SpecialNewsViewHolder(vdoList, true)
        recyclerViewHorizontal.adapter = specialNewsViewHolder


        newsAdapter = NewsAdapter(neuzList,true )
        recyclerView.adapter = newsAdapter
    }

    private fun setupFirebase() {
        val database = FirebaseDatabase.getInstance()
        val newsRef = database.getReference("news")

        newsRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                neuzList.clear()
                vdoList.clear()

                for (data in snapshot.children) {
                    val newsItem = data.getValue(NewsItem::class.java)
                    newsItem?.let { neuzList.add(it) }
                    newsItem?.let { vdoList.add(it) }
                }

                newsAdapter.notifyDataSetChanged()
                specialNewsViewHolder.notifyDataSetChanged()
                shimmer.stopShimmer()
                shimmer.visibility = View.GONE
                shimmero.stopShimmer()
                shimmero.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                recyclerViewHorizontal.visibility = View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read news data", error.toException())
            }
        })
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

    companion object {
        val HOME_ID = R.id.home
        val CHAT_ID = R.id.chat
        val NOTIFICATION_ID = R.id.notfication

    }

}
