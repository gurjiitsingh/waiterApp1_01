package com.it10x.waiterApp1_01.data.pos.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_sync")
data class InventorySyncEntity(

    @PrimaryKey
    val orderItemId: String,

    val syncedAt: Long
)