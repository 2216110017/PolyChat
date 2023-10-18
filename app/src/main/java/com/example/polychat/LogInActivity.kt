package com.example.polychat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.example.polychat.databinding.ActivityLogInBinding
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

class LogInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogInBinding
    private lateinit var mDbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var customToken: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )

        mDbRef = Firebase.database.reference
        auth = Firebase.auth

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
            Log.d("LoginActivity", "로그인 버튼 클릭")

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
            lifecycleScope.launch {
                val customToken = fetchCustomTokenFromServer(validUser.uId)
                customToken?.let {
                    signInWithFirebase(it, validUser)
                }
            }
        } else {
            // 로그인 실패 처리
            Toast.makeText(this, "로그인에 실패하였습니다. 이름과 학번이 올바르게 입력되었는지 확인해주세요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithFirebase(customToken: String, validUser: User) {
        auth.signInWithCustomToken(customToken)
            .addOnCompleteListener(this@LogInActivity) { task ->
                if (task.isSuccessful) {
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
                        .addOnSuccessListener {
                            // 데이터 저장 성공
                        }
                        .addOnFailureListener { exception ->
                            // 데이터 저장 실패
                            Log.e("FirebaseError", "uId 저장 실패: ${exception.message}")
                        }

                    Log.d("LoginActivity", "User UID: ${validUser.uId}")

                    val intent = Intent(this@LogInActivity, NewActivity::class.java)
                    intent.putExtra("stuName", validUser.stuName)
                    intent.putExtra("department", validUser.department)
                    intent.putExtra("stuNum", validUser.stuNum)
                    intent.putExtra("uId", validUser.uId)
                    startActivity(intent)
                    finish()
                } else {
                    // 로그인 실패 처리
                    Log.e("LoginActivity", "Firebase auth 에러: ${task.exception?.message}")
                    Toast.makeText(this@LogInActivity, "로그인에 실패하였습니다. 이름과 학번이 올바르게 입력되었는지 확인해주세요", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Retrofit 인터페이스
    interface CustomTokenService {
        @GET("/getCustomToken/{userId}")
        suspend fun getCustomToken(@Path("userId") userId: String): CustomTokenResponse
    }

    data class CustomTokenResponse(val token: String)

    // Retrofit 인스턴스 생성
    val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.75.166:3000/")
//        .baseUrl("https://your-server-url.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val tokenService = retrofit.create(CustomTokenService::class.java)

    private suspend fun fetchCustomTokenFromServer(userId: String): String? {
        return try {
            val response = tokenService.getCustomToken(userId)
            Log.d("LoginActivity", "서버 응답: $response")
            response.token
        } catch (e: Exception) {
            Log.e("LoginActivity", "토큰을 가져오는 중에 오류가 발생했습니다.: ${e.message}")
            null
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
