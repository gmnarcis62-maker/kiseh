package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CategorySummary(
    val category: String,
    val total: Long
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    suspend fun getTransactionsByDateRange(start: Long, end: Long): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions WHERE date BETWEEN :start AND :end AND isIncome = 0")
    suspend fun getTotalSpent(start: Long, end: Long): Long?

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE date BETWEEN :start AND :end AND isIncome = 0 GROUP BY category ORDER BY total DESC")
    suspend fun getSpendingByCategory(start: Long, end: Long): List<CategorySummary>

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long)

    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName")
    suspend fun updateCategoryName(oldName: String, newName: String)

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAllTransactionsList(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getTransactionsFlow(start: Long, end: Long): Flow<List<Transaction>>
}

