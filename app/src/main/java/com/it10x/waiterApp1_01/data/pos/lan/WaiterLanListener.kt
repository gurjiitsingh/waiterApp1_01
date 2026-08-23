package com.it10x.waiterApp1_01.data.pos.lan



import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WaiterLanListener(
    private val scope: CoroutineScope,
    private val onEvent: suspend (LanEvent) -> Unit
) {

    companion object {
        private const val TAG = "WAITER_LAN"
        private const val PORT = 3456
        private const val BUFFER_SIZE = 8192
    }

    private val gson = Gson()

    private var socket: DatagramSocket? = null
    private var job: Job? = null

    fun start() {

        if (job?.isActive == true) {
            Log.d(TAG, "Listener already running")
            return
        }

        job = scope.launch(Dispatchers.IO) {

            try {

                socket = DatagramSocket(PORT).apply {
                    broadcast = true
                    reuseAddress = true
                }

                Log.d(
                    TAG,
                    "===================================="
                )

                Log.d(
                    TAG,
                    "UDP WAITER LISTENER STARTED"
                )

                Log.d(
                    TAG,
                    "PORT: $PORT"
                )

                Log.d(
                    TAG,
                    "===================================="
                )

                val buffer =
                    ByteArray(BUFFER_SIZE)

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
                            packet.offset,
                            packet.length,
                            Charsets.UTF_8
                        )

                    Log.d(
                        TAG,
                        "UDP received from ${packet.address.hostAddress}: $message"
                    )

                    try {

                        val event =
                            gson.fromJson(
                                message,
                                LanEvent::class.java
                            )

                        onEvent(event)

                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "Invalid LAN event: $message",
                            e
                        )
                    }
                }

            } catch (e: CancellationException) {

                Log.d(
                    TAG,
                    "UDP listener cancelled"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "UDP listener failed",
                    e
                )

            } finally {

                socket?.close()
                socket = null
            }
        }
    }

    fun stop() {

        Log.d(
            TAG,
            "Stopping UDP listener"
        )

        job?.cancel()
        job = null

        socket?.close()
        socket = null
    }
}