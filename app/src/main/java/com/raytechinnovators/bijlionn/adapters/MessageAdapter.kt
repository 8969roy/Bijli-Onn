package com.raytechinnovators.bijlionn.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.raytechinnovators.bijlionn.R
import com.raytechinnovators.bijlionn.databinding.ReceiverLayoutBinding
import com.raytechinnovators.bijlionn.databinding.SenderLayoutBinding
import com.raytechinnovators.bijlionn.FullImageActivity
import com.raytechinnovators.bijlionn.VideoPlayerActivity

import java.text.SimpleDateFormat
import java.util.*

import com.raytechinnovators.bijlionn.models.Message
import es.dmoral.toasty.Toasty


class MessageAdapter(
    private val context: Context,
    private val messages: MutableList<Message>,
    private val isAdmin: Boolean

) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            if (binding is SenderLayoutBinding) {
                binding.senderImage.setOnClickListener(this)
                binding.senderVideoThumbnail.setOnClickListener(this)
                binding.senderImage.setOnLongClickListener {
                    if (isAdmin) {
                        showImageOptionsDialog(messages[adapterPosition].imageUrl)
                    }
                    true
                }

                binding.senderVideoThumbnail.setOnLongClickListener {
                    if (isAdmin) {
                        showImageOptionsDialog(messages[adapterPosition].videoUrl)
                    }
                    true
                }

            } else if (binding is ReceiverLayoutBinding) {
                binding.receiverImage.setOnClickListener(this)
                binding.receiverVideoThumbnail.setOnClickListener(this)
                binding.receiverImage.setOnLongClickListener {
                    if (isAdmin) {
                        showImageOptionsDialog(messages[adapterPosition].imageUrl)
                    }
                    true
                }

                binding.receiverVideoThumbnail.setOnLongClickListener {
                    if (isAdmin) {
                        showImageOptionsDialog(messages[adapterPosition].videoUrl)
                    }
                    true
                }
            }


        }

        fun bind(message: Message) {
            if (binding is SenderLayoutBinding) {
                binding.senderMessage.text = message.messageContent ?: ""
                binding.senderTime.text = getTimeString(message.timestamp)

                // Set visibility for the message view
                if (message.messageContent.isNullOrEmpty()) {
                    binding.senderMessage.visibility = View.GONE
                } else {
                    binding.senderMessage.visibility = View.VISIBLE
                    binding.senderMessage.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }

                // Set visibility for the image and video views
                if (message.imageUrl != null) {


                    Glide.with(context).load(message.imageUrl).placeholder(R.drawable.image_placeholder)
                        .into(binding.senderImage)
                    binding.senderImage.visibility = View.VISIBLE
                    binding.senderImageCard.visibility = View.VISIBLE
                    binding.senderImage.layoutParams.height = 500
                    binding.senderImage.layoutParams.width = 850

                    binding.senderVideoThumbnail.visibility = View.GONE
                } else if (message.videoUrl != null) {
                    Glide.with(context)
                        .asBitmap()
                        .load(Uri.parse(message.videoUrl))
                        .frame(1000000)
                        .placeholder(R.drawable.image_placeholder)
                        .into(binding.senderVideoThumbnail)
                    binding.senderVideoThumbnail.visibility = View.VISIBLE
                    binding.senderVideoThumbnailCard.visibility = View.VISIBLE
                    // Ensure the play button is visible
                    binding.playButton.visibility = View.VISIBLE

                    binding.senderVideoThumbnailCard.layoutParams.width = 700
                    binding.senderVideoThumbnailCard.layoutParams.height = 1000


                    binding.senderImage.visibility = View.GONE
                } else {
                    binding.senderImage.visibility = View.GONE
                    binding.senderVideoThumbnail.visibility = View.GONE
                }
            } else if (binding is ReceiverLayoutBinding) {
                binding.receiverMessage.text = message.messageContent ?: ""
                binding.receiverTime.text = getTimeString(message.timestamp)

                // Set visibility for the message view
                if (message.messageContent.isNullOrEmpty()) {
                    binding.receiverMessage.visibility = View.GONE
                } else {
                    binding.receiverMessage.visibility = View.VISIBLE
                }

                // Set visibility for the image and video views
                if (message.imageUrl != null) {


                    Glide.with(context).load(message.imageUrl).placeholder(R.drawable.image_placeholder)
                        .into(binding.receiverImage)
                    binding.receiverImage.visibility = View.VISIBLE
                    binding.receiverImageCard.visibility = View.VISIBLE
                    binding.receiverVideoThumbnail.visibility = View.GONE
                } else if (message.videoUrl != null) {
                    Glide.with(context)
                        .asBitmap()
                        .load(Uri.parse(message.videoUrl))
                        .frame(1000000)
                        .placeholder(R.drawable.image_placeholder)
                        .into(binding.receiverVideoThumbnail)
                    binding.receiverVideoThumbnail.visibility = View.VISIBLE
                    binding.receiverVideoThumbnailCard.visibility = View.VISIBLE
                    binding.receiverImage.visibility = View.GONE
                } else {
                    binding.receiverImage.visibility = View.GONE
                    binding.receiverVideoThumbnail.visibility = View.GONE
                }
            }
        }

        override fun onClick(view: View) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val message = messages[position]
                val url =
                    if (view.id == R.id.senderVideoThumbnail || view.id == R.id.receiverVideoThumbnail) {
                        message.videoUrl
                    } else {
                        message.imageUrl
                    }
                if (!url.isNullOrEmpty()) {
                    val intent = if (message.videoUrl != null) {
                        Intent(context, VideoPlayerActivity::class.java).apply {
                            putExtra("videoUri", message.videoUrl)
                        }
                    } else {
                        Intent(context, FullImageActivity::class.java).apply {
                            putExtra("imageUrl", message.imageUrl)
                        }
                    }
                    context.startActivity(intent)
                }
            }


        }



    }

    private fun showImageOptionsDialog(url: String?) {
        url?.let {
            // Assuming `context` is available here, if not pass it as parameter or retrieve accordingly
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("URL", it)
            clipboard.setPrimaryClip(clip)
            Toasty.normal(context, "URL copied to clipboard", R.drawable.eye).show();
        }
    }

    private fun uploadToNews(imageUrl: String) {

    }

    private fun shareImage(imageUrl: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Sharing image")
            putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUrl))
            type = "image/*"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }



    private fun getTimeString(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = if (viewType == VIEW_TYPE_SENDER) {
            SenderLayoutBinding.inflate(inflater, parent, false)
        } else {
            ReceiverLayoutBinding.inflate(inflater, parent, false)
        }
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (FirebaseAuth.getInstance().uid == message.senderId) {
            VIEW_TYPE_SENDER
        } else {
            VIEW_TYPE_RECEIVER
        }
    }

    companion object {
        private const val VIEW_TYPE_SENDER = 1
        private const val VIEW_TYPE_RECEIVER = 2
    }
}
