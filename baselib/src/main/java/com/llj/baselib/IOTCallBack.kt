package com.llj.baselib

import com.llj.baselib.vm.IOTViewModel

interface IOTCallBack {

    fun webState(state: IOTViewModel.WebSocketType)

}