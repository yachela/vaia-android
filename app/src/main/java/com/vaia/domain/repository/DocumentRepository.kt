package com.vaia.domain.repository

import com.vaia.domain.model.Document
import java.io.File

interface DocumentRepository {
    suspend fun getDocuments(tripId: String): Result<List<Document>>
    suspend fun uploadDocument(tripId: String, file: File, description: String?): Result<Document>
    suspend fun deleteDocument(documentId: String): Result<Unit>
}