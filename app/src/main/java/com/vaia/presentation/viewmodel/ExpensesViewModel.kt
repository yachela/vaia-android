package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.Expense
import com.vaia.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ExpensesViewModel(
    private val expenseRepository: ExpenseRepository,
    private val tripId: String
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _createState = MutableStateFlow<CreateState>(CreateState.Idle)
    val createState: StateFlow<CreateState> = _createState

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState

    private val _receiptState = MutableStateFlow<ReceiptState>(ReceiptState.Idle)
    val receiptState: StateFlow<ReceiptState> = _receiptState

    init {
        loadExpenses()
    }

    fun loadExpenses() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            expenseRepository.getExpenses(tripId).fold(
                onSuccess = { expenses ->
                    _expenses.value = expenses
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Error al cargar gastos"
                    _isLoading.value = false
                }
            )
        }
    }

    fun createExpense(description: String, amount: Double, category: String, date: String) {
        viewModelScope.launch {
            _createState.value = CreateState.Loading

            expenseRepository.createExpense(tripId, amount, description, date, category, null).fold(
                onSuccess = {
                    _createState.value = CreateState.Success
                    loadExpenses()
                },
                onFailure = { exception ->
                    _createState.value = CreateState.Error(exception.message ?: "Error al crear gasto")
                }
            )
        }
    }

    fun resetCreateState() {
        _createState.value = CreateState.Idle
    }

    fun updateExpense(expenseId: String, description: String, amount: Double, category: String, date: String) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Loading

            expenseRepository.updateExpense(tripId, expenseId, amount, description, date, category, null).fold(
                onSuccess = {
                    _updateState.value = UpdateState.Success
                    loadExpenses()
                },
                onFailure = { exception ->
                    _updateState.value = UpdateState.Error(exception.message ?: "Error al actualizar gasto")
                }
            )
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Loading

            expenseRepository.deleteExpense(tripId, expenseId).fold(
                onSuccess = {
                    _deleteState.value = DeleteState.Success
                    loadExpenses()
                },
                onFailure = { exception ->
                    _deleteState.value = DeleteState.Error(exception.message ?: "Error al eliminar gasto")
                }
            )
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    fun downloadReceipt(expenseId: String) {
        viewModelScope.launch {
            _receiptState.value = ReceiptState.Loading
            expenseRepository.downloadReceipt(tripId, expenseId).fold(
                onSuccess = { bytes -> _receiptState.value = ReceiptState.Ready(bytes) },
                onFailure = { e -> _receiptState.value = ReceiptState.Error(e.message ?: "Error al descargar recibo") }
            )
        }
    }

    fun resetReceiptState() {
        _receiptState.value = ReceiptState.Idle
    }

    sealed class CreateState {
        object Idle : CreateState()
        object Loading : CreateState()
        object Success : CreateState()
        data class Error(val message: String) : CreateState()
    }

    sealed class UpdateState {
        object Idle : UpdateState()
        object Loading : UpdateState()
        object Success : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    sealed class DeleteState {
        object Idle : DeleteState()
        object Loading : DeleteState()
        object Success : DeleteState()
        data class Error(val message: String) : DeleteState()
    }

    sealed class ReceiptState {
        object Idle : ReceiptState()
        object Loading : ReceiptState()
        data class Ready(val bytes: ByteArray) : ReceiptState()
        data class Error(val message: String) : ReceiptState()
    }
}
