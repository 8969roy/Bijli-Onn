package com.raytechinnovators.bijlionn.adapters
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.raytechinnovators.bijlionn.R
import com.raytechinnovators.bijlionn.Chatting
import com.raytechinnovators.bijlionn.FirebaseFunctions
import com.raytechinnovators.bijlionn.FirebaseFunctions.getRoomId
import com.raytechinnovators.bijlionn.models.User


class UserAdapter(private val context: Context , private var userList: List<User> ) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.user_row, parent, false)
        return UserViewHolder(view)
    }


    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.usernameTextView.text = user.name
        holder.locationTextView.text = user.location

        // Load profile image using Glide or any other image loading library
        Glide.with(context)
            .load(user.profileImage)
            .placeholder(R.drawable.avtar_placeholder)
            .into(holder.profileImageView)


        // Fetch and display the unread message count for the receiver
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserUid != null && user.uid != null) {
            val roomId = getRoomId(currentUserUid, user.uid!!)
            val unreadMessageCountRef = FirebaseDatabase.getInstance().getReference("chats/$roomId/${currentUserUid}/UnreadMessageCount")

            unreadMessageCountRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val unreadCount = dataSnapshot.getValue(Int::class.java) ?: 0
                    if (unreadCount > 0) {
                        holder.unreadMessageCountTextView.text = unreadCount.toString()
                        holder.unreadMessageCountTextView.visibility = View.VISIBLE
                    } else {
                        // Delay hiding the TextView for 2 seconds
                        Handler(Looper.getMainLooper()).postDelayed({
                            holder.unreadMessageCountTextView.visibility = View.GONE
                        }, 1000) // 1000 milliseconds = 1 seconds
                    }


                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors here
                    holder.unreadMessageCountTextView.visibility = View.GONE
                }
            })
        }



        holder.statusTextView.apply {
            text = if (user.status) "Online" else getTimeAgo(user.lastSeen)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (user.status) R.color.green else R.color.black
                )
            )
        }




        holder.statusTextView.apply {
            text = if (user.status) "Online" else " ${getTimeAgo(user.lastSeen)}"
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (user.status) R.color.green else R.color.black
                )
            )
        }


        holder.itemView.setOnClickListener {
            // Log data for debugging
            Log.d(
                "com.royllow.bijlionn.adapters.UserAdapter",
                "Clicked user: ${user.name}, Profile Image: ${user.profileImage}, Location: ${user.location}"
            )

            // Handle user item click event
            // Clear the unread message count for the clicked user
// Get the current user
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Get user ID and name
                val currentUserName = currentUser.displayName ?: ""


                val intent = Intent(context, Chatting::class.java).apply {
                    putExtra("SelectedUserId", user.uid)
                    putExtra("username", user.name)
                    putExtra("profileImageUrl", user.profileImage)
                    putExtra("location", user.location)
                    putExtra("token", user.token)
                    // Pass the current user's ID and name
                    // Pass the current user's ID (senderId)
                    putExtra("senderId", currentUserUid)
                    putExtra("senderName", currentUserName) // Use user.name as the display name

                }

                context.startActivity(intent)
            }

            val senderId =  FirebaseAuth.getInstance().currentUser?.uid
            val receiverId = user.uid

            if (senderId != null && receiverId != null) {
                FirebaseFunctions.clearUnreadMessageCount(senderId, receiverId)
            }



        }
    }



    fun getTimeAgo(lastSeenTime: Long): String {
        val now = System.currentTimeMillis()
        val timeAgo =
            DateUtils.getRelativeTimeSpanString(lastSeenTime, now, DateUtils.MINUTE_IN_MILLIS)
        return timeAgo.toString()
    }


    override fun getItemCount(): Int {
        return userList.size
    }


    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.userProfileImg)
        val usernameTextView: TextView = itemView.findViewById(R.id.userName)
        val locationTextView: TextView = itemView.findViewById(R.id.location)
        val statusTextView: TextView = itemView.findViewById(R.id.lastTime)
        val unreadMessageCountTextView: TextView = itemView.findViewById(R.id.unreadMessageCountTextView)
    }


}




