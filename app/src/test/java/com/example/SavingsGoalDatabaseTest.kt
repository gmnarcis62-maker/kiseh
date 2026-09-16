package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.SavingsGoal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SavingsGoalDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveSavingsGoal() = runBlocking {
        val dao = db.savingsGoalDao()
        val goal = SavingsGoal(
            title = "خرید لپ‌تاپ",
            targetAmount = 45000000L,
            savedAmount = 15000000L,
            note = "لپ‌تاپ گیمینگ و برنامه‌نویسی",
            iconName = "Laptop",
            colorHex = "#0D5C46"
        )

        val id = dao.insert(goal)
        assertTrue(id > 0)

        val retrieved = dao.getGoalById(id)
        assertNotNull(retrieved)
        assertEquals("خرید لپ‌تاپ", retrieved?.title)
        assertEquals(45000000L, retrieved?.targetAmount)
        assertEquals(15000000L, retrieved?.savedAmount)
        assertEquals(33, retrieved?.progressPercent)
        assertFalse(retrieved?.isCompleted ?: true)
        assertEquals(30000000L, retrieved?.remainingAmount)
    }

    @Test
    fun testDepositSavingsGoal() = runBlocking {
        val dao = db.savingsGoalDao()
        val goal = SavingsGoal(
            title = "سفر شمال",
            targetAmount = 10000000L,
            savedAmount = 4000000L,
            note = "تعطیلات عید",
            iconName = "Flight",
            colorHex = "#D4AF37"
        )

        val id = dao.insert(goal)
        dao.addDeposit(id, 6000000L)

        val updated = dao.getGoalById(id)
        assertNotNull(updated)
        assertEquals(10000000L, updated?.savedAmount)
        assertEquals(100, updated?.progressPercent)
        assertTrue(updated?.isCompleted ?: false)
        assertEquals(0L, updated?.remainingAmount)
    }

    @Test
    fun testDeleteSavingsGoal() = runBlocking {
        val dao = db.savingsGoalDao()
        val goal = SavingsGoal(
            title = "موتورسیکلت",
            targetAmount = 80000000L,
            savedAmount = 10000000L
        )

        val id = dao.insert(goal)
        val inserted = dao.getGoalById(id)
        assertNotNull(inserted)

        dao.delete(inserted!!)
        val all = dao.getAllGoals().first()
        assertTrue(all.isEmpty())
    }
}
