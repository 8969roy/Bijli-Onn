package com.raytechinnovators.bijlionn.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.content.SharedPreferences
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.raytechinnovators.bijlionn.R
import com.raytechinnovators.bijlionn.models.AdsModel
import com.raytechinnovators.bijlionn.models.NewsItem

class NewsAdapter(private val newsList: MutableList<NewsItem>, private val context: Context) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    private var totalNewsCount: Int = newsList.size
    private var expandedPosition = RecyclerView.NO_POSITION
    private val clickedNewsItems = mutableSetOf<String>()
    private var userLoginTimestamp: Long = 0
    private lateinit var sharedPreferences: SharedPreferences

    val database = Firebase.database
    val adsRef = database.getReference("ads")
    // Initialize native ad container view

    init {
        loadUserLoginTimestamp()
        loadClickedNewsItemsFromFirebase()
        loadAdConfig()
        sharedPreferences = context.getSharedPreferences("clicked_items", Context.MODE_PRIVATE)

    }

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.Top_news)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.text_view_text)
        private val viewCountTextView: TextView = itemView.findViewById(R.id.text_view_count)
        private val timestampTextView: TextView = itemView.findViewById(R.id.text_view_timestamp)
        private val fullDescription: TextView = itemView.findViewById(R.id.text_show)
        private val fullImage: ImageView = itemView.findViewById(R.id.image_show)
        private val smallImage: ImageView = itemView.findViewById(R.id.image_view_show)
        private val redDotAnimation: LottieAnimationView =
            itemView.findViewById(R.id.redDotAnimation)
        private val headerLayout: LinearLayout = itemView.findViewById(R.id.headerLayout)
        private val bottomLayout: LinearLayout = itemView.findViewById(R.id.bottomLayout)
        val adView: AdView = itemView.findViewById(R.id.adView)
        val nativeAdContainer: FrameLayout = itemView.findViewById(R.id.native_ad_container)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    expandedPosition =
                        if (position != expandedPosition) position else RecyclerView.NO_POSITION
                    notifyDataSetChanged()

                    // Perform the view count action
                    viewCountAction(position)
                }

                // Check if the item has already been clicked
                if (position != RecyclerView.NO_POSITION) {
                    val newsItem = newsList[position]
                    val newsId = newsItem.generatedNewsId

                    if (newsId != null && !clickedNewsItems.contains(newsId)) {
                        clickedNewsItems.add(newsId)
                        redDotAnimation.visibility = View.INVISIBLE

                        if (totalNewsCount > 0) {
                            totalNewsCount--

                            notifyDataSetChanged()

                            updateNewsCountInFirebase(newsId)
                            updateTotalNewsCountInFirebase(totalNewsCount)
                        }
                    }
                }
            }
        }


        fun bind(newsItem: NewsItem) {
            Glide.with(itemView.context).load(newsItem.imageUrl)
                .placeholder(R.drawable.image_placeholder).error(R.drawable.image_placeholder)
                .into(fullImage)

            Glide.with(itemView.context).load(newsItem.imageUrl)
                .placeholder(R.drawable.image_placeholder).error(R.drawable.image_placeholder)
                .into(smallImage)

            val timeAgo = TimeAgo.using(newsItem.timestamp)
            timestampTextView.text = timeAgo
            titleTextView.text = newsItem.location
            descriptionTextView.text = newsItem.newsData
            fullDescription.text = newsItem.newsData

            viewCountTextView.text = formatViewCount(newsItem.viewCount)

            val isExpanded = adapterPosition == expandedPosition
            headerLayout.visibility = if (isExpanded) GONE else VISIBLE
            bottomLayout.visibility = if (isExpanded) VISIBLE else GONE

            redDotAnimation.visibility =
                if (newsItem.timestamp < userLoginTimestamp || clickedNewsItems.contains(newsItem.generatedNewsId)) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }
            redDotAnimation.playAnimation()
        }

        private fun formatViewCount(viewCount: Double): String {
            return if (viewCount >= 1000) {
                String.format("%.1fK", viewCount / 1000.0)
            } else {
                viewCount.toInt().toString()
            }
        }

        private fun viewCountAction(adapterPosition: Int) {
            if (adapterPosition in newsList.indices) {
                val newsItem = newsList[adapterPosition]
                newsItem.viewCount += 0.5
                FirebaseDatabase.getInstance().getReference("news")
                    .child(newsItem.generatedNewsId ?: return).child("viewCount")
                    .setValue(newsItem.viewCount).addOnSuccessListener {
                        Log.d(
                            "ViewCount",
                            "View count updated successfully for ${newsItem.generatedNewsId}"
                        )
                    }.addOnFailureListener { e ->
                        Log.e("ViewCount", "Failed to update view count: ${e.message}")
                    }
            }
        }


    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = (adView.findViewById<TextView>(R.id.primary)?.apply {
            text = nativeAd.headline
        } ?: Log.e("NativeAd", "Headline view is null")) as View?

        adView.bodyView = (adView.findViewById<TextView>(R.id.secondary)?.apply {
            text = nativeAd.body ?: ""
            visibility = if (text.isNotEmpty()) VISIBLE else GONE
        } ?: Log.e("NativeAd", "Body view is null")) as View?

        adView.callToActionView = (adView.findViewById<Button>(R.id.cta)?.apply {
            text = nativeAd.callToAction ?: ""
            visibility = if (text.isNotEmpty()) VISIBLE else GONE
        } ?: Log.e("NativeAd", "CTA view is null")) as View?

        adView.iconView = (adView.findViewById<ImageView>(R.id.icon)?.apply {
            nativeAd.icon?.let {
                setImageDrawable(it.drawable)
                visibility = VISIBLE
            } ?: run {
                visibility = GONE
            }
        } ?: Log.e("NativeAd", "Icon view is null")) as View?

        adView.findViewById<RatingBar>(R.id.rating_bar)?.apply {
            visibility = if (nativeAd.starRating != null) {
                rating = nativeAd.starRating!!.toFloat()
                VISIBLE
            } else {
                GONE
            }
        } ?: Log.e("NativeAd", "Rating bar is null")

        adView.findViewById<TextView>(R.id.ad_notification_view)?.apply {
            text = "Ad"
        } ?: Log.e("NativeAd", "Ad notification view is null")

        adView.setNativeAd(nativeAd)
    }

    private fun loadUserLoginTimestamp() {
        FirebaseAuth.getInstance().currentUser?.uid?.let { currentUserUid ->
            FirebaseDatabase.getInstance().getReference("users").child(currentUserUid)
                .child("loginTimestamp")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        userLoginTimestamp = snapshot.getValue(Long::class.java) ?: 0
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseError", "Failed to load login timestamp: ${error.message}")
                    }
                })
        }
    }

    private fun loadClickedNewsItemsFromFirebase() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val userReference =
                FirebaseDatabase.getInstance().getReference("users").child(currentUserUid)
                    .child("clickedNewsItems")

            userReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    clickedNewsItems.clear()
                    for (childSnapshot in snapshot.children) {
                        childSnapshot.key?.let { clickedNewsItems.add(it) }
                    }
                    // Load from SharedPreferences
                    val all = sharedPreferences.all
                    for (key in all.keys) {
                        if (all[key] == true) {
                            clickedNewsItems.add(key)
                        }
                    }


                    notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NewsAdapter", "Failed to load clicked news items: ${error.message}")
                }
            })

            val totalNewsCountReference =
                FirebaseDatabase.getInstance().getReference("users").child(currentUserUid)
                    .child("newsCount")

            totalNewsCountReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    totalNewsCount = snapshot.getValue(Int::class.java) ?: newsList.size
                    notifyDataSetChanged() // Notify adapter about the data change
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NewsAdapter", "Failed to load total news count: ${error.message}")
                }
            })
        } else {
            Log.e("NewsAdapter", "Current user UID is null, unable to load data.")
        }
    }

    private fun updateNewsCountInFirebase(newsId: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val userReference =
                FirebaseDatabase.getInstance().getReference("users").child(currentUserUid)
                    .child("clickedNewsItems").child(newsId)

            userReference.setValue(true)

            // Save to SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putBoolean(newsId, true)
            editor.apply()

        }
    }

    private fun updateTotalNewsCountInFirebase(newCount: Int) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val userReference =
                FirebaseDatabase.getInstance().getReference("users").child(currentUserUid)
                    .child("newsCount")

            userReference.setValue(newCount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.news_item, parent, false)
        return NewsViewHolder(view)
    }

    // Ad constants

    private val ITEM_NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"
     var isAdConfigLoaded = false
    // Ad configuration
    private var showItemNativeAd = false
    private var showBannerAd = false
    private var adIdItemNative = ITEM_NATIVE_AD_ID
    private var adIdBanner = "ca-app-pub-3940256099942544/6300978111"

    // Load ad configuration from Firebase Realtime

    private fun loadAdConfig() {
        adsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.getValue(AdsModel::class.java)?.let { adsModel ->
                    showItemNativeAd = adsModel.showItemNativeAd
                    showBannerAd = adsModel.showBannerAd
                }

                // Fetch banner ad ID or use default if not present
                adIdBanner = dataSnapshot.child("adIdBanner").value?.toString() ?: "ca-app-pub-3940256099942544/6300978111"
                adIdItemNative = dataSnapshot.child("adIdItemNative").value?.toString() ?: ITEM_NATIVE_AD_ID

                // Set isAdConfigLoaded to true after loading configuration
                isAdConfigLoaded = true
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("AdConfig", "Failed to load ads config", error.toException())
                // Fallback to default IDs in case of error
                adIdBanner = "ca-app-pub-3940256099942544/6300978111"
            }
        })
    }


    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val newsItem = newsList[position]
        holder.bind(newsItem)

        configureNativeAd(holder, position)
        configureBannerAd(holder)
    }

    private fun configureNativeAd(holder: NewsViewHolder, position: Int) {
        if (position % 3 == 0 && showItemNativeAd) {
            loadNativeAd(adIdItemNative, holder)
            holder.nativeAdContainer.visibility = View.VISIBLE
        } else {
            holder.nativeAdContainer.removeAllViews()
            holder.nativeAdContainer.visibility = View.GONE
        }
    }

    private fun configureBannerAd(holder: NewsViewHolder) {
        if (showBannerAd) {
            val adView = AdView(context)
            adView.adUnitId = adIdBanner
            adView.setAdSize(AdSize.BANNER)
            holder.adView.removeAllViews()
            holder.adView.addView(adView)
            adView.loadAd(AdRequest.Builder().build())
            holder.adView.visibility = View.VISIBLE
        } else {
            holder.adView.visibility = View.GONE
        }
    }


    private fun loadNativeAd(adUnitId: String, holder: NewsViewHolder) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad: NativeAd ->
                val adView = LayoutInflater.from(context)
                    .inflate(R.layout.admob_native_item, null) as NativeAdView
                populateNativeAdView(ad, adView)
                holder.nativeAdContainer.removeAllViews()
                holder.nativeAdContainer.addView(adView)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("NativeAd", "Ad failed to load: ${adError.message}")
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }


    override fun getItemCount(): Int {
        return newsList.size
    }
}
