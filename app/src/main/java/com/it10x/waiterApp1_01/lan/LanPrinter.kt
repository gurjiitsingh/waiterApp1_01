package com.it10x.waiterApp1_01.printer.lan

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

object LanPrinter {

    private const val TAG = "LanPrinter"
    private const val TIMEOUT = 3000

    private val mainHandler = Handler(Looper.getMainLooper())

// -----------------------------
//FULL BILL AS IMAGE PRINT
// -----------------------------
    fun printBitmap1(
        ip: String,
        port: Int,
        bitmap: android.graphics.Bitmap,
        onResult: (Boolean) -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {

            var socket: Socket? = null
            var output: OutputStream? = null

            try {

                socket = Socket()

                socket.connect(
                    InetSocketAddress(ip, port),
                    TIMEOUT
                )

                output = socket.getOutputStream()

                // Initialize Printer
                output.write(byteArrayOf(0x1B, 0x40))

                // Optional Beep
                output.write(byteArrayOf(0x1B, 0x42, 0x03, 0x02))

                // Center Alignment
                output.write(byteArrayOf(0x1B, 0x61, 0x01))

                // Print Bitmap
                printBitmapInChunks(
                    output,
                    bitmap
                )

                // Feed Paper
                output.write(
                    byteArrayOf(
                        0x1B, 0x64, 0x04
                    )
                )

                // Cut Paper
                output.write(
                    byteArrayOf(
                        0x1D, 0x56, 0x01
                    )
                )

                output.flush()

                withContext(Dispatchers.Main) {

                    onResult(true)

                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "LAN bitmap print failed",
                    e
                )

                withContext(Dispatchers.Main) {

                    onResult(false)

                }

            } finally {

                try {
                    output?.close()
                    socket?.close()
                } catch (_: Exception) {
                }

            }

        }

    }


    fun printBitmap(
        ip: String,
        port: Int,
        bitmap: android.graphics.Bitmap,
        onResult: (Boolean) -> Unit
    ) {

        Log.d(
            "IMAGE_TEST",
            "LanPrinter.printBitmap()"
        )

        Log.d(
            "IMAGE_TEST",
            "Bitmap size = ${bitmap.width} x ${bitmap.height}"
        )

        CoroutineScope(Dispatchers.IO).launch {

            var socket: Socket? = null
            var output: OutputStream? = null

            try {

                socket = Socket()
                socket.connect(
                    InetSocketAddress(ip, port),
                    TIMEOUT
                )

                output = socket.getOutputStream()

                //--------------------------------
                // INIT
                //--------------------------------

                output.write(
                    byteArrayOf(
                        0x1B,
                        0x40
                    )
                )

                //--------------------------------
                // BEEP
                //--------------------------------

                output.write(
                    byteArrayOf(
                        0x1B,
                        0x42,
                        0x03,
                        0x02
                    )
                )

                //--------------------------------
                // CENTER
                //--------------------------------

                output.write(
                    byteArrayOf(
                        0x1B,
                        0x61,
                        0x01
                    )
                )

                //--------------------------------
                // IMAGE
                //--------------------------------

                printBitmapInChunks(
                    output,
                    bitmap
                )

                //--------------------------------
                // FEED
                //--------------------------------

                output.write(byteArrayOf(0x0A))

                //--------------------------------
                // CUT
                //--------------------------------

                output.write(
                    byteArrayOf(
                        0x1D,
                        0x56,
                        0x01
                    )
                )

                output.flush()

                withContext(Dispatchers.Main) {
                    onResult(true)
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Bitmap print failed",
                    e
                )

                withContext(Dispatchers.Main) {
                    onResult(false)
                }

            } finally {

                try {
                    output?.close()
                } catch (_: Exception) {
                }

                try {
                    socket?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    // -----------------------------
    // TEST PRINT
    // -----------------------------
    fun printTest(
        ip: String,
        port: Int,
        roleLabel: String,
        onResult: (Boolean) -> Unit
    ) {
        val testText = """
        ****************************
             TEST PRINT
        ****************************
        Printer Role : $roleLabel
        Connection   : LAN
        IP Address   : $ip
        Port         : $port
        Status       : OK
        ----------------------------


    """.trimIndent()

        printText(
            ip = ip,
            port = port,
            text = testText,
            onResult = onResult
        )
    }

    // -----------------------------
    // CORE PRINT
    // -----------------------------
    fun printText(
        ip: String,
        port: Int,
        text: String,
        onResult: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var socket: Socket? = null
            var output: OutputStream? = null

            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ip, port), TIMEOUT)

                output = socket.getOutputStream()

                // ✅ ESC/POS INIT
                output.write(byteArrayOf(0x1B, 0x40))

                // 🔔 BEEP (Kitchen Alert)
                output.write(byteArrayOf(0x1B, 0x42, 0x03, 0x02))

                // ✅ Convert LF → CRLF for consistent printing
                val safeText = text
                    .replace("\n", "\r\n")
                    .toByteArray(Charsets.UTF_8)

                output.write(safeText)

                // ✅ FEED 3 LINES + FULL CUT
                val feedAndCut = byteArrayOf(
                    0x1B, 0x64, 0x03,
                    0x1D, 0x56, 0x01
                )
                output.write(feedAndCut)

                output.flush()

                withContext(Dispatchers.Main) {
                    onResult(true)
                }

            } catch (e: Exception) {
                Log.e(TAG, "LAN print failed", e)
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            } finally {
                try {
                    output?.close()
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
    }

    fun printLogoAndText(
        ip: String,
        port: Int,
        bitmap: android.graphics.Bitmap,
        text: String,
        onResult: (Boolean) -> Unit
    ) {
        Thread {
            var socket: Socket? = null
            var output: OutputStream? = null

            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ip, port), TIMEOUT)

                output = socket.getOutputStream()

                // INIT
                output.write(byteArrayOf(0x1B, 0x40))

                // 🔔 BEEP
                val beep = byteArrayOf(0x1B, 0x42, 0x03, 0x02)
                output.write(beep)

                // CENTER ALIGN
                output.write(byteArrayOf(0x1B, 0x61, 0x01))

                // IMAGE
                val imageBytes = convertBitmapToEscPos(bitmap)
                output.write(imageBytes)

                // ❌ REMOVED SPACE AFTER LOGO

                // LEFT ALIGN
                output.write(byteArrayOf(0x1B, 0x61, 0x00))

                // TEXT
                val safeText = text
                    .replace("\n", "\r\n")
                    .toByteArray(Charsets.UTF_8)

                output.write(safeText)

                // FEED + CUT
                output.write(byteArrayOf(
                    0x1B, 0x64, 0x03,
                    0x1D, 0x56, 0x01
                ))

                output.flush()

                mainHandler.post { onResult(true) }

            } catch (e: Exception) {
                Log.e(TAG, "LAN print logo failed", e)
                mainHandler.post { onResult(false) }
            } finally {
                try {
                    output?.close()
                    socket?.close()
                } catch (_: Exception) {}
            }
        }.start()
    }

