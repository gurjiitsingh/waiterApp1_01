package com.it10x.waiterApp1_01.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WaiterLanClient {

    /*
     * Desktop POS LAN IP
     *
     * Your current desktop:
     * 192.168.31.113
     *
     * Port:
     * 2345
     */
    private const val BASE_URL =
        "http://192.168.31.113:2345/"

    private val retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()

    val api: WaiterLanApi =
        retrofit.create(
            WaiterLanApi::class.java
        )
}