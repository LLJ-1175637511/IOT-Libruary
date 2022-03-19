package com.llj.baselib.bean

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class ReceiveDeviceBean(
    val ID: String,
    val M: String,
    val V: JsonObject
)

