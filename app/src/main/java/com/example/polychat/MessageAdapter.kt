package com.example.polychat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val context: Context, private val messageList: ArrayList<Message>, private val loggedInUserId: String):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private val receive = 1 //받는 타입
    private val send = 2 //보내는 타입

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if(viewType == 1){ //받는 화면
            val view: View = LayoutInflater.from(context).inflate(R.layout.receive, parent, false)
            ReceiveViewHolder(view)
        }else{ //보내는 화면
            val view: View = LayoutInflater.from(context).inflate(R.layout.send, parent, false)
            SendViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //현재 메시지
        val currentMessage = messageList[position]

        when (holder) {
            is SendViewHolder -> {
                holder.sendMessage.text = currentMessage.message
            }
            is ReceiveViewHolder -> {
                holder.receiveMessage.text = currentMessage.message
            }
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun getItemViewType(position: Int): Int {

        //메시지값
        val currentMessage = messageList[position]

        return if(loggedInUserId == currentMessage.sendId){
            send
        }else{
            receive
        }
    }

    //보낸 쪽
    class SendViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val sendMessage: TextView = itemView.findViewById(R.id.send_message_text)
    }

    //받는 쪽
    class ReceiveViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val receiveMessage: TextView = itemView.findViewById(R.id.receive_message_text)
    }
}