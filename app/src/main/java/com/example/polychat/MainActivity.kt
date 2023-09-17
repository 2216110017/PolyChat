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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "user_info")

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var adapter: UserAdapter

    private lateinit var mDbRef: DatabaseReference
    private lateinit var loggedInUser: User

    private lateinit var userList: ArrayList<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
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
        }


        mDbRef = Firebase.database.reference
        userList = ArrayList()
        adapter = UserAdapter(this, userList)
        binding.userRecycelrView.layoutManager = LinearLayoutManager(this)
        binding.userRecycelrView.adapter = adapter

        mDbRef.child("user").addValueEventListener(object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(postSnapshot in snapshot.children){
                    val currentUser = postSnapshot.getValue(User::class.java)
                    if(loggedInUser.uId != currentUser?.uId){
                        userList.add(currentUser!!)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
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
