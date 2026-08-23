package com.it10x.waiterApp1_01.data.models

import com.it10x.waiterApp1_01.data.model.ClientFirebaseConfig



data class ClientResponse(
    val success: Boolean,
    val data: ClientFirebaseConfig
)