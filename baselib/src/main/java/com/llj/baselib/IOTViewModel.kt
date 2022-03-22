package com.llj.baselib

import androidx.lifecycle.*
import com.google.gson.Gson
import com.llj.baselib.bean.ReceiveDeviceBean
import com.llj.baselib.net.IOTRepository
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
    private val receiveDataSucFlag = """"M":"update","ID":"${IOTLib.getUcb().deviceId}""""
    private val receiveUserDataSucFlag = """"M":"say",${userNameFlag}"""
    private val logoutFlag = """"M":"logout""""
    private val pingFlag = """"M":"ping""""

    private var mWebState = WebSocketType.CONNECT_INIT

    private var mCallback: IOTCallBack? = null

    private var mBeanClass: Class<*>? = null

    private val webSocket by lazy {
        val webUri = URI.create(BIGIOT)
        object : WebSocketClient(webUri, Draft_6455()) {

            override fun onOpen(handshakedata: ServerHandshake?) {
                LogUtils.d(IOTLib.TAG, "onOpen()")
            }

            override fun onMessage(message: String) {
                processMessage(message)
                LogUtils.d(IOTLib.TAG, message)
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) {
                LogUtils.d(IOTLib.TAG, "onClose()")
                mWebState = WebSocketType.CONNECT_INIT
            }

            override fun onError(ex: Exception?) {
                LogUtils.d(IOTLib.TAG, "onError() : ${ex?.message}")
            }
        }
    }

    private fun online() {
        mCallback?.onDevLine()
    }

    private fun offline() {
        mCallback?.offDevLine()
    }

    private fun changeState(state: WebSocketType, func: (() -> Unit)? = null) {
        mWebState = state
        mCallback?.webState(state)
        func?.invoke()
    }

    private fun processMessage(message:String){
        viewModelScope.launch(Dispatchers.Main) {
            when {
                message.contains(deviceLoginSucFlag) -> {
                    changeState(WebSocketType.DEVICE_ONLINE){
                        online()
                    }
                }
                message.contains(loginSucFlag) -> {
                    changeState(WebSocketType.USER_LOGIN)
                }
                message.contains(receiveDataSucFlag) -> {
                    changeState(WebSocketType.DEVICE_ONLINE){
                        online()
                        mCallback?.realData(notifyAnalysisJson(message))
                    }
                }
                message.contains(logoutFlag) -> {
                    if (message.contains(userNameFlag)) {
                        changeState(WebSocketType.USER_LOGOUT)
                    } else if (message.contains(deviceIdFlag)) {
                        changeState(WebSocketType.DEVICE_OFFLINE){
                            offline()
                        }
                    }
                }
                message.contains(connectSucFlag) -> {
                    changeState(WebSocketType.CONNECT_BIGIOT)
                }
                message.contains(receiveUserDataSucFlag) -> {
                    LogUtils.d(IOTLib.TAG, "receive web data:")
                }
                message.contains(pingFlag) -> {
                    sendOrderToDevice("ping")
                }
            }
        }
    }

    private fun checkUserAndDeviceStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val state = mWebState
                when (state) {
                    WebSocketType.CONNECT_INIT -> {
                        timerJob(state) {
                            connectBigIot()
                        }
                    }
                    WebSocketType.CONNECT_BIGIOT -> {
                        loginBigIot()
                    }
                    WebSocketType.USER_LOGIN -> {
                        timerJob(state) {
                            getDeviceFirstStatus()
                        }
                        LogUtils.d(IOTLib.TAG, "user_login : 可发消息")
                    }
                    WebSocketType.USER_LOGOUT -> {
                        loginBigIot()
                    }
                    else -> {
                    }// 设备上线 或 离线 仅需监听即可
                }
                delay(200)
            }
        }
    }

    private suspend fun timerJob(state: WebSocketType, func: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var startJob = true
            while (startJob) {
                if (mWebState == state) {
                    func.invoke()
                } else {
                    startJob = false
                }
                delay(1000)
            }
        }
    }

    /*
    获取设备在线状态
     */
    private fun getDeviceFirstStatus() = trySuspendExceptFunction(Dispatchers.IO) {
        LogUtils.d(IOTLib.TAG, "getDeviceFirstStatus")
        val deviceOL = IOTRepository.requestDeviceOL()
        LogUtils.d(IOTLib.TAG, "deviceOL:${deviceOL.toString()}")
        if (deviceOL.online == "1") { //在线
            mWebState = WebSocketType.DEVICE_ONLINE
            mCallback?.webState(WebSocketType.DEVICE_ONLINE)
        }
    }

    private fun notifyAnalysisJson(jsonStr: String): Any? {
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
                    param.getAnnotation(IOTInterfaceId::class.java)
                        ?: throw IllegalArgumentException(
                            "bean 类中成员变量缺少 @IOTInterfaceId 注解"
                        )

                val interfaceId =
                    if (iotInterfaceId.value.isEmpty()) throw IllegalArgumentException(
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

    fun connect(callBack: IOTCallBack, jClass: Class<*>) {
        mCallback = callBack
        mBeanClass = jClass
        checkUserAndDeviceStatus()
    }

    fun setToast(str: String) {
        ToastUtils.toastShort(str)
    }

    fun sendOrderToDevice(content: String) {
        if (mWebState == WebSocketType.DEVICE_ONLINE){
            sendMessage("""{"M":"say",${deviceIdFlag},"C":"$content","SIGN":""}""")
        }else{
            viewModelScope.launch(Dispatchers.Main) {
                val toastStr = "指令发送失败：设备未登录"
                setToast(toastStr)
                LogUtils.d(IOTLib.TAG, toastStr)
            }
        }
    }

    private fun connectBigIot() {
        webSocket.connect()
        LogUtils.d(IOTLib.TAG, "connectBigIot()")
    }

    private fun loginBigIot() {
        sendMessage("""{"M":"login","ID":"${IOTLib.getUcb().userId}","K":"${IOTLib.getUcb().appKey}"}""")
        LogUtils.d(IOTLib.TAG, "loginBigIot()")
    }

    private fun sendMessage(str: String) {
        LogUtils.d(IOTLib.TAG, "sendMessage:${str}")
        if (webSocket.isOpen) {
            webSocket.send(str)
        } else {
            LogUtils.d(IOTLib.TAG, "消息发送失败：连接已关闭")
        }
    }

    enum class WebSocketType {
        CONNECT_INIT, CONNECT_BIGIOT, USER_LOGIN, USER_LOGOUT, DEVICE_ONLINE, DEVICE_OFFLINE
    }

    override fun onCleared() {
        super.onCleared()
        if (webSocket.isOpen) webSocket.close()
    }

    companion object {
        private const val BIGIOT = "wss://www.bigiot.net:8484"
    }
}