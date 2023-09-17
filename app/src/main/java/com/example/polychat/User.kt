package com.example.polychat

data class User(
    var stuName: String,
    var stuNum: String,
    var department: String,
    var email: String,
    var phone: String,
    var uId: String  // 필요없으면 삭제할 것
){
    constructor(): this("", "", "", "", "", "")
}
