package com.llj.baselib

import androidx.lifecycle.*
import com.google.gson.Gson
import com.llj.baselib.bean.ReceiveDeviceBean
import com.llj.baselib.utils.LogUtils
import com.llj.baselib.utils.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.net.URI

abstract class IOTViewModel : ViewModel() {

    private val userNameFlag = """"ID":"U${IOTLib.getUcb().userId}""""
    private val deviceIdFlag = """"ID":"D${IOTLib.getUcb().deviceId}""""
    private val connectSucFlag = """{"M":"WELCOME TO BIGIOT"}"""
    private val deviceLoginSucFlag = """"M":"login",${deviceIdFlag}"""
    private val loginSucFlag = """"M":"loginok",${userNameFlag}"""
    private val receiveDataSucFlag = """"M":"update",${deviceIdFlag}"""
    private val receiveUserDataSucFlag = """"M":"say",${userNameFlag}"""
    private val logoutFlag = """"M":"logout""""

    private var mCanSendOrder = false

    private var mWebState = WebSocketType.CONNECT_INIT

    private var mCallback: IOTCallBack? = null

    private var mIsReConnectAfterCancel = true

    private var mBeanClass:Class<*> ?= null

    fun connect(callBack: IOTCallBack,jClass: Class<*>,isReconnectAfterCancel:Boolean = true) {
        mCallback = callBack
        mBeanClass = jClass
        mIsReConnectAfterCancel = isReconnectAfterCancel
        checkUserAndDeviceStatus()
    }

    private val webSocket by lazy {
        val webUri = URI.create(BIGIOT)
        object : WebSocketClient(webUri, Draft_6455()) {

            override fun onOpen(handshakedata: ServerHandshake?) {
                LogUtils.d(IOTLib.TAG, "onOpen()")
            }

            override fun onMessage(message: String) {
                when {
                    message.contains(deviceLoginSucFlag) -> {
                        mWebState = WebSocketType.DEVICE_ONLINE
                        mCallback?.webState(WebSocketType.DEVICE_ONLINE)
                    }
                    message.contains(loginSucFlag) -> {
                        mWebState = WebSocketType.USER_LOGIN
                        mCallback?.webState(WebSocketType.USER_LOGIN)
                    }
                    message.contains(receiveDataSucFlag) -> {
                        mWebState = WebSocketType.DEVICE_ONLINE
                        mCallback?.webState(WebSocketType.DEVICE_ONLINE)
                        mCallback?.realData(notifyAnalysisJson(message))
                    }
                    message.contains(logoutFlag) -> {
                        if (message.contains(userNameFlag)) {
                            mWebState = WebSocketType.USER_LOGOUT
                            mCallback?.webState(WebSocketType.USER_LOGOUT)
                        } else {
                            mWebState = WebSocketType.DEVICE_OFFLINE
                            mCallback?.webState(WebSocketType.DEVICE_OFFLINE)
                        }
                    }
                    message.contains(connectSucFlag) -> {
                        mWebState = WebSocketType.CONNECT_BIGIOT
                        mCallback?.webState(WebSocketType.CONNECT_BIGIOT)
                    }
                    message.contains(receiveUserDataSucFlag) -> {
                        LogUtils.d(IOTLib.TAG, "receive web data:")
                    }
                }

                LogUtils.d(IOTLib.TAG, message)
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) {
                LogUtils.d(IOTLib.TAG, "onClose()")
            }

            override fun onError(ex: Exception?) {
                LogUtils.d(IOTLib.TAG, "onError() : ${ex?.message}")
            }
        }
    }

