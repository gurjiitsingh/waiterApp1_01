package com.it10x.waiterApp1_01.network

import com.it10x.waiterApp1_01.data.models.ClientResponse

object ApiClient {

    suspend fun getClientConfig(clientId: String): ClientResponse {
        return RetrofitInstance.api.getClientConfig(clientId)
    }
}