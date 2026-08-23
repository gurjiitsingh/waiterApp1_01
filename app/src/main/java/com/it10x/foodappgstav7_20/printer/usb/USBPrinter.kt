package com.it10x.waiterApp1_01.printer.usb

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.*
import android.util.Log
import com.it10x.waiterApp1_01.usb.USBPermissionHelper
import kotlinx.coroutines.*

object USBPrinter {

    private const val TAG = "USBPrinter"
    const val ACTION_USB_PERMISSION = "com.it10x.waiterApp1_01.USB_PERMISSION"

    private var usbManager: UsbManager? = null
    private var usbDevice: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private var outEndpoint: UsbEndpoint? = null

    // =================================================
    // INIT + PERMISSION
    // =================================================
    fun init(
        context: Context,
        device: UsbDevice,
        onReady: (Boolean) -> Unit
    ) {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        usbDevice = device

        USBPermissionHelper.requestPermission(context, device) {
            try {
                setupConnection(device)
                onReady(true)
            } catch (e: Exception) {
                Log.e(TAG, "USB setup failed", e)
                onReady(false)
            }
        }
    }

    // =================================================
    // USB CONNECTION SETUP
    // =================================================
    private fun setupConnection(device: UsbDevice) {
        val iface = device.getInterface(0)
        outEndpoint = null

        for (i in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(i)
            if (
                ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                ep.direction == UsbConstants.USB_DIR_OUT
            ) {
                outEndpoint = ep
                break
            }
        }

        if (outEndpoint == null) {
            throw IllegalStateException("No OUT endpoint found")
        }

        connection = usbManager?.openDevice(device)
            ?: throw IllegalStateException("Unable to open USB device")

        connection?.claimInterface(iface, true)
        Log.d(TAG, "USB printer connected: ${device.deviceName}")
    }

    // =================================================
    // TEST PRINT
    // =================================================
    fun printBitmap(
        context: Context,
        device: UsbDevice,
        bitmap: Bitmap,
        onResult: (Boolean) -> Unit
    ) {

        init(context, device) { ready ->

            if (!ready) {
                onResult(false)
                return@init
            }


            val ep = outEndpoint
            val conn = connection


            if (ep == null || conn == null) {

                Log.e(TAG, "USB connection missing")

                onResult(false)
                return@init
            }


            CoroutineScope(Dispatchers.IO).launch {

                try {


                    // =============================
                    // INIT
                    // =============================

                    conn.bulkTransfer(
                        ep,
                        byteArrayOf(
                            0x1B,
                            0x40
                        ),
                        2,
                        1000
                    )

                    delay(50)



                    // =============================
                    // CENTER
                    // =============================

                    conn.bulkTransfer(
                        ep,
                        byteArrayOf(
                            0x1B,
                            0x61,
                            0x01
                        ),
                        3,
                        1000
                    )

                    delay(50)



                    // =============================
                    // IMAGE
                    // =============================

                    printBitmapInChunksUSB(
                        conn,
                        ep,
                        bitmap
                    )


                    delay(100)



                    // =============================
                    // FEED + CUT
                    // =============================

                    conn.bulkTransfer(
                        ep,
                        byteArrayOf(
                            0x0A,
                            0x0A,
                            0x1D,
                            0x56,
                            0x01
                        ),
                        5,
                        1000
                    )


                    withContext(Dispatchers.Main) {
                        onResult(true)
                    }


                } catch(e:Exception) {


                    Log.e(
                        TAG,
                        "USB bitmap print failed",
                        e
                    )


                    withContext(Dispatchers.Main) {
                        onResult(false)
                    }
                }
            }
        }
    }

