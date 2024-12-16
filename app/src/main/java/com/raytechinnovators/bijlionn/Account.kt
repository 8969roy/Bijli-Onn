package com.raytechinnovators.bijlionn

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.ClipboardManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.helper.widget.MotionEffect
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.raytechinnovators.bijlionn.Chat.Companion
import com.raytechinnovators.bijlionn.databinding.ActivityAccountBinding
import com.raytechinnovators.bijlionn.models.NewsItem
import com.raytechinnovators.bijlionn.R


import es.dmoral.toasty.Toasty
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Account : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding
    private lateinit var uploadNewsButton: Button
    private lateinit var currentUserUid: String
    private var mediaUri: Uri? = null
    private var imageUrl: String = ""
    private var videoUrl: String = ""
    private lateinit var badge: BadgeDrawable
    private var newCount: Int = 0
    private lateinit var newsBadge: BadgeDrawable
    private lateinit var networkChangeReceiver: NetworkChangeReceiver

    private lateinit var newsImageView: ImageView
    private lateinit var videoView: VideoView
    private lateinit var uploadAnimation: LottieAnimationView

    private lateinit var progressDialog: ProgressDialog
    private lateinit var database: FirebaseDatabase
    private lateinit var videoProgressBar: ProgressBar



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = findViewById<Toolbar>(R.id.toolbar2)
        networkChangeReceiver = NetworkChangeReceiver()
        initializeViews()
        currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance()

        // Check if the user is an admin to show the upload button
        checkAdminStatus()

        // Fetch and display user profile information
        fetchAndDisplayUserProfile()

        // Handle log out functionality
        handleLogOut()

        // Handle bottom navigation clicks
        handleBottomNavigation()

        // Handle social media button clicks
        handleSocialMediaLinks()


        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_500)


        fetchTotalUserCount()

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Logging out...")
        progressDialog.setCancelable(false)

        uploadNewsButton.setOnClickListener {
            showBottomSheetDialog()
            // Fetch total news count for the current user
            fetchTotalNewsCount()
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result

                // Log and toast
                Log.d(TAG, "FCM Registration token: $token")
                // Store this token in your database for future use
            }

        changeNavigationBarColor()



    }

    fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigation_bar_color)
    }

    private fun fetchTotalNewsCount() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserUid != null) {
            val userReference =
                FirebaseDatabase.getInstance().getReference("users").child(currentUserUid)

            userReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val newsCount = dataSnapshot.child("newsCount").getValue(Int::class.java) ?: 0
                    // Update the news badge count
                    updateNewsBadge(newsCount)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors
                }
            })
        }
    }

    private fun updateNewsBadge(count: Int) {
        if (count > 0) {
            // Show the badge with the news count
            newsBadge.isVisible = true
            newsBadge.number = count
        } else {
            // Hide the badge if there are no news items
            newsBadge.isVisible = false
        }
    }


    private fun fetchTotalUserCount() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserUid != null) {
            val userChatsRef = FirebaseDatabase.getInstance().getReference("chats")

            userChatsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var totalUserCount = 0

                    // Iterate over each child node (chat room)
                    for (roomSnapshot in dataSnapshot.children) {
                        // Check if the current user is involved in this chat room
                        if (roomSnapshot.child(currentUserUid).exists()) {
                            // Get the user count for the current room
                            val userCount = roomSnapshot.child(currentUserUid).child("UserCount")
                                .getValue(Int::class.java) ?: 0
                            totalUserCount += userCount
                        }
                    }

                    // Now 'totalUserCount' contains the total user count for the specific user across all chat rooms
                    // Update badge count UI
                    updateBadgeCount(totalUserCount)

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors here
                }
            })
        }
    }


    private fun updateBadgeCount(count: Int) {
        if (count > 0) {
            badge.number = count
            badge.isVisible = true
        } else {

            // Delay clearing badge number and hiding it after 2 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                badge.clearNumber()
                badge.isVisible = false
            }, 1000) // 2000 milliseconds = 2 seconds
        }
    }

    private fun initializeViews() {
        uploadNewsButton = findViewById(R.id.Upload)
    }

    private fun fetchAndDisplayUserProfile() {
        // Fetch user profile information from Firebase Realtime Database
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val databaseRef = FirebaseDatabase.getInstance().reference
            databaseRef.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Save user data to SharedPreferences
                        val mySharedPreferences = MyAccountSharedPreferences(this@Account)
                        val profileImage =
                            snapshot.child("profileImage").getValue(String::class.java)
                        val name = snapshot.child("name").getValue(String::class.java)
                        val phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java)
                        mySharedPreferences.saveUserData(name, profileImage, phoneNumber)

                        // Display user data
                        binding.UserName.text = name
                        binding.UserNumber.text = phoneNumber

                        // Load profile image using Glide
                        Glide.with(this@Account)
                            .load(profileImage)
                            .placeholder(R.drawable.avtar_placeholder)
                            .error(R.drawable.avtar_placeholder)
                            .into(binding.UserImg)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d(MotionEffect.TAG, "Failed to read user data", error.toException())
                    }
                })

            // Display user data from SharedPreferences (if available)
            val mySharedPreferences = MyAccountSharedPreferences(this@Account)
            val name = mySharedPreferences.name
            val phoneNumber = mySharedPreferences.number
            val profileImage = mySharedPreferences.profileImage

            binding.UserName.text = name
            binding.UserNumber.text = phoneNumber
            Glide.with(this@Account)
                .load(profileImage)
                .placeholder(R.drawable.avtar_placeholder)
                .into(binding.UserImg)
        }
    }

    private fun handleLogOut() {
        binding.LogOut.setOnClickListener {
            progressDialog.show()
            FirebaseAuth.getInstance().signOut()
            Handler().postDelayed({
                progressDialog.dismiss()
                val intent = Intent(this@Account, Splash::class.java)
                startActivity(intent)
                finish()
            }, 4000)
        }
    }

    private fun handleBottomNavigation() {

        badge = binding.bottomNavigation.getOrCreateBadge(R.id.chat)
        badge.isVisible = false // Ensure badge is visible

        // Initialize the news badge
        newsBadge = binding.bottomNavigation.getOrCreateBadge(R.id.home)
        newsBadge.isVisible = false // Ensure badge is initially hidden

// Change the background color
        binding.bottomNavigation.setBackgroundColor(resources.getColor(R.color.light_green))
        binding.bottomNavigation.selectedItemId = R.id.account
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                ACCOUNT_ID -> return@setOnNavigationItemSelectedListener true
                CHAT_ID -> startActivity(Intent(applicationContext, Chat::class.java))
                QUIZ_ID -> startActivity(Intent(applicationContext, Game::class.java))
                HOME_ID -> startActivity(Intent(applicationContext, MainActivity::class.java))
                BILL_ID -> startActivity(Intent(applicationContext, Bill::class.java))
            }
            overridePendingTransition(0, 0)
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Switch to home tab
                binding.bottomNavigation.selectedItemId = Chat.HOME_ID
                // Call the default behavior of onBackPressed()
                finish()
            }
        })

        }

    private fun handleSocialMediaLinks() {
        binding.UtubeButton.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/channel/UCtOkrq-48rP9YTpCrCVN4vA")
                )
            )
        }
        binding.FbButton.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.facebook.com/RayTechInnovators/")
                )
            )
        }
        binding.InstaButton.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.instagram.com/entrepreneur.bikram.roy/")
                )
            )
        }
        binding.ContactButton.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.instagram.com/entrepreneur.bikram.roy/")
                )
            )
        }
        binding.ShareButton.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Check out my app on the Play Store: https://play.google.com/store/apps/details?id=com.raytechinnovators.bijlionn")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share My App via"))
        }
        binding.policyButton.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://docs.google.com/document/d/e/2PACX-1vTPdNDkqJKe52jXN1PT6_fXDPxZ-TQknAK-BHNFKJ-584zTMIGmh-zP12hVlV9zXfYBQb_VT-d5npAO/pub")
                )
            )
        }
    }

    private fun checkAdminStatus() {
        database.getReference("users").child(currentUserUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val isAdmin = snapshot.child("admin").getValue(Boolean::class.java) ?: false
                        uploadNewsButton.visibility = if (isAdmin) View.VISIBLE else View.GONE
                    } else {
                        Toast.makeText(this@Account, "User data not found", Toast.LENGTH_SHORT)
                            .show()

                        uploadNewsButton.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@Account,
                        "Error fetching admin status: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    uploadNewsButton.visibility = View.GONE
                }
            })
    }

    @SuppressLint("MissingInflatedId")
    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.rounded_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.window?.navigationBarColor = ContextCompat.getColor(this, R.color.dialog_white)

        // Initialize views
        newsImageView = view.findViewById(R.id.mediaPreviewImageView)
        videoView = view.findViewById(R.id.mediaPreviewVideoView)
        val newsTextEdittext = view.findViewById<EditText>(R.id.newsTextEdittext)
        val feederNameSpinner = view.findViewById<Spinner>(R.id.feederNameSpinner)
        val uploadNewsButton = view.findViewById<Button>(R.id.upload_news_button)
        val selectFileButton = view.findViewById<Button>(R.id.select_file)
        val pasteUrlButton = view.findViewById<Button>(R.id.paste_url)
        videoProgressBar = view.findViewById(R.id.videoProgressBar) // Progress bar for video loading
        uploadAnimation = view.findViewById(R.id.redDotAnimation)

