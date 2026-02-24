package com.vaia.domain.usecase

import com.vaia.domain.model.Document
import com.vaia.domain.repository.DocumentRepository
import java.io.File

class GetTripDocumentsUseCase constructor(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(tripId: String): Result<List<Document>> {
        return documentRepository.getDocuments(tripId)
    }
}

class UploadDocumentUseCase constructor(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(tripId: String, file: File, description: String?, category: String?): Result<Document> {
        return documentRepository.uploadDocument(tripId, file, description, category)
    }
}

class DeleteDocumentUseCase constructor(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(documentId: String): Result<Unit> {
        return documentRepository.deleteDocument(documentId)
    }
}
