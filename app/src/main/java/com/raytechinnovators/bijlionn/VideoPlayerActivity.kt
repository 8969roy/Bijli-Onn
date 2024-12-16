package com.raytechinnovators.bijlionn

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.raytechinnovators.bijlionn.R

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var player: ExoPlayer
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        playerView = findViewById(R.id.playerView)
        progressBar = findViewById(R.id.progressBar)

        // Set ProgressBar color to white
        progressBar.indeterminateDrawable.setColorFilter(
            ContextCompat.getColor(this, android.R.color.white),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
        changeNavigationBarColor()

        // Get the video URI from the intent extras
        val videoUriString = intent.getStringExtra("videoUri")
        val videoUri = Uri.parse(videoUriString)

        // Initialize ExoPlayer
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // Create a MediaItem from the URI
        val mediaItem = MediaItem.fromUri(videoUri)

        // Set the MediaItem to the player
        player.setMediaItem(mediaItem)

        // Prepare the player
        player.prepare()

        // Start playing the video
        player.play()

        // Add listener to show/hide ProgressBar based on buffering state
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                when (state) {
                    Player.STATE_BUFFERING -> progressBar.visibility = View.VISIBLE
                    else -> progressBar.visibility = View.GONE
                }
            }
        })
    }

    fun changeNavigationBarColor() {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigation_bar_color)
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
