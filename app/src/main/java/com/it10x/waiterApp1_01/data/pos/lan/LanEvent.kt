package com.it10x.waiterApp1_01.data.pos.lan



data class LanEvent(
    val type: String,
    val tableId: String,
    val sessionId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)