package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.Document
import com.vaia.domain.usecase.DeleteDocumentUseCase
import com.vaia.domain.usecase.GetTripDocumentsUseCase
import com.vaia.domain.usecase.UploadDocumentUseCase
import com.vaia.presentation.ui.common.documentCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File


class DocumentsViewModel(
    private val getTripDocumentsUseCase: GetTripDocumentsUseCase,
    private val uploadDocumentUseCase: UploadDocumentUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val tripId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            getTripDocumentsUseCase(tripId)
                .onSuccess { documents ->
                    val completedCategories = calculateCompletedCategories(documents)
                    _uiState.value = _uiState.value.copy(
                        documents = documents,
                        completedCategories = completedCategories,
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

    private fun calculateCompletedCategories(documents: List<Document>): Set<String> {
        return documents
            .mapNotNull { it.category }
            .filter { it in documentCategories }
            .toSet()
    }

    fun uploadDocument(file: File, description: String?, category: String?) {
        _uiState.value = _uiState.value.copy(isUploading = true, error = null)
        viewModelScope.launch {
            uploadDocumentUseCase(tripId, file, description, category)
                .onSuccess { newDocument ->
                    val updatedDocuments = _uiState.value.documents + newDocument
                    val completedCategories = calculateCompletedCategories(updatedDocuments)
                    _uiState.value = _uiState.value.copy(
                        documents = updatedDocuments,
                        completedCategories = completedCategories,
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

    fun deleteDocument(documentId: String) {
        _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
        viewModelScope.launch {
            deleteDocumentUseCase(documentId)
                .onSuccess {
                    val updatedDocuments = _uiState.value.documents.filter { it.id != documentId }
                    val completedCategories = calculateCompletedCategories(updatedDocuments)
                    _uiState.value = _uiState.value.copy(
                        documents = updatedDocuments,
                        completedCategories = completedCategories,
                        isDeleting = false
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        error = throwable.message,
                        isDeleting = false
                    )
                }
        }
    }
}

data class DocumentsUiState(
    val documents: List<Document> = emptyList(),
    val completedCategories: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null
)

class DocumentsViewModelFactory(
    private val getTripDocumentsUseCase: GetTripDocumentsUseCase,
    private val uploadDocumentUseCase: UploadDocumentUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val tripId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DocumentsViewModel(getTripDocumentsUseCase, uploadDocumentUseCase, deleteDocumentUseCase, tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
