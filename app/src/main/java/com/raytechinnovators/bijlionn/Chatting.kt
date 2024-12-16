package com.raytechinnovators.bijlionn


import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage
import com.raytechinnovators.bijlionn.databinding.ActivityChattingBinding
import com.raytechinnovators.bijlionn.FirebaseFunctions.updateUnreadMessageCount
import com.raytechinnovators.bijlionn.adapters.MessageAdapter
import com.raytechinnovators.bijlionn.models.AdsModel
import com.raytechinnovators.bijlionn.models.Message
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


class Chatting : AppCompatActivity() {

    private lateinit var binding: ActivityChattingBinding
    private lateinit var senderRoom: String
    private lateinit var receiverRoom: String
    private lateinit var selectedUserId: String
    private lateinit var currentUserUid: String
    private lateinit var database: FirebaseDatabase
    private lateinit var messagesRef: DatabaseReference
    private lateinit var messageAdapter: MessageAdapter
    private val messagesList: MutableList<Message> = mutableListOf()
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private var imageUrl: String? = null
    private var senderName: String? = null
    private var recipientToken: String? = null
    // Declare an instance variable for SendNotification
    private lateinit var auth: FirebaseAuth
    private var nativeAd: NativeAd? = null
    private var isAdmin: Boolean = false


    private val TAG = "ChattingActivity"
    private val REQUEST_CODE_IMAGE_PICKER = 1001
    private val REQUEST_CODE_VIDEO_PICKER = 1002


    private val dialog: ProgressDialog by lazy {
        ProgressDialog(this).apply {
            setMessage("Uploading image...")
            setCancelable(false)
        }
    }

