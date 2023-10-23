package com.example.polychat

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

class ZoomedImageActivity : AppCompatActivity() {

    private lateinit var zoomedImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zoomed_image)

        zoomedImageView = findViewById(R.id.zoomed_image_view)

        val fileUrl = intent.getStringExtra("FILE_URL")
        if (fileUrl != null) {
            Glide.with(this)
                .load(fileUrl)
                .into(zoomedImageView)
        }
    }
}