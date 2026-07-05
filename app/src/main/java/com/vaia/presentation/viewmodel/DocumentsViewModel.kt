package com.vaia.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.Document
import com.vaia.domain.usecase.DeleteDocumentUseCase
import com.vaia.domain.usecase.DownloadDocumentUseCase
import com.vaia.domain.usecase.GetTripDocumentsUseCase
import com.vaia.domain.usecase.UploadDocumentUseCase
import com.vaia.presentation.ui.common.documentCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val getTripDocumentsUseCase: GetTripDocumentsUseCase,
    private val uploadDocumentUseCase: UploadDocumentUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val downloadDocumentUseCase: DownloadDocumentUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: String = savedStateHandle.get<String>("tripId") ?: ""

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

    /**
     * Descarga el documento por el endpoint autenticado
     * GET /trips/{trip}/documents/{document}/download (los archivos ya no se
     * exponen por URLs públicas /storage/...) y lo deja en cacheDir para abrirlo.
     */
    fun openDocument(document: Document, cacheDir: File) {
        _uiState.value = _uiState.value.copy(isDownloading = true, error = null)
        viewModelScope.launch {
            downloadDocumentUseCase(tripId, document.id)
                .onSuccess { bytes ->
                    try {
                        val file = File(cacheDir, document.fileName.ifBlank { "documento-${document.id}" })
                        file.writeBytes(bytes)
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            downloadedDocument = DownloadedDocument(file, document.mimeType)
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            error = "No se pudo guardar el documento descargado"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        error = throwable.message
                    )
                }
        }
    }

    fun consumeDownloadedDocument() {
        _uiState.value = _uiState.value.copy(downloadedDocument = null)
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
    val isDownloading: Boolean = false,
    val downloadedDocument: DownloadedDocument? = null,
    val error: String? = null
)

data class DownloadedDocument(
    val file: File,
    val mimeType: String
)
