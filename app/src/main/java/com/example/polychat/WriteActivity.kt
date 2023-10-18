package com.example.polychat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.launch

class WriteActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var noticeCheckbox: CheckBox
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private var uploadedFileUri: Uri? = null
    private val filePickerActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedFileUri = result.data?.data
            val fileReference = storageReference.child(System.currentTimeMillis().toString())
            val uploadTask = fileReference.putFile(selectedFileUri!!)
            uploadTask.addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { uri ->
                    uploadedFileUri = uri  // uri 값을 uploadedFileUri 변수에 저장
                }
            }.addOnFailureListener {
                Toast.makeText(this, "파일 업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()            // 뒤로가기 시 실행할 코드
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )

        val department = intent.getStringExtra("department") ?: ""
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

            // Post 객체 생성 시, fileUrl 필드에 uploadedFileUri 값을 할당합니다.
            val post = Post(
                title = title,
                content = content,
                uid = uId,
                noticechk = noticechk,
                department = department,
                fileUrl = uploadedFileUri?.toString()
            )
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

        storageReference = FirebaseStorage.getInstance().getReference("uploads")
        findViewById<ImageButton>(R.id.attach_btn).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            filePickerActivityResultLauncher.launch(intent)
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