    private val dialogTwo: ProgressDialog by lazy {
        ProgressDialog(this).apply {
            setMessage("Uploading video...")
            setCancelable(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChattingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        networkChangeReceiver = NetworkChangeReceiver()
        selectedUserId = intent.getStringExtra("SelectedUserId") ?: ""

        changeNavigationBarColor()
// Initialize SendNotification instance
        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.skywhite)

        senderRoom = intent.getStringExtra("senderRoom") ?: ""
        receiverRoom = intent.getStringExtra("receiverRoom") ?: ""

        // Retrieve sender name and recipient token from intent
        senderName = intent.getStringExtra("senderName")
        recipientToken = intent.getStringExtra("token")


        currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance()
        messagesRef = database.getReference("chats/messages")

        senderRoom = currentUserUid + selectedUserId
        receiverRoom = selectedUserId + currentUserUid

        Log.d(TAG, "onCreate: SelectedUserId = $selectedUserId")
        Log.d(TAG, "onCreate: currentUserUid = $currentUserUid")
        Log.d(TAG, "onCreate: senderRoom = $senderRoom")
        Log.d(TAG, "onCreate: receiverRoom = $receiverRoom")

        setupUI()
        setupRecyclerView()
        displayUserData()
        loadMessages()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        currentUser?.let {
            fetchIsAdmin(it.uid)
        }
    }

    fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigation_bar_color)
    }

    private fun fetchIsAdmin(userId: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        userRef.child("admin").get().addOnSuccessListener {
            isAdmin = it.getValue(Boolean::class.java) ?: false
            setupRecyclerView()
        }.addOnFailureListener {
            Log.e("MainActivity", "Failed to fetch isAdmin flag: ${it.message}")
        }
    }

    private fun displayUserData() {
        binding.apply {
            userNameTextView.text = intent.getStringExtra("username")
            locationText.text = intent.getStringExtra("location")




            Glide.with(this@Chatting)
                .load(intent.getStringExtra("profileImageUrl"))
                .placeholder(R.drawable.avtar_placeholder)
                .into(userProfileImageView)

            backIcon.setOnClickListener {
                finish()
            }
        }
    }

    private fun setupUI() {
        binding.apply {
            sendBtn.setOnClickListener {
                val messageContent = editTextMsg.text.toString().trim()

                if (messageContent.isNotEmpty()) {
                    val senderId = FirebaseAuth.getInstance().currentUser?.uid
                    val receiverId = selectedUserId

                    if (senderId != null && receiverId.isNotEmpty()) {
                        val message = Message(
                            senderId,
                            receiverId,
                            messageContent,
                            System.currentTimeMillis(),
                            "", // Empty string for imageUrl
                            ""  // Empty string for videoUrl
                        )
                        sendMessage(message)
                        editTextMsg.text.clear()

                        // Pass context to sendNotification
                        senderName?.let { it1 ->
                            recipientToken?.let { it2 ->
                                sendNotification(senderId,
                                    it1, messageContent, it2
                                )
                            }
                        };


                        rootLayout.setBackgroundResource(R.drawable.msg_bg)
                        editTextMsg.setBackgroundResource(R.drawable.msg_bg)
                        editTextMsg.requestFocus()

                        // Load and show the ad in a dialog
                        showSuccessDialog()
                    } else {
                        showMessage("Sender ID or receiver ID is empty.")
                    }
                } else {
                    showMessage("Message content is empty.")
                }
            }

            editTextMsg.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s.isNullOrEmpty()) {
                        rootLayout.setBackgroundResource(R.drawable.edit_bg)
                        editTextMsg.setBackgroundResource(R.drawable.edit_bg)
                    } else {
                        rootLayout.setBackgroundResource(R.drawable.msg_bg)
                        editTextMsg.setBackgroundResource(R.drawable.msg_bg)
                    }
                }
            })

            // Set a click listener for sending images
            selectImages.setOnClickListener {
                openImagePicker()
            }
        }
    }

    private fun openImagePicker() {
        val options = arrayOf("Image", "Video")

        AlertDialog.Builder(this)
            .setTitle("Select Media Type")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImage()
                    1 -> pickVideo()
                }
            }
            .show()
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
    }

    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_CODE_VIDEO_PICKER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_IMAGE_PICKER -> {
                    val selectedImageUri = data?.data
                    selectedImageUri?.let {
                        uploadImageToFirebase(it)
                    }
                }

                REQUEST_CODE_VIDEO_PICKER -> {
                    val selectedVideoUri = data?.data
                    if (selectedVideoUri != null) {
                        val mimeType = contentResolver.getType(selectedVideoUri)
                        if (mimeType != null && mimeType.startsWith("video/")) {
                            uploadVideoToFirebase(selectedVideoUri)
                        } else {
                            // Show error message if the selected file is not a video
                            Toast.makeText(this, "Please select a video file", Toast.LENGTH_SHORT)
                                .show()
                            pickVideo() // Re-launch the video picker
                        }
                    }
                }
            }
        }
    }

    private fun uploadVideoToFirebase(videoUri: Uri) {
        val senderId = FirebaseAuth.getInstance().currentUser?.uid
        val receiverId = selectedUserId

        val storageRef = FirebaseStorage.getInstance().reference
        val videoRef = storageRef.child("chats").child("videos/${System.currentTimeMillis()}.mp4")
        dialogTwo.show()
        val uploadTask = videoRef.putFile(videoUri)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            videoRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                val videoUrl = downloadUri.toString()

                val message = Message(senderId, receiverId , "", System.currentTimeMillis(), "", videoUrl)
                sendMessage(message)

                dialogTwo.dismiss()
                showMessage("Video uploaded successfully.")
            } else {
                dialogTwo.dismiss()
                showMessage("Failed to upload video.")
            }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val senderId = FirebaseAuth.getInstance().currentUser?.uid
        val receiverId = selectedUserId
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("chats").child("images/${System.currentTimeMillis()}.jpg")
        dialog.show()
        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                imageUrl = downloadUri.toString()

                val message = Message(senderId, receiverId , "", System.currentTimeMillis(), imageUrl, "")
                sendMessage(message)
                dialog.dismiss()
                showMessage("Image uploaded successfully.")
            } else {
                dialog.dismiss()
                showMessage("Failed to upload image.")
            }
        }
    }

    private fun setupRecyclerView() {
        binding.messageContainer.apply {
            messageAdapter = MessageAdapter(this@Chatting, messagesList,isAdmin)
            layoutManager = LinearLayoutManager(this@Chatting).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun sendMessage(message: Message) {
        val senderRoomRef =
            database.getReference("chats").child(senderRoom).child("messages").push()
        val receiverRoomRef =
            database.getReference("chats").child(receiverRoom).child("messages").push()

        // Conditionally set the imageUrl based on whether it's empty or not
        val imageUrl = if (message.imageUrl?.isNotEmpty() == true) message.imageUrl else null
        val videoUrl = if (message.videoUrl?.isNotEmpty() == true) message.videoUrl else null
        val messageContent = if (message.messageContent?.isNotEmpty() == true) message.messageContent else null

        val newMessage = Message(
            message.senderId,
            message.receiverId,
            messageContent,
            message.timestamp,
            imageUrl,
            videoUrl
        )

        // Inside the activity where you handle incoming messages
        val senderId = newMessage.senderId // Assuming newMessage contains the sender's ID
        val receiverId = newMessage.receiverId

        if (senderId != null && receiverId != null) {
            updateUnreadMessageCount(senderId, receiverId)
        }

        // Send message to sender room
        senderRoomRef.setValue(newMessage)
            .addOnSuccessListener {




                if (messageContent != null && senderName != null && recipientToken != null) {
                    // Pass senderId along with senderName
                    sendNotification(senderId!!, senderName!!, messageContent, recipientToken!!)
                }


            }
            .addOnFailureListener { e ->
                showMessage("Failed to send message to senderRoom: ${e.message}")
            }

        // Send message to receiver room
        receiverRoomRef.setValue(newMessage)
            .addOnFailureListener { e ->
                showMessage("Failed to send message to receiverRoom: ${e.message}")
            }


    }

    private fun sendNotification(
        senderId: String,
        senderName: String,
        messageContent: String,
        recipientToken: String
    ) {
        val accessTokenProvider = AccessToken(this) // Pass the context
        val accessToken = accessTokenProvider.accessToken
        if (accessToken != null) {
            try {
                val uniqueMessageId = System.currentTimeMillis().toString() // Generate a unique message ID

                val payload = JSONObject()
                payload.put(
                    "message", JSONObject()
                        .put("token", recipientToken)
                        .put(
                            "data", JSONObject()
                                .put("senderId", senderId)
                                .put("senderName", senderName)
                                .put("messageContent", messageContent)
                                .put("messageId", uniqueMessageId) // Add unique message ID
                        )
                )
                Thread {
                    try {
                        val url = URL("https://fcm.googleapis.com/v1/projects/bijlionn/messages:send")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Authorization", "Bearer $accessToken")
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.doOutput = true
                        val outputStreamWriter = OutputStreamWriter(connection.outputStream)
                        outputStreamWriter.write(payload.toString())
                        outputStreamWriter.flush()
                        outputStreamWriter.close()
                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Log.d("Notification", "Notification sent successfully")
                        } else {
                            Log.e("Notification", "Failed to send notification. Response code: $responseCode")

                            // Read the error response
                            val errorStream = connection.errorStream
                            if (errorStream != null) {
                                val reader = BufferedReader(InputStreamReader(errorStream))
                                val errorResponse = StringBuilder()
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    errorResponse.append(line)
                                }
                                reader.close()
                                Log.e("Notification", "Error response: $errorResponse")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("Notification", "Error sending notification: " + e.message)
                    }
                }.start()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Notification", "Error constructing notification payload: " + e.message)
            }
        } else {
            Log.e("Notification", "Failed to get access token")
        }
    }


    private fun showSuccessDialog() {
        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.show()

        // Find the native ad container and close button in the dialog layout
        val closeButton = dialogView.findViewById<Button>(R.id.close_dialog)

        val database = Firebase.database
        val adsRef = database.getReference("ads")
        adsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Fetch AdsModel and ad configuration
                val adsModel = dataSnapshot.getValue(AdsModel::class.java)
                val showSuccessNativeAd = adsModel?.showSuccessNativeAd ?: false
                val SUCCESS_NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"
                val adUnitId = dataSnapshot.child("adIdSuccessNative").value?.toString() ?: SUCCESS_NATIVE_AD_ID

                // Load native ad if enabled
                if (showSuccessNativeAd) {
                    loadNativeAd(adUnitId, dialogView) // Pass dialogView here
                } else {
                    Log.d("AdConfig", "Ads are disabled, skipping ad load")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }


        })

        // Set the close button click listener
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun loadNativeAd(adUnitId: String, dialogView: View) {
        val adLoader = AdLoader.Builder(this, adUnitId)
            .forNativeAd { ad: NativeAd ->
                nativeAd?.destroy()
                nativeAd = ad

                // Inflate the ad view
                val adView = layoutInflater.inflate(R.layout.admob_native_small, null) as NativeAdView

                // Populate ad view with the ad data
                populateNativeAdView(ad, adView)

                val adContainer = dialogView.findViewById<FrameLayout>(R.id.native_ad_container)
                // Add the new ad view to the container
                adContainer.removeAllViews()
                adContainer.addView(adView)
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("AdLoadError", "Ad failed to load: ${adError.message}")
                    showToast("Ad failed to load: ${adError.message}")
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }


    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Populate headline view
        adView.headlineView = adView.findViewById<TextView>(R.id.primary)?.apply {
            text = nativeAd.headline
        } ?: run {
            Log.e("NativeAd", "Headline view is null")
            null
        }

        // Populate body view (secondary text)
        adView.bodyView = adView.findViewById<TextView>(R.id.secondary)?.apply {
            text = nativeAd.body ?: ""
            visibility = if (text.isNotEmpty()) VISIBLE else GONE
        } ?: run {
            Log.e("NativeAd", "Body view is null")
            null
        }

        // Populate call-to-action button
        adView.callToActionView = adView.findViewById<Button>(R.id.cta)?.apply {
            text = nativeAd.callToAction ?: ""
            visibility = if (text.isNotEmpty()) VISIBLE else GONE
        } ?: run {
            Log.e("NativeAd", "CTA view is null")
            null
        }

        // Populate icon image
        adView.iconView = adView.findViewById<ImageView>(R.id.icon)?.apply {
            nativeAd.icon?.let {
                setImageDrawable(it.drawable)
                visibility = VISIBLE
            } ?: run {
                visibility = GONE
            }
        } ?: run {
            Log.e("NativeAd", "Icon view is null")
            null
        }

        // Populate rating bar
        adView.findViewById<RatingBar>(R.id.rating_bar)?.apply {
            visibility = if (nativeAd.starRating != null) {
                rating = nativeAd.starRating!!.toFloat()
                VISIBLE
            } else {
                GONE
            }
        } ?: Log.e("NativeAd", "Rating bar is null")

        // Set ad notification view (e.g., "Ad")
        adView.findViewById<TextView>(R.id.ad_notification_view)?.apply {
            text = "Ad"
        } ?: Log.e("NativeAd", "Ad notification view is null")

        // Set the native ad to the NativeAdView
        adView.setNativeAd(nativeAd)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }



    private fun loadMessages() {
        // Clear the existing messagesList to avoid duplicates
        messagesList.clear()

        val messagesRef = database.getReference("chats").child(senderRoom).child("messages")

        // Add a listener to retrieve new messages
        messagesRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                message?.let {
                    // Check if the message already exists in the list before adding
                    if (!messagesList.contains(message)) {
                        messagesList.add(it)
                        messageAdapter.notifyItemInserted(messagesList.size - 1)
                        scrollToBottom()
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle if needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle if needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle if needed
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching messages: ${error.message}")
            }
        })
    }


    private fun scrollToBottom() {
        binding.messageContainer.scrollToPosition(messagesList.size - 1)
    }

    override fun onResume() {
        super.onResume()
        MainActivity.UserStatusManager.setIsOnline(true)
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        MainActivity.UserStatusManager.setIsOnline(false)
        unregisterReceiver(networkChangeReceiver)
    }


}
