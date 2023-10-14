package com.example.polychat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import kotlinx.coroutines.launch

class BoardActivity : AppCompatActivity() {

    private lateinit var noticeListView: ListView
    private lateinit var normalListView: ListView
    private lateinit var databaseReference: DatabaseReference
    private lateinit var toggleNoticeIcon: ImageView
    private val noticeList = mutableListOf<Post>()
    private val normalList = mutableListOf<Post>()

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()  // 뒤로가기 시 실행할 코드
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        val stuName = intent.getStringExtra("stuName")
        val department = intent.getStringExtra("department") ?: ""
        val stuNum = intent.getStringExtra("stuNum")
        val uId = intent.getStringExtra("uId")
        Log.d("BoardActivity", "NewActivity에서 받은 UID: $uId")

        noticeListView = findViewById(R.id.noticeListView)
        normalListView = findViewById(R.id.normalListView)
        toggleNoticeIcon = findViewById(R.id.toggleNoticeIcon)
        databaseReference = FirebaseDatabase.getInstance().getReference("post")

        // Firebase에서 게시물 불러오기
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                noticeList.clear()
                normalList.clear()
                for (postSnapshot in snapshot.children) {
                    try {
                        val postDepartment = postSnapshot.child("department").getValue(String::class.java) ?: ""
                        if (postDepartment == department) {
                            val title = postSnapshot.child("title").getValue(String::class.java) ?: ""
                            val content = postSnapshot.child("content").getValue(String::class.java) ?: ""
                            val uid = postSnapshot.child("uid").getValue(String::class.java) ?: ""
                            val noticechk = postSnapshot.child("noticechk").getValue(Long::class.java)?.toInt() ?: 0
                            val fileUrl = postSnapshot.child("fileUrl").getValue(String::class.java)  // fileUrl 불러오기

                            val post = Post(title, content, uid, noticechk, department, postSnapshot.key, fileUrl)  // postSnapshot.key를 사용하여 postId 값을 설정 및 fileUrl 추가
                            if (noticechk == 1) {
                                noticeList.add(post)
                            } else {
                                normalList.add(post)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BoardActivity", "게시글 불러오기 실패: ${e.message}")
                    }
                }

                val normalAdapter = PostAdapter(this@BoardActivity, normalList)
                normalListView.adapter = normalAdapter

                val noticeAdapter = PostAdapter(this@BoardActivity, noticeList)
                noticeListView.adapter = noticeAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BoardActivity, "게시물 불러오기 중 오류 발생: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })


        toggleNoticeIcon.setOnClickListener {
            if (noticeListView.visibility == View.VISIBLE) {
                noticeListView.visibility = View.GONE
                toggleNoticeIcon.setImageResource(R.drawable.baseline_expand_more_24)
            } else {
                noticeListView.visibility = View.VISIBLE
                setListViewHeightBasedOnChildren(noticeListView)
                toggleNoticeIcon.setImageResource(R.drawable.baseline_expand_less_24)
            }
        }

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

        // 공지사항 ListView 아이템 클릭 리스너
        noticeListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedPost = noticeList[position]
            val intent = Intent(this@BoardActivity, PostDetailActivity::class.java)
            intent.putExtra("postTitle", selectedPost.title)
            intent.putExtra("postContent", selectedPost.content)
            intent.putExtra("postUid", selectedPost.postID)
            intent.putExtra("department", department)
            intent.putExtra("uId", uId)
            startActivity(intent)
        }

        // 일반 게시물 ListView 아이템 클릭 리스너
        normalListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedPost = normalList[position]
            val intent = Intent(this@BoardActivity, PostDetailActivity::class.java)
            intent.putExtra("postTitle", selectedPost.title)
            intent.putExtra("postContent", selectedPost.content)
            intent.putExtra("postUid", selectedPost.postID)
            intent.putExtra("department", department)
            intent.putExtra("uId", uId)
            startActivity(intent)
        }

        this.onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun setListViewHeightBasedOnChildren(listView: ListView) {
        val listAdapter = listView.adapter ?: return
        var totalHeight = 0
        val desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.AT_MOST)
        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
            totalHeight += listItem.measuredHeight
        }
        val params = listView.layoutParams
        params.height = totalHeight + (listView.dividerHeight * (listAdapter.count - 1))
        listView.layoutParams = params
        listView.requestLayout()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.log_out) {
            val intent = Intent(this@BoardActivity, LogInActivity::class.java)
            startActivity(intent)
            finish()
            return true
        }
        return true
    }
}
