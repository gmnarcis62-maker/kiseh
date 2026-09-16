package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingBankSmsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sms: PendingBankSms): Long

    @Update
    suspend fun update(sms: PendingBankSms)

    @Delete
    suspend fun delete(sms: PendingBankSms)

    @Query("SELECT * FROM pending_bank_sms WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getAllPending(): Flow<List<PendingBankSms>>

    @Query("SELECT * FROM pending_bank_sms WHERE status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getAllPendingList(): List<PendingBankSms>

    @Query("SELECT * FROM pending_bank_sms ORDER BY createdAt DESC")
    suspend fun getAllList(): List<PendingBankSms>

    @Query("SELECT * FROM pending_bank_sms WHERE id = :id")
    suspend fun getById(id: Long): PendingBankSms?

    @Query("UPDATE pending_bank_sms SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM pending_bank_sms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_bank_sms WHERE createdAt < :thresholdTimestamp AND status != 'PENDING'")
    suspend fun deleteOldProcessedSms(thresholdTimestamp: Long)

    @Query("SELECT * FROM pending_bank_sms WHERE bankName = :bankName AND amount = :amount AND isIncome = :isIncome AND createdAt >= :sinceTimestamp ORDER BY createdAt DESC")
    suspend fun findRecentSimilar(
        bankName: String,
        amount: Long,
        isIncome: Boolean,
        sinceTimestamp: Long
    ): List<PendingBankSms>
}
