package com.llj.baselib.net

import com.llj.baselib.IOTLib
import com.llj.baselib.bean.Const
import com.llj.baselib.bean.DeviceOLStatus
import com.llj.baselib.bean.LoginBean
import com.llj.baselib.bean.TokenBean
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface IOTServer {

    @FormUrlEncoded
    @POST("oauth/token")
    fun getShellAccessInfo(
        @FieldMap map: Map<String, String>,
        @Header("Content-Type") Content_Type: String = "application/x-www-form-urlencoded"
    ): Call<TokenBean>

    @GET("oauth/dev")
    fun getDeviceOL(
        @Query(Const.ACCESS_TOKEN) accessToken:String ,
        @Query(Const.ID) id:String = IOTLib.getUcb().deviceId
    ): Call<DeviceOLStatus>
}