    private fun checkUserAndDeviceStatus() {
        var getFirstStatus = false
        var operasTime = System.currentTimeMillis()
        val retryTime = 1000 * 2
        var isConnecting = false
        var isUserLogining = false
        viewModelScope.launch(Dispatchers.IO) {
            while (mIsReConnectAfterCancel) {
                delay(100)
                val cTime = System.currentTimeMillis()
                when (mWebState) {
                    WebSocketType.CONNECT_INIT -> {
                        mCanSendOrder = false
                        if (!isConnecting) {
                            connectBigIot()
                            isConnecting = true
                        }
                        if (cTime - operasTime > retryTime) { //每s检测一次
                            isConnecting = false
                            operasTime = cTime
                        }
                    }
                    WebSocketType.NOT_CONNECT_BIGIOT -> {
                        mCanSendOrder = false
                        if (!isConnecting) {
                            reConnectBigIot()
                            isConnecting = true
                        }
                        if (cTime - operasTime > retryTime) { //每s检测一次
                            isConnecting = false
                            operasTime = cTime
                        }
                    }
                    WebSocketType.CONNECT_BIGIOT -> {
                        mCanSendOrder = false
                        if (!isUserLogining) {
                            loginBigIot()
                            isUserLogining = true
                        }
                        if (cTime - operasTime > retryTime) { //每s检测一次
                            isUserLogining = false
                            operasTime = cTime
                        }
                    }
                    WebSocketType.USER_LOGOUT -> {
                        mCanSendOrder = false
                        if (!isUserLogining) {
                            loginBigIot()
                            isUserLogining = true
                        }
                        if (cTime - operasTime > retryTime) { //每s检测一次
                            isUserLogining = false
                            operasTime = cTime
                        }
                    }
                    WebSocketType.USER_LOGIN -> {
                        mCanSendOrder = false
                        if (!getFirstStatus) {
                            getFirstStatus = true
//                            getDeviceFirstStatus()
                        }
                    }
                    WebSocketType.DEVICE_OFFLINE -> {
                        mCanSendOrder = false
                    }
                    WebSocketType.DEVICE_ONLINE -> {
                        if (mCanSendOrder) {
                            mCanSendOrder = true
                            setToast("设备已连接")
                        }
                    }
                }
            }
        }
    }

    fun notifyAnalysisJson(jsonStr: String):Any? {
        kotlin.runCatching {
            val gson = Gson()
            val receiveDeviceBean = gson.fromJson(jsonStr, ReceiveDeviceBean::class.java)
                ?: return null
            val jClass = mBeanClass ?: throw IllegalArgumentException("未初始化bean类型")

            val jsonObject = receiveDeviceBean.V

            val mainDataBean = jClass.newInstance()

            jClass.declaredFields.forEach { param ->
                param.isAccessible = true
                val iotInterfaceId =
                    param.getAnnotation(IOTInterfaceId::class.java) ?: throw IllegalArgumentException(
                        "bean 类中成员变量缺少 @IOTInterfaceId 注解"
                    )

                val interfaceId = if (iotInterfaceId.value.isEmpty()) throw IllegalArgumentException(
                    "@IOTInterfaceId 注解中未赋值"
                ) else iotInterfaceId.value

                if (param.type.name == "int") {
                    param[mainDataBean] = jsonObject[interfaceId].asInt
                } else {
                    param[mainDataBean] = jsonObject[interfaceId].asFloat
                }
            }
            LogUtils.d(IOTLib.TAG, "beanData : $mainDataBean")
            return mainDataBean
        }.onFailure {
            LogUtils.d(IOTLib.TAG, "notifyAnalysisJson_Error: ${it.message}")
        }
        return null
    }


    fun setToast(str: String) {
        ToastUtils.toastShort(str)
    }

    fun closeConnect() {
        mIsReConnectAfterCancel = false
        webSocket.close()
        mWebState = WebSocketType.CONNECT_INIT
    }

    fun sendOrderToDevice(content: String) {
        sendMessage("""{"M":"say",${deviceIdFlag},"C":"$content","SIGN":""}""")
    }

    @Synchronized
    private fun reConnectBigIot() = trySuspendExceptFunction(Dispatchers.IO) {
        if (webSocket.isClosed) {
            webSocket.reconnect()
            LogUtils.d(IOTLib.TAG, "reconnect")
        }
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
        CONNECT_INIT, CONNECT_BIGIOT, NOT_CONNECT_BIGIOT, USER_LOGIN, USER_LOGOUT, DEVICE_ONLINE, DEVICE_OFFLINE
    }

    override fun onCleared() {
        super.onCleared()
        if (webSocket.isOpen) closeConnect()
    }

    companion object {
        private const val BIGIOT = "wss://www.bigiot.net:8484"
    }
}