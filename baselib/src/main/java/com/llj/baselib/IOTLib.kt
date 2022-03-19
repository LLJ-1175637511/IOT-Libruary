package com.llj.baselib

import android.annotation.SuppressLint
import android.content.Context
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

}