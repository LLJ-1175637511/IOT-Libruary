package com.llj.baselib

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.llj.baselib.bean.Const
import com.llj.baselib.bean.UserConfigBean
import java.lang.IllegalArgumentException

@SuppressLint("StaticFieldLeak")
object IOTLib {

    private lateinit var context:Context

    private var ucb:UserConfigBean ?= null

    private var isInit = false

    @JvmStatic
    fun init(context:Context,userConfigBean: UserConfigBean){
        if (isInit){
            throw IllegalArgumentException("请勿多次初始化")
        }
        this.context = context.applicationContext
        this.ucb = userConfigBean
    }

    fun getC() = context

    fun getUcb(): UserConfigBean {
        return ucb ?: throw IllegalArgumentException("未初始化用户配置")
    }

    const val TAG = "IOTLib"

    fun buildToken(
        client_id: String,
        client_secret: String,
        username:String,
        password:String
    ):Map<String,String> = mutableMapOf<String,String>().apply {
        put(Const.GRANT_TYPE,"password")
        put(Const.CLIENT_ID,client_id)
        put(Const.CLIENT_SECRET,client_secret)
        put(Const.USERNAME,username)
        put(Const.PASSWORD,password)
    }

    fun getSP(key: String): SharedPreferences =
        context.getSharedPreferences(key, Context.MODE_PRIVATE)

}