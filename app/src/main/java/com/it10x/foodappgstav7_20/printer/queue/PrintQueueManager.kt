package com.it10x.waiterApp1_01.printer.queue

import android.util.Log
import com.it10x.waiterApp1_01.data.printqueue.PrintQueueEntity
import com.it10x.waiterApp1_01.data.PrinterRole
import com.it10x.waiterApp1_01.data.printqueue.PrintQueueDao

import com.it10x.waiterApp1_01.printer.PrinterManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File

class PrintQueueManager private constructor(
    private val dao: PrintQueueDao,
    private val printerManager: PrinterManager
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    private val channels = mutableMapOf<PrinterRole, Channel<PrintQueueEntity>>()

    companion object {

        @Volatile
        private var INSTANCE: PrintQueueManager? = null


        fun getInstance(
            dao: PrintQueueDao,
            printerManager: PrinterManager
        ): PrintQueueManager {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: PrintQueueManager(
                    dao,
                    printerManager
                ).also {
                    INSTANCE = it
                }
            }
        }
    }

    init {

        Log.e(
            "QUEUE_INIT",
            "🔥 PrintQueueManager CREATED ${hashCode()}"
        )

        PrinterRole.values().forEach { role ->
            val channel = Channel<PrintQueueEntity>(Channel.UNLIMITED)
            channels[role] = channel
            startWorker(channel)
        }

       // Log.e("QUEUE_INIT", "🔥 PrintQueueManager CREATED ${System.currentTimeMillis()}")

        scope.launch {
            loadPendingJobs()
        }
    }

    suspend fun enqueue(
        role: PrinterRole,
        text: String,
        paymentMode: String? = null,
        grandTotal: Double? = null,
        referenceId: String,
    ) {

        if (dao.existsByReferenceId(referenceId)) {

            Log.e(
                "PRINT_QUEUE",
                "Duplicate TEXT blocked $referenceId"
            )

            return
        }


        val job = PrintQueueEntity(
            id = UUID.randomUUID().toString(),

            referenceId = referenceId,

            role = role.name,

            jobType = "TEXT",

            text = text,
            imagePath = null,

            paymentMode = paymentMode,
            grandTotal = grandTotal,

            status = "PENDING",
            retryCount = 0,

            createdAt = System.currentTimeMillis()
        )


        try {

            dao.insert(job)

        } catch (e: Exception) {

            Log.e(
                "PRINT_QUEUE",
                "Database duplicate blocked $referenceId",
                e
            )

            return
        }


        channels[role]?.send(job)
    }


    suspend fun enqueueImage(
        referenceId: String,
        role: PrinterRole,
        imagePath: String,
        paymentMode: String? = null,
        grandTotal: Double? = null
    ) {
//        Log.e(
//            "PRINT_REQUEST",
//            """
//    NEW IMAGE PRINT REQUEST
//    referenceId=$referenceId
//    role=$role
//    image=$imagePath
//    time=${System.currentTimeMillis()}
//    """.trimIndent()
//        )




        if (dao.existsByReferenceId(referenceId)) {

            Log.e(
                "PRINT_QUEUE",
                "Duplicate blocked $referenceId"
            )

            return
        }


        val job = PrintQueueEntity(
            id = UUID.randomUUID().toString(),

            referenceId = referenceId,

            role = role.name,
            jobType = "IMAGE",

            text = null,
            imagePath = imagePath,

            paymentMode = paymentMode,
            grandTotal = grandTotal,

            status = "PENDING",
            retryCount = 0,

            createdAt = System.currentTimeMillis()
        )


        try {

            dao.insert(job)

        } catch (e: Exception) {

            Log.e(
                "PRINT_QUEUE",
                "Database duplicate blocked $referenceId",
                e
            )

            return
        }

        channels[role]?.send(job)
    }

    private fun startWorker(channel: Channel<PrintQueueEntity>) {
        scope.launch {
            for (job in channel) {
                processJob(job)
            }
        }
    }


    private suspend fun processJob(job: PrintQueueEntity) {

        val role = PrinterRole.valueOf(job.role)

//        Log.e(
//            "PRINT_PROCESS",
//            """
//    START PRINT
//    jobId=${job.id}
//    referenceId=${job.referenceId}
//    role=${job.role}
//    retry=${job.retryCount}
//    image=${job.imagePath}
//    """.trimIndent()
//        )

        dao.updateStatus(job.id, "PRINTING", job.retryCount)

        try {

            // ⏱ Prevent infinite waiting if printer never responds
//            withTimeout(15000) {
//
//                suspendCancellableCoroutine<Unit> { cont ->
//
//                    when (job.jobType) {
//
//                        "TEXT" -> {
//                            printerManager.printText(
//                                role,
//                                requireNotNull(job.text) {
//                                    "Text job has no text."
//                                },
//                                job.paymentMode,
//                                job.grandTotal
//                            ) {
//                                if (cont.isActive) cont.resume(Unit)
//                            }
//                        }
//
//                        "IMAGE" -> {
//                            printerManager.printBitmap(
//                                role = role,
//                                imagePath = requireNotNull(job.imagePath) {
//                                    "Image job has no image path."
//                                }
//                            ) {
//                                if (cont.isActive) cont.resume(Unit)
//                            }
//                        }
//
//                        else -> {
//                            throw IllegalArgumentException(
//                                "Unknown job type ${job.jobType}"
//                            )
//                        }
//                    }
//                }
//            }

            withTimeout(30000) {

                suspendCancellableCoroutine<Unit> { cont ->

                    when (job.jobType) {

                        "TEXT" -> {
                            printerManager.printText(
                                role,
                                requireNotNull(job.text),
                                job.paymentMode,
                                job.grandTotal
                            ) {
                                if (cont.isActive) {
                                    cont.resume(Unit)
                                }
                            }
                        }

                        "IMAGE" -> {
                            printerManager.printBitmap(
                                role = role,
                                imagePath = requireNotNull(job.imagePath)
                            ) {
                                if (cont.isActive) {
                                    cont.resume(Unit)
                                }
                            }
                        }
                    }
                }
            }

            // ✅ Success → remove from queue
            // ✅ Success → remove from queue
            dao.delete(job.id)

// ✅ Delete printed image file
            job.imagePath?.let { path ->
                try {
                    val deleted = File(path).delete()

                    Log.d(
                        "PRINT_QUEUE",
                        "IMAGE DELETE path=$path success=$deleted"
                    )

                } catch (e: Exception) {

                    Log.e(
                        "PRINT_QUEUE",
                        "Failed to delete image file $path",
                        e
                    )
                }
            }

            Log.d("PRINT_QUEUE", "SUCCESS ${job.id}")



        } catch (e: Exception) {

            Log.e("PRINT_QUEUE", "FAILED ${job.id}: ${e.message}")

            val newRetry = job.retryCount + 1

            // 🔁 Retry logic (max 3 attempts)
            if (newRetry <= 1) {

                delay(3000)

                dao.updateStatus(
                    job.id,
                    "PENDING",
                    newRetry
                )

                channels[role]?.send(
                    job.copy(
                        retryCount = newRetry
                    )
                )

            } else {

                // ❌ Permanent failure
                dao.updateStatus(job.id, "FAILED", newRetry)

                Log.e("PRINT_QUEUE", "GAVE UP ${job.id}")
            }
        }

        Log.d("PRINT_QUEUE", "END ${job.id}")
    }




//    private suspend fun loadPendingJobs() {
//        val jobs = dao.getPending()
//
//        jobs.forEach { job ->
//            val role = PrinterRole.valueOf(job.role)
//            channels[role]?.send(job)
//        }
//    }


    private suspend fun loadPendingJobs() {

        val jobs = dao.getPending()

        jobs.forEach { job ->

            val updated = dao.updateStatusIfPending(
                id = job.id,
                oldStatus = "PENDING",
                newStatus = "QUEUED"
            )

            if (updated == 1) {
                val role = PrinterRole.valueOf(job.role)
                channels[role]?.send(
                    job.copy(status = "QUEUED")
                )
            }
        }
    }



}