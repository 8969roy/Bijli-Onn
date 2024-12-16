package com.raytechinnovators.bijlionn

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.raytechinnovators.bijlionn.R

class OTPActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otpactivity)

        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Add yeh code onCreate() function mein
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Get the stored verification ID passed from PhoneActivity
        val storedVerificationId = intent.getStringExtra("storedVerificationId")

        // Find views
        val verifyButton = findViewById<Button>(R.id.ContinueBtn)
        val otpGiven = findViewById<EditText>(R.id.id_otp)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        // Set up action listener for the OTP EditText
        otpGiven.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                closeKeyboard()
                true
            } else {
                false
            }
        }

        // Set up click listener for the verification button
        verifyButton.setOnClickListener {
            val otp = otpGiven.text.toString().trim()
            if (otp.isNotEmpty()) {
                val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(
                    storedVerificationId!!, otp
                )
                signInWithPhoneAuthCredential(credential)
            } else {
                Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show()
            }

            // Hide the verification button and show progress bar while processing
            verifyButton.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
        }
        changeNavigationBarColor()
    }

    // Handle back arrow click
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Handle backpress
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, PhoneActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
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

    // Function to sign in with phone authentication credential
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Get the phone number passed from PhoneActivity
                    val phoneNumber = intent.getStringExtra("phoneNumber")

                    val intent = Intent(applicationContext, SetProfile::class.java).apply {
                        putExtra("phoneNumber", phoneNumber)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Handle authentication failures, such as invalid OTP
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                    } else {
                        // Handle other authentication failures
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                    // Show the verification button again and hide progress bar
                    val verifyButton = findViewById<Button>(R.id.ContinueBtn)
                    val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                    progressBar.visibility = View.GONE
                    verifyButton.visibility = View.VISIBLE
                }
            }
    }
}
