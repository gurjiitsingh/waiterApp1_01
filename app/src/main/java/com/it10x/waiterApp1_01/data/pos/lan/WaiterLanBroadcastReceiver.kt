package com.it10x.waiterApp1_01.data.pos.lan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.it10x.waiterApp1_01.data.pos.AppDatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaiterLanBroadcastReceiver : BroadcastReceiver() {

    private val gson = Gson()

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        Log.d(
            "WAITER_LAN_TEST",
            "📡 Broadcast received"
        )

        val message =
            intent.getStringExtra("lan_test")

        Log.d(
            "WAITER_LAN_TEST",
            "📦 DATA = $message"
        )
    }


//    override fun onReceive(
//        context: Context,
//        intent: Intent
//    ) {
//
//        Log.d(TAG, "📡 onReceive() CALLED")
//        Log.d(TAG, "📡 Action = ${intent.action}")
//
//        if (intent.action != ACTION_LAN_EVENT) {
//
//            Log.d(
//                TAG,
//                "⛔ Ignored action: ${intent.action}"
//            )
//
//            return
//        }
//
//        val json =
//            intent.getStringExtra(EXTRA_EVENT)
//
//        if (json == null) {
//
//            Log.e(
//                TAG,
//                "❌ No LAN event JSON received"
//            )
//
//            return
//        }
//
//        Log.d(TAG, "====================================")
//        Log.d(TAG, "📡 LAN EVENT RECEIVED")
//        Log.d(TAG, "JSON = $json")
//        Log.d(TAG, "====================================")
//
//        try {
//
//            val event =
//                gson.fromJson(
//                    json,
//                    LanEvent::class.java
//                )
//
//            Log.d(
//                TAG,
//                "EVENT TYPE = ${event.type}"
//            )
//
//            Log.d(
//                TAG,
//                "TABLE ID = ${event.tableId}"
//            )
//
//            Log.d(
//                TAG,
//                "SESSION ID = ${event.sessionId}"
//            )
//
//            when (event.type) {
//
//                "BILL_CLEARED" -> {
//
//                    Log.d(
//                        TAG,
//                        "🧹 BILL_CLEARED RECEIVED"
//                    )
//
//                    clearWaiterTable(
//                        context,
//                        event
//                    )
//                }
//
//                else -> {
//
//                    Log.d(
//                        TAG,
//                        "⚠️ Unknown LAN event: ${event.type}"
//                    )
//                }
//            }
//
//        } catch (e: Exception) {
//
//            Log.e(
//                TAG,
//                "❌ Failed to parse LAN event",
//                e
//            )
//        }
//    }

    private fun clearWaiterTable(
        context: Context,
        event: LanEvent
    ) {

        Log.d(
            TAG,
            "🧹 Starting local table clear..."
        )

        Log.d(
            TAG,
            "Table = ${event.tableId}"
        )

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val db =
                    AppDatabaseProvider.get(
                        context.applicationContext
                    )

                Log.d(
                    TAG,
                    "✅ Database opened"
                )

                val before =
                    db.kotItemDao()
                        .getItemsByTable(
                            event.tableId
                        )

                Log.d(
                    TAG,
                    "📦 Items BEFORE delete = ${before.size}"
                )

                db.kotItemDao()
                    .clearForTable(
                        event.tableId
                    )

                Log.d(
                    TAG,
                    "🗑️ deleteByTable completed"
                )

                val after =
                    db.kotItemDao()
                        .getItemsByTable(
                            event.tableId
                        )

                Log.d(
                    TAG,
                    "📦 Items AFTER delete = ${after.size}"
                )

                if (after.isEmpty()) {

                    Log.d(
                        TAG,
                        "🎉 SUCCESS: Waiter table cleared"
                    )

                } else {

                    Log.e(
                        TAG,
                        "❌ FAILED: Items still exist"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "❌ Failed to clear waiter table",
                    e
                )

            } finally {

                pendingResult.finish()

                Log.d(
                    TAG,
                    "🏁 LAN processing finished"
                )
            }
        }
    }

    companion object {

        const val ACTION_LAN_EVENT =
            "com.it10x.waiterApp1_01.LAN_EVENT"

        const val EXTRA_EVENT =
            "lan_event"

        private const val TAG =
            "WAITER_LAN_RECEIVER"
    }
}

//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.util.Log
//import com.google.gson.Gson
//import com.it10x.waiterApp1_01.data.pos.AppDatabaseProvider
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch




//class WaiterLanBroadcastReceiver : BroadcastReceiver() {
//
//    private val gson = Gson()
//
//    override fun onReceive(
//        context: Context,
//        intent: Intent
//    ) {
//
//        if (intent.action != ACTION_LAN_EVENT) {
//            return
//        }
//
//        val json =
//            intent.getStringExtra(EXTRA_EVENT)
//                ?: return
//
//        Log.d(
//            TAG,
//            "===================================="
//        )
//
//        Log.d(
//            TAG,
//            "LAN EVENT RECEIVED"
//        )
//
//        Log.d(
//            TAG,
//            "JSON: $json"
//        )
//
//        Log.d(
//            TAG,
//            "===================================="
//        )
//
//        try {
//
//            val event =
//                gson.fromJson(
//                    json,
//                    LanEvent::class.java
//                )
//
//            when (event.type) {
//
//                "BILL_CLEARED" -> {
//
//                    Log.d(
//                        TAG,
//                        "🧹 BILL CLEARED"
//                    )
//
//                    Log.d(
//                        TAG,
//                        "Table: ${event.tableId}"
//                    )
//
//                    Log.d(
//                        TAG,
//                        "Session: ${event.sessionId}"
//                    )
//
//                    clearWaiterTable(
//                        context,
//                        event
//                    )
//                }
//
//                else -> {
//
//                    Log.d(
//                        TAG,
//                        "Unknown LAN event: ${event.type}"
//                    )
//                }
//            }
//
//        } catch (e: Exception) {
//
//            Log.e(
//                TAG,
//                "Failed to process LAN event",
//                e
//            )
//        }
//    }
//
//    private fun clearWaiterTable(
//        context: Context,
//        event: LanEvent
//    ) {
//
//        val pendingResult = goAsync()
//
//        CoroutineScope(Dispatchers.IO).launch {
//
//            try {
//
//                val db =
//                    AppDatabaseProvider.get(
//                        context.applicationContext
//                    )
//
//                db.kotItemDao()
//                    .deleteByTableId(
//                        event.tableId
//                    )
//
//                Log.d(
//                    TAG,
//                    "✅ Waiter table cleared locally: ${event.tableId}"
//                )
//
//            } catch (e: Exception) {
//
//                Log.e(
//                    TAG,
//                    "❌ Failed to clear waiter table",
//                    e
//                )
//
//            } finally {
//
//                pendingResult.finish()
//            }
//        }
//    }
//
//    companion object {
//
//        const val ACTION_LAN_EVENT =
//            "com.it10x.waiterApp1_01.LAN_EVENT"
//
//        const val EXTRA_EVENT =
//            "lan_event"
//
//        private const val TAG =
//            "WAITER_LAN_RECEIVER"
//    }
//}