package com.llj.baselib.utils

import android.widget.Toast
import com.llj.baselib.IOTLib

object ToastUtils {

    fun toastShort(msg:String){
        Toast.makeText(IOTLib.getC(),msg,Toast.LENGTH_SHORT).show()
    }

    fun toastLong(msg:String){
        Toast.makeText(IOTLib.getC(),msg,Toast.LENGTH_LONG).show()
    }
}