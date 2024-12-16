package com.raytechinnovators.bijlionn.models

class NewsItem {



    var location: String? = null
    var generatedNewsId: String? = null
    var newsData: String? = null
    var timestamp: Long = 0
    var viewCount : Double = 0.0
    var videoCount: Double = 0.0
    var imageUrl: String? = null
    var videoUrl: String? = null

    constructor() {
        // Default constructor required for Firebase
    }

    constructor(
        generatedNewsId: String,
        location: String?,
        videoCount: Double,
        newsData: String,
        timestamp: Long,
        viewCount: Double,
        imageUrl: String,


        videoUrl: String

    ) {
        this.location = location
        this.newsData = newsData
        this.videoCount= videoCount
        this.timestamp = timestamp
        this.viewCount = viewCount
        this.imageUrl = imageUrl
        this.videoUrl = videoUrl
        this.generatedNewsId = generatedNewsId


    }
}
