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
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class BudgetAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).transactionDao()

        // محاسبه شروع ماه
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        val endOfMonth = System.currentTimeMillis()

        // دریافت مجموع خرج ماه
        val totalSpent = dao.getTotalSpent(startOfMonth, endOfMonth) ?: 0

        // دریافت بودجه ذخیره شده
        val prefs = applicationContext.getSharedPreferences("kiseh_prefs", Context.MODE_PRIVATE)
        val budgetLimit = prefs.getLong("budget_limit", 0)

        // اگه از بودجه رد شده
        if (budgetLimit > 0 && totalSpent > budgetLimit) {
            val overBudget = totalSpent - budgetLimit

            // محاسبه چند درصد از بودجه رد شده
            val percentOver = ((overBudget.toFloat() / budgetLimit) * 100).toInt()

            showBudgetAlert(overBudget, percentOver)
        }

        return Result.success()
    }

    private fun showBudgetAlert(overBudget: Long, percentOver: Int) {
        val channelId = "budget_alerts"
        createNotificationChannel(channelId)

        // ساخت Intent برای باز کردن اپ
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ هشدار بودجه!")
            .setContentText("${percentOver}٪ از بودجه ماه رد شدی (${formatMoney(overBudget)} تومان)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(1001, notification)
        }
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "هشدار بودجه"
            val descriptionText = "اعلان هشدار عبور از سقف بودجه"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatMoney(amount: Long): String {
        return NumberFormat.getNumberInstance(Locale("fa", "IR")).format(amount)
    }
}
