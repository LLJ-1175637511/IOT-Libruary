package com.llj.baselib

import androidx.lifecycle.*
import com.llj.baselib.utils.LogUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun ViewModel.trySuspendExceptFunction(
    threadType: CoroutineDispatcher = Dispatchers.Default,
    block: suspend () -> Unit
){
    try {
        viewModelScope.launch(threadType) {
            block()
        }
    } catch (e: InterruptedException) {
        e.printStackTrace()
        LogUtils.d(IOTLib.TAG, e.message.toString())
    }
}
