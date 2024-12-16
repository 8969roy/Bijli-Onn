package com.raytechinnovators.bijlionn

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import java.io.IOException

class AccessToken(private val context: Context) {

    val accessToken: String?
        get() = try {
            val serviceAccount = context.assets.open("serviceAccountKey.json")
            val googleCredentials = GoogleCredentials.fromStream(serviceAccount)
                .createScoped("https://www.googleapis.com/auth/firebase.messaging")
            googleCredentials.refreshIfExpired()
            googleCredentials.getAccessToken().tokenValue
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
}
