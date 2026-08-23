package com.it10x.waiterApp1_01.data.pos.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(
    tableName = "pos_terminal"
)
data class PosTerminalEntity(

    @PrimaryKey
    val terminalId: String,

    val terminalName: String = "",

    val outletId: String = "",

    val ownerId: String = "",

    val ipAddress: String = "",

    val port: Int = 8787,

    val deviceType: String = "POS",

    val isActive: Boolean = true,

    val lastSeenAt: Long = 0L,

    val updatedAt: Long = 0L
)