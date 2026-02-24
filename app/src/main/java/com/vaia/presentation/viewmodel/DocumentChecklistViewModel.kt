package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.ChecklistDocument
import com.vaia.domain.model.ChecklistItem
import com.vaia.domain.model.DocumentProgress
import com.vaia.domain.model.TripDocumentChecklist
import com.vaia.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class DocumentChecklistViewModel(
    private val documentRepository: DocumentRepository,
    private val tripId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentChecklistUiState())
    val uiState: StateFlow<DocumentChecklistUiState> = _uiState.asStateFlow()

    init {
        loadChecklist()
    }

    fun loadChecklist() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            documentRepository.getChecklist(tripId)
                .onSuccess { checklist ->
                    _uiState.value = _uiState.value.copy(
                        checklist = checklist,
                        isLoading = false
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        error = throwable.message,
                        isLoading = false
                    )
                }
        }
    }

    fun addItem(name: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            documentRepository.addChecklistItem(tripId, name)
                .onSuccess { item ->
                    val currentItems = _uiState.value.checklist?.items.orEmpty()
                    val updatedChecklist = _uiState.value.checklist?.copy(
                        items = currentItems + item
                    )
                    _uiState.value = _uiState.value.copy(
                        checklist = updatedChecklist,
                        isLoading = false
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        error = throwable.message,
                        isLoading = false
                    )
                }
        }
    }

    fun toggleItemComplete(itemId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            documentRepository.toggleChecklistItemComplete(itemId, isCompleted)
                .onSuccess { updatedItem ->
                    val currentItems = _uiState.value.checklist?.items.orEmpty()
                    val updatedItems = currentItems.map {
                        if (it.id == itemId) updatedItem else it
                    }
                    val progress = calculateProgress(updatedItems)
                    _uiState.value = _uiState.value.copy(
                        checklist = _uiState.value.checklist?.copy(
                            items = updatedItems,
                            progress = progress
                        )
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(error = throwable.message)
                }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            documentRepository.deleteChecklistItem(itemId)
                .onSuccess {
                    val currentItems = _uiState.value.checklist?.items.orEmpty()
                    val updatedItems = currentItems.filter { it.id != itemId }
                    val progress = calculateProgress(updatedItems)
                    _uiState.value = _uiState.value.copy(
                        checklist = _uiState.value.checklist?.copy(
                            items = updatedItems,
                            progress = progress
                        )
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(error = throwable.message)
                }
        }
    }

    fun uploadDocument(itemId: String, file: File) {
        _uiState.value = _uiState.value.copy(isUploading = true, error = null)
        viewModelScope.launch {
            documentRepository.uploadChecklistDocument(itemId, file)
                .onSuccess { document ->
                    val currentItems = _uiState.value.checklist?.items.orEmpty()
                    val updatedItems = currentItems.map { item ->
                        if (item.id == itemId) item.copy(document = document, isCompleted = true) else item
                    }
                    val progress = calculateProgress(updatedItems)
                    _uiState.value = _uiState.value.copy(
                        checklist = _uiState.value.checklist?.copy(
                            items = updatedItems,
                            progress = progress
                        ),
                        isUploading = false
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        error = throwable.message,
                        isUploading = false
                    )
                }
        }
    }

    fun importFromGoogleDrive(itemId: String, fileId: String, accessToken: String) {
        _uiState.value = _uiState.value.copy(isUploading = true, error = null)
        viewModelScope.launch {
            documentRepository.importFromGoogleDrive(itemId, fileId, accessToken)
                .onSuccess { document ->
                    val currentItems = _uiState.value.checklist?.items.orEmpty()
                    val updatedItems = currentItems.map { item ->
                        if (item.id == itemId) item.copy(document = document, isCompleted = true) else item
                    }
                    val progress = calculateProgress(updatedItems)
                    _uiState.value = _uiState.value.copy(
                        checklist = _uiState.value.checklist?.copy(
                            items = updatedItems,
                            progress = progress
                        ),
                        isUploading = false
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        error = throwable.message,
                        isUploading = false
                    )
                }
        }
    }

    fun deleteDocument(documentId: String, itemId: String) {
        viewModelScope.launch {
            documentRepository.deleteChecklistDocument(documentId)
                .onSuccess {
                    val currentItems = _uiState.value.checklist?.items.orEmpty()
                    val updatedItems = currentItems.map { item ->
                        if (item.id == itemId) item.copy(document = null, isCompleted = false) else item
                    }
                    val progress = calculateProgress(updatedItems)
                    _uiState.value = _uiState.value.copy(
                        checklist = _uiState.value.checklist?.copy(
                            items = updatedItems,
                            progress = progress
                        )
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(error = throwable.message)
                }
        }
    }

    fun previewDocument(documentId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            documentRepository.previewChecklistDocument(documentId)
                .onSuccess { url ->
                    onResult(url)
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(error = throwable.message)
                }
        }
    }

    private fun calculateProgress(items: List<ChecklistItem>): DocumentProgress {
        val completed = items.count { it.isCompleted }
        val total = items.size
        val percentage = if (total > 0) (completed * 100) / total else 0
        return DocumentProgress(completed, total, percentage)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class DocumentChecklistUiState(
    val checklist: TripDocumentChecklist? = null,
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null
)

class DocumentChecklistViewModelFactory(
    private val documentRepository: DocumentRepository,
    private val tripId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentChecklistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DocumentChecklistViewModel(documentRepository, tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
