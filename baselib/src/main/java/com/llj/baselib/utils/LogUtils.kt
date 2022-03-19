package com.llj.baselib.utils

import android.util.Log

object LogUtils {
    var FLAG = true

    fun d(tag:String,msg:String){
        if (FLAG) Log.d(tag, msg)
    }

    fun closeLog(){
        FLAG = false
    }

    fun openLog(){
        FLAG = false
    }
}