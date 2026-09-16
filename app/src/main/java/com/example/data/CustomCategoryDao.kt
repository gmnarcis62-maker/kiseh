package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCategoryDao {
    @Query("SELECT * FROM custom_categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CustomCategory>>

    @Query("SELECT * FROM custom_categories WHERE isIncome = :isIncome ORDER BY id ASC")
    fun getCategoriesByType(isIncome: Boolean): Flow<List<CustomCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CustomCategory): Long

    @Update
    suspend fun updateCategory(category: CustomCategory)

    @Delete
    suspend fun deleteCategory(category: CustomCategory)

    @Query("SELECT * FROM custom_categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CustomCategory?

    @Query("SELECT COUNT(*) FROM custom_categories")
    suspend fun getCategoriesCount(): Int

    @Query("SELECT * FROM custom_categories ORDER BY id ASC")
    suspend fun getAllCategoriesList(): List<CustomCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CustomCategory>)
}
