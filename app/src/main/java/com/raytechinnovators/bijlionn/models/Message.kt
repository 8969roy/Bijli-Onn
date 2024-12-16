package com.raytechinnovators.bijlionn.models

data class Message(
    var senderId: String? = null,
    var receiverId: String? = null,
    var messageContent: String? = null,
    var timestamp: Long = 0,
    var imageUrl: String? = null,
    var videoUrl: String? = null,
    var chatStatus: Boolean = false,
    var unreadMessageCount: Int = 0,
    var userCount: Int = 0,
    var lastMessageTime:Long = 0


) {
    // Default constructor required for calls to DataSnapshot.getValue(Message::class.java)
    constructor() : this(null, null, null, 0, null, null ,false ,0 , 0,0)
}
