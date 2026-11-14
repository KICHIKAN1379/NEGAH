package com.kichikan.negah

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class AutomaticBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun backupArchiveManager(): BackupArchiveManager
    }

    override suspend fun doWork(): Result {
        if (BackupArchiveManager.hasPendingRestore(applicationContext)) return Result.success()
        val manager = EntryPointAccessors.fromApplication(
            applicationContext, Dependencies::class.java,
        ).backupArchiveManager()
        return runCatching { manager.createAutomaticBackup() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "negah-automatic-backup"

        /** A short delay lets the app cancel the request if it immediately returns to foreground. */
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<AutomaticBackupWorker>()
                .setInitialDelay(20, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
