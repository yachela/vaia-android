package com.vaia.domain.repository

import com.vaia.domain.model.Expense

interface ExpenseRepository {
    suspend fun getExpenses(tripId: String): Result<List<Expense>>
    suspend fun getExpense(tripId: String, expenseId: String): Result<Expense>
    suspend fun createExpense(tripId: String, amount: Double, description: String, date: String, category: String, receiptImage: ByteArray?): Result<Expense>
    suspend fun updateExpense(tripId: String, expenseId: String, amount: Double, description: String, date: String, category: String, receiptImage: ByteArray?): Result<Expense>
    suspend fun deleteExpense(tripId: String, expenseId: String): Result<Unit>
    suspend fun downloadReceipt(tripId: String, expenseId: String): Result<ByteArray>
}
