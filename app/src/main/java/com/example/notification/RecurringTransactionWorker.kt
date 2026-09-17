package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.RecurrencePeriod
import com.example.data.RecurringTransactionRepository
import com.example.data.Transaction
import com.example.ui.util.PersianUtils
import java.util.Calendar

class RecurringTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val PREFS_NAME = "recurring_worker_prefs"
        private const val KEY_LAST_UPCOMING_NOTIFY = "last_upcoming_notify_day"
        private const val MAX_CATCH_UP = 60  // حداکثر تعداد ثبت جامانده در یک اجرا
    }

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val recurringDao = database.recurringTransactionDao()
        val transactionDao = database.transactionDao()

        val now = System.currentTimeMillis()

        // ============ ۱. پردازش و ثبت خودکار تراکنش‌های سررسید شده (با پشتیبانی از جامانده‌ها) ============
        val dueTransactions = recurringDao.getDueTransactions(now)

        for (recurring in dueTransactions) {
            var currentExecution = recurring.nextExecutionDate
            var lastExecuted = currentExecution
            var caught = 0
            var stillActive = true

            // ثبت تمام سررسیدهای جامانده
            while (currentExecution <= now && stillActive && caught < MAX_CATCH_UP) {
                val description = buildString {
                    append(recurring.title)
                    if (recurring.note.isNotBlank()) {
                        append(" (")
                        append(recurring.note)
                        append(")")
                    }
                    append(" - ثبت خودکار دوره‌ای")
                }

                val newTx = Transaction(
                    amount = recurring.amount,
                    category = recurring.category,
                    description = description,
                    date = currentExecution,  // ← تاریخ واقعی سررسید، نه الان
                    isIncome = recurring.isIncome
                )
                transactionDao.insert(newTx)
                lastExecuted = currentExecution
                caught++

                val nextDate = RecurringTransactionRepository.calculateNextDate(
                    currentExecution,
                    recurring.recurrencePeriod
                )

                if (recurring.endDate != null && nextDate > recurring.endDate) {
                    stillActive = false
                    currentExecution = nextDate
                    break
                }

                currentExecution = nextDate
            }

            // اگر بیش از حد مجاز جامانده بود، از ادامه صرف‌نظر کن
            if (caught >= MAX_CATCH_UP && currentExecution <= now) {
                currentExecution = RecurringTransactionRepository.calculateNextDate(
                    now,
                    recurring.recurrencePeriod
                )
            }

            recurringDao.updateExecutionDates(
                id = recurring.id,
                nextDate = currentExecution,
                lastExecuted = lastExecuted
            )

            if (!stillActive) {
                recurringDao.setActiveState(recurring.id, false)
            }

            if (caught > 0) {
                showAutoRegisteredNotification(recurring, caught)
            }
        }

        // ============ ۲. بررسی و ارسال اعلان سررسید نزدیک (فقط یک بار در روز) ============
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotifyDay = prefs.getLong(KEY_LAST_UPCOMING_NOTIFY, 0L)
        val todayStart = getTodayStartMillis()

        if (lastNotifyDay < todayStart) {
            val upcomingThreshold = now + (2L * 24 * 60 * 60 * 1000)
            val upcomingList = recurringDao.getUpcomingDueTransactions(now, upcomingThreshold)

            for (upcoming in upcomingList) {
                showUpcomingReminderNotification(upcoming)
            }

            prefs.edit().putLong(KEY_LAST_UPCOMING_NOTIFY, now).apply()
        }

        return Result.success()
    }

    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun showAutoRegisteredNotification(
        recurring: com.example.data.RecurringTransaction,
        count: Int
    ) {
        val channelId = "recurring_transactions_channel"
        createNotificationChannel(
            channelId,
            "تراکنش‌های دوره‌ای",
            "اعلان‌های ثبت خودکار و سررسید دوره‌ای"
        )

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            recurring.id.toInt() + 2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val typeTitle = if (recurring.isIncome) "درآمد دوره‌ای" else "هزینه دوره‌ای"
        val countText = if (count > 1) " ($count بار)" else ""

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("✅ ثبت خودکار $typeTitle$countText")
            .setContentText("«${recurring.title}» به مبلغ ${PersianUtils.formatCurrencyToman(recurring.amount)} ثبت شد.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext)
                .notify((recurring.id + 20000).toInt(), notification)
        }
    }

    private fun showUpcomingReminderNotification(upcoming: com.example.data.RecurringTransaction) {
        val channelId = "recurring_transactions_channel"
        createNotificationChannel(
            channelId,
            "تراکنش‌های دوره‌ای",
            "اعلان‌های ثبت خودکار و سررسید دوره‌ای"
        )

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            upcoming.id.toInt() + 3000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ یادآوری سررسید نزدیک")
            .setContentText("موعد «${upcoming.title}» (${PersianUtils.formatCurrencyToman(upcoming.amount)}) نزدیک است.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext)
                .notify((upcoming.id + 30000).toInt(), notification)
        }
    }

    private fun createNotificationChannel(channelId: String, name: String, descriptionText: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}