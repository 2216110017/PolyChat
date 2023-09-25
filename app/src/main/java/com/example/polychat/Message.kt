package com.example.polychat

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val currentFormattedTime = SimpleDateFormat("a h:mm", Locale.KOREA).apply {
    timeZone = TimeZone.getTimeZone("Asia/Seoul")
}.format(Date())

val currentFullDate = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).apply {
    timeZone = TimeZone.getTimeZone("Asia/Seoul")
}.format(Date())

data class Message(
    var message: String?,
    var sendId: String?,
    var sentTime: String = currentFormattedTime,
    var fullDate: String = currentFullDate
){
    constructor():this("", "", "", "")
}