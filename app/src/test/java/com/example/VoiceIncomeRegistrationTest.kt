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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceIncomeRegistrationTest {

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
    fun testVoiceIncomeAnalysisAndRegistration() = runBlocking {
        val voiceInput = "۱ میلیون تومان حقوق گرفتم"
        val result = analyzer.analyze(voiceInput)

        // بررسی استخراج هوشمند درآمد
        assertNotNull(result)
        assertEquals(1000000L, result.amount)
        assertTrue(result.isIncome)
        assertEquals("درآمد", result.category)
        assertTrue(result.description.contains("حقوق"))

        // ثبت درآمد در دیتابیس
        val transaction = Transaction(
            amount = result.amount,
            category = result.category,
            description = result.description,
            isIncome = result.isIncome,
            date = System.currentTimeMillis()
        )
        repository.insert(transaction)

        // اعتبارسنجی ثبت موفق درآمد
        val allTransactions = repository.allTransactions.first()
        assertEquals(1, allTransactions.size)
        val saved = allTransactions[0]
        assertEquals(1000000L, saved.amount)
        assertTrue(saved.isIncome)
        assertEquals("درآمد", saved.category)
    }
}
