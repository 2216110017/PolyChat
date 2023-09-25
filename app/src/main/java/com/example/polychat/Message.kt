package com.example.polychat

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val currentFormattedTime = SimpleDateFormat("a h:mm", Locale.KOREA).apply {
    timeZone = TimeZone.getTimeZone("Asia/Seoul")
}.format(Date())

data class Message(
    var message: String?,
    var sendId: String?,
    var sentTime: String = currentFormattedTime
){
    constructor():this("", "", "")
}