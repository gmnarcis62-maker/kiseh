package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.PendingBankSms
import com.example.data.PendingBankSmsRepository
import com.example.data.SmsStatus
import com.example.data.TransactionRepository
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * تست‌های یکپارچگی مرحله ۷.۴:
 * ارزیابی کارکرد ViewModel و جریان تایید، رد و ویرایش پیامک‌های بانکی
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BankSmsViewModelIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var database: AppDatabase
    private lateinit var pendingSmsRepo: PendingBankSmsRepository
    private lateinit var transactionRepo: TransactionRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        context = application
        com.example.billing.BillingManager.setProUserForTesting(context, true)

        database = AppDatabase.getDatabase(context)
        pendingSmsRepo = PendingBankSmsRepository(database.pendingBankSmsDao())
        transactionRepo = TransactionRepository(database.transactionDao())
        viewModel = MainViewModel(application)
    }

    @After
    fun tearDown() {
        runBlocking {
            database.pendingBankSmsDao().getAllList().forEach {
                database.pendingBankSmsDao().delete(it)
            }
            database.transactionDao().deleteAll()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun testConfirmPendingSmsConvertsToTransaction() = testScope.runTest {
        // ۱. ایجاد پیامک بانکی
        val sms = PendingBankSms(
            bankName = "بانک پاسارگاد",
            amount = 250000L,
            isIncome = false,
            cardLastDigits = "5678",
            merchant = "اسنپ",
            suggestedCategory = "حمل‌ونقل"
        )
        val smsId = pendingSmsRepo.insert(sms)
        val insertedSms = sms.copy(id = smsId)

        // ۲. تایید پیامک از طریق ViewModel
        viewModel.confirmPendingSms(insertedSms)
        advanceUntilIdle()

        // ۳. بررسی اضافه شدن به لیست تراکنش‌های اصلی
        val transactions = transactionRepo.allTransactions.first()
        val matchingTx = transactions.find { it.amount == 250000L }
        assertNotNull("تراکنش باید در جدول اصلی ثبت شده باشد", matchingTx)
        assertEquals("حمل‌ونقل", matchingTx?.category)
        assertTrue(matchingTx?.description?.contains("بانک پاسارگاد") == true)
        assertTrue(matchingTx?.description?.contains("اسنپ") == true)
        assertTrue(matchingTx?.description?.contains("5678") == true)

        // ۴. بررسی تغییر وضعیت پیامک به CONFIRMED
        val updatedSms = pendingSmsRepo.getById(smsId)
        assertEquals(SmsStatus.CONFIRMED.name, updatedSms?.status)
    }

    @Test
    fun testRejectPendingSms() = testScope.runTest {
        // ۱. ایجاد پیامک بانکی
        val sms = PendingBankSms(
            bankName = "بانک سامان",
            amount = 90000L,
            isIncome = false,
            cardLastDigits = "9999",
            suggestedCategory = "سایر"
        )
        val smsId = pendingSmsRepo.insert(sms)
        val insertedSms = sms.copy(id = smsId)

        // ۲. رد پیامک از طریق ViewModel
        viewModel.rejectPendingSms(insertedSms)
        advanceUntilIdle()

        // ۳. اطمینان از عدم افزودن به تراکنش‌های اصلی
        val transactions = transactionRepo.allTransactions.first()
        val hasTransaction = transactions.any { it.amount == 90000L }
        assertTrue("تراکنش نباید ثبت شده باشد", !hasTransaction)

        // ۴. بررسی تغییر وضعیت به REJECTED
        val updatedSms = pendingSmsRepo.getById(smsId)
        assertEquals(SmsStatus.REJECTED.name, updatedSms?.status)
    }

    @Test
    fun testEditAndConfirmPendingSms() = testScope.runTest {
        // ۱. ایجاد پیامک بانکی
        val sms = PendingBankSms(
            bankName = "بانک ملی",
            amount = 500000L,
            isIncome = true,
            cardLastDigits = "4321",
            merchant = "حقوق فروردین",
            suggestedCategory = "سایر"
        )
        val smsId = pendingSmsRepo.insert(sms)
        val insertedSms = sms.copy(id = smsId)

        // ۲. کاربر دسته‌بندی و توضیحات را ویرایش کرده و تایید می‌کند
        viewModel.editAndConfirmPendingSms(
            pendingSms = insertedSms,
            amount = 520000L, // کاربر مبلغ را اصلاح کرده
            category = "درآمد",
            description = "واریز حقوق کارگاه",
            isIncome = true
        )
        advanceUntilIdle()

        // ۳. بررسی تراکنش ذخیره‌شده
        val transactions = transactionRepo.allTransactions.first()
        val matchingTx = transactions.find { it.amount == 520000L }
        assertNotNull(matchingTx)
        assertEquals("درآمد", matchingTx?.category)
        assertEquals("واریز حقوق کارگاه", matchingTx?.description)
        assertTrue(matchingTx?.isIncome == true)

        // ۴. بررسی وضعیت پیامک
        val updatedSms = pendingSmsRepo.getById(smsId)
        assertEquals(SmsStatus.CONFIRMED.name, updatedSms?.status)
    }
}
