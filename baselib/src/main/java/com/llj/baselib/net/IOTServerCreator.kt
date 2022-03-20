package com.llj.baselib.net

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object IOTServerCreator {

    private const val BASE_URL = "https://www.bigiot.net/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> create(server: Class<T>): T = retrofit.create(server)

    inline fun <reified T> create(): T = create(T::class.java)
}