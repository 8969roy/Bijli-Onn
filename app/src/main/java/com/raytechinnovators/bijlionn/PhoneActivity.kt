package com.raytechinnovators.bijlionn

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.messaging.FirebaseMessaging

import java.util.concurrent.TimeUnit

class PhoneActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var progressBar: ProgressBar
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var phoneBox: EditText
    private lateinit var rootView: LinearLayout // Root layout of the activity

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Initialize UI components
        val login = findViewById<Button>(R.id.Send)
        phoneBox = findViewById(R.id.phoneBox)
        progressBar = findViewById(R.id.progressBar)
        rootView = findViewById(R.id.rootView) // Root layout initialization
        changeNavigationBarColor()
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        // Check if user is already logged in, if yes, redirect to MainActivity
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }

        // Set listener for login button click
        login.setOnClickListener {
            login()
            closeKeyboard()
        }

        // Set up PhoneAuthProvider callbacks
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Handle verification completed
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finish()
                // Handle verification completed
                val phoneNumber = "+91${phoneBox.text}"
                val intent = Intent(applicationContext, SetProfile::class.java).apply {
                    putExtra("phoneNumber", phoneNumber)
                }
                startActivity(intent)
                finish()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // Handle verification failed
                Toast.makeText(applicationContext, "Failed", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                login.visibility = View.VISIBLE
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                // Handle code sent
                storedVerificationId = verificationId
                resendToken = token
                val phoneNumber = "+91${phoneBox.text}"
                val intent = Intent(applicationContext, OTPActivity::class.java)

                intent.putExtra("storedVerificationId", storedVerificationId)
                intent.putExtra("phoneNumber", phoneNumber)

                startActivity(intent)

            }

        }

        // Get FCM token for silent push notifications
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("PhoneActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            Log.d("PhoneActivity", "FCM Token: $token")
        }

        phoneBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val login = findViewById<Button>(R.id.Send)
                if (s?.length == 10) {
                    login.isEnabled = true
                    login.background = ContextCompat.getDrawable(applicationContext, R.drawable.enabled_button_background)
                } else {
                    login.isEnabled = false
                    login.background = ContextCompat.getDrawable(applicationContext, R.drawable.disabled_button_background)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })



    }

    fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.dialog_white)
    }

    // Function to close the keyboard
    private fun closeKeyboard() {
        val view: View? = currentFocus
        if (view != null) {
            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // Function to initiate phone number verification
    private fun login() {
        val phoneNumber = "+91${phoneBox.text}"
        sendVerificationCode(phoneNumber)
    }

    // Function to send verification code using Firebase Phone Auth
    private fun sendVerificationCode(phoneNumber: String) {
        val login = findViewById<Button>(R.id.Send)
        login.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}
