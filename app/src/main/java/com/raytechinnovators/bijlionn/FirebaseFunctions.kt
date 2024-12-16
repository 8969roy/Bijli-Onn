package com.raytechinnovators.bijlionn

import com.google.firebase.database.*

object FirebaseFunctions {
    private val database = FirebaseDatabase.getInstance()
    private val chatsRef = database.getReference("chats")


    // Construct the room ID
    fun getRoomId(senderId: String, receiverId: String): String {
        return if (senderId < receiverId) {
            senderId + receiverId
        } else {
            receiverId + senderId
        }
    }


    // Update unread message count when a new message is received
    // Update unread message count and user count when a new message is received
    fun updateUnreadMessageCount(senderId: String, receiverId: String) {


        val roomId = getRoomId(senderId, receiverId)
        val userCountRef = chatsRef.child(roomId).child(receiverId).child("UserCount")
        val unreadMessageCountRef = chatsRef.child(roomId).child(receiverId).child("UnreadMessageCount")
        val lastMessageTimeRef = chatsRef.child(roomId).child(receiverId).child("lastMessageTime")

        userCountRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                var userCount = mutableData.getValue(Int::class.java) ?: 0
                if (userCount == 0) {
                    userCount = 1 // Increment userCount if it's 0
                    mutableData.value = userCount
                }
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                if (committed) {
                    // User count updated successfully, now update unread message count and last message time
                    unreadMessageCountRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val currentUnreadCount = dataSnapshot.getValue(Int::class.java) ?: 0
                            unreadMessageCountRef.setValue(currentUnreadCount + 1)
                            lastMessageTimeRef.setValue(System.currentTimeMillis()) // Update lastMessageTime
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle errors here
                        }
                    })
                } else {
                    // Transaction failed, handle accordingly
                }
            }
        })
    }

    // Function to read unread message count with a callback
    fun readUnreadMessageCount(roomId: String, receiverId: String, callback: (Int) -> Unit) {
        val unreadMessageCountRef = chatsRef.child(roomId).child(receiverId).child("UnreadMessageCount")

        unreadMessageCountRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val unreadCount = dataSnapshot.getValue(Int::class.java) ?: 0
                callback(unreadCount)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors here
                callback(0)
            }
        })
    }




    // Update unread message count to zero
    fun clearUnreadMessageCount(senderId: String, receiverId: String) {
        val roomId = getRoomId(senderId, receiverId)
        val unreadMessageCountRef = chatsRef.child(roomId).child(senderId).child("UnreadMessageCount")
        unreadMessageCountRef.setValue(0)

        // Also set userCount to 0 when clearing unreadMessageCount
        val userCountRef = chatsRef.child(roomId).child(senderId).child("UserCount")
        userCountRef.setValue(0)
    }


}
