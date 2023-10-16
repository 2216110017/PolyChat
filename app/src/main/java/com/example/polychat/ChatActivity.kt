package com.example.polychat

import android.app.Activity
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.example.polychat.databinding.ActivityChatBinding
import com.google.firebase.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


class ChatActivity : AppCompatActivity() {

    private lateinit var receiverName: String
    private lateinit var receiverUid: String
    private lateinit var binding: ActivityChatBinding
    private lateinit var mDbRef: DatabaseReference
    private lateinit var receiverRoom: String
    private lateinit var senderRoom: String
    private lateinit var loggedInUser: User
    private lateinit var messageList: ArrayList<Message>

    private var isUserInitialized = false // loggedInUser가 초기화되었는지 확인하는 변수

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()            // 뒤로가기 시 실행할 코드
        }
    }

    //파일 업로드 로직 불러오기
    private val filePickerActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val fileUri = result.data?.data
            fileUri?.let {
                uploadFileToAWSS3(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 로그인된 사용자 정보 가져오기
        lifecycleScope.launch {
            val stuName = dataStore.data.map { preferences ->
                preferences[stringPreferencesKey("stuName")] ?: ""
            }.first()
            val stuNum = dataStore.data.map { preferences ->
                preferences[stringPreferencesKey("stuNum")] ?: ""
            }.first()
            val department = dataStore.data.map { preferences ->
                preferences[stringPreferencesKey("department")] ?: ""
            }.first()
            val email = dataStore.data.map { preferences ->
                preferences[stringPreferencesKey("email")] ?: ""
            }.first()
            val phone = dataStore.data.map { preferences ->
                preferences[stringPreferencesKey("phone")] ?: ""
            }.first()
            val uId =
                dataStore.data.map { preferences -> preferences[stringPreferencesKey("uId")] ?: "" }
                    .first()

            loggedInUser = User(stuName, stuNum, department, email, phone, uId)
            isUserInitialized = true
        }

        messageList = ArrayList()

        val messageAdapter = MessageAdapter(this, messageList, loggedInUser.uId)

        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = messageAdapter

        receiverName = intent.getStringExtra("name").toString()
        receiverUid = intent.getStringExtra("uId").toString()

        mDbRef = FirebaseDatabase.getInstance().reference

        val senderUid = loggedInUser.uId

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        binding.ChatTextViewTopName.text = receiverName
//        val chatTopNameTextView: TextView = findViewById(R.id.Chat_textView_topName)
//        chatTopNameTextView.text = receiverName


        //메시지 전송 버튼 이벤트
        binding.sendBtn.setOnClickListener {
            if (!isUserInitialized) {
                // loggedInUser가 초기화되지 않았을 경우 사용자에게 알림 표시
                Toast.makeText(this, "사용자 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val message = binding.messageEdit.text.toString().trim() // trim() = 공백 제거

            if (message.isEmpty()) {
                // 메시지가 비어있는 경우 사용자에게 알림을 표시합니다.
                Toast.makeText(this, "메시지를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentTime = SimpleDateFormat("a h:mm", Locale.KOREA).apply {
                timeZone = TimeZone.getTimeZone("Asia/Seoul")
            }.format(System.currentTimeMillis())

            val messageObject = Message(message, loggedInUser.uId, currentTime)

            mDbRef.child("chats").child(senderRoom).child("messages").push()
                .setValue(messageObject).addOnSuccessListener {
                    mDbRef.child("chats").child(receiverRoom).child("messages").push()
                        .setValue(messageObject)
                }
            binding.messageEdit.setText("")
        }

        mDbRef.child("chats").child(senderRoom).child("messages")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()

                    for(postSnapshat in snapshot.children){
                        val message = postSnapshat.getValue(Message::class.java)
                        messageList.add(message!!)
                    }
                    messageAdapter.notifyDataSetChanged()
                    // RecyclerView를 최하단으로 스크롤
                    binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
                }


                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })

        // 첨부 버튼 클릭 리스너 설정
        binding.attachBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            filePickerActivityResultLauncher.launch(intent)
        }

        this.onBackPressedDispatcher.addCallback(this,onBackPressedCallback) // 뒤로가기 콜백
    }

    // 파일 업로드 로직
    private fun uploadFileToAWSS3(fileUri: Uri) {
        // AWS 자격 증명 및 S3 클라이언트 초기화
        val credentials = BasicAWSCredentials("AKIAU64OPUVYX5RFXHJ2", "5HYf1qg0uhDoqLNVgEAka7aFBprEFetdrkdkJgRV")
        val s3Client = AmazonS3Client(credentials, Region.getRegion(Regions.AP_NORTHEAST_2))

        // 실제 파일 경로 가져오기
        val realPath = getPathFromUri(applicationContext, fileUri)
        if (realPath == null) {
            Log.e("ChatActivity", "URI에서 실제 경로를 가져오지 못했습니다.: $fileUri")
            return
        }
        val file = File(realPath)

        // TransferUtility 초기화
        val transferUtility = TransferUtility.builder()
            .context(applicationContext)
            .s3Client(s3Client)
            .build()

        val uploadObserver = transferUtility.upload(
            "aws-s3-bucket-2216110017",
            fileUri.lastPathSegment,
            file
        )

        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    // 업로드 완료 후 파일 URL 가져오기
                    val fileUrl = s3Client.getResourceUrl("aws-s3-bucket-2216110017", fileUri.lastPathSegment)

                    // 파일 URL을 채팅 메시지로 보내기
                    val currentTime = SimpleDateFormat("a h:mm", Locale.KOREA).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Seoul")
                    }.format(System.currentTimeMillis())
                    val messageObject = Message("", loggedInUser.uId, currentTime, fileUrl)

                    mDbRef.child("chats").child(senderRoom).child("messages").push()
                        .setValue(messageObject).addOnSuccessListener {
                            mDbRef.child("chats").child(receiverRoom).child("messages").push()
                                .setValue(messageObject)
                        }
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                // 업로드 진행률 업데이트 (선택 사항)
            }

            override fun onError(id: Int, ex: Exception) {
                // 업로드 중 오류 발생
                Log.e("GroupChat_S3_업로드_에러", "업로드 중 에러: $id", ex)
            }
        })
    }

    private fun getPathFromUri(context: Context, uri: Uri): String? {
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(uri, proj, null, null, null)
            val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()
            return cursor?.getString(columnIndex!!)
        } finally {
            cursor?.close()
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

            val intent = Intent(this@ChatActivity, LogInActivity::class.java)
            startActivity(intent)
            finish()
            return true
        }
        return true
    }
}