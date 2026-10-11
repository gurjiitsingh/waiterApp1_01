
package com.it10x.waiterApp1_01.data.pos.repository

import android.util.Log
import com.it10x.waiterApp1_01.data.pos.dao.KotItemDao
import com.it10x.waiterApp1_01.data.pos.dao.PosTerminalDao
import com.it10x.waiterApp1_01.data.pos.entities.PosKotItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class WaiterPosUpdateReceiver(
    private val posTerminalDao: PosTerminalDao,
    private val kotItemDao: KotItemDao
) {

    companion object {
        private const val TAG = "WEBSOCKET"
        private const val WS_PORT = 2346

        // Marker used to distinguish Electron items from waiter-created KOT items.
        private const val PC_POS_SOURCE = "ELECTRON_PC"
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val receiverScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val snapshotMutex = Mutex()
    @Volatile
    private var reconnectScheduled = false
    @Volatile
    private var webSocket: WebSocket? = null

    suspend fun connect() {
        if (webSocket != null) {
            Log.d(TAG, "WebSocket connection already started")
            return
        }

        val terminal = posTerminalDao.getActiveTerminal()

        if (terminal == null) {
            Log.e(TAG, "No active POS terminal found in Room")
            return
        }

        val ipAddress = terminal.ipAddress

        if (ipAddress.isBlank()) {
            Log.e(TAG, "Main POS IP address is empty")
            return
        }

        val url = "ws://$ipAddress:$WS_PORT"

        Log.d(TAG, "Main POS IP: $ipAddress")
        Log.d(TAG, "Connecting to Electron POS WebSocket: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {

                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response
                ) {
                    Log.d(TAG, "CONNECTED to main Electron POS")
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String
                ) {
                    try {
                        val json = JSONObject(text)
                        val type = json.optString("type")

                        if (type != "BILL_ITEMS_SNAPSHOT") {
                            Log.d(TAG, "Ignoring message type: $type")
                            return
                        }

                        val items = json.optJSONArray("items")

                        Log.d(TAG, "========== POS SNAPSHOT START ==========")
                        Log.d(TAG, "Message type: $type")
                        Log.d(TAG, "Snapshot timestamp: ${json.opt("timestamp")}")
                        Log.d(TAG, "Bill item count: ${items?.length() ?: 0}")

                        if (items != null) {
                            for (i in 0 until items.length()) {
                                val item = items.optJSONObject(i) ?: continue

                                Log.d(TAG, "---------- ITEM ${i + 1} START ----------")

                                val keys = item.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    Log.d(TAG, "ITEM[$i] $key = ${item.opt(key)}")
                                }

                                Log.d(TAG, "---------- ITEM ${i + 1} END ----------")
                            }
                        }

                        Log.d(TAG, "========== POS SNAPSHOT END ==========")

                        if (items == null) {
                            Log.e(TAG, "Snapshot has no items array")
                            return
                        }

                        // Process snapshots in order so they cannot overlap.
                        receiverScope.launch {
                            snapshotMutex.withLock {
                                try {
                                    var insertedOrUpdated = 0
                                    var deleted = 0

                                    val snapshotEntities = mutableListOf<PosKotItemEntity>()

                                    for (i in 0 until items.length()) {
                                        val item = items.optJSONObject(i)
                                            ?: continue

                                        val id = item.optString("id")

                                        if (id.isBlank()) {
                                            Log.w(
                                                TAG,
                                                "Skipping item $i: missing id"
                                            )
                                            continue
                                        }

                                        val status = item.optString("status")

                                        // Remove the previously synchronized
                                        // item when Electron marks it deleted.
                                        if (status == "DELETED") {
                                            kotItemDao.deleteItemById(id)
                                            deleted++

                                            Log.d(
                                                TAG,
                                                "Removed deleted PC item: $id"
                                            )
                                            continue
                                        }

                                        val entity = PosKotItemEntity(
                                            id = id,

                                            // Dummy values for KOT-specific
                                            // fields not supplied by Electron.
                                            kotNumber = "",
                                            kotBatchId = "",
                                            productMode = "",
                                            currentStock = 0.0,
                                            createdById = "",
                                            createdByName = "",

                                            sessionId = item.optString("sessionId")
                                                .takeUnless { it.isBlank() },

                                            tableNo = item.optString("tableNo")
                                                .takeUnless { it.isBlank() },

                                            tableName = item.optString("tableName")
                                                .takeUnless { it.isBlank() },

                                            productId = item.optString("productId"),
                                            name = item.optString("name"),
                                            categoryId = item.optString("categoryId"),
                                            categoryName = item.optString("categoryName"),

                                            parentId = item.opt("parentId")
                                                ?.takeUnless {
                                                    it == JSONObject.NULL
                                                }
                                                ?.toString(),

                                            isVariant = item.optInt("isVariant") == 1,
                                            discountEligible =
                                                item.optInt("discountEligible") == 1,

                                            basePrice = item.optDouble("basePrice", 0.0),
                                            finalPrice = item.optDouble("finalPrice", 0.0),
                                            modifierTotal =
                                                item.optDouble("modifierTotal", 0.0),

                                            quantity = item.optInt("quantity", 0),
                                            taxRate = item.optDouble("taxRate", 0.0),
                                            taxType = item.optString("taxType"),

                                            // Do not treat PC items as new,
                                            // printable waiter KOT requests.
                                            status = "DONE",
                                            kitchenPrintReq = false,
                                            kitchenPrinted = false,

                                            note = item.optString("note"),
                                            modifiersJson =
                                                item.optString("modifiersJson", "[]"),

                                            createdAt = item.optLong("createdAt", 0L),

                                            // Dedicated marker lets us identify
                                            // PC items without changing waiter KOTs.
                                            source = PC_POS_SOURCE,

                                            syncedToCloud = false,
                                            syncedFromCloud = true
                                        )

                                        snapshotEntities.add(entity)
                                        Log.d(
                                            TAG,
                                            "PC item saved: table=${entity.tableName}, " +
                                                    "item=${entity.name}, " +
                                                    "qty=${entity.quantity}, id=${entity.id}"
                                        )
                                    }

// ADD THE REPLACEMENT CODE HERE — OUTSIDE THE SECOND LOOP
                                    kotItemDao.replaceAllKotItems(snapshotEntities)

                                    insertedOrUpdated = snapshotEntities.size

                                    Log.d(
                                        TAG,
                                        "Full snapshot replaced: saved=$insertedOrUpdated, deleted/excluded=$deleted"
                                    )

                                    Log.d(
                                        TAG,
                                        "Room now contains only the latest Electron snapshot items"
                                    )

                                    Log.d(
                                        TAG,
                                        "Snapshot processed: saved/updated=" +
                                                "$insertedOrUpdated, deleted=$deleted"
                                    )
                                } catch (e: Exception) {
                                    Log.e(
                                        TAG,
                                        "Failed to save PC snapshot to Room",
                                        e
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse POS snapshot", e)
                    }
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    Log.w(TAG, "WebSocket closed: $code, $reason")

                    this@WaiterPosUpdateReceiver.webSocket = null

                    scheduleReconnect("Connection closed")
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {
                    Log.e(
                        TAG,
                        "WebSocket connection failed; HTTP=${response?.code}",
                        t
                    )

                    this@WaiterPosUpdateReceiver.webSocket = null

                    scheduleReconnect("Connection failed")
                }
            }
        )
    }

    private fun scheduleReconnect(reason: String) {
        synchronized(this@WaiterPosUpdateReceiver) {
            if (reconnectScheduled) {
                return
            }
            reconnectScheduled = true
        }

        receiverScope.launch {
            try {
                while (webSocket == null) {
                    delay(3000)

                    if (webSocket == null) {
                        Log.w(
                            TAG,
                            "Reconnecting to Electron POS. Reason: $reason"
                        )

                        connect()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "WebSocket reconnection error", e)
            } finally {
                synchronized(this@WaiterPosUpdateReceiver) {
                    reconnectScheduled = false
                }
            }
        }
    }
    fun disconnect() {
        webSocket?.close(1000, "Receiver stopped")
        webSocket = null

        receiverScope.cancel()
        client.dispatcher.cancelAll()

        Log.d(TAG, "Receiver disconnected")
    }
}
