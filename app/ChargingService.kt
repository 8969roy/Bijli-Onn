import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ChargingService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isCharging = intent?.getBooleanExtra("isCharging", false) ?: false
        updateChargingStatusInFirebase(isCharging)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun updateChargingStatusInFirebase(isCharging: Boolean) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val userReference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUserUid)
                .child("chargingStatus")

            userReference.setValue(isCharging).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ChargingService", "Charging status updated successfully in Firebase")
                } else {
                    Log.e(
                        "ChargingService",
                        "Failed to update charging status in Firebase: ${task.exception?.message}"
                    )
                }
                stopSelf()  // Stop the service after task is complete
            }
        }
    }
}
