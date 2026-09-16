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

class RecurringTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val recurringDao = database.recurringTransactionDao()
        val transactionDao = database.transactionDao()

        val now = System.currentTimeMillis()

        // 1. پردازش و ثبت خودکار تراکنش‌های سررسید شده
        val dueTransactions = recurringDao.getDueTransactions(now)
        var autoRegisteredCount = 0

        for (recurring in dueTransactions) {
            // ایجاد رکورد واقعی در جدول تراکنش‌ها
            val description = if (recurring.note.isNotBlank()) {
                "${recurring.title} (${recurring.note}) - ثبت خودکار دوره‌ای"
            } else {
                "${recurring.title} - ثبت خودکار دوره‌ای"
            }

            val newTx = Transaction(
                amount = recurring.amount,
                category = recurring.category,
                description = description,
                date = now,
                isIncome = recurring.isIncome
            )
            transactionDao.insert(newTx)

            // محاسبه موعد بعدی
            val nextDate = RecurringTransactionRepository.calculateNextDate(
                recurring.nextExecutionDate,
                recurring.recurrencePeriod
            )

            // بررسی رسیدن به تاریخ پایان
            val stillActive = recurring.endDate == null || nextDate <= recurring.endDate

            recurringDao.updateExecutionDates(
                id = recurring.id,
                nextDate = nextDate,
                lastExecuted = now
            )

            if (!stillActive) {
                recurringDao.setActiveState(recurring.id, false)
            }

            autoRegisteredCount++
            showAutoRegisteredNotification(recurring)
        }

        // 2. بررسی و ارسال اعلان برای تراکنش‌های نزدیک به سررسید (طی ۲۴ تا ۴۸ ساعت آینده)
        val upcomingThreshold = now + (2L * 24 * 60 * 60 * 1000)
        val upcomingList = recurringDao.getUpcomingDueTransactions(now, upcomingThreshold)

        for (upcoming in upcomingList) {
            showUpcomingReminderNotification(upcoming)
        }

        return Result.success()
    }

    private fun showAutoRegisteredNotification(recurring: com.example.data.RecurringTransaction) {
        val channelId = "recurring_transactions_channel"
        createNotificationChannel(channelId, "تراکنش‌های دوره‌ای", "اعلان‌های ثبت خودکار و سررسید دوره‌ای")

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
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("✅ ثبت خودکار $typeTitle")
            .setContentText("«${recurring.title}» به مبلغ ${PersianUtils.formatCurrencyToman(recurring.amount)} ثبت شد.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify((recurring.id + 20000).toInt(), notification)
        }
    }

    private fun showUpcomingReminderNotification(upcoming: com.example.data.RecurringTransaction) {
        val channelId = "recurring_transactions_channel"
        createNotificationChannel(channelId, "تراکنش‌های دوره‌ای", "اعلان‌های ثبت خودکار و سررسید دوره‌ای")

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
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify((upcoming.id + 30000).toInt(), notification)
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
