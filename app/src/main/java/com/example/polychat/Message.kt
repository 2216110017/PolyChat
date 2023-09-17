package com.example.polychat

data class Message(
    var message: String?,
    var sendId: String?
){
    constructor():this("","")
}