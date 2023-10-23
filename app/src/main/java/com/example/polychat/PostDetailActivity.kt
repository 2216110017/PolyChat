package com.example.polychat

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

class PostDetailActivity : AppCompatActivity() {

    private lateinit var titleLabel: TextView
    private lateinit var contentLabel: TextView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private lateinit var backButton: Button
    private lateinit var databaseReference: DatabaseReference
    private lateinit var filePreview: ImageView
    private lateinit var downloadButton: ImageButton
    private var postUID: String? = null
    private var userUID: String? = null
    private var noticechk: Int = 0  // 기본값은 0으로 설정
    private var fileUrl: String? = null



    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()            // 뒤로가기 시 실행할 코드
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )

        titleLabel = findViewById(R.id.title_label)
        contentLabel = findViewById(R.id.content_label)
        editButton = findViewById(R.id.edit_button)
        deleteButton = findViewById(R.id.delete_button)
        backButton = findViewById(R.id.back_button)
        filePreview = findViewById(R.id.file_preview)
        downloadButton = findViewById(R.id.download_button)

        databaseReference = FirebaseDatabase.getInstance().getReference("post")

        postUID = intent.getStringExtra("postUid")
        Log.d("PostDetailActivity", "BoardActivity에서 받아온 postUID: $postUID") //로그확인용
        userUID = intent.getStringExtra("uId")
        Log.d("PostDetailActivity", "BoardActivity에서 받아온 loginUID: $userUID") //로그확인용

        fetchPostDetails()

        filePreview.setOnClickListener {
            fileUrl?.let { url ->
                val intent = Intent(this@PostDetailActivity, ZoomedImageActivity::class.java)
                intent.putExtra("IMAGE_URL", url)
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        editButton.setOnClickListener {
            val intent = Intent(this@PostDetailActivity, PostEditActivity::class.java)
            // 게시글의 UID, 제목, 내용, 그리고 사용자의 UID를 Intent에 넣기
            intent.putExtra("post_uid", postUID)
            intent.putExtra("post_title", titleLabel.text.toString())
            intent.putExtra("post_content", contentLabel.text.toString())
            intent.putExtra("user_uid", userUID)
            intent.putExtra("notice", noticechk == 1)
            intent.putExtra("department", contentLabel.tag.toString()) // 학과 정보 추가
            startActivity(intent)
        }


        deleteButton.setOnClickListener {
            // Firebase에서 게시물 삭제
            databaseReference.child(postUID ?: "").removeValue()
            finish()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchPostDetails() {
        postUID?.let { it ->
            databaseReference.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Firebase에서 반환되는 DataSnapshot 객체의 값을 로그로 출력
                    Log.d("PostDetailActivity", "DataSnapshot 값: ${snapshot.value}")

                    val post = snapshot.getValue(Post::class.java)
                    // Post 객체로의 변환 후 로그 출력
                    Log.d("PostDetailActivity", "변환된 게시물 객체 Converted Post object: $post")

                    post?.let {
                        titleLabel.text = it.title
                        contentLabel.text = it.content
                        contentLabel.tag = it.department // 학과 정보를 tag에 저장
                        noticechk = it.noticechk
                        fileUrl = it.fileUrl
                        Log.d("PostDetailActivity", "불러온 fileUrl: $fileUrl")

                        // 첨부된 파일의 URL을 사용하여 미리보기 및 다운로드 버튼 설정
                        it.fileUrl?.let { fileUrl ->
                            // Glide를 사용하여 이미지 로드
                            Glide.with(this@PostDetailActivity)
                                .load(fileUrl)
                                .into(filePreview)
                            filePreview.visibility = View.VISIBLE

                            downloadButton.visibility = View.VISIBLE
                            downloadButton.setOnClickListener {
                                downloadFile()
                            }
                        } ?: run {
                            downloadButton.visibility = View.GONE
                        }

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
                    Log.e("PostDetailActivity", "데이터베이스 읽기 실패: ${error.message}")
                    // 사용자에게 오류 알림 표시
                    Toast.makeText(this@PostDetailActivity, "데이터를 불러오는 데 실패했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                }
            })
        }
        this.onBackPressedDispatcher.addCallback(this,onBackPressedCallback) // 뒤로가기 콜백
    }

    private fun downloadFile() {
        postUID?.let { postUid ->
            databaseReference.child(postUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val post = snapshot.getValue(Post::class.java)
                    val fileUrl = post?.fileUrl
                    if (fileUrl != null) {
                        downloadFileFromFirebaseStorage(fileUrl)
                    } else {
                        Toast.makeText(this@PostDetailActivity, "파일 URL을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PostDetailActivity, "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: run {
            Toast.makeText(this, "게시글 UID가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadFileFromFirebaseStorage(fileUrl: String) {
        Log.d("PostDetailActivity", "다운로드할 파일 URL: $fileUrl")
        val storageRef = Firebase.storage.getReferenceFromUrl(fileUrl)
        val localFile = File.createTempFile("temp", "file")

        storageRef.getFile(localFile).addOnSuccessListener {
            // 파일이 성공적으로 다운로드되었습니다.
            Toast.makeText(this@PostDetailActivity, "파일이 다운로드되었습니다.", Toast.LENGTH_SHORT).show()

            try {
                // 파일을 사용자의 다운로드 폴더로 이동
                val mimeType = URLConnection.guessContentTypeFromName(localFile.name) ?: "application/octet-stream"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, localFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.SIZE, localFile.length())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                }
                val contentResolver = applicationContext.contentResolver
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                } else {
                    // Android 9 (Pie) 이하에서는 MediaStore를 사용하지 않고 직접 파일을 다운로드 폴더에 저장
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val destinationFile = File(downloadsDir, localFile.name)
                    localFile.copyTo(destinationFile, true)
                    Uri.fromFile(destinationFile)
                }
                contentResolver.openOutputStream(uri!!).use { outputStream ->
                    FileInputStream(localFile).use { inputStream ->
                        inputStream.copyTo(outputStream!!)
                    }
                }

                // 파일 열기
                val openFileIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(openFileIntent)
            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity, "파일을 저장하는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                Log.e("PostDetailActivity", "파일 저장 실패: ${e.message}")
            }
        }.addOnFailureListener {
            // 파일 다운로드에 실패했습니다.
            Toast.makeText(this@PostDetailActivity, "파일을 다운로드하는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
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
