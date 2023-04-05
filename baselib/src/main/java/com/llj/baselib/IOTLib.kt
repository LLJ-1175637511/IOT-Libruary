package com.llj.baselib

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.llj.baselib.bean.Const
import com.llj.baselib.bean.UserConfigBean
import java.lang.IllegalArgumentException

@SuppressLint("StaticFieldLeak")
object IOTLib {

    private lateinit var context: Context

    private var ucb: UserConfigBean? = null

    @JvmStatic
    fun init(context: Context, userConfigBean: UserConfigBean?) {
        this.context = context.applicationContext
        this.ucb = userConfigBean
    }

    fun initContext(context: Context) {
        if (!this::context.isInitialized) {
            this.context = context
        }
    }

    fun updateConfig(userConfigBean: UserConfigBean) {
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
        username: String,
        password: String
    ): Map<String, String> = mutableMapOf<String, String>().apply {
        put(Const.GRANT_TYPE, "password")
        put(Const.CLIENT_ID, client_id)
        put(Const.CLIENT_SECRET, client_secret)
        put(Const.USERNAME, username)
        put(Const.PASSWORD, password)
    }

    fun getSP(key: String): SharedPreferences =
        context.getSharedPreferences(key, Context.MODE_PRIVATE)

    fun saveConfigJson(configInfo: String) {
        IOTLib.getSP(Const.SPUser).save {
            putString(Const.USER_IS_BIND_DEV, configInfo)
        }
    }

    fun saveUserInfo(name: String, pwd: String) {
        IOTLib.getSP(Const.SPUser).save {
            putString(Const.SPUserName, name)
            putString(Const.SPUserPwd, pwd)
        }
    }

    fun getConfigJson(): String {
        IOTLib.getSP(Const.SPUser).apply {
            return getString(Const.USER_IS_BIND_DEV, "") ?: ""
        }
    }

    fun getConfigBean(): UserConfigBean {
        val json = getConfigJson()
        return Gson().fromJson(json, UserConfigBean::class.java)
    }

    fun loadUserData(): Pair<String, String> {
        getSP(Const.SPUser).let { sp ->
            var mUserName = ""
            var mPassWord = ""
            if (sp.contains(Const.SPUserName)) {
                mUserName = sp.getString(Const.SPUserName, "").toString()
            }
            if (sp.contains(Const.SPUserPwd)) {
                mPassWord = sp.getString(Const.SPUserPwd, "").toString()
            }
            return Pair(mUserName, mPassWord)
        }
    }

    fun savedToken(token: String) {
        getSP(Const.SPNet).save {
            putString(Const.SPToken, token)
        }
    }
}