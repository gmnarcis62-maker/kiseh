package com.example.data

import kotlinx.coroutines.flow.Flow

class SavingsGoalRepository(private val dao: SavingsGoalDao) {
    val allGoals: Flow<List<SavingsGoal>> = dao.getAllGoals()

    suspend fun insert(goal: SavingsGoal): Long = dao.insert(goal)

    suspend fun update(goal: SavingsGoal) = dao.update(goal)

    suspend fun delete(goal: SavingsGoal) = dao.delete(goal)

    suspend fun addDeposit(goalId: Long, amount: Long) = dao.addDeposit(goalId, amount)
}
