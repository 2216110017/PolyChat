package com.example.polychat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.storage.FirebaseStorage

class MessageAdapter(
    private val context: Context,
    private val messageList: ArrayList<Message>,
    private val loggedInUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val RECEIVE = 1
    private val SEND = 2
    private val SEND_IMAGE = 3
    private val RECEIVE_IMAGE = 4

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            RECEIVE -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.receive, parent, false)
                ReceiveViewHolder(view)
            }

            SEND -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.send, parent, false)
                SendViewHolder(view)
            }

            SEND_IMAGE -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.send_image, parent, false)
                SendImageViewHolder(view)
            }

            RECEIVE_IMAGE -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.receive_image, parent, false)
                ReceiveImageViewHolder(view)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        when (holder) {
            is SendViewHolder -> {
                holder.sendMessage.text = currentMessage.message
                holder.sendTime.text = currentMessage.sentTime
            }

            is ReceiveViewHolder -> {
                holder.receiveMessage.text = currentMessage.message
                holder.receiveTime.text = currentMessage.sentTime
            }

            is SendImageViewHolder -> {
                val storageReference =
                    FirebaseStorage.getInstance().getReferenceFromUrl(currentMessage.imageUrl!!)
                Glide.with(context)
                    .load(storageReference)
                    .into(holder.sendImage)
            }

            is ReceiveImageViewHolder -> {
                val storageReference =
                    FirebaseStorage.getInstance().getReferenceFromUrl(currentMessage.imageUrl!!)
                Glide.with(context)
                    .load(storageReference)
                    .into(holder.receiveImage)
            }
        }
    }


    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]
        return when {
            loggedInUserId == currentMessage.sendId && currentMessage.fileType == "image" -> SEND_IMAGE
            loggedInUserId != currentMessage.sendId && currentMessage.fileType == "image" -> RECEIVE_IMAGE
            loggedInUserId == currentMessage.sendId -> SEND
            else -> RECEIVE
        }
    }

    class SendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sendMessage: TextView = itemView.findViewById(R.id.send_message_text)
        val sendTime: TextView = itemView.findViewById(R.id.send_message_time)
    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage: TextView = itemView.findViewById(R.id.receive_message_text)
        val receiveTime: TextView = itemView.findViewById(R.id.receive_message_time)
    }

    class SendImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sendImage: ImageView = itemView.findViewById(R.id.send_image_view)
        // 필요한 경우 추가 뷰 바인딩
    }

    class ReceiveImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveImage: ImageView = itemView.findViewById(R.id.receive_image_view)
    }
}