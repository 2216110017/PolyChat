package com.example.polychat

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class WriteActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var noticeCheckbox: CheckBox
    private lateinit var databaseReference: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

//        val stuName = intent.getStringExtra("stuName")
//        val department = intent.getStringExtra("department")
//        val stuNum = intent.getStringExtra("stuNum")
        val uid = intent.getStringExtra("loginUID") ?: ""

        titleEditText = findViewById(R.id.title_edittext)
        contentEditText = findViewById(R.id.content_edittext)
        noticeCheckbox = findViewById(R.id.notice_checkbox)
        databaseReference = FirebaseDatabase.getInstance().getReference("post")

        findViewById<View>(R.id.write_post_button).setOnClickListener {
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()
            val noticechk = if (noticeCheckbox.isChecked) 1 else 0

            val post = Post(title, content, uid = uid, noticechk = noticechk)
            databaseReference.child(uid).setValue(post)
            finish()
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
            .setMessage("작성중인 내용이 있습니다. '확인'을 누르시면 게시물 작성을 취소하고 게시판으로 이동합니다.")
            .setPositiveButton("확인") { _, _ ->
                finish()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
