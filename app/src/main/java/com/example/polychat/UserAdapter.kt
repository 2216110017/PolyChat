package com.example.polychat

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.annotation.GlideModule

@GlideModule
class UserAdapter(private val context: Context, private val userList: ArrayList<User>):
    RecyclerView.Adapter<UserAdapter.UserViewHolder>(){

    /**
     * 화면 설정
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View = LayoutInflater.from(context).
        inflate(R.layout.user_layout, parent, false)

        return UserViewHolder(view)
    }

    /**
     * 데이터 설정
     */
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        //데이터 담기
        val currentUser = userList[position]
        //화면에 데이터 보여주기
        holder.nameText.text = currentUser.stuName

        // 프로필 사진 설정
        if (currentUser.profileImageUrl != null) {
            Glide.with(context)
                .load(currentUser.profileImageUrl)
                .error(R.drawable.default_profile)
                .into(holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.default_profile)
        }

        //아이템 클릭 이벤트
        holder.itemView.setOnClickListener {
            val intent  = Intent(context, ChatActivity::class.java)
            //넘길 데이터
            intent.putExtra("stuName", currentUser.stuName)
            intent.putExtra("uId", currentUser.uId)
            context.startActivity(intent)
        }
    }
    /**
     * 데이터 갯수 가져오기
     */
    override fun getItemCount(): Int {
        return userList.size
    }

    class UserViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val nameText: TextView = itemView.findViewById(R.id.name_text)
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
    }
}