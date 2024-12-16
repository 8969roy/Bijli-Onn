package com.raytechinnovators.bijlionn

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.raytechinnovators.bijlionn.databinding.ActivitySetProfileBinding
import com.raytechinnovators.bijlionn.models.User
import com.raytechinnovators.bijlionn.R
import es.dmoral.toasty.Toasty

class SetProfile : AppCompatActivity() {
    private val binding by lazy { ActivitySetProfileBinding.inflate(layoutInflater) }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private var selectedImage: Uri? = null
    private val dialog: ProgressDialog by lazy {
        ProgressDialog(this).apply {
            setMessage("Updating profile...")
            setCancelable(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val phoneNumber = intent.getStringExtra("phoneNumber")
        changeNavigationBarColor()
        // Initialize Spinner
        val feederNames = arrayOf("Select Feeder Location", "Jamtara", "Karon", "Mohra", "Narayanpur", "Kurwa" ,"Dumka","Dhanbad","Deoghar")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, feederNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.feederBox.adapter = adapter

        // Check if user details exist in the database
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.reference.child("users").child(currentUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val user = dataSnapshot.getValue(User::class.java)
                        if (user != null) {
                            // Populate UI fields with existing user details
                            binding.nameBox.setText(user.name)
                            // Set Spinner selection
                            val locationIndex = feederNames.indexOf(user.location)
                            if (locationIndex >= 0) {
                                binding.feederBox.setSelection(locationIndex)
                            }

                            // Load profile image if it exists
                            if (!user.profileImage.isNullOrEmpty()) {
                                val imageUrl = user.profileImage
                                Glide.with(this@SetProfile)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.avtar)
                                    .into(binding.imageView)
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e(TAG, "Error fetching user details: ${databaseError.message}")
                    }
                })
        }

        binding.imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            startActivityForResult(intent, 45)
        }

        binding.continueBtn.setOnClickListener {
            val name = binding.nameBox.text.toString()
            val location = binding.feederBox.selectedItem.toString()

            if (name.isEmpty()) {
                binding.nameBox.error = "Please type your name"
                return@setOnClickListener
            }

            if (location == "Select Feeder Location") {
                // Use Toasty to show an error message
                Toasty.error(this, "Please select a Feeder", Toasty.LENGTH_SHORT, true).show()
                binding.feederBox.requestFocus()
                return@setOnClickListener
            }

            dialog.show()

            if (selectedImage != null) {
                if (phoneNumber != null) {
                    uploadImageAndStoreUserDetails(name, phoneNumber, location)
                }
            } else {
                val currentUser = auth.currentUser

                if (currentUser != null) {
                    database.reference.child("users").child(currentUser.uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val existingUser = dataSnapshot.getValue(User::class.java)
                                val isAdmin = existingUser?.isAdmin ?: false
                                val loginTimestamp = System.currentTimeMillis()

                                val imageUrl = existingUser?.profileImage ?: ""

                                val user = User(
                                    currentUser.uid,
                                    name,
                                    phoneNumber ?: "",
                                    imageUrl,
                                    location,
                                    isAdmin,
                                    false,
                                    0,
                                    0,
                                    0,
                                    0,
                                    "",
                                    0,
                                    loginTimestamp
                                )
                                storeUserDetails(user)
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.e(TAG, "Error fetching user details: ${databaseError.message}")
                            }
                        })
                }
            }
        }
    }

    fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.set_bar_color)
    }

    private fun uploadImageAndStoreUserDetails(name: String, phoneNumber: String, location: String) {
        val uid = auth.uid ?: ""
        val profileImagesRef = storage.reference.child("profile_images").child(uid)
        val uploadTask = profileImagesRef.putFile(selectedImage!!)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            profileImagesRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                val imageUrl = downloadUri.toString()

                // Fetch existing user data to avoid overwriting
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    database.reference.child("users").child(currentUser.uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val existingUser = dataSnapshot.getValue(User::class.java)
                                val isAdmin = existingUser?.isAdmin ?: false
                                val loginTimestamp = System.currentTimeMillis()

                                // Ensure phoneNumber is included when creating User object
                                val user = User(
                                    uid, name, phoneNumber, imageUrl, location,
                                    isAdmin, false, 0, 0, 0, 0, "", 0, loginTimestamp
                                )
                                storeUserDetails(user)
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                dialog.dismiss()
                                Log.e(TAG, "Error fetching user details: ${databaseError.message}")
                            }
                        })
                }
            } else {
                dialog.dismiss()
                Toast.makeText(this, "Failed to upload image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun storeUserDetails(user: User) {
        user.phoneNumber = user.phoneNumber ?: "" // Ensure phoneNumber is not null
        user.uid?.let { uid ->
            val userRef = database.reference.child("users").child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val existingUser = dataSnapshot.getValue(User::class.java)
                    val previousLocation = existingUser?.location

                    userRef.setValue(user)
                        .addOnSuccessListener {
                            if (previousLocation != user.location) {
                                previousLocation?.let { updateTotalUsersCount(it, increment = false) }
                                user.location?.let { it1 -> updateTotalUsersCount(it1, increment = true) }
                            }
                            user.name?.let { it1 -> updateProfile(it1) }
                        }
                        .addOnFailureListener { e ->
                            dialog.dismiss()
                            Toast.makeText(this@SetProfile, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    dialog.dismiss()
                    Log.e(TAG, "Error fetching user details: ${databaseError.message}")
                }
            })
        }
    }

    private fun updateProfile(displayName: String) {
        val user = auth.currentUser
        user?.let {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            it.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    dialog.dismiss()
                    if (task.isSuccessful) {
                        Log.d(TAG, "User profile updated.")
                        val intent = Intent(this@SetProfile, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to update user profile.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun updateTotalUsersCount(location: String, increment: Boolean) {
        val totalUsersRef = database.reference.child("TotalUsers").child(location)
        totalUsersRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                var currentCount = mutableData.getValue(Int::class.java)
                if (currentCount == null) {
                    currentCount = 0
                }
                mutableData.value = if (increment) currentCount + 1 else currentCount - 1
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                Log.d(TAG, "updateTotalUsersCount:onComplete:${databaseError?.message}")
            }
        })
    }


        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            if (data.data != null) {
                binding.imageView.setImageURI(data.data)
                selectedImage = data.data
            }
        }
    }

    companion object {
        private const val TAG = "com.raytechinnovators.bijlionn.SetProfile"
    }
}
