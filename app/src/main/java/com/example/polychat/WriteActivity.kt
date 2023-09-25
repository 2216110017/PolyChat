package com.example.polychat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class WriteActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var noticeCheckbox: CheckBox
    private lateinit var databaseReference: DatabaseReference

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()            // 뒤로가기 시 실행할 코드
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

//        val stuName = intent.getStringExtra("stuName")
//        val department = intent.getStringExtra("department")
//        val stuNum = intent.getStringExtra("stuNum")
        val uId = intent.getStringExtra("uId")
        Log.d("WriteActivity", "BoardActivity에서 받은 uId값 : $uId")

        titleEditText = findViewById(R.id.title_edittext)
        contentEditText = findViewById(R.id.content_edittext)
        noticeCheckbox = findViewById(R.id.notice_checkbox)
        databaseReference = FirebaseDatabase.getInstance().getReference("post")

        findViewById<View>(R.id.write_post_button).setOnClickListener {
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()
            val noticechk = if (noticeCheckbox.isChecked) 1 else 0

            if (uId!!.isBlank()) {
                Toast.makeText(this, "UID가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val post = Post(title, content, uid = uId, noticechk = noticechk)
            val postKey = databaseReference.push().key  // 고유한 키 생성
            if (postKey != null) {
                databaseReference.child(postKey).setValue(post)
            }
            finish()
        }

        findViewById<View>(R.id.back_button).setOnClickListener {
            if (titleEditText.text.isNotEmpty() || contentEditText.text.isNotEmpty()) {
                showWarningDialog()
            } else {
                finish()
            }
        }

        this.onBackPressedDispatcher.addCallback(this,onBackPressedCallback) // 뒤로가기 콜백

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

            val intent = Intent(this@WriteActivity, LogInActivity::class.java)
            startActivity(intent)
            finish()
            return true
        }
        return true
    }
}
