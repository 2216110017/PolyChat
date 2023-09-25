package com.example.polychat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

class PostDetailActivity : AppCompatActivity() {

    private lateinit var titleLabel: TextView
    private lateinit var contentLabel: TextView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private lateinit var backButton: Button
    private lateinit var databaseReference: DatabaseReference
    private var postUID: String? = null
    private var userUID: String? = null
    private var noticechk: Int = 0  // 기본값은 0으로 설정

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()            // 뒤로가기 시 실행할 코드
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        titleLabel = findViewById(R.id.title_label)
        contentLabel = findViewById(R.id.content_label)
        editButton = findViewById(R.id.edit_button)
        deleteButton = findViewById(R.id.delete_button)
        backButton = findViewById(R.id.back_button)

        databaseReference = FirebaseDatabase.getInstance().getReference("post")

        postUID = intent.getStringExtra("post_uid")
        userUID = intent.getStringExtra("user_uid")

//        // Firebase에서 게시물 데이터를 검색하고 라벨 설정
//        // postUID가 userUID와 동일하면 편집 및 삭제 버튼을 표시
//        if (postUID == userUID) {
//            editButton.visibility = View.VISIBLE
//            deleteButton.visibility = View.VISIBLE
//        } else {
//            editButton.visibility = View.GONE
//            deleteButton.visibility = View.GONE
//        }

        fetchPostDetails()

        editButton.setOnClickListener {
            val intent = Intent(this@PostDetailActivity, PostEditActivity::class.java)
            // 게시글의 UID, 제목, 내용, 그리고 사용자의 UID를 Intent에 넣기
            intent.putExtra("post_uid", postUID)
            intent.putExtra("post_title", titleLabel.text.toString())
            intent.putExtra("post_content", contentLabel.text.toString())
            intent.putExtra("user_uid", userUID)
            intent.putExtra("notice", noticechk == 1)
            startActivity(intent)
        }

        deleteButton.setOnClickListener {
            // Firebase에서 게시물 삭제
            databaseReference.child(postUID ?: "").removeValue()
            val intent = Intent(this@PostDetailActivity, BoardActivity::class.java)
            startActivity(intent)
            finish()
        }

        backButton.setOnClickListener {
            val intent = Intent(this@PostDetailActivity, BoardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun fetchPostDetails() {
        postUID?.let { it ->
            databaseReference.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val post = snapshot.getValue(Post::class.java)
                    post?.let {
                        titleLabel.text = it.title
                        contentLabel.text = it.content
                        noticechk = it.noticechk

                        // postUID가 userUID와 동일하면 편집 및 삭제 버튼을 표시
                        if (it.uid == userUID) {
                            editButton.visibility = View.VISIBLE
                            deleteButton.visibility = View.VISIBLE
                        } else {
                            editButton.visibility = View.GONE
                            deleteButton.visibility = View.GONE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PostDetailActivity", "Database read failed: ${error.message}")
                    // 사용자에게 오류 알림 표시
                    Toast.makeText(this@PostDetailActivity, "데이터를 불러오는 데 실패했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                }
            })
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

            val intent = Intent(this@PostDetailActivity, LogInActivity::class.java)
            startActivity(intent)
            finish()
            return true
        }
        return true
    }
}
