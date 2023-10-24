package com.example.polychat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.polychat.databinding.ActivityMainBinding
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "user_info")

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mDbRef: DatabaseReference
    private lateinit var loggedInUser: User
    private lateinit var userList: ArrayList<User>
    lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )

        mDbRef = Firebase.database.reference
        userList = ArrayList()
        adapter = UserAdapter(this, userList)
        binding.userRecycelrView.layoutManager = LinearLayoutManager(this)
        binding.userRecycelrView.adapter = adapter
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

            fetchUsersByDepartment(loggedInUser.department)
        }

        binding.groupChatButton.setOnClickListener {
            val intent = Intent(this, GroupChatActivity::class.java)
            intent.putExtra("department", loggedInUser.department)  // 학과 정보 전달
            startActivity(intent)
        }
    }

    private fun fetchUsersByDepartment(department: String) {
        mDbRef.child("user").orderByChild("department").equalTo(department)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.clear() // 기존 목록을 지우고 새로운 데이터로 업데이트
                    for (postSnapshot in snapshot.children) {
                        val currentUser = postSnapshot.getValue(User::class.java)
                        if (loggedInUser.uId != currentUser?.uId) {
                            userList.add(currentUser!!)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // 에러 처리
                }
            })
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

            val intent = Intent(this@MainActivity, LogInActivity::class.java)
            startActivity(intent)
            finish()
            return true
        }
        return true
    }
}
