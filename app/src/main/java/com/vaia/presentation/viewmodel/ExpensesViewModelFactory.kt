package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vaia.domain.repository.ExpenseRepository

class ExpensesViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val tripId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpensesViewModel::class.java)) {
            return ExpensesViewModel(expenseRepository, tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}