package com.it10x.waiterApp1_01.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.it10x.waiterApp1_01.data.PrinterConfig
import com.it10x.waiterApp1_01.data.PrinterPreferences
import com.it10x.waiterApp1_01.data.PrinterRole
import com.it10x.waiterApp1_01.data.PrinterType
import com.it10x.waiterApp1_01.data.print.OutletInfo
import com.it10x.waiterApp1_01.printer.bluetooth.BluetoothPrinter
import com.it10x.waiterApp1_01.printer.lan.LanPrinter
import com.it10x.waiterApp1_01.printer.usb.USBPrinter
import com.it10x.waiterApp1_01.data.print.OutletMapper
import com.it10x.waiterApp1_01.data.pos.AppDatabaseProvider
import com.it10x.waiterApp1_01.data.pos.entities.PosKotItemEntity
import com.it10x.waiterApp1_01.ui.sales.SalesUiState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.it10x.waiterApp1_01.printer.PrintJob
import kotlinx.coroutines.runBlocking
import com.it10x.waiterApp1_01.data.printqueue.PrintQueueDao
import com.it10x.waiterApp1_01.printer.kotImage.KitchenBitmapGenerator
import com.it10x.waiterApp1_01.printer.queue.PrintQueueManager
import com.it10x.waiterApp1_01.printer.utils.QrUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PrinterManager private constructor(
    private val context: Context
) {

    companion object {
        @Volatile
        private var INSTANCE: PrinterManager? = null

        fun getInstance(context: Context): PrinterManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PrinterManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }




    private val scope = CoroutineScope(Dispatchers.IO)

    private val queueManager: PrintQueueManager by lazy {

        val db = AppDatabaseProvider.get(context)

        PrintQueueManager.getInstance(
            dao = db.printQueueDao(),
            printerManager = this
        )
    }

    private val prefs by lazy { PrinterPreferences(context) }
    fun appContext(): Context = context.applicationContext

//IMAGE PRINT QUE
//fun enqueueImagePrint(  // THIS IS CHANGED TO SUSPEND
suspend fun enqueueImagePrint1(
    referenceId: String,
    role: PrinterRole,
    bitmap: Bitmap,
    paymentMode: String? = null,
    grandTotal: Double? = null
) {

    try {

        val printDir = File(
            context.filesDir,
            "print_queue"
        )

        if (!printDir.exists()) {
            printDir.mkdirs()
        }

        val imageFile = File(
            printDir,
            "${role.name.lowercase()}_${System.currentTimeMillis()}.png"
        )


// Preview file
//        val previewFile = File(
//            context.cacheDir,
//            "receipt_preview.png"
//        )
//
//        previewFile.outputStream().use {
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
//        }
        Log.d(
            "IMAGE_TEST",
            "Saving file at: ${imageFile.absolutePath}"
        )
        Log.d("IMAGE_TEST", "File exists before save = ${imageFile.exists()}")

        imageFile.outputStream().use { stream ->

            val success = bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                stream
            )
            Log.d("IMAGE_TEST", "File exists after save = ${imageFile.exists()}")
            Log.d("IMAGE_TEST", "File size = ${imageFile.length()}")

            Log.d(
                "IMAGE_TEST",
                "Bitmap compress success: $success"
            )
        }


        queueManager.enqueueImage(
            referenceId = referenceId,
            role = role,
            imagePath = imageFile.absolutePath,
            paymentMode = paymentMode,
            grandTotal = grandTotal
        )


    } catch (e: Exception) {

        Log.e(
            "IMAGE_TEST",
            "Failed to save or enqueue image",
            e
        )
    }
}

    suspend fun enqueueImagePrint(
        referenceId: String,
        role: PrinterRole,
        bitmap: Bitmap,
        paymentMode: String? = null,
        grandTotal: Double? = null
    ) {
        try {
            val printDir = File(context.filesDir, "print_queue")

            if (!printDir.exists()) {
                printDir.mkdirs()
            }

            val imageFile = File(
                printDir,
                "${role.name.lowercase()}_${System.currentTimeMillis()}.png"
            )

            imageFile.outputStream().use { stream ->
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    stream
                )
            }


// Preview file
//        val previewFile = File(
//            context.cacheDir,
//            "receipt_preview.png"
//        )
//
//        previewFile.outputStream().use {
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
//        }

            queueManager.enqueueImage(
                referenceId = referenceId,
                role = role,
                imagePath = imageFile.absolutePath,
                paymentMode = paymentMode,
                grandTotal = grandTotal
            )

        } catch (e: Exception) {
            Log.e(
                "IMAGE_PRINT",
                "Failed to save or enqueue image",
                e
            )
        }
    }


  suspend  fun enqueueBillImage(
        order: PrintOrder,
        paymentMode: String,
        outletInfo: OutletInfo,
        kotNumberText: String,
        stewardName: String,
        referenceId: String,
    ) {
      Log.d("IMAGE_ORDER", "----------------------------")
      Log.d("IMAGE_ORDER", "itemTotal    = ${order.itemTotal}")
      Log.d("IMAGE_ORDER", "discount     = ${order.discount}")
      Log.d("IMAGE_ORDER", "itemTax      = ${order.itemTax}")
      Log.d("IMAGE_ORDER", "deliveryFee  = ${order.deliveryFee}")
      Log.d("IMAGE_ORDER", "deliveryTax  = ${order.deliveryTax}")
      Log.d("IMAGE_ORDER", "taxTotal     = ${order.tax}")
      Log.d("IMAGE_ORDER", "grandTotal   = ${order.grandTotal}")

      Log.d("IMAGE_ORDER", "taxType      = ${outletInfo.taxType}")
      Log.d("IMAGE_ORDER", "taxMode      = ${outletInfo.taxMode}")
      Log.d(
          "IMAGE_TEST",
          "encu-------------------------"
      )
        //----------------------------------
        // ✅ ADD THIS LINE (LOAD LOGO)
        //----------------------------------
        val logoBitmap = loadSavedLogo()

        //----------------------------------
        // ✅ PASS LOGO HERE
        //----------------------------------
        val bitmap = ReceiptFormatter.billing48IMAGE(
            context = context,
            order = order,
            outletInfo = outletInfo,
            logo = logoBitmap,   // ✅ IMPORTANT
            kotNumberText= kotNumberText,
            stewardName = stewardName,

        )

//        val size = prefs.getPrinterSize(PrinterRole.BILLING) ?: "80mm"
//
//        val bitmap = when (size) {
//            "80mm" -> ReceiptFormatter.billing48_IMAGE(
//                context = context,
//                order = order,
//                outletInfo = outletInfo,
//                logo = logoBitmap   // ✅ IMPORTANT
//            )
//            else -> ReceiptFormatter.billing32_IMAGE(
//                context = context,
//                order = order,
//                outletInfo = outletInfo,
//                logo = logoBitmap   // ✅ IMPORTANT
//            )
//        }

        enqueueImagePrint(
            role = PrinterRole.BILLING,
            bitmap = bitmap,
            referenceId = referenceId,
            paymentMode = paymentMode,
            grandTotal = order.grandTotal
        )
    }

    //TEXT PRINT QUE
    fun enqueueBill(
        order: PrintOrder,
        paymentMode: String,
        outletInfo: OutletInfo,
        referenceId: String,
    ) {
        val size = prefs.getPrinterSize(PrinterRole.BILLING) ?: "80mm"

        val receiptText = when (size) {
            "80mm" -> ReceiptFormatter.billing48(order, outletInfo)
            else -> ReceiptFormatter.billing(order, outletInfo)
        }

        // enqueuePrint(PrinterRole.BILLING, receiptText)

        enqueuePrint(
            role = PrinterRole.BILLING,
            text = receiptText,
            referenceId = referenceId,
            paymentMode = paymentMode,
            grandTotal = order.grandTotal
        )
    }
    fun enqueuePrint(
        role: PrinterRole,
        text: String,
        referenceId: String,
        paymentMode: String? = null,
        grandTotal: Double? = null
    ) {
       // Log.e("PRINT_DEBUG", "🔥 enqueuePrint CALLED role=$role")
        scope.launch {
            queueManager.enqueue(role, text, paymentMode, grandTotal,referenceId)
        }
    }





    fun enqueueKitchen(
        sessionKey: String,
        tableName:String,
        orderType: String,
        items: List<PosKotItemEntity>,
        kotNumber: String,
        referenceId: String,
    ) {

        val text = ReceiptFormatter.posKitchen(
            sessionKey = sessionKey,
            tableName = tableName,
            orderType = orderType,
            items = items,
            kotNumber = kotNumber,

            )

        enqueuePrint(PrinterRole.KITCHEN, text,referenceId)
    }


    fun printBitmap(
        role: PrinterRole,
        imagePath: String,
        onResult: (Boolean) -> Unit = {}
    )
    {
        Log.d(
            "IMAGE_TEST",
            "PrinterManager.printBitmap()"
        )
        val config = prefs.getPrinterConfig(role)

        Log.d("IMAGE_TEST", "Config = $config")

        if (config == null) {
            Log.d("IMAGE_TEST", "Config is NULL")
            Log.e("PRINT_BITMAP", "No printer configured for role=$role")
            onResult(false)
            return
        }

        //--------------------------------------
        // Load Bitmap
        //--------------------------------------

        val bitmap = BitmapFactory.decodeFile(imagePath)

        if (bitmap == null) {
            Log.e("IMAGE_TEST", "Unable to decode bitmap: $imagePath")
            onResult(false)
            return
        }

        //--------------------------------------
        // Print
        //--------------------------------------


//        LanPrinter.printBitmap(
//            ip = config.ip,
//            port = config.port,
//            bitmap = bitmap,
//            onResult = onResult
//        )


        when (config.type) {


            PrinterType.LAN -> {

                Log.d(
                    "IMAGE_TEST",
                    "Entering LAN bitmap print"
                )

                if (config.ip.isBlank()) {

                    Log.e(
                        "PRINT_BITMAP",
                        "LAN IP missing"
                    )

                    onResult(false)
                    return
                }


                LanPrinter.printBitmap(
                    ip = config.ip,
                    port = config.port,
                    bitmap = bitmap,
                    onResult = onResult
                )
            }



            PrinterType.BLUETOOTH -> {

                BluetoothPrinter.printBitmap(
                    mac = config.bluetoothAddress,
                    bitmap = bitmap,
                    onResult = onResult
                )
            }


            PrinterType.USB -> {

                val usbManager =
                    context.getSystemService(Context.USB_SERVICE)
                            as android.hardware.usb.UsbManager


                val saved = prefs.getUSBPrinter(role)


                if (saved == null) {
                    Log.e("USB", "No saved USB printer")
                    onResult(false)
                    return
                }


                val (vendorId, productId) = saved


                val device = usbManager.deviceList.values.find {

                    it.vendorId == vendorId &&
                            it.productId == productId

                }


                if (device == null) {

                    Log.e("USB", "Device not found")

                    onResult(false)
                    return
                }


                if (!usbManager.hasPermission(device)) {

                    Log.e("USB", "No permission")

                    onResult(false)
                    return
                }


                try {

                    USBPrinter.printBitmap(
                        context = context,
                        device = device,
                        bitmap = bitmap,
                        onResult = onResult
                    )


                } catch (e: Exception) {

                    Log.e(
                        "USB",
                        "USB Bitmap print failed",
                        e
                    )

                    onResult(false)
                }
            }


            PrinterType.WIFI -> {
                onResult(false)
            }
        }




    }
    // --------------------------------
    // SELECT FORMAT DIRECT PRINT
    // --------------------------------




    fun printTextNew(
        role: PrinterRole,
        order: PrintOrder,
        onResult: (Boolean) -> Unit = {}
    )
    {
        //  Log.e("PRINT_NEW", "Printing for role=$role")

        // Get printer configuration and preferences
        val config = prefs.getPrinterConfig(role)


        if (config == null) {
            Log.e("PPRINTTEST", "No printer configured for role=$role")
            onResult(false)
            return
        }

        // ✅ Select format based on page size
        val size = prefs.getPrinterSize(role) ?: "80mm"

        // ✅ Auto-load outlet info if not provided

        val info = getOutletInfoOrNull()
        if (info == null) {
            onResult(false)
            return
        }




        // ✅ Select format based on printer page size
        val receiptText = when (size) {
            "80mm" -> ReceiptFormatter.billing48(order, info)
            else -> ReceiptFormatter.billing(order, info)
        }


        Log.e(
            "PRINTTEST",
            "\n================= BILL NEWTEXT =================\n$receiptText\n=================================================="
        )

        // ✅ Printing logic (kept same as before)
        when (config.type) {
            PrinterType.BLUETOOTH -> {
                if (config.bluetoothAddress.isBlank()) {
                    Log.e("PRINT_NEW", "Bluetooth address missing")
                    onResult(false)
                    return
                }
                BluetoothPrinter.printText(
                    config.bluetoothAddress,
                    receiptText,
                    onResult
                )
            }

            PrinterType.LAN -> {
                if (config.ip.isBlank()) {
                    Log.e("PRINT_NEW", "LAN IP missing")
                    onResult(false)
                    return
                }
                LanPrinter.printText(
                    config.ip,
                    config.port,
                    receiptText,
                    onResult
                )
            }

            PrinterType.USB -> {

                val usbManager = context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager

                val saved = prefs.getUSBPrinter(role)

                if (saved == null) {
                    Log.e("PRINT_NEW", "No saved USB printer")
                    onResult(false)
                    return
                }

                val (vendorId, productId) = saved

                val device = usbManager.deviceList.values.find {
                    it.vendorId == vendorId && it.productId == productId
                }

                if (device == null) {
                    Log.e("PRINT_NEW", "USB device not found")
                    onResult(false)
                    return
                }

                if (!usbManager.hasPermission(device)) {
                    Log.e("PRINT_NEW", "USB permission denied")
                    onResult(false)
                    return
                }

                // ✅ CORRECT CALL (with device)
                USBPrinter.printText(
                    context,
                    device,

                    receiptText,
                    onResult
                )
            }

            PrinterType.WIFI -> {
                Log.e("PRINT_NEW", "WiFi printing not supported yet")
                onResult(false)
            }
        }
    }

    // --------------------------------
    // NEW PRINT JOB STRATAGY
    // --------------------------------


    fun print(job: PrintJob, onResult: (Boolean) -> Unit = {}) {
        when (job) {



            is PrintJob.SalesReport -> {

                val size = prefs.getPrinterSize(PrinterRole.BILLING) ?: "80mm"
                val width = if (size == "80mm") 48 else 32


                val info = getOutletInfoOrNull()
                if (info == null) {
                    onResult(false)
                    return
                }

                val text = ReceiptFormatter.salesFullReport(
                    state = job.state,
                    info = info,
                    width = width,
                    printMillis = job.printMillis
                )

                printText(PrinterRole.BILLING, text)
            }


            is PrintJob.CategoryWiseSalesReport -> {

                val size = prefs.getPrinterSize(PrinterRole.BILLING) ?: "80mm"
                val width = if (size == "80mm") 48 else 32

                val info = getOutletInfoOrNull()
                if (info == null) {
                    onResult(false)
                    return
                }

                val text = ReceiptFormatter.categoryWiseSalesReport(
                    categorySales = job.categorySales,
                    info = info,
                    width = width,
                    fromMillis = job.fromMillis,
                    toMillis = job.toMillis,
                    printMillis = job.printMillis
                )

                printText(PrinterRole.BILLING, text)
            }

            is PrintJob.SingleCategoryDetail -> {
                val size = prefs.getPrinterSize(PrinterRole.BILLING) ?: "80mm"
                val width = if (size == "80mm") 48 else 32

                val info = getOutletInfoOrNull()
                if (info == null) {
                    onResult(false)
                    return
                }

                val text = ReceiptFormatter.salesBySingleCategory(
                    category = job.category,
                    items = job.items,
                    outletInfo = info,
                    width = width,
                    fromMillis = job.fromMillis,
                    toMillis = job.toMillis,
                    printMillis = job.printMillis
                )

                printText(PrinterRole.BILLING, text)
            }

            // ✅ NEW: Category Summary


            is PrintJob.TotalSalesReport -> {

                val size = prefs.getPrinterSize(PrinterRole.BILLING) ?: "80mm"
                val width = if (size == "80mm") 48 else 32

                val info = getOutletInfoOrNull()
                if (info == null) {
                    onResult(false)
                    return
                }

                val text = ReceiptFormatter.totalSalesReport(
                    beforeDiscount = job.beforeDiscount,
                    discount = job.discount,
                    afterDiscount = job.afterDiscount,
                    tax = job.tax,
                    info = info,
                    width = width,
                    fromMillis = job.fromMillis,
                    toMillis = job.toMillis,
                    printMillis = job.printMillis
                )

                printText(PrinterRole.BILLING, text)
            }


            is PrintJob.CategorySummary -> {

                val size = prefs.getPrinterSize(PrinterRole.BILLING) ?: "80mm"
                val width = if (size == "80mm") 48 else 32

                val info = getOutletInfoOrNull()
                if (info == null) {
                    onResult(false)
                    return
                }

                val text = ReceiptFormatter.salesCategorySummary(
                    category = job.category,
                    totalQty = job.qty,
                    totalAmount = job.amount,
                    info = info,
                    width = width,
                    fromMillis = job.fromMillis,
                    toMillis = job.toMillis,
                    printMillis = job.printMillis
                )

                printText(PrinterRole.BILLING, text)
            }

            is PrintJob.ProductSummary -> {
                val size = prefs.getPrinterSize(PrinterRole.BILLING) ?: "80mm"
                val width = if (size == "80mm") 48 else 32

                val info = getOutletInfoOrNull()
                if (info == null) {
                    onResult(false)
                    return
                }

                val text = ReceiptFormatter.salesProductSummary(
                    product = job.product,
                    qty = job.qty,
                    amount = job.amount,
                    info = info,
                    width = width,
                    fromMillis = job.fromMillis,
                    toMillis = job.toMillis,
                    printMillis = job.printMillis
                )

                printText(PrinterRole.BILLING, text)
            }

            // ✅ ADD THIS BLOCK
            is PrintJob.CategoryProductReport -> {

                val size = prefs.getPrinterSize(PrinterRole.BILLING) ?: "80mm"
                val width = if (size == "80mm") 48 else 32

                val info = getOutletInfoOrNull()
                if (info == null) {
                    onResult(false)
                    return
                }

                val text = ReceiptFormatter.salesCategoryProductList(
                    category = job.category,
                    items = job.items,
                    outletInfo = info,
                    width = width,
                    fromMillis = job.fromMillis,
                    toMillis = job.toMillis,
                    printMillis = job.printMillis
                )

                printText(PrinterRole.BILLING, text)
            }
        }
    }





    // --------------------------------
    // TEST PRINT (already OK)
    // --------------------------------
    fun printTest(
        config: PrinterConfig,
        onResult: (Boolean) -> Unit
    ) {
        val roleLabel = config.role.name

        when (config.type) {

            PrinterType.BLUETOOTH -> {
                //    Log.d("PRINT_BT", "Test BT address='${config.bluetoothAddress}'")
                if (config.bluetoothAddress.isBlank()) {
                    onResult(false)
                    return
                }
                BluetoothPrinter.printTest(
                    config.bluetoothAddress,
                    roleLabel,
                    onResult
                )
            }

            PrinterType.LAN -> {
                if (config.ip.isBlank()) {
                    onResult(false)
                    return
                }
                LanPrinter.printTest(
                    config.ip,
                    config.port,
                    roleLabel,
                    onResult
                )
            }



            PrinterType.USB -> {
                val device = config.usbDevice ?: run {
                    onResult(false)
                    return
                }

                USBPrinter.printTest(
                    context = context,
                    device = device,
                    roleLabel = roleLabel,
                    onResult = onResult
                )
            }






            PrinterType.WIFI -> onResult(false)
        }
    }

    // --------------------------------
    // REAL PRINT (USED BY BUTTON + AUTO)
    // --------------------------------
    fun printText(
        role: PrinterRole,
        text: String,
        paymentMode: String? = null,
        grandTotal: Double? = null,
        onResult: (Boolean) -> Unit = {}
    ) {

        Log.e(
            "PRINTTEST",
            "\n================= printText =================\n$text\n=================================================="
        )
        val config = prefs.getPrinterConfig(role)
        if (config == null) {
            Log.e("PRINTTEST", "No printer configured for role=$role")
            onResult(false)
            return
        }


        val outletInfo = getOutletInfoOrNull()

        val qrBitmap =
            if (role != PrinterRole.KITCHEN && outletInfo?.qrEnabled == true) {

                try {

                    if (
                        paymentMode == "UPI" &&
                        !outletInfo.upiId.isNullOrBlank()
                    ) {
                        // 🔥 UPI QR

                        val upiId = outletInfo.upiId!!
                        val name = outletInfo.upiName ?: "Shop"
                        val encodedName = java.net.URLEncoder.encode(name, "UTF-8")

                        val amount = String.format("%.2f", grandTotal)

                        val upiLink =
                            "upi://pay?pa=$upiId&pn=$encodedName&am=$amount&cu=INR"

                        QrUtils.generateQr(upiLink)

                    } else {
                        // 🔥 fallback QR (or null if you want)
                        QrUtils.loadSavedQr(context)
                    }

                } catch (e: Exception) {
                    Log.e("QR", "QR generation failed", e)
                    null
                }
            } else null






        when (config.type) {

//PRINT QR CODE FORM TEXT

            PrinterType.BLUETOOTH -> {
                if (config.bluetoothAddress.isBlank()) {
                    onResult(false)
                    return
                }

                try {
                    val isKitchen = role == PrinterRole.KITCHEN

                    val logoBitmap = loadSavedLogo()

                    // =============================
                    // 🔥 MAIN LOGIC
                    // =============================
                    if (!isKitchen && (logoBitmap != null || qrBitmap != null)) {
                        BluetoothPrinter.printLogoTextQr(
                            mac = config.bluetoothAddress,
                            logoBitmap = logoBitmap,
                            qrBitmap = qrBitmap,
                            text = text,
                            onResult = onResult
                        )
                    } else {
                        BluetoothPrinter.printText(
                            config.bluetoothAddress,
                            text,
                            onResult
                        )
                    }

                } catch (e: Exception) {
                    BluetoothPrinter.printText(
                        config.bluetoothAddress,
                        text,
                        onResult
                    )
                }
            }

            PrinterType.LAN -> {
                if (config.ip.isBlank()) {
                    onResult(false)
                    return
                }

                try {
                    val isKitchen = role == PrinterRole.KITCHEN

                   // val size = prefs.getPrinterSize(role) ?: "80mm"

                    val logoFile = java.io.File(context.filesDir, "logo.png")

                    val logoBitmap = loadSavedLogo()


                    if (!isKitchen && (logoBitmap != null || qrBitmap != null)) {

                        LanPrinter.printLogoTextQr(
                            ip = config.ip,
                            port = config.port,
                            logoBitmap = logoBitmap,
                            qrBitmap = qrBitmap,
                            text = text,
                            onResult = onResult
                        )

                    } else {

                        LanPrinter.printText(
                            config.ip,
                            config.port,
                            text,
                            onResult
                        )
                    }

                } catch (e: Exception) {
                    LanPrinter.printText(
                        config.ip,
                        config.port,
                        text,
                        onResult
                    )
                }
            }

            PrinterType.USB -> {

                val usbManager = context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager

                val saved = prefs.getUSBPrinter(role)

                if (saved == null) {
                    Log.e("USB", "No saved USB printer")
                    onResult(false)
                    return
                }

                val (vendorId, productId) = saved

                val device = usbManager.deviceList.values.find {
                    it.vendorId == vendorId && it.productId == productId
                }

                if (device == null) {
                    Log.e("USB", "Device not found")
                    onResult(false)
                    return
                }

                if (!usbManager.hasPermission(device)) {
                    Log.e("USB", "No permission")
                    onResult(false)
                    return
                }

                try {
                    val isKitchen = role == PrinterRole.KITCHEN

                    val logoFile = java.io.File(context.filesDir, "logo.png")

                    val logoBitmap = loadSavedLogo()


                  if (!isKitchen && (logoBitmap != null || qrBitmap != null)) {


                        USBPrinter.printLogoTextQrUSB(
                            context,
                            device,
                            logoBitmap,   // can be null
                            qrBitmap,      // 🔥 NEW
                            text,
                            onResult
                        )
                    } else {
                        USBPrinter.printText(
                            context,
                            device,
                            text,
                            onResult
                        )
                    }

                } catch (e: Exception) {
                    Log.e("USB", "Print failed", e)
                    onResult(false)
                }
            }

            PrinterType.WIFI -> onResult(false)
        }
    }









    fun printTextKitchen(
        role: PrinterRole,
        tableName: String,
        sessionKey: String,
        orderType: String,
        items: List<PosKotItemEntity>,
        kotNumber: String,
        onResult: (Boolean) -> Unit = {}
    ) {

        val config = prefs.getPrinterConfig(role)

        if (config == null) {
            Log.e("PRINTTEST", "No printer configured for role=$role")
            onResult(false)
            return
        }

        val text = ReceiptFormatter.posKitchen(
            sessionKey = sessionKey,
            tableName = tableName,
            orderType = orderType,
            items = items,
            kotNumber = kotNumber
        )

        Log.e(
            "PRINTTEST",
            "\n================= KITCHEN RECEIPT =================\n$text\n=================================================="
        )

        when (config.type) {

            PrinterType.BLUETOOTH -> {

                if (config.bluetoothAddress.isBlank()) {
                    onResult(false)
                    return
                }

                BluetoothPrinter.printText(
                    config.bluetoothAddress,
                    text,
                    onResult
                )
            }

            PrinterType.LAN -> {

                if (config.ip.isBlank()) {
                    onResult(false)
                    return
                }

                LanPrinter.printText(
                    config.ip,
                    config.port,
                    text,
                    onResult
                )
            }

            PrinterType.USB -> {

                val usbManager =
                    context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager

                val saved = prefs.getUSBPrinter(role)

                if (saved == null) {
                    Log.e("PRINT_NEW", "No saved USB printer")
                    onResult(false)
                    return
                }

                val (vendorId, productId) = saved

                val device = usbManager.deviceList.values.find {
                    it.vendorId == vendorId &&
                            it.productId == productId
                }

                if (device == null) {
                    Log.e("PRINT_NEW", "USB device not found")
                    onResult(false)
                    return
                }

                if (!usbManager.hasPermission(device)) {
                    Log.e("PRINT_NEW", "USB permission denied")
                    onResult(false)
                    return
                }

                USBPrinter.printText(
                    context,
                    device,
                    text,
                    onResult
                )
            }

            PrinterType.WIFI -> {
                onResult(false)
            }
        }
    }


    private fun loadSavedLogo(): Bitmap? {

        val logoFile = java.io.File(context.filesDir, "logo.png")

        return if (logoFile.exists()) {
            BitmapFactory.decodeFile(logoFile.absolutePath)
        } else null
    }


    private fun getOutletInfoOrNull(): OutletInfo? {
        val outletDao = AppDatabaseProvider.get(context).outletDao()
        val outletEntity = runBlocking { outletDao.getOutlet() }

        return if (outletEntity == null) {
            Log.e("PRINTER", "Outlet not configured")
            null
        } else {
            OutletMapper.fromEntity(outletEntity)
        }
    }


