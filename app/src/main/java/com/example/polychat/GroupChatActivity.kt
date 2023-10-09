package com.example.polychat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.example.polychat.databinding.ActivityGroupChatBinding
import com.google.firebase.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.google.firebase.storage.FirebaseStorage
import java.util.Date


class GroupChatActivity : AppCompatActivity() {

    private lateinit var receiverName: String
    private lateinit var receiverUid: String
    private lateinit var binding: ActivityGroupChatBinding
    private lateinit var mDbRef: DatabaseReference
    private lateinit var receiverRoom: String
    private lateinit var groupReceiverRoom: String
    private lateinit var senderRoom: String
    private lateinit var groupSenderRoom: String
    private lateinit var loggedInUser: User
    private lateinit var messageList: ArrayList<Message>
    private var isUserInitialized = false // loggedInUser가 초기화되었는지 확인하는 변수
    private val REQUEST_CODE_FILE_PICKER = 1001

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()            // 뒤로가기 시 실행할 코드
        }
    }

    private val filePickerActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val fileUri = result.data?.data
            fileUri?.let {
                uploadFileToFirebaseStorage(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
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
            groupSenderRoom = loggedInUser.department
        }

        messageList = ArrayList()

        val messageAdapter = MessageAdapter(this, messageList, loggedInUser.uId)
        // Firebase 사용자 정보 노드 참조
        val userRef = FirebaseDatabase.getInstance().reference.child("user")

        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = messageAdapter

        receiverName = intent.getStringExtra("name").toString()
        receiverUid = intent.getStringExtra("uId").toString()

        mDbRef = FirebaseDatabase.getInstance().reference

        val senderUid = loggedInUser.uId

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        supportActionBar?.title = receiverName

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
            // 학과 이름을 groupSenderRoom 저장
            groupSenderRoom = loggedInUser.department

            mDbRef.child("chats").child(groupSenderRoom).child("messages").push()
                .setValue(messageObject)

            binding.messageEdit.setText("")
        }

        mDbRef.child("chats").child(groupSenderRoom).child("messages")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()

                    for(postSnapshat in snapshot.children){
                        val message = postSnapshat.getValue(Message::class.java)
                        // 사용자 이름 가져오기
                        userRef.child(message?.sendId ?: "").addListenerForSingleValueEvent(object: ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val userName = userSnapshot.child("stuName").value.toString()
                                message?.userName = userName
                                messageList.add(message!!)
                                messageAdapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(userError: DatabaseError) {
                                // Handle error
                            }
                        })
                    }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FILE_PICKER && resultCode == Activity.RESULT_OK) {
            val fileUri = data?.data
            fileUri?.let {
                uploadFileToFirebaseStorage(it)
            }
        }
    }

    private fun uploadFileToFirebaseStorage(fileUri: Uri) {
        val currentDate = SimpleDateFormat("yyyyMMdd").format(Date())
        val storagePath = "/$currentDate/$groupSenderRoom/${loggedInUser.uId}/${fileUri.lastPathSegment}"
        val storageRef = FirebaseStorage.getInstance().getReference(storagePath)

        storageRef.putFile(fileUri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val fileUrl = uri.toString()

                // 파일 URL을 채팅 메시지로 보내기
                val currentTime = SimpleDateFormat("a h:mm", Locale.KOREA).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Seoul")
                }.format(System.currentTimeMillis())
                val messageObject = Message("", loggedInUser.uId, currentTime, fileUrl)

                mDbRef.child("chats").child(groupSenderRoom).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        mDbRef.child("chats").child(groupReceiverRoom).child("messages").push()
                            .setValue(messageObject)
                    }
            }
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

            val intent = Intent(this@GroupChatActivity, LogInActivity::class.java)
            startActivity(intent)
            finish()
            return true
        }
        return true
    }
}