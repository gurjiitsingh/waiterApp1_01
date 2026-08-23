package com.it10x.waiterApp1_01.data.mapper

import android.util.Log
import com.it10x.waiterApp1_01.data.pos.entities.PosKotItemEntity
import com.it10x.waiterApp1_01.data.online.models.OrderProductData


object OnlineOrderMapper {

    fun toKotItems(
        orderProducts: List<OrderProductData>,
        kotBatchId: String = "ONLINE",
        tableNo: String? = null
    ): List<PosKotItemEntity> {
        val now = System.currentTimeMillis()


        Log.d("PRINT", "Mapping ${orderProducts.size} products to KOT items...")

        val kotItems = orderProducts.mapIndexed { index, item ->
            Log.d(
                "PRINT",
                "Item[$index]: name='${item.name}', qty=${item.quantity}, price=${item.priceDouble()}, total=${item.finalTotalDouble()}"
            )
}
        return orderProducts.map { item ->
            PosKotItemEntity(
                id = item.id.ifBlank { "ONLINE-${System.nanoTime()}" },
                categoryName = item.categoryName,
                productMode = "",
                currentStock = 0.0,
                sessionId = "",
                kotBatchId = kotBatchId,
                tableNo = tableNo ?: "ONLINE",
               tableName = "ONLINE",
                productId = item.id, // or another product identifier if you have one
                name = item.name,
                categoryId = item.productCat.ifBlank { "ONLINE" },

                parentId = null,
                isVariant = false,

                basePrice = item.priceDouble(),
                finalPrice = 0.0,
                modifierTotal = 0.0,
                quantity = item.quantity,

                taxRate = (item.taxRate as? Number)?.toDouble() ?: 0.0,
                taxType = "exclusive", // assume online prices are exclusive
                kotNumber = "",
                status = "DONE",
                kitchenPrinted = false,

                createdAt = now
            )
        }
    }
}
