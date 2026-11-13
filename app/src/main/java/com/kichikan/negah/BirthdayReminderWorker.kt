package com.kichikan.negah

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kichikan.negah.domain.member.BirthdayReminder
import com.kichikan.negah.domain.member.BirthdayReminderCalculator
import com.kichikan.negah.domain.member.MemberRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Notifies the coach 3 days before a member's birthday. Each reminder is sent
 * at most once per birthday (tracked locally) and is only marked as sent when
 * the notification was really posted, so granting the permission later still
 * delivers a pending reminder.
 */
class BirthdayReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun memberRepository(): MemberRepository
    }

    override suspend fun doWork(): Result {
        val repository = EntryPointAccessors
            .fromApplication(applicationContext, Dependencies::class.java)
            .memberRepository()
        val members = repository.observeActiveMembers().first()
        val reminders = BirthdayReminderCalculator.upcoming(members, LocalDate.now())
        if (reminders.isEmpty() || !canNotify(applicationContext)) return Result.success()

        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sent = prefs.getStringSet(KEY_SENT, emptySet()).orEmpty().toMutableSet()
        ensureChannel(applicationContext)
        reminders.forEach { reminder ->
            val key = "${reminder.memberId}-${reminder.birthdayDate}"
            if (key in sent) return@forEach
            if (notify(applicationContext, reminder)) sent += key
        }
        prefs.edit().putStringSet(KEY_SENT, sent).apply()
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun notify(context: Context, reminder: BirthdayReminder): Boolean {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (reminder.daysUntil == 1) {
            "فردا تولد ${reminder.displayName} است"
        } else {
            "تولد ${reminder.displayName} تا ${reminder.daysUntil} روز دیگر است"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("یادآوری تولد")
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(reminder.memberId.hashCode(), notification)
        }.isSuccess
    }

    companion object {
        private const val CHANNEL_ID = "birthday_reminders"
        private const val PREFS_NAME = "birthday_reminders"
        private const val KEY_SENT = "sent"
        private const val PERIODIC_NAME = "birthday-reminders-daily"
        private const val ON_LAUNCH_NAME = "birthday-reminders-on-launch"

        fun canNotify(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        private fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "یادآوری تولد", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }

        /** Daily check plus an immediate check when the app opens. */
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<BirthdayReminderWorker>(1, TimeUnit.DAYS).build(),
            )
            workManager.enqueueUniqueWork(
                ON_LAUNCH_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<BirthdayReminderWorker>().build(),
            )
        }
    }
}