    fun printLogoTextQr(
        ip: String,
        port: Int,
        logoBitmap: android.graphics.Bitmap?,
        qrBitmap: android.graphics.Bitmap?,
        text: String,
        onResult: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {

            var socket: Socket? = null
            var output: OutputStream? = null

            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ip, port), TIMEOUT)

                output = socket.getOutputStream()

                // INIT
                output.write(byteArrayOf(0x1B, 0x40))

                // 🔔 BEEP
                output.write(byteArrayOf(0x1B, 0x42, 0x03, 0x02))

                // CENTER ALIGN
                output.write(byteArrayOf(0x1B, 0x61, 0x01))

                // =============================
                // LOGO
                // =============================
                if (logoBitmap != null) {
                    val logoBytes = convertBitmapToRaster(logoBitmap)
                    output.write(logoBytes)
                }

                // =============================
                // QR
                // =============================
                if (qrBitmap != null) {

                    output.write(byteArrayOf(0x0A))

                    output.flush()

//                    val qrBytes = convertBitmapToRaster(qrBitmap)
//                    output.write(qrBytes)
                    printBitmapInChunks(output, qrBitmap)

                    output.flush()
                    Thread.sleep(100)

                    output.write(byteArrayOf(0x0A))
                }

                // =============================
                // TEXT
                // =============================
                output.write(byteArrayOf(0x1B, 0x61, 0x00))

                val safeText = text
                    .replace("\n", "\r\n")
                    .toByteArray(Charsets.UTF_8)

                output.write(safeText)

                output.write(byteArrayOf(0x0A, 0x0A))

                // =============================
                // CUT
                // =============================
                output.write(byteArrayOf(
                    0x0A, 0x0A,
                    0x1D, 0x56, 0x01
                ))

                output.flush()

                withContext(Dispatchers.Main) {
                    onResult(true)
                }

            } catch (e: Exception) {
                Log.e(TAG, "LAN logo + QR failed", e)
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            } finally {
                try {
                    output?.close()
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
    }


    private fun printBitmapInChunks(
        output: OutputStream,
        bitmap: android.graphics.Bitmap
    ) {
        val chunkHeight = 48

        var y = 0

        while (y < bitmap.height) {

            val height = minOf(chunkHeight, bitmap.height - y)

            val chunk = android.graphics.Bitmap.createBitmap(
                bitmap,
                0,
                y,
                bitmap.width,
                height
            )

            val bytes = convertBitmapToRaster(chunk)

            output.write(bytes)
            output.flush()

            Thread.sleep(40)

            y += height
        }
    }
    private fun convertBitmapToRaster(bitmap: android.graphics.Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height

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
    private fun convertBitmapToEscPos(bitmap: android.graphics.Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height

        val bytes = ArrayList<Byte>()

        for (y in 0 until height step 24) {

            bytes.add(0x1B)
            bytes.add(0x2A)
            bytes.add(33)

            bytes.add((width % 256).toByte())
            bytes.add((width / 256).toByte())

            for (x in 0 until width) {
                for (k in 0 until 3) {

                    var slice = 0

                    for (b in 0 until 8) {
                        val yPos = y + (k * 8) + b

                        if (yPos < height) {
                            val pixel = bitmap.getPixel(x, yPos)

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

            if (y + 24 < height) {
                bytes.add(0x0A)
            }
        }

        return bytes.toByteArray()
    }

}
