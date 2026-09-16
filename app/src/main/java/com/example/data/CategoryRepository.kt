package com.example.data

import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val customCategoryDao: CustomCategoryDao) {

    val allCustomCategories: Flow<List<CustomCategory>> = customCategoryDao.getAllCategories()

    fun getCategoriesByType(isIncome: Boolean): Flow<List<CustomCategory>> {
        return customCategoryDao.getCategoriesByType(isIncome)
    }

    suspend fun insertCategory(category: CustomCategory): Long {
        return customCategoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CustomCategory) {
        customCategoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CustomCategory) {
        if (!category.isDefault) {
            customCategoryDao.deleteCategory(category)
        }
    }

    suspend fun getCategoryByName(name: String): CustomCategory? {
        return customCategoryDao.getCategoryByName(name)
    }
}
