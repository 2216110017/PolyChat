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

        val imageUrl = intent.getStringExtra("IMAGE_URL")
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(zoomedImageView)
        }
    }
}
//
//private lateinit var zoomedImageView: SubsamplingScaleImageView
//
//override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//    setContentView(R.layout.activity_zoomed_image)
//
//    zoomedImageView = findViewById(R.id.zoomed_image_view)
//
//    val imageUrl = intent.getStringExtra("IMAGE_URL")
//    if (imageUrl != null) {
//        zoomedImageView.setImage(ImageSource.uri(imageUrl))
//    }
//}