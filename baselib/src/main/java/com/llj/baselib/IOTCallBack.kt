package com.llj.baselib

interface IOTCallBack {

    fun webState(state: IOTViewModel.WebSocketType)

    fun realData(data: Any?)

    fun online()

    fun offline()

}