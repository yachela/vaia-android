package com.vaia.domain.repository

import com.vaia.domain.model.Document
import com.vaia.domain.model.ChecklistDocument
import com.vaia.domain.model.ChecklistItem
import com.vaia.domain.model.TripDocumentChecklist
import java.io.File

interface DocumentRepository {
    suspend fun getDocuments(tripId: String): Result<List<Document>>
    suspend fun uploadDocument(tripId: String, file: File, description: String?, category: String?): Result<Document>
    suspend fun deleteDocument(documentId: String): Result<Unit>

    // Checklist methods
    suspend fun getChecklist(tripId: String): Result<TripDocumentChecklist>
    suspend fun addChecklistItem(tripId: String, name: String): Result<ChecklistItem>
    suspend fun toggleChecklistItemComplete(itemId: String, isCompleted: Boolean): Result<ChecklistItem>
    suspend fun deleteChecklistItem(itemId: String): Result<Unit>
    suspend fun uploadChecklistDocument(itemId: String, file: File): Result<ChecklistDocument>
    suspend fun importFromGoogleDrive(itemId: String, fileId: String, accessToken: String): Result<ChecklistDocument>
    suspend fun deleteChecklistDocument(documentId: String): Result<Unit>

    /**
     * Copia local del documento en este dispositivo, o null si no la hay. Los documentos no se
     * sirven desde el backend para su visualización, así que sin copia local no hay vista previa.
     */
    fun localChecklistDocumentFile(documentId: String, mimeType: String): File?
}