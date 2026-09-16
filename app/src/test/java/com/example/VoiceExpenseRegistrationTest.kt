package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.analyzer.ExpenseAnalyzer
import com.example.data.AppDatabase
import com.example.data.Transaction
import com.example.data.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceExpenseRegistrationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private val analyzer = ExpenseAnalyzer()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .allowMainThreadQueries()
            .build()
        repository = TransactionRepository(db.transactionDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testVoiceExpenseAnalysisAndRegistration() = runBlocking {
        val voiceInput = "امروز ۵۰۰۰۰ تومان خرید کردم"
        val result = analyzer.analyze(voiceInput)

        // بررسی استخراج هوشمند
        assertNotNull(result)
        assertEquals(50000L, result.amount)
        assertFalse(result.isIncome)
        assertTrue(result.description.isNotBlank())

        // ثبت در دیتابیس
        val transaction = Transaction(
            amount = result.amount,
            category = result.category,
            description = result.description,
            isIncome = result.isIncome,
            date = System.currentTimeMillis()
        )
        repository.insert(transaction)

        // اعتبارسنجی ثبت موفق
        val allTransactions = repository.allTransactions.first()
        assertEquals(1, allTransactions.size)
        val saved = allTransactions[0]
        assertEquals(50000L, saved.amount)
        assertFalse(saved.isIncome)
    }
}