//IMAGE PRINT OF KOT

  suspend  fun enqueueKitchenImage(
        sessionKey: String,
        tableName: String,
        orderType: String,
        items: List<PosKotItemEntity>,
        referenceId: String,
        kotNumber: String,
    ) {

       // Log.d("IMAGE_TEST", "before formatter---------")

        try {
            val bitmap = KitchenBitmapGenerator.generate(
                context = context,
                sessionKey = sessionKey,
                tableName,
                orderType = orderType,
                items = items,
                kotNumber = kotNumber,
            )

         //   Log.d("IMAGE_TEST", "before enqueue---------")

            enqueueImagePrint(
                role = PrinterRole.KITCHEN,
                bitmap = bitmap,
                referenceId = referenceId,
                paymentMode = "",
                grandTotal = 0.0
            )

        } catch (e: Exception) {
            Log.e("IMAGE_TEST", "KOT generation failed", e)
        }
    }





}//END OF CLASS






//fun enqueueImagePrint_old(
//    role: PrinterRole,
//    bitmap: Bitmap,
//    paymentMode: String? = null,
//    grandTotal: Double? = null
//) {
//
//    scope.launch {
//
////            val imageFile = File(
////                context.cacheDir,
////                "receipt_${System.currentTimeMillis()}.png"
////            )
//
////            val imageFile = File(
////                context.cacheDir,
////                "receipt_preview.png"
////            )
//
//        val imageFile = File(
//            context.cacheDir,
//            "kot_preview1.png")
//
////            val imageFile = File(
////                context.cacheDir,
////                "kot_${System.currentTimeMillis()}.png"
////            )
//
////            val fileName = when (role) {
////                PrinterRole.KITCHEN -> "kot_preview.png"      // ✅ KOT file
////                PrinterRole.BILLING -> "receipt_preview.png"  // ✅ BILL file
////                else -> "receipt_preview.png"
////            }
////
////            val imageFile = File(
////                context.cacheDir,
////                fileName
////            )
//
//        imageFile.outputStream().use {
//            bitmap.compress(
//                Bitmap.CompressFormat.PNG,
//                100,
//                it
//            )
//        }
//
//        Log.d(
//            "IMAGE_TEST",
//            "Saved image: ${imageFile.absolutePath}"
//        )
//
//        queueManager.enqueueImage(
//            role = role,
//            imagePath = imageFile.absolutePath,
//            paymentMode = paymentMode,
//            grandTotal = grandTotal
//        )
//    }
//}
