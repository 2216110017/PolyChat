package com.example.polychat

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.annotation.GlideModule
import com.example.polychat.databinding.ActivityNewBinding
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import kotlinx.coroutines.launch

@GlideModule
class NewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewBinding
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()            // 뒤로가기 시 실행할 코드
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uId = intent.getStringExtra("uId")
        if (uId != null) {
            loadProfile(uId)
        } else {
            // uId가 없는 경우, 오류 처리
        }


        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )

        // intent에서 사용자 세부정보 가져오기
        val stuName = intent.getStringExtra("stuName")
        val department = intent.getStringExtra("department")
        val stuNum = intent.getStringExtra("stuNum")
//        val uId = intent.getStringExtra("uId")

        binding.stuNameText.text = stuName
        binding.departmentText.text = department
        binding.stuNumText.text = stuNum


        binding.profileSettingsButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            // 필요한 데이터를 intent에 담아서 전달
            intent.putExtra("stuName", stuName)
            intent.putExtra("stuNum", stuNum)
            intent.putExtra("department", department)
            intent.putExtra("uId", uId)
            startActivity(intent)
        }

        binding.boardButton.setOnClickListener {
            val intent = Intent(this, BoardActivity::class.java)
            intent.putExtra("stuName", stuName)
            intent.putExtra("department", department)
            intent.putExtra("stuNum", stuNum)
            intent.putExtra("uId", uId)
            startActivity(intent)
        }

        binding.chatButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("stuName", stuName)
            intent.putExtra("department", department)
            intent.putExtra("stuNum", stuNum)
            intent.putExtra("uId", uId)
            startActivity(intent)
        }

        this.onBackPressedDispatcher.addCallback(this,onBackPressedCallback) // 뒤로가기 콜백
    }

    private fun loadProfile(uId: String) {
        firebaseDatabase.reference.child("user").child(uId).child("profile").get().addOnSuccessListener { snapshot ->
            val profile = snapshot.getValue(Profile::class.java)
            if (profile?.url != null) {
                Glide.with(this@NewActivity)
                    .load(profile.url)
                    .into(binding.profileImage)
            } else {
                // 프로필 사진이 없는 경우, 기본 이미지 설정
                binding.profileImage.setImageResource(R.drawable.default_profile)
            }
        }.addOnFailureListener { exception ->
            // 프로필 정보 불러오기 실패 시, 오류 처리
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

            val intent = Intent(this@NewActivity, LogInActivity::class.java)
            startActivity(intent)
            finish()
            return true
        }
        return true
    }
}
