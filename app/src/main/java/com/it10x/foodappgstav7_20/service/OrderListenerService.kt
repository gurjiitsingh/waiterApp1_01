package com.it10x.waiterApp1_01.service

import android.app.*
import android.content.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.it10x.waiterApp1_01.R
import com.it10x.waiterApp1_01.printer.AutoPrintManager
import com.it10x.waiterApp1_01.data.online.repository.OrdersRepository
import com.it10x.waiterApp1_01.printer.PrinterManager

class OrderListenerService : Service() {

    private lateinit var listener: ServiceRealtimeOrdersListener
    private var isReceiverRegistered = false

    private val stopSoundReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.e("STOP_SOUND", "Broadcast received")
            listener.stopRingtone()
            android.util.Log.e("STOP_SOUND", "Ringtone STOP requested")
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (FirebaseApp.getApps(this).isEmpty()) {
            stopSelf()
            return
        }

        val printerManager = PrinterManager.getInstance(this)
        val ordersRepo = OrdersRepository()

        val autoPrint = AutoPrintManager(
            printerManager = printerManager,
            ordersRepository = ordersRepo
        )

        listener = ServiceRealtimeOrdersListener(
            context = application,
            autoPrintManager = autoPrint
        )

        listener.startListening()



        val filter = IntentFilter("STOP_RINGTONE")

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                stopSoundReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stopSoundReceiver, filter)
        }

        isReceiverRegistered = true

        startForeground(99, buildNotification())
    }

    private fun buildNotification(): Notification {
        val channelId = "orders_monitor"
        val channelName = "Order Monitoring"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Order Monitoring Active")
            .setContentText("Listening for new orders…")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {

        if (isReceiverRegistered) {
            try {
                unregisterReceiver(stopSoundReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isReceiverRegistered = false
        }

        if (::listener.isInitialized) {
            listener.stopListening()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}