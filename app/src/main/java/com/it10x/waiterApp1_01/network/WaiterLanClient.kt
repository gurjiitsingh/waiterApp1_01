package com.it10x.waiterApp1_01.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object WaiterLanClient {

    // =====================================================
    // CREATE API FOR POS TERMINAL
    // =====================================================

    fun createApi(
        ipAddress: String,
        port: Int
    ): WaiterLanApi {

        val baseUrl = "http://$ipAddress:$port/"

        val retrofit =
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()

        return retrofit.create(
            WaiterLanApi::class.java
        )
    }
}