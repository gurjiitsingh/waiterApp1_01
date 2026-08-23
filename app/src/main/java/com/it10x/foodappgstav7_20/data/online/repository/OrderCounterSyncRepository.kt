package com.it10x.waiterApp1_01.data.online.repository


import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.it10x.waiterApp1_01.data.pos.AppDatabase
import com.it10x.waiterApp1_01.data.pos.entities.OrderCounterEntity
import kotlinx.coroutines.tasks.await

class OrderCounterSyncRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore
) {

    suspend fun syncLastOrderSerialNo() {

        val snapshot = firestore
            .collection("settings")
            .document("orderCounter")
            .get()
            .await()

        // First install and no document exists yet
        if (!snapshot.exists()) {
            Log.d("ORDER_COUNTER", "No counter found in Firestore")
            return
        }

        val counter = OrderCounterEntity(
            invoiceSerialNo = snapshot.getLong("invoiceSerialNo") ?: 0L,
            updatedAt = System.currentTimeMillis()
        )



        db.orderCounterDao().save(counter)

        Log.d(
            "ORDER_COUNTER",
            "Downloaded order serial = ${counter.invoiceSerialNo}"
        )
    }
}

