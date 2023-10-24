package com.example.polychat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.annotation.GlideModule
import com.example.polychat.databinding.ActivityProfileBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

@GlideModule
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var selectedImageUri: Uri? = null
    private var currentProfileImageUrl: String? = null
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            binding.profileImageView.setImageURI(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uId = intent.getStringExtra("uId")
        if (uId == null) {
            Toast.makeText(this, "오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 기존 프로필 정보 불러오기
        loadProfile(uId)

        binding.profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        binding.saveProfileButton.setOnClickListener {
            saveProfile(uId)
        }
    }

    private fun loadProfile(uId: String) {
        firebaseDatabase.reference.child("user").child(uId).child("profile").get().addOnSuccessListener { snapshot ->
            val profile = snapshot.getValue(Profile::class.java)
            binding.profileDescriptionEditText.setText(profile?.text)
            if (profile?.url != null) {
                Glide.with(this@ProfileActivity)
                    .load(profile.url)
                    .into(binding.profileImageView)
            } else {
                binding.profileImageView.setImageResource(R.drawable.default_profile)
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this@ProfileActivity, "프로필 정보 불러오기 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfile(uId: String) {
        val profileText = binding.profileDescriptionEditText.text.toString()

        if (selectedImageUri != null) {
            val storageRef = firebaseStorage.reference.child("profile/$uId/picture")
            storageRef.putFile(selectedImageUri!!).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    // Profile 객체 생성 및 저장
                    val profile = Profile(profileText, imageUrl)
                    firebaseDatabase.reference.child("user").child(uId).child("profile").setValue(profile)
                        .addOnSuccessListener {
                            // 저장 성공 후 이미지 뷰 업데이트
                            Glide.with(this@ProfileActivity)
                                .load(imageUrl)
                                .into(binding.profileImageView)
                            Toast.makeText(this@ProfileActivity, "프로필 저장 성공", Toast.LENGTH_SHORT).show()
                            finish() // 이미지 로드가 완료된 후에 액티비티 종료
                        }
                }.addOnFailureListener { exception ->
                    Toast.makeText(this@ProfileActivity, "프로필 사진 URL 가져오기 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                    finish() // 오류 발생 시에도 액티비티 종료
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this@ProfileActivity, "프로필 사진 업로드 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish() // 오류 발생 시에도 액티비티 종료
            }
        } else {
            // 이미지가 선택되지 않았을 경우, 기존 이미지 URL 사용
            val profile = Profile(profileText, currentProfileImageUrl)
            firebaseDatabase.reference.child("user").child(uId).child("profile").setValue(profile)
                .addOnSuccessListener {
                    // 저장 성공 후 이미지 뷰 업데이트
                    if (currentProfileImageUrl != null) {
                        Glide.with(this@ProfileActivity)
                            .load(currentProfileImageUrl)
                            .into(binding.profileImageView)
                    } else {
                        binding.profileImageView.setImageResource(R.drawable.default_profile)
                    }
                    Toast.makeText(this@ProfileActivity, "프로필 저장 성공", Toast.LENGTH_SHORT).show()
                    finish() // 이미지 로드가 완료된 후에 액티비티 종료
                }
        }
    }



}
