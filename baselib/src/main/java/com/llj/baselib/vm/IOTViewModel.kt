package com.llj.baselib.vm

import androidx.lifecycle.*
import com.llj.baselib.IOTCallBack
import com.llj.baselib.IOTLib
import com.llj.baselib.trySuspendExceptFunction
import com.llj.baselib.utils.LogUtils
import com.llj.baselib.utils.ToastUtils
import kotlinx.coroutines.Dispatchers
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

abstract class IOTViewModel: ViewModel() {

    private val userNameFlag = """"ID":"U${IOTLib.getUcb().userId}""""
    private val deviceIdFlag = """"ID":"D${IOTLib.getUcb().deviceId}""""
    private val connectSucFlag = """{"M":"WELCOME TO BIGIOT"}"""
    private val loginFlag = """"M":"login""""
    private val loginSucFlag = """"M":"loginok""""
    private val receiveDataSucFlag = """"M":"update""""
    private val logoutFlag = """"M":"logout""""

    private val _receiveDeviceDataLiveData = MutableLiveData<String>()
    val receiveDevData: LiveData<String> = _receiveDeviceDataLiveData

    private val _canSendOrder = MutableLiveData<Boolean>(false)
    val canSendOrder: LiveData<Boolean> = _canSendOrder

    private val _webState = MutableLiveData<WebSocketType>(WebSocketType.CONNECT_INIT)
    val webState: LiveData<WebSocketType> = _webState

    private var callback: IOTCallBack?= null

    fun connect(callBack : IOTCallBack){
        this.callback = callback
    }

    private val webSocket by lazy {
        val webUri = URI.create(BIGIOT)
        object : WebSocketClient(webUri, Draft_6455()) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                LogUtils.d(IOTLib.TAG, "onOpen()")
            }

            override fun onMessage(message: String) {
                //message就是接收到的消息
                if (message.contains(loginFlag) && message.contains(deviceIdFlag)) {
                    _webState.postValue(WebSocketType.DEVICE_ONLINE)
                    callback?.webState(WebSocketType.DEVICE_ONLINE)
                }else if (message.contains(loginSucFlag)&&message.contains(userNameFlag)) {
                    _webState.postValue(WebSocketType.USER_LOGIN)
                    callback?.webState(WebSocketType.USER_LOGIN)
                } else if (message.contains(receiveDataSucFlag)) {
                    _webState.postValue(WebSocketType.DEVICE_ONLINE)
                    callback?.webState(WebSocketType.DEVICE_ONLINE)
                    _receiveDeviceDataLiveData.postValue(message)
//                    notifyAnalysisJson(message)
                } else if (message.contains(logoutFlag)) {
                    if (message.contains(userNameFlag)){
                        _webState.postValue(WebSocketType.USER_LOGOUT)
                        callback?.webState(WebSocketType.USER_LOGOUT)
                    } else{
                        _webState.postValue(WebSocketType.DEVICE_OFFLINE)
                        callback?.webState(WebSocketType.DEVICE_OFFLINE)
                    }
                } else if (message.contains(connectSucFlag)) {
                    _webState.postValue(WebSocketType.CONNECT_BIGIOT)
                    callback?.webState(WebSocketType.CONNECT_BIGIOT)
                } else {
                }
                LogUtils.d(IOTLib.TAG, message)
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) {
                _webState.postValue(WebSocketType.NOT_CONNECT_BIGIOT)
            }

            override fun onError(ex: Exception?) {
                LogUtils.d(IOTLib.TAG, "onError() : ${ex?.message}")
            }
        }
    }

    fun setToast(str: String) {
        ToastUtils.toastShort(str)
    }

    @Synchronized
    private fun connectBigIot() = trySuspendExceptFunction(Dispatchers.IO) {
        LogUtils.d(IOTLib.TAG, "connectBigIot()")
        webSocket.connect()
    }

    @Synchronized
    private fun loginBigIot() {
        LogUtils.d(IOTLib.TAG, "loginBigIot()")
        sendMessage("""{"M":"login","ID":"${IOTLib.getUcb().userId}","K":"${IOTLib.getUcb().appKey}"}""")
    }

    fun sendOrderToDevice(content: String) {
        sendMessage("""{"M":"say","ID":"D${IOTLib.getUcb().deviceId}","C":"$content","SIGN":""}""")
    }

    private fun sendMessage(str: String) {
        if (webSocket.isOpen) {
            LogUtils.d(IOTLib.TAG, "sendMessage:${str}")
            webSocket.send(str)
        } else {
            LogUtils.d(IOTLib.TAG, "sendMessage:当前webSocket已关闭")
            setToast("连接已关闭")
        }
    }

    enum class WebSocketType {
        CONNECT_INIT, CONNECT_BIGIOT, NOT_CONNECT_BIGIOT, USER_LOGIN, USER_LOGOUT ,DEVICE_ONLINE ,DEVICE_OFFLINE
    }

    override fun onCleared() {
        super.onCleared()
        if (webSocket.isOpen) webSocket.close()
    }

    companion object {
        private const val BIGIOT = "wss://www.bigiot.net:8484"
    }
}