    // =================================================
    // TEST PRINT
    // =================================================
    fun printTest(
        context: Context,
        device: UsbDevice,
        roleLabel: String,
        onResult: (Boolean) -> Unit
    ) {
        init(context, device) { ready ->
            if (!ready) {
                onResult(false)
                return@init
            }

            val testText = """
            ****************************
                 TEST PRINT
            ****************************
            Printer Role : $roleLabel
            Connection   : USB
            Device Name  : ${device.deviceName}
            Status       : OK
            ----------------------------


        """.trimIndent()

//            printText(testText) { success ->
//                onResult(success)
//            }
        }
    }

    // =================================================
    // CORE PRINT (ORDER / AUTO)
    // =================================================




    private fun convertBitmapToRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val originalHeight = bitmap.height

        val height = originalHeight

        val bytes = ArrayList<Byte>()
        val bytesPerLine = (width + 7) / 8

        // GS v 0
        bytes.add(0x1D)
        bytes.add(0x76)
        bytes.add(0x30)
        bytes.add(0x00)

        bytes.add((bytesPerLine % 256).toByte())
        bytes.add((bytesPerLine / 256).toByte())

        bytes.add((height % 256).toByte())
        bytes.add((height / 256).toByte())

        for (y in 0 until height) {
            for (x in 0 until bytesPerLine * 8 step 8) {

                var byte = 0

                for (bit in 0 until 8) {
                    val xPos = x + bit

                    if (xPos < width) {
                        val pixel = bitmap.getPixel(xPos, y)

                        val r = (pixel shr 16) and 0xff
                        val g = (pixel shr 8) and 0xff
                        val b = pixel and 0xff

                        val gray = (r + g + b) / 3

                        if (gray < 128) {
                            byte = byte or (1 shl (7 - bit))
                        }
                    }
                }

                bytes.add(byte.toByte())
            }
        }

