package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Transaction
import com.example.report.CsvGenerator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CsvGeneratorTest {

    @Test
    fun testEmptyTransactionsCsv() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = CsvGenerator.createExpenseReportCsv(context, emptyList())
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertTrue(file.length() > 0)

        // Read content and check BOM
        val bytes = file.readBytes()
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])

        val content = String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        assertTrue(content.contains("شناسه,عنوان,دسته‌بندی,نوع,مبلغ (تومان),تاریخ"))
    }

    @Test
    fun testTransactionsWithPersianTextAndSpecialCharacters() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val transactions = listOf(
            Transaction(
                id = 1,
                amount = 250000,
                category = "خوراکی، میوه",
                description = "خرید نان سنگک، شیر و پنیر\nخط دوم توضیحات",
                date = System.currentTimeMillis(),
                isIncome = false
            ),
            Transaction(
                id = 2,
                amount = 5000000,
                category = "حقوق",
                description = "واریز حقوق پروژه آزادکار",
                date = System.currentTimeMillis(),
                isIncome = true
            )
        )

        val file = CsvGenerator.createExpenseReportCsv(context, transactions)
        assertNotNull(file)
        assertTrue(file!!.exists())

        val text = file.readText(StandardCharsets.UTF_8)
        // Check Persian text exists and special characters are handled
        assertTrue(text.contains("خوراکی، میوه"))
        assertTrue(text.contains("خرید نان سنگک، شیر و پنیر خط دوم توضیحات"))
        assertTrue(text.contains("هزینه"))
        assertTrue(text.contains("درآمد"))
        assertTrue(text.contains("250000"))
        assertTrue(text.contains("5000000"))
    }

    @Test
    fun testLargeTransactionCount() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val largeList = (1..1000).map { i ->
            Transaction(
                id = i.toLong(),
                amount = (i * 1000).toLong(),
                category = "دسته $i",
                description = "توضیح خرید شماره $i",
                date = System.currentTimeMillis(),
                isIncome = i % 2 == 0
            )
        }

        val file = CsvGenerator.createExpenseReportCsv(context, largeList)
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertTrue(file.length() > 10000)
    }
}
