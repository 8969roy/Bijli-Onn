package com.royllow.admin.Models

class NewsItem {



    var location: String? = null
    var newsData: String? = null
    var timestamp: Long = 0
    var viewCount = 0
    var imageUrl: String? = null

    constructor() {
        // Default constructor required for Firebase
    }

    constructor(
        location: String?,
        newsData: String?,
        timestamp: Long,
        viewCount: Int,
        imageUrl: String?
    ) {
        this.location = location
        this.newsData = newsData
        this.timestamp = timestamp
        this.viewCount = viewCount
        this.imageUrl = imageUrl
    }
}
