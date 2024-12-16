package com.royllow.admin.Adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.royllow.admin.Models.NewsItem
import com.royllow.bijlionn.R

class SpecialNewsViewHolder(private var vdoList: MutableList<NewsItem>, private val context: Boolean) : RecyclerView.Adapter<SpecialNewsViewHolder.NewsViewHolder>() {

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val videoShow : ImageView = itemView.findViewById(R.id.video_show)
        val videoCount: TextView = itemView.findViewById(R.id.video_count)
        val titleShow: TextView = itemView.findViewById(R.id.tittle_show)





        @SuppressLint("NotifyDataSetChanged")
        fun bind(newsItem: NewsItem) {
            // Load image using Glide
            Glide.with(itemView.context)
                .load(newsItem.imageUrl)
                .placeholder(R.drawable.mobile_phone) // Placeholder image while loading
                .error(R.drawable.electric_tran_1) // Error image if loading fails
                .into(videoShow)


            titleShow.text = newsItem.newsData
            videoCount.text = newsItem.viewCount.toString()

            // Toggle expand/collapse when clicking on the item




        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.news_video, parent, false)
        return NewsViewHolder(view)
    }


    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(vdoList[position])
    }

    override fun getItemCount(): Int = vdoList.size




}