        return bytes.toByteArray()
    }
    fun printText(
        context: Context,
        device: UsbDevice,
        text: String,
        onResult: (Boolean) -> Unit
    ) {
        init(context, device) { ready ->

            if (!ready) {
                onResult(false)
                return@init
            }

            val ep = outEndpoint
            val conn = connection

            if (ep == null || conn == null) {
                onResult(false)
                return@init
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val init = byteArrayOf(0x1B, 0x40)

                    val safeText = text
                        .replace("\n", "\r\n")
                        .toByteArray(Charsets.US_ASCII)

                    val feedCut = byteArrayOf(
                        0x1B, 0x64, 0x03,
                        0x1D, 0x56, 0x01
                    )

                    // ✅ SEND STEP BY STEP
                    conn.bulkTransfer(ep, init, init.size, 1000)
                    delay(50)

                    conn.bulkTransfer(ep, safeText, safeText.size, 5000)
                    delay(100)

                    conn.bulkTransfer(ep, feedCut, feedCut.size, 1000)

                    withContext(Dispatchers.Main) {
                        onResult(true)
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onResult(false)
                    }
                }
            }
        }
    }




    fun printLogoTextQrUSB(
        context: Context,
        device: UsbDevice,
        logoBitmap: Bitmap?,   // nullable
        qrBitmap: Bitmap?,     // nullable
        text: String,
        onResult: (Boolean) -> Unit
    ) {
        init(context, device) { ready ->

            if (!ready) {
                onResult(false)
                return@init
            }

            val ep = outEndpoint
            val conn = connection

            if (ep == null || conn == null) {
                onResult(false)
                return@init
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // =============================
                    // INIT
                    // =============================
                    conn.bulkTransfer(ep, byteArrayOf(0x1B, 0x40), 2, 1000)
                    delay(50)

                    // =============================
                    // CENTER ALIGN
                    // =============================
                    conn.bulkTransfer(ep, byteArrayOf(0x1B, 0x61, 0x01), 3, 1000)
                    delay(50)

                    // =============================
                    // PRINT LOGO
                    // =============================
                    if (logoBitmap != null) {
                        printBitmapInChunksUSB(conn, ep, logoBitmap)
                        delay(100)
                    }

                    // =============================
                    // PRINT QR (🔥 THIS WAS MISSING)
                    // =============================
                    if (qrBitmap != null) {

                        // 🔥 VERY IMPORTANT
                        delay(200)

                        printBitmapInChunksUSB(conn, ep, qrBitmap)

                        delay(200)

                        conn.bulkTransfer(ep, byteArrayOf(0x0A), 1, 1000)
                    }

                    // =============================
                    // LEFT ALIGN
                    // =============================
                    conn.bulkTransfer(ep, byteArrayOf(0x1B, 0x61, 0x00), 3, 1000)
                    delay(50)

                    // =============================
                    // TEXT
                    // =============================
                    val safeText = text
                        .replace("\n", "\r\n")
                        .toByteArray(Charsets.US_ASCII)

                    conn.bulkTransfer(ep, safeText, safeText.size, 5000)

                    // small spacing
                    conn.bulkTransfer(ep, byteArrayOf(0x0A, 0x0A), 2, 1000)

                    delay(100)

                    // =============================
                    // CUT
                    // =============================
                    conn.bulkTransfer(
                        ep,
                        byteArrayOf(0x1D, 0x56, 0x01),
                        3,
                        1000
                    )

                    withContext(Dispatchers.Main) {
                        onResult(true)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "USB logo + QR print failed", e)
                    withContext(Dispatchers.Main) {
                        onResult(false)
                    }
                }
            }
        }
    }
    private fun convertBitmapToEscPos(bitmap: Bitmap): ByteArray {

        val width = bitmap.width
        val height = bitmap.height

        val bytes = ArrayList<Byte>()

        val bytesPerRow = (width + 7) / 8

        // GS v 0
        bytes.add(0x1D)
        bytes.add(0x76)
        bytes.add(0x30)
        bytes.add(0x00)

        bytes.add((bytesPerRow % 256).toByte())
        bytes.add((bytesPerRow / 256).toByte())

        bytes.add((height % 256).toByte())
        bytes.add((height / 256).toByte())

        for (y in 0 until height) {
            for (x in 0 until bytesPerRow * 8 step 8) {

                var slice = 0

                for (b in 0 until 8) {
                    val xPos = x + b

                    if (xPos < width) {
                        val pixel = bitmap.getPixel(xPos, y)

                        val r = (pixel shr 16) and 0xff
                        val g = (pixel shr 8) and 0xff
                        val bVal = pixel and 0xff

                        val gray = (r + g + bVal) / 3

                        if (gray < 128) {
                            slice = slice or (1 shl (7 - b))
                        }
                    }
                }

                bytes.add(slice.toByte())
            }
        }

        return bytes.toByteArray()
    }

    private fun printBitmapInChunksUSB(
        conn: UsbDeviceConnection,
        ep: UsbEndpoint,
        bitmap: Bitmap
    ) {
        val chunkHeight = 48

        var y = 0
        while (y < bitmap.height) {

            val height = minOf(chunkHeight, bitmap.height - y)

            val chunk = Bitmap.createBitmap(bitmap, 0, y, bitmap.width, height)
            val bytes = convertBitmapToRaster(chunk)

            var offset = 0
            val packetSize = 2048

            while (offset < bytes.size) {
                val end = minOf(offset + packetSize, bytes.size)
                val part = bytes.copyOfRange(offset, end)

                conn.bulkTransfer(ep, part, part.size, 3000)
                offset = end

                Thread.sleep(20) // 🔥 IMPORTANT
            }

            Thread.sleep(40) // 🔥 VERY IMPORTANT
            y += height
        }
    }





    // =================================================
    // DEVICE LIST
    // =================================================
    fun getConnectedUSBDevices(context: Context): List<UsbDevice> {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return manager.deviceList.values.toList()
    }

    // =================================================
    // RELEASE
    // =================================================
    fun release() {
        try {
            connection?.close()
        } catch (_: Exception) {}
        connection = null
        outEndpoint = null
        usbDevice = null
    }
}
