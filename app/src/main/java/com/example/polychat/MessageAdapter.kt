package com.example.polychat

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions

class MessageAdapter(
    private val context: Context,
    private val messageList: ArrayList<Message>,
    private val loggedInUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val sendText = 1
    private val receiveText = 2
    private val sendImage = 3
    private val receiveImage = 4
    private val sendFile = 5
    private val receiveFile = 6

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            sendText -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.send, parent, false)
                SendViewHolder(view)
            }
            receiveText -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.receive, parent, false)
                ReceiveViewHolder(view)
            }
            sendImage -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.send_image, parent, false)
                SendImageViewHolder(view)
            }
            receiveImage -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.receive_image, parent, false)
                ReceiveImageViewHolder(view)
            }
            sendFile -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.send_file, parent, false)
                SendFileViewHolder(view)
            }
            receiveFile -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.receive_file, parent, false)
                ReceiveFileViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val requestOptions = RequestOptions()
            .transform(RoundedCorners(16)) // 16은 둥근 모서리의 반경입니다. 원하는 값으로 조정할 수 있습니다.
            .diskCacheStrategy(DiskCacheStrategy.ALL) // 디스크 캐시 전략 설정
            .error(R.drawable.baseline_image_not_supported_24) // 에러 발생 시 표시될 이미지
            .placeholder(R.drawable.baseline_image_24) // 로딩 중에 표시될 이미지
            .override(300, 300)
            .fitCenter()

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
            is SendImageViewHolder -> {
                Glide.with(context)
                    .load(currentMessage.imageUrl)
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade()) // 크로스 페이드 애니메이션
                    .into(holder.sendImageView)
                holder.sendImageTime.text = currentMessage.sentTime
                holder.sendUserName.text = currentMessage.userName
                holder.bind(currentMessage)
            }
            is ReceiveImageViewHolder -> {
                Glide.with(context)
                    .load(currentMessage.imageUrl)
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade()) // 크로스 페이드 애니메이션
                    .into(holder.receiveImageView)
                holder.receiveImageTime.text = currentMessage.sentTime
                holder.receiveUserName.text = currentMessage.userName
                holder.bind(currentMessage)
            }
            is SendFileViewHolder -> {
                holder.sendFileIconView.setImageResource(R.drawable.baseline_insert_drive_file_24)
                holder.sendFileTime.text = currentMessage.sentTime
                holder.sendUserName.text = currentMessage.userName
            }
            is ReceiveFileViewHolder -> {
                holder.receiveFileIconView.setImageResource(R.drawable.baseline_insert_drive_file_24)
                holder.receiveFileTime.text = currentMessage.sentTime
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
            loggedInUserId == currentMessage.sendId && currentMessage.messageType == "text" -> sendText
            loggedInUserId != currentMessage.sendId && currentMessage.messageType == "text" -> receiveText
            loggedInUserId == currentMessage.sendId && currentMessage.messageType == "image" -> sendImage
            loggedInUserId != currentMessage.sendId && currentMessage.messageType == "image" -> receiveImage
            loggedInUserId == currentMessage.sendId && currentMessage.messageType == "file" -> sendFile
            else -> receiveFile
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
    class SendImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sendImageView: ImageView = itemView.findViewById(R.id.send_image_view)
        val sendImageTime: TextView = itemView.findViewById(R.id.send_image_time)
        val sendUserName: TextView = itemView.findViewById(R.id.send_user_name)

        fun bind(message: Message) {
            sendImageView.setOnClickListener {
                val intent = Intent(itemView.context, ZoomedImageActivity::class.java)
                intent.putExtra("IMAGE_URL", message.imageUrl)
                itemView.context.startActivity(intent)
            }
        }
    }

    class ReceiveImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveImageView: ImageView = itemView.findViewById(R.id.receive_image_view)
        val receiveImageTime: TextView = itemView.findViewById(R.id.receive_image_time)
        val receiveUserName: TextView = itemView.findViewById(R.id.receive_user_name)

        fun bind(message: Message) {
            receiveImageView.setOnClickListener {
                val intent = Intent(itemView.context, ZoomedImageActivity::class.java)
                intent.putExtra("IMAGE_URL", message.imageUrl)
                itemView.context.startActivity(intent)
            }
        }
    }

    class SendFileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sendFileIconView: ImageView = itemView.findViewById(R.id.send_file_icon_view)
        val sendFileTime: TextView = itemView.findViewById(R.id.send_file_time)
        val sendUserName: TextView = itemView.findViewById(R.id.send_user_name)
    }
    class ReceiveFileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveFileIconView: ImageView = itemView.findViewById(R.id.receive_file_icon_view)
        val receiveFileTime: TextView = itemView.findViewById(R.id.receive_file_time)
        val receiveUserName: TextView = itemView.findViewById(R.id.receive_user_name)
    }
}