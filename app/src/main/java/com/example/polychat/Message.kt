package com.example.polychat

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val currentFormattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
data class Message(
    var message: String?,
    var sendId: String?,
    var sentTime: String = currentFormattedTime
){
    constructor():this("", "", "")
}