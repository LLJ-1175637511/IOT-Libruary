package com.llj.baselib.net

import com.llj.baselib.IOTLib
import com.llj.baselib.bean.Const
import com.llj.baselib.bean.DeviceOLStatus
import com.llj.baselib.bean.TokenBean

object IOTRepository {

    private val iotSever by lazy { IOTServerCreator.create<IOTServer>() }

    fun requestToken(): TokenBean {
        val tokenParams = IOTLib.buildToken(
            client_id = IOTLib.getUcb().clientId,
            client_secret = IOTLib.getUcb().clientSecret,
            username = IOTLib.getUcb().userId,
            password = IOTLib.getUcb().appKey
        )
        val tokenRequest = iotSever.getShellAccessInfo(tokenParams).execute()
        if (!tokenRequest.isSuccessful) {
            throw Exception(tokenRequest.message())
        }
        val bean = tokenRequest.body()
        return bean ?: throw Exception("AccessRepository_requestToken : data is null")
    }

    fun requestDeviceOL(): DeviceOLStatus {
        val token = IOTLib.getSP(Const.SPNet).getString(Const.SPToken,"") ?: throw NullPointerException("token缓存异常")
        val deviceOLRequest = iotSever.getDeviceOL(token).execute()
        if (!deviceOLRequest.isSuccessful) throw Exception(deviceOLRequest.message())
        val bean = deviceOLRequest.body()
        return bean ?: throw Exception("AccessRepository_requestDeviceOL : data is null")
    }

}