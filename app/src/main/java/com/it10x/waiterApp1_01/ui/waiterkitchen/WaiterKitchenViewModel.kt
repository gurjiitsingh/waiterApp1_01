package com.it10x.waiterApp1_01.ui.waiterkitchen

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.compose.foundation.lazy.items

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.it10x.waiterApp1_01.data.PrinterRole
import com.it10x.waiterApp1_01.data.pos.AppDatabaseProvider
import com.it10x.waiterApp1_01.data.pos.entities.PosCartEntity
import com.it10x.waiterApp1_01.data.pos.entities.PosKotBatchEntity
import com.it10x.waiterApp1_01.data.pos.entities.PosKotItemEntity
import com.it10x.waiterApp1_01.data.pos.repository.CartRepository
import com.it10x.waiterApp1_01.data.pos.repository.POSOrdersRepository
import com.it10x.waiterApp1_01.data.pos.usecase.KotToBillUseCase
import com.it10x.waiterApp1_01.printer.PrinterManager
import com.it10x.waiterApp1_01.ui.cart.CartViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import com.it10x.waiterApp1_01.data.pos.repository.KotRepository



import com.google.firebase.firestore.FirebaseFirestore
import com.it10x.waiterApp1_01.data.online.models.waiter.WaiterOrder
import com.it10x.waiterApp1_01.data.online.models.waiter.WaiterOrderItem

import com.it10x.waiterApp1_01.data.pos.repository.WaiterKitchenRepository
import com.it10x.waiterApp1_01.data.pos.repository.WaiterLanRepository