// Focus on feederNameSpinner Spinner when dialog opens
        feederNameSpinner.requestFocus()

        // Set up the spinner with feeder names
        val feederNames = arrayOf("Select Feeder", "Jamtara","Karon", "Mohra", "Narayanpur", "Kurwa","Dumka","Dhanbad","Deoghar")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, feederNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        feederNameSpinner.adapter = adapter
        // Set default selection
        feederNameSpinner.setSelection(0)

// Select media file
        selectFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*, video/*"
            startActivityForResult(intent, REQUEST_PICK_MEDIA)
        }

// Paste URL button to get URL from clipboard
        pasteUrlButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pasteData = clipData.getItemAt(0).text.toString()
                if (pasteData.contains("http")) {
                    // Display the uploadNewsButton
                    uploadNewsButton.visibility = View.VISIBLE

                    // Store the pasted URL in a variable
                    val pastedUrl = pasteData

                    // Display the media preview based on the pasted URL
                    if (pastedUrl.contains("video")) {
                        videoView.visibility = View.VISIBLE
                        newsImageView.visibility = View.GONE
                        videoProgressBar.visibility =
                            View.VISIBLE // Show progress bar while loading
                        videoView.setVideoURI(Uri.parse(pastedUrl))
                        videoView.setOnPreparedListener { mp ->
                            // When video is prepared, hide progress bar
                            videoProgressBar.visibility = View.GONE
                            // Start playing the video
                            mp.start()
                        }
                        videoView.setOnErrorListener { mp, what, extra ->
                            // Handle error if video fails to load
                            Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show()
                            videoProgressBar.visibility = View.GONE
                            false
                        }
                    } else {
                        newsImageView.visibility = View.VISIBLE
                        videoView.visibility = View.GONE
                        Glide.with(this).load(pastedUrl).into(newsImageView)
                    }

                    // Handle upload button click for URL upload
                    uploadNewsButton.setOnClickListener {
                        val newsData = newsTextEdittext.text.toString().trim()
                        val location = feederNameSpinner.selectedItem.toString().trim()

                        if (location == "Select Feeder") {
                            // Handle the error case where no feeder is selected
                            // You can display an error message or take other actions
                            // Example: showToast("Please select a feeder")
                            feederNameSpinner.requestFocus()
                            Toasty.error(this, "Please select a feeder", Toasty.LENGTH_SHORT, true)
                                .show()
                            feederNameSpinner.requestFocus()

                        } else if (newsData.isEmpty()) {
                            newsTextEdittext.error = "News data is required"
                            newsTextEdittext.requestFocus()
                        } else {
                            val isVideo = pastedUrl.contains("video")
                            val imageUrl = if (isVideo) "" else pastedUrl
                            val videoUrl = if (isVideo) pastedUrl else ""

                            // Upload news data to Firebase with the pasted URL
                            uploadDataToFirebase(location, newsData, imageUrl, videoUrl)
                            bottomSheetDialog.dismiss()
                        }
                    }
                } else {
                    Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }


        // Upload news button click event for media upload
        uploadNewsButton.setOnClickListener {
            if (mediaUri != null) {

                val newsData = newsTextEdittext.text.toString().trim()
                val location = feederNameSpinner.selectedItem.toString().trim()

                if (location == "Select Feeder") {
                    // Handle the error case where no feeder is selected
                    // You can display an error message or take other actions
                    // Example: showToast("Please select a feeder")
                    feederNameSpinner.requestFocus()
                    Toasty.error(this, "Please select a feeder", Toasty.LENGTH_SHORT, true).show()
                    feederNameSpinner.requestFocus()


                } else if (newsData.isEmpty()) {
                    newsTextEdittext.error = "News data is required"
                    newsTextEdittext.requestFocus()
                } else {
                    // Show loading animation
                    uploadNewsButton.visibility = View.GONE
                    uploadAnimation.visibility = View.VISIBLE
                    uploadAnimation.playAnimation()

                    // Upload the media and then upload data to Firebase
                    uploadMediaToStorage(mediaUri!!) { mediaUrl ->
                        if (mediaUri!!.toString().contains("video")) {
                            videoUrl = mediaUrl
                            imageUrl = "" // Clear imageUrl if video is uploaded
                        } else {
                            imageUrl = mediaUrl
                            videoUrl = "" // Clear videoUrl if image is uploaded
                        }

                        // Upload news data to Firebase after media upload is successful
                        uploadDataToFirebase(location, newsData, imageUrl, videoUrl)

                        // Hide loading animation and show the button
                        uploadAnimation.visibility = View.GONE
                        uploadNewsButton.visibility = View.VISIBLE
                        uploadAnimation.cancelAnimation()
                        bottomSheetDialog.dismiss()
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Please select and upload media before submitting",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Show bottom sheet dialog
        bottomSheetDialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_PICK_MEDIA) {
            data?.data?.let { selectedMediaUri ->
                mediaUri = selectedMediaUri
                displayMediaPreview(mediaUri!!)
            }
        }
    }

    private fun uploadMediaToStorage(mediaUri: Uri, callback: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val mediaFileName = "news_${System.currentTimeMillis()}"

        val mediaRef = if (mediaUri.toString().contains("video")) {
            storageRef.child("videos/$mediaFileName")
        } else {
            storageRef.child("images/$mediaFileName")
        }

        mediaRef.putFile(mediaUri)
            .addOnSuccessListener { taskSnapshot ->
                mediaRef.downloadUrl.addOnSuccessListener { uri ->
                    val mediaUrl = uri.toString()
                    callback(mediaUrl)
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to get media URL: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    e.printStackTrace()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload media: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                e.printStackTrace()
            }
    }

    private fun displayMediaPreview(mediaUri: Uri) {
        if (mediaUri.toString().contains("video")) {
            videoView.visibility = View.VISIBLE
            newsImageView.visibility = View.GONE
            videoView.setVideoURI(mediaUri)
            videoView.start()
        } else {
            newsImageView.visibility = View.VISIBLE
            videoView.visibility = View.GONE
            Glide.with(this).load(mediaUri).into(newsImageView)
        }
    }

    private fun uploadDataToFirebase(
        location: String,
        newsData: String,
        imageUrl: String,
        videoUrl: String
    ) {
        val timestamp = System.currentTimeMillis()
        val viewCount: Double = 0.0
        val videoCount: Double = 0.0
        val generatedNewsId =
            FirebaseDatabase.getInstance().getReference("news").push().key ?: return
        val news = NewsItem(
            generatedNewsId,
            location,
            videoCount,
            newsData,
            timestamp,
            viewCount,
            imageUrl,
            videoUrl
        )

        val newsRef = FirebaseDatabase.getInstance().getReference("news")
        newsRef.child(generatedNewsId).setValue(news)
            .addOnSuccessListener {
                Toasty.success(this, "News uploaded successfully", Toast.LENGTH_SHORT, true).show()

                // Increment newsCount for the current user
                incrementNewsCountForAllUsers()

                val senderId =
                    FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                val senderName = "News - $location" // Or retrieve the admin name dynamically

                // Notify all users about the new news item
                sendNotificationToAllUsers(senderId, senderName, "You have new News")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload news", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }


    private fun sendNotificationToAllUsers(
        senderId: String,
        senderName: String,
        messageContent: String

    ) {
        Log.d(TAG, "Sending notification to all users")
        val databaseRef = FirebaseDatabase.getInstance().reference.child("users")
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    val userToken = dataSnapshot.child("token").getValue(String::class.java)
                    userToken?.let { token ->
                        sendNotification(senderId, senderName, messageContent, token)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to retrieve user tokens: ${error.message}")
            }
        })
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
                val uniqueMessageId =
                    System.currentTimeMillis().toString() // Generate a unique message ID

                val payload = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", recipientToken)
                        put("data", JSONObject().apply {
                            put("senderId", senderId)
                            put("senderName", senderName)
                            put("messageContent", messageContent)
                            put("messageId", uniqueMessageId) // Add unique message ID
                        })
                    })
                }

                Thread {
                    try {
                        val url =
                            URL("https://fcm.googleapis.com/v1/projects/bijlionn/messages:send")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer $accessToken"
                        )
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.doOutput = true

                        OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }

                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Log.d("Notification", "Notification sent successfully")
                        } else {
                            Log.e(
                                "Notification",
                                "Failed to send notification. Response code: $responseCode"
                            )

                            // Read the error response
                            connection.errorStream?.let {
                                BufferedReader(InputStreamReader(it)).use { reader ->
                                    val errorResponse = StringBuilder()
                                    reader.forEachLine { line -> errorResponse.append(line) }
                                    Log.e("Notification", "Error response: $errorResponse")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Notification", "Error sending notification: ${e.message}")
                    }
                }.start()
            } catch (e: Exception) {
                Log.e(
                    "Notification",
                    "Error constructing notification payload: ${e.message}"
                )
            }
        } else {
            Log.e("Notification", "Failed to get access token")
        }
    }


    private fun incrementNewsCountForAllUsers() {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    val userUid = userSnapshot.key
                    if (userUid != null) {
                        incrementNewsCountForUser(userUid)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
            }
        })
    }

    private fun incrementNewsCountForUser(uid: String) {
        val userReference = FirebaseDatabase.getInstance().getReference("users").child(uid)

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val currentCount =
                    dataSnapshot.child("newsCount").getValue(Int::class.java) ?: 0
                val updatedCount = currentCount + 1
                userReference.child("newsCount").setValue(updatedCount)

// Clear badge number before updating with new count

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
            }
        })
    }


    companion object {
        val HOME_ID = R.id.home
        val CHAT_ID = R.id.chat
        val BILL_ID = R.id.bill
        val QUIZ_ID = R.id.quiz
        val ACCOUNT_ID = R.id.account
        private const val REQUEST_PICK_MEDIA = 101
    }

    override fun onResume() {
        super.onResume()
        // Fetch total news count again in case it has changed while the activity was paused
        fetchTotalNewsCount()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filter)
    }
    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkChangeReceiver)
    }

}

