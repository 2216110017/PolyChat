package com.example.polychat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.example.polychat.databinding.ActivityLogInBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

class LogInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogInBinding
    private lateinit var mDbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mDbRef = Firebase.database.reference

        // 로그인 버튼 이벤트
        binding.loginBtn.setOnClickListener {

            val stuName = binding.stuNameEdit.text.toString()
            val stuNum = binding.stuNumEdit.text.toString()

            if (binding.checkBoxRememberLogin.isChecked) {
                lifecycleScope.launch {
                    dataStore.edit { preferences ->
                        preferences[stringPreferencesKey("savedStuName")] = stuName
                        preferences[stringPreferencesKey("savedStuNum")] = stuNum
                    }
                }
            }

            loginUser(stuName, stuNum)
        }

        lifecycleScope.launch {
            val savedStuName = dataStore.data.map { preferences ->
                preferences[stringPreferencesKey("savedStuName")] ?: ""
            }.first()
            val savedStuNum = dataStore.data.map { preferences ->
                preferences[stringPreferencesKey("savedStuNum")] ?: ""
            }.first()

            binding.stuNameEdit.setText(savedStuName)
            binding.stuNumEdit.setText(savedStuNum)
        }

    }

    private fun loginUser(stuName: String, stuNum: String) {
        val validUser = checkUser(stuName, stuNum)
        if (validUser != null) {
            // 로그인 성공 시 사용자 정보 저장
            lifecycleScope.launch {
                dataStore.edit { preferences ->
                    preferences[stringPreferencesKey("stuName")] = validUser.stuName
                    preferences[stringPreferencesKey("stuNum")] = validUser.stuNum
                    preferences[stringPreferencesKey("department")] = validUser.department
                    preferences[stringPreferencesKey("email")] = validUser.email
                    preferences[stringPreferencesKey("phone")] = validUser.phone
                    preferences[stringPreferencesKey("uId")] = validUser.uId
                }
            }

            // Firebase에 사용자 정보 저장
            mDbRef.child("user").child(validUser.uId).setValue(validUser)

            val intent = Intent(this@LogInActivity, NewActivity::class.java)
            intent.putExtra("stuName", validUser.stuName)
            intent.putExtra("department", validUser.department)
            intent.putExtra("stuNum", validUser.stuNum)
            intent.putExtra("loginUID", validUser.uId)
            startActivity(intent)
            finish()
        } else {
            // 로그인 실패 처리
            Toast.makeText(this, "로그인에 실패하였습니다. 이름과 학번이 올바르게 입력되었는지 확인해주세요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUser(stuName: String, stuNum: String): User? {
        try {
            val inputStream = assets.open("LoginDB.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            val jsonString = String(buffer, Charsets.UTF_8)
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val userObject = jsonArray.getJSONObject(i)
                if (userObject.getString("stuName") == stuName && userObject.getString("stuNum") == stuNum) {
                    return User(
                        stuName = userObject.getString("stuName"),
                        stuNum = userObject.getString("stuNum"),
                        department = userObject.getString("department"),
                        email = userObject.getString("email"),
                        phone = userObject.getString("phone"),
                        uId = userObject.getString("userID")
                    )
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }
}