import kotlinx.coroutines.tasks.await
class WaiterKitchenViewModel(
    app: Application,
    private val tableId: String,
    private val tableName: String,
    private val sessionId: String,
    private val orderType: String,
    private val repository: POSOrdersRepository,
    private val waiterKitchenRepository: WaiterKitchenRepository,
    private val cartViewModel: CartViewModel,
) : AndroidViewModel(app) {

    private var kotPrintJob: Job? = null
    private val pendingKotItems = mutableListOf<PosKotItemEntity>( )
    private var pendingBatchId: String? = null
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading
    private val kotItemDao =
        AppDatabaseProvider.get(app).kotItemDao()

    private val _sendSuccess = MutableStateFlow(false)
    val sendSuccess: StateFlow<Boolean> = _sendSuccess

    private val kotToBillUseCase =
        KotToBillUseCase(kotItemDao)

    private val kotRepository = KotRepository(
        AppDatabaseProvider.get(app).kotBatchDao(),
        AppDatabaseProvider.get(app).kotItemDao(),
        AppDatabaseProvider.get(app).tableDao()
    )

    val kotItems: StateFlow<List<PosKotItemEntity>> =
        kotItemDao.getAllKotItems()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private var isProcessing = false


    private val db =
        AppDatabaseProvider.get(app)
    private val waiterLanRepository =
        WaiterLanRepository(
            posTerminalDao = db.posTerminalDao()
        )

    private val cartRepository = CartRepository(
        AppDatabaseProvider.get(app).cartDao(),
        AppDatabaseProvider.get(app).tableDao()
    )

    private val printerManager =
        PrinterManager.getInstance(app.applicationContext)

    fun waiterCartTo_FireStore_Bill(
        cartList: List<PosCartEntity>,
        tableNo: String,
        deviceId: String,
        deviceName: String?,
        role: String
    ) {

        Log.d(
            "WaiterKitchenVM",
            "WaiterKitchenViewModel is sending Waiter kot--------------"
        )

        // ✅ Prevent double click immediately
        if (isProcessing) return
        isProcessing = true

        viewModelScope.launch {

            val dao =
                AppDatabaseProvider
                    .get(getApplication())
                    .cartDao()

            val latestCart = try {

                dao.getCartItemsByTableId(tableNo)
                    .first()

            } catch (e: Exception) {

                Log.e(
                    "WaiterKitchenVM",
                    "❌ Failed to load cart from DB: ${e.message}",
                    e
                )

                emptyList()
            }


            if (latestCart.isEmpty()) {

                Log.w(
                    "WaiterKitchenVM",
                    "⚠️ No items found in DB for table=$tableNo"
                )

                // ✅ unlock
                isProcessing = false
                return@launch
            }


            _loading.value = true


            try {

                latestCart.forEach {

                    Log.d(
                        "WAITER_SEND",
                        """
                    product=${it.name}
                    createdById=${it.createdById}
                    createdByName=${it.createdByName}
                    """.trimIndent()
                    )
                }


//                val success = withContext(Dispatchers.IO) {
//                    waiterKitchenRepository.sendOrderToFireStore(
//                        cartList = latestCart,
//                        tableNo = tableNo,
//                        tableName = tableName,
//                        sessionId = sessionId,
//
//                        orderType = orderType,
//                        deviceId = deviceId,
//                        deviceName = deviceName
//                    )
//                }


//                if (!success) {
//                    Log.e(
//                        "WaiterKitchenVM",
//                        "❌ Firestore upload failed"
//                    )
//                    return@launch
//                }


//                val success = withContext(Dispatchers.IO) {
//                    waiterLanRepository.pingDesktop()
//                }


                // =================================================
                // SEND TO DESKTOP + SAVE LOCAL BILL
                // =================================================

                val desktopAccepted =
                    withContext(Dispatchers.IO) {

                        waiterLanRepository.sendOrderToDesktop(
                            cartList = latestCart,
                            tableNo = tableNo,
                            tableName = tableName,
                            sessionId = sessionId,
                            orderType = orderType,
                            deviceId = deviceId,
                            deviceName = deviceName
                        )
                    }


                if (!desktopAccepted) {

                    Log.e(
                        "WAITER_LAN",
                        "❌ Desktop did not accept KOT — local bill not updated"
                    )

                    return@launch
                }


                Log.d(
                    "WAITER_LAN",
                    "✅ Desktop accepted KOT — saving local bill"
                )


                // =================================================
                // SAVE LOCAL BILL
                // =================================================

                val localBillSaved =
                    withContext(Dispatchers.IO) {

                        saveWaiterKotLocally(
                            orderType = orderType,
                            sessionId = sessionId,
                            tableNo = tableNo,
                            tableName = tableName,
                            cartItems = latestCart,
                            deviceId = deviceId,
                            deviceName = deviceName,
                            appVersion = "1.0",
                            role = role
                        )
                    }


                if (!localBillSaved) {

                    Log.e(
                        "WAITER_LAN",
                        "❌ Local bill save failed — cart will NOT be cleared"
                    )

                    return@launch
                }


                Log.d(
                    "WAITER_LAN",
                    "✅ Local bill saved successfully"
                )


                // =================================================
                // CLEAR CART ONLY AFTER BOTH SUCCEED
                // =================================================

                repository.clearCart(
                    orderType,
                    tableNo
                )

                cartRepository.syncCartCount(
                    tableNo
                )


                Log.d(
                    "WaiterKitchenVM",
                    "✅ KOT sent successfully and local bill updated"
                )


                _sendSuccess.value = true


            } catch (e: Exception) {

                Log.e(
                    "WaiterKitchenVM",
                    "❌ Error in waiterCartToBill",
                    e
                )

            } finally {

                _loading.value = false

                // ✅ always unlock
                isProcessing = false
            }
        }
    }

    fun waiterCartTo_FireStore_Bill_old(
        cartList: List<PosCartEntity>,
        tableNo: String,
        deviceId: String,
        deviceName: String?,
        role: String
    ) {
        Log.d("WaiterKitchenVM","WaiterKitchenViewModel is sending Waiter kot--------------")
        // ✅ Prevent double click immediately
        if (isProcessing) return
        isProcessing = true

        viewModelScope.launch {

            val dao = AppDatabaseProvider.get(getApplication()).cartDao()

            val latestCart = try {
                dao.getCartItemsByTableId(tableNo).first()
            } catch (e: Exception) {
                Log.e(
                    "WaiterKitchenVM",
                    "❌ Failed to load cart from DB: ${e.message}",
                    e
                )
                emptyList()
            }

            if (latestCart.isEmpty()) {
                Log.w(
                    "WaiterKitchenVM",
                    "⚠️ No items found in DB for table=$tableNo"
                )

                // ✅ unlock
                isProcessing = false
                return@launch
            }

            _loading.value = true

            try {

                latestCart.forEach {
                    Log.d(
                        "WAITER_SEND",
                        """
        product=${it.name}
        createdById=${it.createdById}
        createdByName=${it.createdByName}
        """.trimIndent()
                    )
                }

//                val success = withContext(Dispatchers.IO) {
//                    waiterKitchenRepository.sendOrderToFireStore(
//                        cartList = latestCart,
//                        tableNo = tableNo,
//                        tableName = tableName,
//                        sessionId = sessionId,
//
//                        orderType = orderType,
//                        deviceId = deviceId,
//                        deviceName = deviceName
//                    )
//                }


                //                if (!success) {
//                    Log.e(
//                        "WaiterKitchenVM",
//                        "❌ Firestore upload failed"
//                    )
//                    return@launch
//                }

//                val success = withContext(Dispatchers.IO) {
//                    waiterLanRepository.pingDesktop()
//                }

                val success = withContext(Dispatchers.IO) {

                    val success = waiterLanRepository.sendOrderToDesktop(
                        cartList = cartList,
                        tableNo = tableNo,
                        tableName = tableName,
                        sessionId = sessionId,
                        orderType = orderType,
                        deviceId = deviceId,
                        deviceName = deviceName
                    )

                    if (success) {

                        Log.d(
                            "WAITER_LAN",
                            "Desktop accepted KOT — saving local bill"
                        )

                        saveWaiterKotLocally(
                            orderType = orderType,
                            sessionId = sessionId,
                            tableNo = tableNo,
                            tableName = tableName,
                            cartItems = cartList,
                            deviceId = deviceId,
                            deviceName = deviceName,
                            appVersion = "1.0",
                            role = "WAITER"
                        )

                    } else {

                        Log.e(
                            "WAITER_LAN",
                            "Desktop did not accept KOT — local bill not updated"
                        )
                    }
                }



                Log.d(
                    "WaiterKitchenVM",
                    "✅ Desktop LAN KOT sent successfully"
                )





                repository.clearCart(orderType, tableNo)
                cartRepository.syncCartCount(tableNo)

                Log.d(
                    "WaiterKitchenVM",
                    "✅ KOT send succefully"
                )

                _sendSuccess.value = true

            } catch (e: Exception) {

                Log.e(
                    "WaiterKitchenVM",
                    "❌ Error in waiterCartToBill",
                    e
                )

            } finally {

                _loading.value = false

                // ✅ always unlock
                isProcessing = false
            }
        }
    }




    fun posFireStoreToWaiterBill(
        cartList: List<PosCartEntity>,
        tableNo: String,
        deviceId: String,
        deviceName: String?,
        role : String
    ) {
        if (isProcessing) return   // 🔥 Prevent duplicate presses

        viewModelScope.launch {
            // Always get fresh items from DB
            val dao = AppDatabaseProvider.get(getApplication()).cartDao()
            val latestCart = try {
                dao.getCartItemsByTableId(tableNo).first()
            } catch (e: Exception) {
                Log.e("WaiterKitchenVM", "❌ Failed to load cart from DB: ${e.message}", e)
                emptyList()
            }

            if (latestCart.isEmpty()) {
                Log.w("WaiterKitchenVM", "⚠️ No items found in DB for table=$tableNo")
                return@launch
            }


            if (isProcessing) return@launch
            isProcessing = true
            _loading.value = true

            try {
                val billSaved = saveCartItemToBillView(
                    orderType = orderType,
                    sessionId = sessionId,
                    tableNo = tableNo,
                    cartItems = cartList,
                    deviceId = deviceId,
                    deviceName = deviceName,
                    appVersion = "appVersion",
                    role = role,
                )

                if (!billSaved) {
                    Log.e("WaiterKitchenVM", "❌ Bill save failed")
                    return@launch
                }

               // repository.clearCart(orderType, tableNo)
                cartRepository.syncCartCount(tableNo)

                Log.d("WaiterKitchenVM", "✅ Firestore + Bill saved successfully")

                // ✅ MOVE HERE
                _sendSuccess.value = true

            } catch (e: Exception) {
                Log.e("WaiterKitchenVM", "❌ Error in waiterCartToBill", e)
            } finally {

                // ⭐ VERY IMPORTANT
                _loading.value = false
                isProcessing = false
            }
        }





    }




    private suspend fun saveCartItemToBillView(
        orderType: String,
        sessionId: String,
        tableNo: String?,
        cartItems: List<PosCartEntity>,
        deviceId: String,
        deviceName: String?,
        appVersion: String?,
        role: String,
    ): Boolean = withContext(Dispatchers.IO) {

        val tableNo = tableNo?: "";
        try {
            val db = AppDatabaseProvider.get(printerManager.appContext())
            val kotBatchDao = db.kotBatchDao()
            val kotItemDao = db.kotItemDao()

            val batchId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            repository.markAllSent(tableNo)

            val batch = PosKotBatchEntity(
                id = batchId,
                kotNumber = "",
                sessionId = sessionId,
                tableNo = tableNo,
                tableName = tableName,
                orderType = orderType,
                deviceId = deviceId,
                deviceName = deviceName,
                appVersion = appVersion,
                createdAt = now,
                sentBy = "WAITRER",
                syncStatus = "DONE",
                lastSyncedAt = null
            )

            kotBatchDao.insert(batch)

            Log.d("SYNC", "--- WaiterKitchenViewmodel----1")

//            cartItems.forEach {
//                Log.d(
//                    "KOT_DEBUG",
//                    "Item: ${it.name} qty=${it.quantity}"
//                )
//            }

            val items = cartItems.map { cart ->
          //    Log.d("KOT_DEBUG", "Saving item: ${cart.name} qty=${cart.quantity}")
                PosKotItemEntity(
                    id = UUID.randomUUID().toString(),
                    kotNumber = "",
                    sessionId = sessionId,
                    kotBatchId = batchId,
                    tableNo = tableNo,
                    tableName = tableName,
                    productId = cart.productId,
                    name = cart.name,
                    productMode = cart.productMode ,
                    currentStock = cart.currentStock,
                    categoryId = cart.categoryId,
                    categoryName = cart.categoryName,
                    createdById = cart.createdById,
                    createdByName = cart.createdByName,

                    parentId = cart.parentId,
                    isVariant = cart.isVariant,
                    basePrice = cart.basePrice,
                    finalPrice = cart.finalPrice,
                    modifierTotal = cart.modifierTotal,
                    quantity = cart.quantity,
                    taxRate = cart.taxRate,
                    taxType = cart.taxType,
                    note = cart.note,
                    modifiersJson = cart.modifiersJson,
                    kitchenPrinted = true,
                    status = "DONE",
                    createdAt = now
                )
            }

            kotRepository.insertItemsInBill(tableNo, items,role)


                kotRepository.markDoneAll(tableNo)
                kotRepository.syncKinchenCount(tableNo)
                kotRepository.syncBillCount(tableNo)


            true

        } catch (e: Exception) {
            Log.e("KOT", "❌ Failed to save KOT", e)
            false
        }
    }



    fun getPendingItems(orderRef: String, orderType: String): Flow<List<PosKotItemEntity>> {


        return if (orderType == "DINE_IN") {
            kotItemDao.getPendingItemsForTable(orderRef)
        } else {
            kotItemDao.getPendingItemsForTable(orderType)
          //  kotItemDao.getPendingItemsForSession(orderRef)
        }
    }
     // ✅ POS signal: kitchen completed for table
    fun resetSendSuccess() {
        _sendSuccess.value = false
    }



    private suspend fun saveWaiterKotLocally(
        orderType: String,
        sessionId: String,
        tableNo: String,
        tableName: String,
        cartItems: List<PosCartEntity>,
        deviceId: String,
        deviceName: String?,
        appVersion: String,
        role: String
    ): Boolean = withContext(Dispatchers.IO) {

        try {

            if (cartItems.isEmpty()) {
                Log.w(
                    "WAITER_LOCAL_KOT",
                    "Cart is empty. Nothing to save."
                )
                return@withContext false
            }

            val db =
                AppDatabaseProvider.get(
                    printerManager.appContext()
                )

            val kotBatchDao =
                db.kotBatchDao()

            val kotItemDao =
                db.kotItemDao()

            // -------------------------------------------------
            // GENERATE LOCAL BATCH ID
            // -------------------------------------------------

            val batchId =
                UUID.randomUUID().toString()

            val now =
                System.currentTimeMillis()

            // -------------------------------------------------
            // GENERATE KOT NUMBER
            // -------------------------------------------------

            val kotNumber =
                kotRepository.generateNextKotNumber()

            // -------------------------------------------------
            // CREATE BATCH
            // -------------------------------------------------

            val batch =
                PosKotBatchEntity(

                    id =
                        batchId,

                    kotNumber =
                        kotNumber,

                    sessionId =
                        sessionId,

                    tableNo =
                        tableNo,

                    tableName =
                        tableName,

                    orderType =
                        orderType,

                    deviceId =
                        deviceId,

                    deviceName =
                        deviceName,

                    appVersion =
                        appVersion,

                    createdAt =
                        now,

                    sentBy =
                        "WAITER",

                    syncStatus =
                        "DONE",

                    lastSyncedAt =
                        null
                )

            kotBatchDao.insert(batch)

            Log.d(
                "WAITER_LOCAL_KOT",
                "Batch saved: $batchId"
            )

            // -------------------------------------------------
            // CREATE ITEMS
            // -------------------------------------------------

            val items =
                cartItems.map { cart ->

                    PosKotItemEntity(

                        id =
                            UUID.randomUUID().toString(),

                        kotNumber =
                            kotNumber,

                        sessionId =
                            sessionId,

                        kotBatchId =
                            batchId,

                        tableNo =
                            tableNo,

                        tableName =
                            tableName,

                        productId =
                            cart.productId,

                        name =
                            cart.name,

                        productMode =
                            cart.productMode,

                        currentStock =
                            cart.currentStock,

                        categoryId =
                            cart.categoryId,

                        categoryName =
                            cart.categoryName,

                        createdById =
                            cart.createdById,

                        createdByName =
                            cart.createdByName,

                        parentId =
                            cart.parentId,

                        isVariant =
                            cart.isVariant,

                        basePrice =
                            cart.basePrice,

                        finalPrice =
                            cart.finalPrice,

                        modifierTotal =
                            cart.modifierTotal,

                        quantity =
                            cart.quantity,

                        taxRate =
                            cart.taxRate,

                        taxType =
                            cart.taxType,

                        note =
                            cart.note,

                        modifiersJson =
                            cart.modifiersJson,

                        kitchenPrintReq =
                            cart.kitchenPrintReq,

                        kitchenPrinted =
                            true,

                        status =
                            "DONE",

                        createdAt =
                            now
                    )
                }

            // -------------------------------------------------
            // SAVE ITEMS TO BILL VIEW
            // -------------------------------------------------

            kotRepository.insertItemsInBill(
                tableNo,
                items,
                role
            )

            // -------------------------------------------------
            // UPDATE LOCAL COUNTERS
            // -------------------------------------------------

            kotRepository.markDoneAll(
                tableNo
            )

            kotRepository.syncKinchenCount(
                tableNo
            )

            kotRepository.syncBillCount(
                tableNo
            )

            Log.d(
                "WAITER_LOCAL_KOT",
                "===================================="
            )

            Log.d(
                "WAITER_LOCAL_KOT",
                "LOCAL WAITER KOT SAVED"
            )

            Log.d(
                "WAITER_LOCAL_KOT",
                "KOT: $kotNumber"
            )

            Log.d(
                "WAITER_LOCAL_KOT",
                "Table: $tableNo"
            )

            Log.d(
                "WAITER_LOCAL_KOT",
                "Items: ${items.size}"
            )

            Log.d(
                "WAITER_LOCAL_KOT",
                "===================================="
            )

            true

        } catch (e: Exception) {

            Log.e(
                "WAITER_LOCAL_KOT",
                "Failed to save local waiter KOT",
                e
            )

            false
        }
    }



}





