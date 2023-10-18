package com.example.polychat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        when (holder) {
            is SendViewHolder -> {
                holder.sendMessage.text = currentMessage.message
                holder.sendTime.text = currentMessage.sentTime
                holder.sendUserName.text = currentMessage.userName
            }

            is ReceiveViewHolder -> {
                holder.receiveMessage.text = currentMessage.message
                holder.receiveTime.text = currentMessage.sentTime
                holder.receiveUserName.text = currentMessage.userName
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
        val sendUserName: TextView = itemView.findViewById(R.id.send_user_name)
    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage: TextView = itemView.findViewById(R.id.receive_message_text)
        val receiveTime: TextView = itemView.findViewById(R.id.receive_message_time)
        val receiveUserName: TextView = itemView.findViewById(R.id.receive_user_name)
    }
}