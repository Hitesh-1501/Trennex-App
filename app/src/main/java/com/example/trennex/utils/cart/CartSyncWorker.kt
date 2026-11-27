package com.example.trennex.utils.cart

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.trennex.data.local.cart.AppDatabase
import com.example.trennex.crash.AppCrash
import com.example.trennex.crash.CrashArea
import com.example.trennex.repository.cart.CartRepository
import java.util.concurrent.TimeUnit

class CartSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        if (CartRepository(AppDatabase.getInstance(applicationContext)).drainPending()) Result.success()
        else Result.retry()
    } catch (error: Exception) {
        AppCrash.get().recordUnexpected(CrashArea.ROOM, error)
        Result.retry()
    }

    companion object {
        private const val WORK_NAME = "account_scoped_cart_sync"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<CartSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            // Serialize requests so a slower old write cannot land after a newer cart quantity.
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
