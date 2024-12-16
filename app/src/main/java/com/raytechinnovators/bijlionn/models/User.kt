package com.raytechinnovators.bijlionn.models

class User {
    var uid: String? = null
    var name: String? = null
    var phoneNumber: String? = null
    var profileImage: String? = null
    var location: String? = null
    var isAdmin: Boolean = false
    var status: Boolean = false
    var lastSeen: Long = 0
    var lastMessageTime: Long = 0
    var unreadMessageCount: Int = 0
    var userCount: Int = 0
    var token: String? = null
    var newsCount: Int = 0 // New property to store total news count for the user
    var loginTimestamp: Long = 0L // New property to store login timestamp

    constructor()

    constructor(
        uid: String?,
        name: String?,
        phoneNumber: String?,
        profileImage: String?,
        location: String?,
        isAdmin: Boolean,
        status: Boolean,
        lastSeen: Long,
        lastMessageTime: Long,
        unreadMessageCount: Int,
        userCount: Int,
        token: String?,
        newsCount: Int,
        loginTimestamp: Long // Added to the constructor
    ) {
        this.uid = uid
        this.name = name
        this.phoneNumber = phoneNumber
        this.profileImage = profileImage
        this.location = location
        this.isAdmin = isAdmin
        this.status = status
        this.lastSeen = lastSeen
        this.lastMessageTime = lastMessageTime
        this.unreadMessageCount = unreadMessageCount
        this.userCount = userCount
        this.token = token
        this.newsCount = newsCount
        this.loginTimestamp = loginTimestamp
    }
}
