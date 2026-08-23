package com.it10x.waiterApp1_01.data.model.inventory

enum class InventoryUnit {
    pcs,
    kg,
    gm,
    ltr,
    ml
}

data class ProductRecipe(
    val id: String = "",

    val productId: String = "",

    val productName: String = "",

    val inventoryItemId: String = "",

    val inventoryItemName: String = "",

    val quantity: Double = 0.0,

    val wastagePercent: Double = 0.0,

    val unit: String = "",

    val createdAt: Long = 0L
)