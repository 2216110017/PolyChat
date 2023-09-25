package com.example.polychat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.*
import kotlinx.coroutines.launch

class BoardActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var databaseReference: DatabaseReference
    private val postList = mutableListOf<Post>()

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()            // 뒤로가기 시 실행할 코드
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        val stuName = intent.getStringExtra("stuName")
        val department = intent.getStringExtra("department")
        val stuNum = intent.getStringExtra("stuNum")
        val uId = intent.getStringExtra("uId")
        Log.d("BoardActivity", "Received UID: $uId")

        listView = findViewById(R.id.listView)
        databaseReference = FirebaseDatabase.getInstance().getReference("post")

        // Firebase에서 게시물 불러오기
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    try {
                        val title = postSnapshot.child("title").getValue(String::class.java) ?: ""
                        val content = postSnapshot.child("content").getValue(String::class.java) ?: ""
                        val uid = postSnapshot.child("uid").getValue(String::class.java) ?: ""
                        val noticechk = postSnapshot.child("noticechk").getValue(Long::class.java)?.toInt() ?: 0

                        val post = Post(title, content, uid, noticechk)
                        postList.add(post)
                    } catch (e: Exception) {
                        // 데이터 변환 중에 문제가 발생한 경우 로그를 출력하거나 오류 메시지를 표시
                        Log.e("BoardActivity", "Error reading post data: ${e.message}")
                    }
                }
                // 'noticechk' 값을 기준으로 목록을 정렬
                postList.sortByDescending { it.noticechk }
                // PostAdapter를 listView로 설정
                val adapter = PostAdapter(this@BoardActivity, postList)
                listView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BoardActivity, "게시물 불러오기 중 오류 발생: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        findViewById<View>(R.id.write_button).setOnClickListener {
            val intent = Intent(this@BoardActivity, WriteActivity::class.java)
            intent.putExtra("stuName", stuName)
            intent.putExtra("department", department)
            intent.putExtra("stuNum", stuNum)
            intent.putExtra("uId", uId)
            Log.d("BoardActivity", "WriteActivity에 UID 보내기: $uId")
            startActivity(intent)
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

        this.onBackPressedDispatcher.addCallback(this,onBackPressedCallback) // 뒤로가기 콜백

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.log_out){
            // 로그아웃 로직 처리
            lifecycleScope.launch {
                dataStore.edit { preferences ->
                    preferences.clear()
                }
            }

            val intent = Intent(this@BoardActivity, LogInActivity::class.java)
            startActivity(intent)
            finish()
            return true
        }
        return true
    }
}
