package com.it10x.waiterApp1_01.data.pos.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_recipes")
data class ProductRecipeEntity(

    @PrimaryKey
    val id: String,

    val productId: String,

    val productName: String,

    val inventoryItemId: String,

    val inventoryItemName: String,

    val quantity: Double,

    val wastagePercent: Double,

    val unit: String,

    val createdAt: Long
)