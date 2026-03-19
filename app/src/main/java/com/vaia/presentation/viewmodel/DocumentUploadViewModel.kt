package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.domain.usecase.UploadDocumentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class DocumentUploadViewModel(
    private val uploadDocumentUseCase: UploadDocumentUseCase
) : ViewModel() {

    private val _uploadState = MutableStateFlow<DocumentUploadState>(DocumentUploadState.Idle)
    val uploadState: StateFlow<DocumentUploadState> = _uploadState

    fun uploadDocument(tripId: String, file: File, fileName: String, category: String? = null) {
        viewModelScope.launch {
            _uploadState.value = DocumentUploadState.Uploading

            uploadDocumentUseCase(tripId, file, fileName, category).fold(
                onSuccess = { document ->
                    _uploadState.value = DocumentUploadState.Success(document.id)
                },
                onFailure = { exception ->
                    _uploadState.value = DocumentUploadState.Error(
                        exception.message ?: "Error al subir documento"
                    )
                }
            )
        }
    }

    fun resetState() {
        _uploadState.value = DocumentUploadState.Idle
    }

    sealed class DocumentUploadState {
        data object Idle : DocumentUploadState()
        data object Uploading : DocumentUploadState()
        data class Success(val documentId: String) : DocumentUploadState()
        data class Error(val message: String) : DocumentUploadState()
    }
}
