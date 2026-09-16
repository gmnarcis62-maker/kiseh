package com.example.report

import android.content.Context
import android.os.Environment
import com.example.data.Transaction
import com.example.ui.util.PersianUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvGenerator {

    fun createExpenseReportCsv(
        context: Context,
        transactions: List<Transaction>
    ): File? {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val file = File(dir, "kiseh_report_$timestamp.csv")

            val outputStream = FileOutputStream(file)
            // افزودن BOM برای پشتیبانی درست از UTF-8 در اکسل
            outputStream.write(0xEF)
            outputStream.write(0xBB)
            outputStream.write(0xBF)

            val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
            
            // هدر ستون‌ها
            writer.write("شناسه,عنوان,دسته‌بندی,نوع,مبلغ (تومان),تاریخ\n")

            // ردیف‌ها
            for (t in transactions) {
                val type = if (t.isIncome) "درآمد" else "هزینه"
                val date = PersianUtils.formatPersianDate(t.date)
                
                // پاکسازی ویرگول‌ها از عنوان برای جلوگیری از تداخل در CSV
                val safeTitle = t.description.replace(",", "،").replace("\n", " ")
                val safeCategory = t.category.replace(",", "،").replace("\n", " ")
                
                writer.write("${t.id},$safeTitle,$safeCategory,$type,${t.amount},$date\n")
            }

            writer.flush()
            writer.close()
            outputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
