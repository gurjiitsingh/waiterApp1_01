package com.it10x.waiterApp1_01.network



import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WaiterLanApi {

    @GET("api/waiter/ping")
    suspend fun ping(): Response<JsonObject>

    @POST("api/waiter/kot")
    suspend fun sendKot(
        @Body body: JsonObject
    ): Response<JsonObject>
}