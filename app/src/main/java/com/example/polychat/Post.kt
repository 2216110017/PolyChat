package com.example.polychat

data class Post(
    var title: String? = null,
    var content: String? = null,
    var uid: String? = null,
    var noticechk: Int = 0,
    var department: String? = null,
    var postID: String? = null, // firebase 게시글 고유값
    var fileUrl: String? = null
)