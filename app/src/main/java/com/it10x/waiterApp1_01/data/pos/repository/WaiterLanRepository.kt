package com.it10x.waiterApp1_01.data.pos.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.it10x.waiterApp1_01.data.pos.dao.PosTerminalDao
import com.it10x.waiterApp1_01.data.pos.entities.PosCartEntity
import com.it10x.waiterApp1_01.network.WaiterLanClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.content.Context
import android.widget.Toast

class WaiterLanRepository(
    private val context: Context,
    private val posTerminalDao: PosTerminalDao
) {

    private val gson =
        Gson()


    // =====================================================
    // GET POS API
    // =====================================================

    private suspend fun getPosApi() =
        try {
           // logPosTerminal()
            val terminal =
                posTerminalDao.getActiveTerminal()




            if (terminal == null) {

                Log.e(
                    "WAITER_LAN",
                    "No active POS terminal found in Room"
                )

                null

            } else {

//                val ip =
//                    terminal.ipAddress
//
//                val port =
//                    terminal.port
//
//                val url =
//                    "http://$ip:$port/"
//
//                Toast.makeText(
//                    context,
//                    "Sending KOT to:\n$ip:$port",
//                    Toast.LENGTH_LONG
//                ).show()

                if (terminal.ipAddress.isBlank()) {

                    Log.e(
                        "WAITER_LAN",
                        "POS terminal IP address is empty"
                    )

                    null

                } else {

                    Log.d(
                        "WAITER_LAN",
                        "Connecting to POS ${terminal.ipAddress}:${terminal.port}"
                    )

                    WaiterLanClient.createApi(
                        ipAddress =
                            terminal.ipAddress,
                        port =
                            terminal.port
                    )
                }

            }

        } catch (e: Exception) {

            Log.e(
                "WAITER_LAN",
                "Failed to get POS terminal",
                e
            )

            null
        }


    // =====================================================
    // PING DESKTOP
    // =====================================================

    suspend fun pingDesktop(): Boolean {

        return try {

            Log.d(
                "WAITER_LAN",
                "Pinging desktop..."
            )


            val api =
                getPosApi()


            if (api == null) {

                return false

            }


            val response =
                api.ping()


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
     //   logPosTerminal()
        return try {

            if (cartList.isEmpty()) {

                Log.e(
                    "WAITER_LAN",
                    "Cart is empty"
                )

                return false
            }


            // =================================================
            // GET POS
            // =================================================

            val api =
                getPosApi()


            if (api == null) {

                Log.e(
                    "WAITER_LAN",
                    "POS terminal unavailable"
                )

                return false
            }


            // =================================================
            // TIME
            // =================================================

            val now =
                System.currentTimeMillis()


            val businessDate =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(
                    Date(now)
                )


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

                        addProperty(
                            "productMode",
                            cartItem.productMode
                                ?: "raw_stock"
                        )

                        addProperty(
                            "currentStock",
                            cartItem.currentStock ?: 0
                        )

                        addProperty(
                            "sessionId",
                            cartItem.sessionId
                                ?: sessionId
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
                            cartItem.taxType
                                ?: "exclusive"
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


            // =================================================
            // LOG
            // =================================================

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
                api.sendKot(
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


    private suspend fun logPosTerminal() {

        try {

            val terminal =
                posTerminalDao.getActiveTerminal()

            if (terminal == null) {

                Log.e(
                    "WAITER_LAN",
                    "❌ NO ACTIVE POS TERMINAL FOUND IN ROOM"
                )

                return
            }

            Log.d(
                "WAITER_LAN",
                "===================================="
            )

            Log.d(
                "WAITER_LAN",
                "POS TERMINAL FROM ROOM"
            )

            Log.d(
                "WAITER_LAN",
                "Terminal ID: ${terminal.terminalId}"
            )

            Log.d(
                "WAITER_LAN",
                "Terminal Name: ${terminal.terminalName}"
            )

            Log.d(
                "WAITER_LAN",
                "Outlet ID: ${terminal.outletId}"
            )

            Log.d(
                "WAITER_LAN",
                "IP ADDRESS: ${terminal.ipAddress}"
            )

            Log.d(
                "WAITER_LAN",
                "PORT: ${terminal.port}"
            )

            Log.d(
                "WAITER_LAN",
                "ACTIVE: ${terminal.isActive}"
            )

            Log.d(
                "WAITER_LAN",
                "URL: http://${terminal.ipAddress}:${terminal.port}/"
            )

            Log.d(
                "WAITER_LAN",
                "===================================="

            )

        } catch (e: Exception) {

            Log.e(
                "WAITER_LAN",
                "❌ Failed to read POS terminal from Room",
                e
            )

        }
    }
}