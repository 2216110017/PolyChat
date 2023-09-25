package com.example.polychat

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class PostEditActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var noticeCheckbox: CheckBox
    private lateinit var databaseReference: DatabaseReference

    private var postUID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_edit)

        // Intent에서 게시글 정보와 사용자의 UID를 가져옵니다.
        postUID = intent.getStringExtra("post_uid")
        val postTitle = intent.getStringExtra("post_title")
        val postContent = intent.getStringExtra("post_content")
        val userUID = intent.getStringExtra("user_uid")

        titleEditText = findViewById(R.id.title_edittext)
        contentEditText = findViewById(R.id.content_edittext)
        noticeCheckbox = findViewById(R.id.notice_checkbox)
        databaseReference = FirebaseDatabase.getInstance().getReference("post")

        // PostDetailActivity에서 전달된 데이터로 필드를 채웁니다.
        titleEditText.setText(intent.getStringExtra("post_title"))
        contentEditText.setText(intent.getStringExtra("post_content"))
        noticeCheckbox.isChecked = intent.getBooleanExtra("notice", false)

//        findViewById<View>(R.id.edit_post_button).apply {
//            setOnClickListener {
//                updatePost()
//            }
//        }

        findViewById<View>(R.id.edit_post_button).setOnClickListener {
                updatePost()
        }

        findViewById<View>(R.id.back_button).setOnClickListener {
            if (titleEditText.text.isNotEmpty() || contentEditText.text.isNotEmpty()) {
                showWarningDialog()
            } else {
                finish()
            }
        }
    }

    private fun showWarningDialog() {
        AlertDialog.Builder(this)
            .setMessage("작성중인 내용이 있습니다. '확인'을 누르시면 게시물 수정을 취소하고 게시판으로 이동합니다.")
            .setPositiveButton("확인") { _, _ ->
                finish()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updatePost() {
        val title = titleEditText.text.toString()
        val content = contentEditText.text.toString()
        val noticechk = if (noticeCheckbox.isChecked) 1 else 0

        val post = Post(title, content, uid = postUID!!, noticechk = noticechk)
        databaseReference.child(postUID!!).setValue(post).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
