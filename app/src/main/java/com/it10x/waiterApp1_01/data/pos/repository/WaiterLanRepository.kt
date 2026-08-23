package com.it10x.waiterApp1_01.data.pos.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.it10x.waiterApp1_01.data.pos.entities.PosCartEntity
import com.it10x.waiterApp1_01.network.WaiterLanClient
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WaiterLanRepository {

    private val gson = Gson()

    // =====================================================
    // PING DESKTOP
    // =====================================================

    suspend fun pingDesktop(): Boolean {
        return try {
            Log.d("WAITER_LAN", "Pinging desktop...")

            val response = WaiterLanClient.api.ping()

            if (response.isSuccessful) {
                Log.d(
                    "WAITER_LAN",
                    "Desktop ping successful: ${response.body()}"
                )
                true
            } else {
                Log.e(
                    "WAITER_LAN",
                    "Ping failed: HTTP ${response.code()}"
                )
                false
            }

        } catch (e: Exception) {
            Log.e(
                "WAITER_LAN",
                "Desktop ping exception",
                e
            )
            false
        }
    }

    // =====================================================
    // SEND KOT
    // =====================================================

    suspend fun sendOrderToDesktop(
        cartList: List<PosCartEntity>,
        tableNo: String,
        tableName: String,
        sessionId: String,
        orderType: String,
        deviceId: String,
        deviceName: String?,
    ): Boolean {

        return try {

            if (cartList.isEmpty()) {
                Log.e("WAITER_LAN", "Cart is empty")
                return false
            }

            val now = System.currentTimeMillis()

            val businessDate =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Date(now))

            val kotBatchId =
                UUID.randomUUID().toString()

            // =================================================
            // BATCH
            // =================================================

            val batch =
                JsonObject().apply {

                    addProperty(
                        "id",
                        kotBatchId
                    )

                    // Electron generates the actual KOT number
                    addProperty(
                        "kotNumber",
                        ""
                    )

                    addProperty(
                        "sessionId",
                        sessionId
                    )

                    addProperty(
                        "tableNo",
                        tableNo
                    )

                    addProperty(
                        "tableName",
                        tableName
                    )

                    addProperty(
                        "orderType",
                        orderType
                    )

                    addProperty(
                        "businessDate",
                        businessDate
                    )

                    addProperty(
                        "deviceId",
                        deviceId
                    )

                    addProperty(
                        "deviceName",
                        deviceName ?: "Waiter"
                    )

                    addProperty(
                        "appVersion",
                        "1.0"
                    )

                    addProperty(
                        "createdAt",
                        now
                    )

                    addProperty(
                        "sentBy",
                        "WAITER"
                    )

                    addProperty(
                        "syncStatus",
                        "PENDING"
                    )

                    add(
                        "lastSyncedAt",
                        com.google.gson.JsonNull.INSTANCE
                    )
                }

            // =================================================
            // ITEMS
            // =================================================

            val items =
                JsonArray()

            cartList.forEach { cartItem ->

                val item =
                    JsonObject().apply {

                        addProperty(
                            "id",
                            UUID.randomUUID().toString()
                        )

                        addProperty(
                            "kotNumber",
                            ""
                        )

                        addProperty(
                            "categoryName",
                            cartItem.categoryName ?: ""
                        )

                        // Required by Electron SQLite
                        addProperty(
                            "productMode",
                            cartItem.productMode ?: "raw_stock"
                        )

                        addProperty(
                            "currentStock",
                            cartItem.currentStock ?: 0
                        )

                        addProperty(
                            "sessionId",
                            cartItem.sessionId ?: sessionId
                        )

                        addProperty(
                            "kotBatchId",
                            kotBatchId
                        )

                        addProperty(
                            "tableNo",
                            tableNo
                        )

                        addProperty(
                            "tableName",
                            tableName
                        )

                        addProperty(
                            "productId",
                            cartItem.productId
                        )

                        addProperty(
                            "name",
                            cartItem.name ?: ""
                        )

                        addProperty(
                            "categoryId",
                            cartItem.categoryId ?: ""
                        )

                        addProperty(
                            "createdById",
                            cartItem.createdById ?: ""
                        )

                        addProperty(
                            "createdByName",
                            cartItem.createdByName ?: ""
                        )

                        addProperty(
                            "parentId",
                            cartItem.parentId
                        )

                        addProperty(
                            "isVariant",
                            cartItem.isVariant
                        )

                        addProperty(
                            "basePrice",
                            cartItem.basePrice
                        )

                        addProperty(
                            "finalPrice",
                            cartItem.finalPrice
                        )

                        addProperty(
                            "modifierTotal",
                            cartItem.modifierTotal
                        )

                        addProperty(
                            "quantity",
                            cartItem.quantity
                        )

                        addProperty(
                            "taxRate",
                            cartItem.taxRate
                        )

                        addProperty(
                            "taxType",
                            cartItem.taxType ?: "exclusive"
                        )

                        addProperty(
                            "status",
                            "PENDING"
                        )

                        addProperty(
                            "note",
                            cartItem.note ?: ""
                        )

                        addProperty(
                            "modifiersJson",
                            cartItem.modifiersJson ?: ""
                        )

                        addProperty(
                            "kitchenPrintReq",
                            cartItem.kitchenPrintReq
                        )

                        addProperty(
                            "kitchenPrinted",
                            false
                        )

                        addProperty(
                            "createdAt",
                            now
                        )

                        addProperty(
                            "source",
                            "WAITER"
                        )

                        addProperty(
                            "syncedToCloud",
                            false
                        )

                        addProperty(
                            "syncedFromCloud",
                            false
                        )
                    }

                items.add(item)
            }

            // =================================================
            // FINAL REQUEST
            // =================================================

            val request =
                JsonObject().apply {

                    add(
                        "batch",
                        batch
                    )

                    add(
                        "items",
                        items
                    )
                }

            Log.d(
                "WAITER_LAN",
                "===================================="
            )

            Log.d(
                "WAITER_LAN",
                "Sending waiter KOT to desktop"
            )

            Log.d(
                "WAITER_LAN",
                "Table: $tableNo"
            )

            Log.d(
                "WAITER_LAN",
                "Items: ${cartList.size}"
            )

            Log.d(
                "WAITER_LAN",
                "REQUEST: ${gson.toJson(request)}"
            )

            Log.d(
                "WAITER_LAN",
                "===================================="
            )

            // =================================================
            // SEND
            // =================================================

            val response =
                WaiterLanClient.api.sendKot(
                    request
                )

            // =================================================
            // RESPONSE
            // =================================================

            if (response.isSuccessful) {

                Log.d(
                    "WAITER_LAN",
                    "KOT accepted by desktop"
                )

                Log.d(
                    "WAITER_LAN",
                    "Response: ${response.body()}"
                )

                true

            } else {

                val error =
                    response.errorBody()
                        ?.string()

                Log.e(
                    "WAITER_LAN",
                    "KOT rejected"
                )

                Log.e(
                    "WAITER_LAN",
                    "HTTP: ${response.code()}"
                )

                Log.e(
                    "WAITER_LAN",
                    "Error: $error"
                )

                false
            }

        } catch (e: Exception) {

            Log.e(
                "WAITER_LAN",
                "Failed to send KOT",
                e
            )

            false
        }
    }
}