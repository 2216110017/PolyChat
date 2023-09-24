package com.example.polychat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


class PostAdapter(private val context: Context, private val dataSource: List<Post>) : BaseAdapter() {
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val userReference = FirebaseDatabase.getInstance().getReference("user")

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.post_item_layout, parent, false)
        val titleTextView = view.findViewById<TextView>(R.id.post_item_title)
        val authorTextView = view.findViewById<TextView>(R.id.post_item_author)
        val post = getItem(position) as Post

        titleTextView.text = post.title
        titleTextView.typeface = if (post.noticechk == 1) {
            android.graphics.Typeface.DEFAULT_BOLD
        } else {
            android.graphics.Typeface.DEFAULT
        }

        // 작성자의 이름을 가져오기
        post.uid?.let { uid ->
            userReference.child(uid).child("stuName").addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val authorName = snapshot.value as? String
                    authorTextView.text = "작성자: $authorName"
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
        }
        return view
    }
}
