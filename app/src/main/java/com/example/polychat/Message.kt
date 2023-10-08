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
    var fullDate: String = currentFullDate,
    var imageUrl: String? = null,
    var fileName: String? = null,
    var fileUrl: String? = null,  // 추가된 필드
    var fileType: String? = null  // 'image', 'video', 'audio', 'text' 등의 값이 될 수 있습니다.
){
    constructor():this("", "", "", "",null,null, null, null)
}
