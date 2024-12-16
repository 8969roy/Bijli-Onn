package com.royllow.admin.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.firebase.database.FirebaseDatabase
import com.royllow.admin.Models.NewsItem
import com.royllow.bijlionn.R


class NewsAdapter(private val neuzList: List<NewsItem>, b: Boolean) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {



    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)  {
        val titleTextView: TextView = itemView.findViewById(R.id.Top_news)
        val descriptionTextView: TextView = itemView.findViewById(R.id.text_view_text)
        val viewCountTextView: TextView = itemView.findViewById(R.id.text_view_count)
        val timestampTextView: TextView = itemView.findViewById(R.id.text_view_timestamp)
        val fullDescription: TextView = itemView.findViewById(R.id.text_show)
        val fullImage: ImageView = itemView.findViewById(R.id.image_show)
        val smallImage: ImageView = itemView.findViewById(R.id.image_view_show)

        val headerLayout: LinearLayout = itemView.findViewById(R.id.headerLayout)
        val bottomLayout: LinearLayout = itemView.findViewById(R.id.bottomLayout)

        val database = FirebaseDatabase.getInstance()

    fun bind(newsItem: NewsItem) {
            // Load image using Glide
            Glide.with(itemView.context)
                .load(newsItem.imageUrl)
                .placeholder(R.drawable.mobile_phone) // Placeholder image while loading
                .error(R.drawable.electric_tran_1) // Error image if loading fails
                .into(fullImage)

            Glide.with(itemView.context)
                .load(newsItem.imageUrl)
                .placeholder(R.drawable.mobile_phone) // Placeholder image while loading
                .error(R.drawable.electric_tran_1) // Error image if loading fails
                .into(smallImage)


            val timestampInMillis = newsItem.timestamp
            val timeAgo = TimeAgo.using(timestampInMillis)
            timestampTextView.text = timeAgo
            titleTextView.text = newsItem.location
            descriptionTextView.text = newsItem.newsData
            fullDescription.text = newsItem.newsData
            viewCountTextView.text = newsItem.viewCount.toString()




            // Toggle expand/collapse when clicking on the item
            itemView.setOnClickListener {
                // Update expandedPosition when an item is clicked
                expandedPosition =
                    if (adapterPosition != expandedPosition) adapterPosition else RecyclerView.NO_POSITION
                notifyDataSetChanged() // Notify RecyclerView that data set has changed


            }

            // Set the visibility of your expandable layout based on isExpanded
            val isExpanded = adapterPosition == expandedPosition
            headerLayout.visibility = if (isExpanded) View.GONE else View.VISIBLE
            bottomLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.news_item, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(neuzList[position])
    }

    override fun getItemCount(): Int = neuzList.size

    private var expandedPosition = RecyclerView.NO_POSITION // Initialize expandedPosition with a default value


}








