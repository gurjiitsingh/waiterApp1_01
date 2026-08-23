package com.it10x.waiterApp1_01.utils

import com.it10x.waiterApp1_01.data.online.models.OrderMasterData

fun OrderMasterData.createdAtMillis(): Long {
    return createdAt?.toDate()?.time ?: createdAtMillis
}