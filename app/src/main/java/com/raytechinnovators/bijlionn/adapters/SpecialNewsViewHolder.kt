package com.raytechinnovators.bijlionn.adapters
import android.content.ContentValues.TAG
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.raytechinnovators.bijlionn.R
import com.google.firebase.storage.FirebaseStorage
import com.raytechinnovators.bijlionn.VideoPlayerActivity
import com.raytechinnovators.bijlionn.models.NewsItem



class SpecialNewsViewHolder(
    private val newsItems: MutableList<NewsItem>,
    b: Boolean
) : RecyclerView.Adapter<SpecialNewsViewHolder.NewsViewHolder>() {

    private val clickedNewsItems = mutableSetOf<String>()
    private var totalNewsCount: Int = 0
    private var loginTimestamp: Long = 0L

    init {
        loadUserDetailsFromFirebase()
    }

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleShow: TextView = itemView.findViewById(R.id.tittle_show)
        private val videoCount: TextView = itemView.findViewById(R.id.video_count)
        private val videoThumbnail: ImageView = itemView.findViewById(R.id.VideoThumbnail)
        private val redDotAnimation: LottieAnimationView = itemView.findViewById(R.id.redDotAnimation)

        fun bind(newsItem: NewsItem) {
            titleShow.text = newsItem.newsData
            videoCount.text = formatViewCount(newsItem.videoCount.toInt())

            val videoStoragePath = extractStoragePathFromUrl(newsItem.videoUrl)
            val newsRef = videoStoragePath?.let { FirebaseStorage.getInstance().getReference(it) }

            newsRef?.downloadUrl?.addOnSuccessListener { uri ->
                val videoThumbnailUri = uri.toString()
                Glide.with(itemView.context)
                    .load(videoThumbnailUri)
                    .placeholder(R.drawable.image_placeholder) // Placeholder image while loading
                    .error(R.drawable.image_placeholder) // Error image
                    .into(videoThumbnail)

                videoThumbnail.setOnClickListener {
                    playVideo(newsItem, uri.toString())
                }
            }?.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get video thumbnail URL", exception)
            }

            // Show red dot if the news item is newer than the last login and hasn't been clicked yet
            if (isNewerThanLastLogin(newsItem) && !clickedNewsItems.contains(newsItem.generatedNewsId)) {
                redDotAnimation.visibility = View.VISIBLE
                redDotAnimation.playAnimation()
            } else {
                redDotAnimation.visibility = View.GONE
            }
        }

        private fun playVideo(newsItem: NewsItem, videoUri: String) {
            val newsId = newsItem.generatedNewsId
            if (newsId != null && !clickedNewsItems.contains(newsId)) {
                clickedNewsItems.add(newsId)
                redDotAnimation.visibility = View.INVISIBLE

                if (totalNewsCount > 0) {
                    totalNewsCount--
                    updateTotalNewsCountInFirebase(totalNewsCount)
                    updateNewsCountInFirebase(newsId)
                }
            }

            newsItem.videoCount++
            videoCount.text = formatViewCount(newsItem.videoCount.toInt())
            updateVideoCountInDatabase(newsItem.generatedNewsId, newsItem.videoCount)

            val intent = Intent(itemView.context, VideoPlayerActivity::class.java)
            intent.putExtra("videoUri", videoUri)
            itemView.context.startActivity(intent)
        }

        private fun formatViewCount(viewCount: Int): String {
            return when {
                viewCount >= 1_000_000_000 -> "${viewCount / 1_000_000_000}B"
                viewCount >= 1_000_000 -> "${viewCount / 1_000_000}M"
                viewCount >= 1_000 -> "${viewCount / 1_000}K"
                else -> viewCount.toString()
            }
        }

        private fun extractStoragePathFromUrl(videoUrl: String?): String? {
            if (!videoUrl.isNullOrEmpty() && videoUrl.startsWith("https://firebasestorage.googleapis.com")) {
                val startIndex = videoUrl.indexOf("/o/") + 3
                val endIndex = videoUrl.indexOf("?alt=")
                return if (startIndex != -1 && endIndex != -1) {
                    videoUrl.substring(startIndex, endIndex).replace("%2F", "/")
                } else {
                    null
                }
            }
            return null
        }
    }

    private fun loadUserDetailsFromFirebase() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val userReference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUserUid)

            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.child("clickedNewsItems").children.forEach { childSnapshot ->
                        childSnapshot.key?.let { clickedNewsItems.add(it) }
                    }
                    totalNewsCount = snapshot.child("newsCount").getValue(Int::class.java) ?: newsItems.size
                    loginTimestamp = snapshot.child("loginTimestamp").getValue(Long::class.java) ?: 0L
                    notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SpecialNewsViewHolder", "Failed to load user details: ${error.message}")
                }
            })
        }
    }

    private fun isNewerThanLastLogin(newsItem: NewsItem): Boolean {
        return newsItem.timestamp > loginTimestamp
    }

    private fun updateNewsCountInFirebase(newsId: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val userReference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUserUid)
                .child("clickedNewsItems")
                .child(newsId)

            userReference.setValue(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SpecialNewsViewHolder", "News count updated successfully in Firebase")
                } else {
                    Log.e("SpecialNewsViewHolder", "Failed to update news count in Firebase: ${task.exception?.message}")
                }
            }
        }
    }

    private fun updateTotalNewsCountInFirebase(newCount: Int) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val userReference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUserUid)
                .child("newsCount")

            userReference.setValue(newCount).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SpecialNewsViewHolder", "Total news count updated successfully in Firebase")
                } else {
                    Log.e("SpecialNewsViewHolder", "Failed to update total news count in Firebase: ${task.exception?.message}")
                }
            }
        }
    }

    private fun updateVideoCountInDatabase(generatedNewsId: String?, videoCount: Double) {
        val databaseRef = generatedNewsId?.let { FirebaseDatabase.getInstance().getReference("news").child(it) }
        databaseRef?.child("videoCount")?.setValue(videoCount)?.addOnSuccessListener {
            Log.d(TAG, "Video count updated successfully in database")
        }?.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to update video count in database", exception)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.news_video, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(newsItems[position])
    }

    override fun getItemCount(): Int = newsItems.size
}
