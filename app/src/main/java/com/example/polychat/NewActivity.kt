package com.example.polychat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.polychat.databinding.ActivityNewBinding

class NewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // intent에서 사용자 세부정보 가져오기
        val stuName = intent.getStringExtra("stuName")
        val department = intent.getStringExtra("department")
        val stuNum = intent.getStringExtra("stuNum")
        val loginUID = intent.getStringExtra("loginUID")

        binding.stuNameText.text = stuName
        binding.departmentText.text = department
        binding.stuNumText.text = stuNum

        binding.boardButton.setOnClickListener {
            val intent = Intent(this, BoardActivity::class.java)
            intent.putExtra("stuName", stuName)
            intent.putExtra("department", department)
            intent.putExtra("stuNum", stuNum)
            intent.putExtra("loginUID", loginUID)
            startActivity(intent)
        }

        binding.chatButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("stuName", stuName)
            intent.putExtra("department", department)
            intent.putExtra("stuNum", stuNum)
            intent.putExtra("loginUID", loginUID)
            startActivity(intent)
        }
    }
}
