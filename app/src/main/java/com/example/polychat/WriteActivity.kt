package com.example.polychat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WriteActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var noticeCheckbox: CheckBox
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var attachedFilesRecyclerView: RecyclerView
    private lateinit var attachedFilesAdapter: AttachedFileAdapter
    private val attachedFiles = ArrayList<Uri>()
    private var uploadedFileUri: Uri? = null


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

        attachedFilesAdapter = AttachedFileAdapter(attachedFiles) { uri ->
            val position = attachedFiles.indexOf(uri)
            if (position != -1) {
                attachedFiles.removeAt(position)
                attachedFilesAdapter.notifyItemRemoved(position)
            }
        }
        attachedFilesRecyclerView = findViewById(R.id.attachedFilesRecyclerView)
        attachedFilesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        attachedFilesRecyclerView.adapter = attachedFilesAdapter

        val department = intent.getStringExtra("department") ?: ""
        val uId = intent.getStringExtra("uId")
        Log.d("WriteActivity", "BoardActivity에서 받은 uId값 : $uId")

        titleEditText = findViewById(R.id.title_edittext)
        contentEditText = findViewById(R.id.content_edittext)
        noticeCheckbox = findViewById(R.id.notice_checkbox)
        databaseReference = FirebaseDatabase.getInstance().getReference("post")
        storageReference = FirebaseStorage.getInstance().reference

        findViewById<ImageButton>(R.id.attach_btn).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "*/*"
            filePickerActivityResultLauncher.launch(intent)
        }

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

        this.onBackPressedDispatcher.addCallback(this,onBackPressedCallback) // 뒤로가기 콜백
    }

    private val filePickerActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedFileUri = result.data?.data
            findViewById<ImageView>(R.id.image_preview).apply {
                visibility = View.VISIBLE
                setImageURI(selectedFileUri)
            }
            uploadImageToFirebaseStorage(selectedFileUri)
        }
    }

    private fun uploadImageToFirebaseStorage(fileUri: Uri?) {
        if (fileUri != null) {
            val contentResolver = applicationContext.contentResolver
            val mimeType = contentResolver.getType(fileUri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val date = dateFormat.format(Date())
            val department = intent.getStringExtra("department") ?: "unknown"
            val filePath = "$date/$department/${System.currentTimeMillis()}.$extension"


            val fileReference = storageReference.child(filePath)
            fileReference.putFile(fileUri).continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                return@Continuation fileReference.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    uploadedFileUri = task.result
                    Toast.makeText(this, "파일 업로드 성공!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "파일 업로드 실패.", Toast.LENGTH_SHORT).show()
                }
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
