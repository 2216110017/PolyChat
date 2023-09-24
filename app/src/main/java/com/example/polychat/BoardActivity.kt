package com.example.polychat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class BoardActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var databaseReference: DatabaseReference
    private val postList = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        val stuName = intent.getStringExtra("stuName")
        val department = intent.getStringExtra("department")
        val stuNum = intent.getStringExtra("stuNum")
        val loginUID = intent.getStringExtra("loginUID")

        listView = findViewById(R.id.listView)
        databaseReference = FirebaseDatabase.getInstance().getReference("post")

        // Firebase에서 게시물 불러오기
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null) {
                        postList.add(post)
                    }
                }
                // 'noticechk' 값을 기준으로 목록을 정렬
                postList.sortByDescending { it.noticechk }
                // PostAdapter를 listView로 설정
                val adapter = PostAdapter(this@BoardActivity, postList)
                listView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BoardActivity, "Error loading posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        findViewById<View>(R.id.write_button).setOnClickListener {
            startActivity(Intent(this@BoardActivity, WriteActivity::class.java))
            intent.putExtra("stuName", stuName)
            intent.putExtra("department", department)
            intent.putExtra("stuNum", stuNum)
            intent.putExtra("loginUID", loginUID)
        }

        findViewById<View>(R.id.back_button).setOnClickListener {
            finish()
        }

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedPost = postList[position]
            // Handle item click
            val intent = Intent(this@BoardActivity, PostDetailActivity::class.java)
            intent.putExtra("postTitle", selectedPost.title)
            intent.putExtra("postContent", selectedPost.content)
            intent.putExtra("postUid", selectedPost.uid)  // 게시글 작성자의 uid 전달
            startActivity(intent)
        }


    }
}
