package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.RecurrencePeriod
import com.example.data.RecurringTransaction
import com.example.data.RecurringTransactionDao
import com.example.data.RecurringTransactionRepository
import com.example.data.Transaction
import com.example.data.TransactionDao
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
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class RecurringTransactionDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var recurringDao: RecurringTransactionDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        recurringDao = db.recurringTransactionDao()
        transactionDao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveRecurringTransaction() = runBlocking {
        val now = System.currentTimeMillis()
        val item = RecurringTransaction(
            title = "اجاره خانه",
            amount = 12000000L,
            isIncome = false,
            category = "اجاره و مسکن",
            period = RecurrencePeriod.MONTHLY.name,
            startDate = now,
            nextExecutionDate = now,
            isActive = true,
            note = "واریز به کارت صاحب‌خانه"
        )

        val id = recurringDao.insert(item)
        assertTrue(id > 0)

        val retrieved = recurringDao.getById(id)
        assertNotNull(retrieved)
        assertEquals("اجاره خانه", retrieved?.title)
        assertEquals(12000000L, retrieved?.amount)
        assertEquals(RecurrencePeriod.MONTHLY, retrieved?.recurrencePeriod)
        assertTrue(retrieved?.isActive == true)
    }

    @Test
    fun testDueTransactionsDetectionAndExecution() = runBlocking {
        val now = System.currentTimeMillis()
        val dueItem = RecurringTransaction(
            title = "قسط وام ازدواج",
            amount = 1000000L,
            isIncome = false,
            category = "وام و بدهی",
            period = RecurrencePeriod.MONTHLY.name,
            startDate = now - 10000,
            nextExecutionDate = now - 5000, // سررسید شده
            isActive = true
        )

        val notDueItem = RecurringTransaction(
            title = "حقوق ماهانه",
            amount = 25000000L,
            isIncome = true,
            category = "درآمد و حقوق",
            period = RecurrencePeriod.MONTHLY.name,
            startDate = now,
            nextExecutionDate = now + 86400000L, // فردا
            isActive = true
        )

        recurringDao.insert(dueItem)
        recurringDao.insert(notDueItem)

        // بررسی سررسید شده‌ها
        val dueList = recurringDao.getDueTransactions(now)
        assertEquals(1, dueList.size)
        assertEquals("قسط وام ازدواج", dueList[0].title)

        // شبیه‌سازی ثبت خودکار در جدول تراکنش‌ها
        val target = dueList[0]
        val registeredTx = Transaction(
            amount = target.amount,
            category = target.category,
            description = "${target.title} - ثبت خودکار دوره‌ای",
            date = now,
            isIncome = target.isIncome
        )
        val txId = transactionDao.insert(registeredTx)
        assertTrue(txId > 0)

        // محاسبه موعد بعدی
        val nextDate = RecurringTransactionRepository.calculateNextDate(
            target.nextExecutionDate,
            target.recurrencePeriod
        )
        recurringDao.updateExecutionDates(target.id, nextDate, now)

        // اکنون دیگر نباید سررسید شده باشد
        val dueAfterUpdate = recurringDao.getDueTransactions(now)
        assertEquals(0, dueAfterUpdate.size)
    }

    @Test
    fun testToggleActiveStateAndDeletion() = runBlocking {
        val now = System.currentTimeMillis()
        val item = RecurringTransaction(
            title = "اشتراک اینترنت",
            amount = 300000L,
            isIncome = false,
            category = "قبض و اینترنت",
            period = RecurrencePeriod.MONTHLY.name,
            startDate = now,
            nextExecutionDate = now + 100000,
            isActive = true
        )

        val id = recurringDao.insert(item)
        recurringDao.setActiveState(id, false)

        val inactiveItem = recurringDao.getById(id)
        assertNotNull(inactiveItem)
        assertFalse(inactiveItem!!.isActive)

        // تست حذف
        recurringDao.delete(inactiveItem)
        val afterDelete = recurringDao.getById(id)
        assertEquals(null, afterDelete)
    }

    @Test
    fun testRecurrencePeriodCalculations() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.MAY, 1, 10, 0, 0)
        val initialTime = cal.timeInMillis

        val nextDay = RecurringTransactionRepository.calculateNextDate(initialTime, RecurrencePeriod.DAILY)
        val nextWeek = RecurringTransactionRepository.calculateNextDate(initialTime, RecurrencePeriod.WEEKLY)
        val nextMonth = RecurringTransactionRepository.calculateNextDate(initialTime, RecurrencePeriod.MONTHLY)
        val nextYear = RecurringTransactionRepository.calculateNextDate(initialTime, RecurrencePeriod.YEARLY)

        assertTrue(nextDay > initialTime)
        assertTrue(nextWeek > nextDay)
        assertTrue(nextMonth > nextWeek)
        assertTrue(nextYear > nextMonth)
    }
}
