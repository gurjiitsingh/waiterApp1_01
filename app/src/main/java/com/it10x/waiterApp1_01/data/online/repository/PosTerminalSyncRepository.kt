package com.it10x.waiterApp1_01.data.online.repository


import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.it10x.waiterApp1_01.data.pos.dao.OutletDao
import com.it10x.waiterApp1_01.data.pos.dao.PosTerminalDao
import com.it10x.waiterApp1_01.data.pos.entities.PosTerminalEntity
import kotlinx.coroutines.tasks.await


class PosTerminalSyncRepository(
    private val posTerminalDao: PosTerminalDao,
    private val outletDao: OutletDao,
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    // =====================================================
    // SYNC POS TERMINAL
    // =====================================================

    suspend fun syncPosTerminal() {

        // -------------------------------------------------
        // GET LOCAL OUTLET
        // -------------------------------------------------

        val outlet =
            outletDao.getOutlet()
                ?: throw IllegalStateException(
                    "Outlet not configured"
                )


        val outletId =
            outlet.outletId


        if (outletId.isBlank()) {

            throw IllegalStateException(
                "Outlet ID is empty"
            )

        }


        Log.d(
            "POS_TERMINAL_SYNC",
            "Syncing POS terminal for outlet=$outletId"
        )


        // -------------------------------------------------
        // FIRESTORE
        // -------------------------------------------------

        val snapshot =
            firestore
                .collection("posTerminals")
                .whereEqualTo(
                    "outletId",
                    outletId
                )
                .whereEqualTo(
                    "isActive",
                    true
                )
                .limit(1)
                .get()
                .await()


        if (snapshot.isEmpty) {

            Log.w(
                "POS_TERMINAL_SYNC",
                "No active POS terminal found for outlet=$outletId"
            )

            return
        }


        // -------------------------------------------------
        // GET TERMINAL
        // -------------------------------------------------

        val document =
            snapshot.documents.first()


        val terminalId =
            document.getString(
                "terminalId"
            )
                ?: document.id


        val terminalName =
            document.getString(
                "terminalName"
            )
                ?: "POS"


        val ownerId =
            document.getString(
                "ownerId"
            )
                ?: ""


        val ipAddress =
            document.getString(
                "ipAddress"
            )
                ?: ""


        val port =
            document.getLong(
                "port"
            )?.toInt()
                ?: 8787


        val deviceType =
            document.getString(
                "deviceType"
            )
                ?: "POS"


        val isActive =
            document.getBoolean(
                "isActive"
            )
                ?: true


        val lastSeenAt =
            document.getLong(
                "lastSeenAt"
            )
                ?: 0L


        val updatedAt =
            document.getLong(
                "updatedAt"
            )
                ?: 0L


        // -------------------------------------------------
        // VALIDATE IP
        // -------------------------------------------------

        if (ipAddress.isBlank()) {

            throw IllegalStateException(
                "POS terminal IP address is empty"
            )

        }


        // -------------------------------------------------
        // ROOM ENTITY
        // -------------------------------------------------

        val terminal =
            PosTerminalEntity(

                terminalId =
                    terminalId,

                terminalName =
                    terminalName,

                outletId =
                    outletId,

                ownerId =
                    ownerId,

                ipAddress =
                    ipAddress,

                port =
                    port,

                deviceType =
                    deviceType,

                isActive =
                    isActive,

                lastSeenAt =
                    lastSeenAt,

                updatedAt =
                    updatedAt
            )


        // -------------------------------------------------
        // SAVE TO ROOM
        // -------------------------------------------------

        posTerminalDao.insert(
            terminal
        )


        Log.d(
            "POS_TERMINAL_SYNC",
            "===================================="
        )

        Log.d(
            "POS_TERMINAL_SYNC",
            "POS TERMINAL SYNCED"
        )

        Log.d(
            "POS_TERMINAL_SYNC",
            "Terminal ID: $terminalId"
        )

        Log.d(
            "POS_TERMINAL_SYNC",
            "Terminal Name: $terminalName"
        )

        Log.d(
            "POS_TERMINAL_SYNC",
            "Outlet ID: $outletId"
        )

        Log.d(
            "POS_TERMINAL_SYNC",
            "IP: $ipAddress"
        )

        Log.d(
            "POS_TERMINAL_SYNC",
            "Port: $port"
        )

        Log.d(
            "POS_TERMINAL_SYNC",
            "===================================="
        )
    }
}