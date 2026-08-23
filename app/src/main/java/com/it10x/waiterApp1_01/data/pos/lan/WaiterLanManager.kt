package com.it10x.waiterApp1_01.data.pos.lan



import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.it10x.waiterApp1_01.network.lan.WaiterLanUdpService

object WaiterLanManager {

    fun start(
        context: Context
    ) {

        val intent =
            Intent(
                context,
                WaiterLanUdpService::class.java
            )

        context.startService(intent)

//        val intent =
//            Intent(
//                context,
//                WaiterLanUdpService::class.java
//            )
//
//        ContextCompat.startForegroundService(
//            context,
//            intent
//        )
    }

    fun stop(
        context: Context
    ) {

        val intent =
            Intent(
                context,
                WaiterLanUdpService::class.java
            )

        context.stopService(intent)
    }
}