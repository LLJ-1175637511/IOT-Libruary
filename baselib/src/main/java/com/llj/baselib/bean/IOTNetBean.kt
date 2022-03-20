package com.llj.baselib.bean

data class TokenBean(
    val access_token: String, // 1d1910d2334f6146a3b04de1e293f98a73a63b9b
    val expires_in: Int, // 172800
    val refresh_token: String, // 0e5ccd8c628dcc7a50c1738903e0afdf20bf912a
    val scope: Any, // null
    val token_type: String // Bearer
)

data class DeviceOLStatus(
    val description: String,
    val encrypt: String,
    val id: String,
    val image: String,
    val lat: String,
    val lng: String,
    val online: String,
    val online_time: String,
    val `open`: String,
    val open_listen: String,
    val title: String
)

data class LoginBean(val code:Int,val data:String)