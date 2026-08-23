package com.it10x.waiterApp1_01.network.lan

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.it10x.waiterApp1_01.data.pos.lan.WaiterLanBroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket

class WaiterLanUdpService : Service() {

    private val serviceJob = Job()

    private val scope =
        CoroutineScope(
            Dispatchers.IO + serviceJob
        )

    private var socket: DatagramSocket? = null

    override fun onCreate() {

        super.onCreate()

        Log.d(
            TAG,
            "===================================="
        )

        Log.d(
            TAG,
            "WAITER LAN UDP SERVICE STARTED"
        )

        Log.d(
            TAG,
            "Listening on UDP port $PORT"
        )

        Log.d(
            TAG,
            "===================================="
        )

        startListening()
    }

    private fun startListening() {

        scope.launch {

            try {

                socket =
                    DatagramSocket(PORT).apply {
                        broadcast = true
                    }

                val buffer =
                    ByteArray(8192)

                while (isActive) {

                    val packet =
                        DatagramPacket(
                            buffer,
                            buffer.size
                        )

                    socket?.receive(packet)

                    val message =
                        String(
                            packet.data,
                            0,
                            packet.length,
                            Charsets.UTF_8
                        )

                    val senderIp =
                        packet.address
                            ?.hostAddress
                            ?: "UNKNOWN"

                    Log.d(
                        TAG,
                        "===================================="
                    )

                    Log.d(
                        TAG,
                        "LAN BROADCAST RECEIVED"
                    )

                    Log.d(
                        TAG,
                        "Sender IP: $senderIp"
                    )

                    Log.d(
                        TAG,
                        "Message: $message"
                    )

                    Log.d(
                        TAG,
                        "===================================="
                    )

                    val intent =
                        Intent(
                            WaiterLanBroadcastReceiver.ACTION_LAN_EVENT
                        ).apply {

                            setPackage(
                                applicationContext.packageName
                            )

                            putExtra(
                                WaiterLanBroadcastReceiver.EXTRA_EVENT,
                                message
                            )
                        }

                    sendBroadcast(intent)
                }

            } catch (e: Exception) {

                if (scope.isActive) {

                    Log.e(
                        TAG,
                        "UDP listener failed",
                        e
                    )
                }
            }
        }
    }

    override fun onDestroy() {

        Log.d(
            TAG,
            "Stopping waiter UDP listener"
        )

        socket?.close()

        scope.cancel()

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    companion object {

        const val PORT = 3456

        private const val TAG =
            "WAITER_UDP"
    }
}