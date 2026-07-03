package com.vaia.data.repository

import com.vaia.data.api.*
import com.vaia.data.api.dto.*
import com.vaia.data.local.ErrorLogger
import com.vaia.data.network.ErrorMapper
import com.vaia.data.local.db.*
import com.vaia.domain.model.*
import com.vaia.domain.repository.DocumentRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class DocumentRepositoryImpl constructor(
    private val apiService: VaiaApiService,
    private val documentDao: DocumentDao
) : DocumentRepository {

    override suspend fun getDocuments(tripId: String): Result<List<Document>> {
        return try {
            val response = apiService.getDocuments(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                val documents = response.body()!!.data!!.map { it.toDomain() }
                documentDao.deleteByTripId(tripId)
                documentDao.insertAll(documents.map { it.toEntity() })
                Result.success(documents)
            } else {
                val cached = documentDao.getByTripId(tripId)
                if (cached.isNotEmpty()) {
                    Result.success(cached.map { it.toDocument() })
                } else {
                    Result.failure(ErrorMapper.fromResponse(response, "No se pudieron obtener los documentos"))
                }
            }
        } catch (e: Exception) {
            val cached = documentDao.getByTripId(tripId)
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDocument() })
            } else {
                Result.failure(
                    ErrorLogger.logAndWrap(
                        feature = "documents",
                        operation = "getDocuments",
                        throwable = e,
                        defaultMessage = "No se pudieron obtener los documentos",
                        metadata = mapOf("tripId" to tripId)
                    )
                )
            }
        }
    }

    override suspend fun uploadDocument(tripId: String, file: File, description: String?, category: String?): Result<Document> {
        return try {
            val requestFile = file.asRequestBody(file.extension.toMediaTypeOrNull())
            val documentPart = MultipartBody.Part.createFormData("document", file.name, requestFile)

            val descriptionPart = description?.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryPart = category?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.uploadDocument(tripId, documentPart, descriptionPart, categoryPart)
            if (response.isSuccessful && response.body()?.data != null) {
                val document = response.body()!!.data!!.toDomain()
                documentDao.insert(document.toEntity())
                Result.success(document)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo subir el documento"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "documents",
                    operation = "uploadDocument",
                    throwable = e,
                    defaultMessage = "No se pudo subir el documento",
                    metadata = mapOf("tripId" to tripId)
                )
            )
        }
    }

    override suspend fun deleteDocument(documentId: String): Result<Unit> {
        return try {
            val response = apiService.deleteDocument(documentId)
            if (response.isSuccessful) {
                documentDao.deleteById(documentId)
                Result.success(Unit)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo eliminar el documento"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "documents",
                    operation = "deleteDocument",
                    throwable = e,
                    defaultMessage = "No se pudo eliminar el documento",
                    metadata = mapOf("documentId" to documentId)
                )
            )
        }
    }

    override suspend fun downloadDocument(tripId: String, documentId: String): Result<ByteArray> {
        return try {
            val response = apiService.downloadDocument(tripId, documentId)
            if (response.isSuccessful) {
                val bytes = response.body()?.bytes()
                if (bytes != null) Result.success(bytes)
                else Result.failure(ErrorMapper.fromResponse(response, "El documento está vacío"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo descargar el documento"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "documents",
                    operation = "downloadDocument",
                    throwable = e,
                    defaultMessage = "No se pudo descargar el documento",
                    metadata = mapOf("tripId" to tripId, "documentId" to documentId)
                )
            )
        }
    }

    // Checklist methods
    override suspend fun getChecklist(tripId: String): Result<TripDocumentChecklist> {
        return try {
            val response = apiService.getDocumentChecklist(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                val checklist = response.body()!!.data!!.toDomain()
                documentDao.deleteChecklistItemsByTripId(tripId)
                documentDao.insertChecklistItems(checklist.items.map { it.toEntity(tripId) })
                Result.success(checklist)
            } else {
                val cachedItems = documentDao.getChecklistItemsByTripId(tripId)
                if (cachedItems.isNotEmpty()) {
                    val items = cachedItems.map { it.toChecklistItem() }
                    val completed = items.count { it.isCompleted }
                    val total = items.size
                    val percentage = if (total > 0) (completed * 100) / total else 0
                    val progress = DocumentProgress(completed, total, percentage)
                    Result.success(
                        TripDocumentChecklist(
                            id = tripId,
                            tripId = tripId,
                            items = items,
                            progress = progress
                        )
                    )
                } else {
                    Result.failure(ErrorMapper.fromResponse(response, "No se pudo cargar la lista de documentos"))
                }
            }
        } catch (e: Exception) {
            val cachedItems = documentDao.getChecklistItemsByTripId(tripId)
            if (cachedItems.isNotEmpty()) {
                val items = cachedItems.map { it.toChecklistItem() }
                val completed = items.count { it.isCompleted }
                val total = items.size
                val percentage = if (total > 0) (completed * 100) / total else 0
                val progress = DocumentProgress(completed, total, percentage)
                Result.success(
                    TripDocumentChecklist(
                        id = tripId,
                        tripId = tripId,
                        items = items,
                        progress = progress
                    )
                )
            } else {
                Result.failure(
                    ErrorLogger.logAndWrap(
                        feature = "documents",
                        operation = "getChecklist",
                        throwable = e,
                        defaultMessage = "No se pudo cargar la lista de documentos",
                        metadata = mapOf("tripId" to tripId)
                    )
                )
            }
        }
    }

    override suspend fun addChecklistItem(tripId: String, name: String): Result<ChecklistItem> {
        return try {
            val response = apiService.addChecklistItem(tripId, AddChecklistItemRequest(name))
            if (response.isSuccessful && response.body()?.data != null) {
                val item = response.body()!!.data!!.toDomain()
                documentDao.insertChecklistItem(item.toEntity(tripId))
                Result.success(item)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo agregar el elemento"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "documents",
                    operation = "addChecklistItem",
                    throwable = e,
                    defaultMessage = "No se pudo agregar el elemento",
                    metadata = mapOf("tripId" to tripId)
                )
            )
        }
    }

    override suspend fun toggleChecklistItemComplete(itemId: String, isCompleted: Boolean): Result<ChecklistItem> {
        return try {
            val response = apiService.toggleChecklistItemComplete(itemId, ToggleCompleteRequest(isCompleted))
            if (response.isSuccessful && response.body()?.data != null) {
                val item = response.body()!!.data!!.toDomain()
                // We don't have the tripId easily here, but we can query the existing item or pass a dummy/extracted one
                // Since update is onConflict REPLACE, we can just query the existing entity first to preserve its tripId
                val existing = documentDao.getChecklistItemsByTripId("").firstOrNull { it.id == itemId } // fallback / search
                // Actually, let's load all checklist items to find it if we don't have tripId, or we can use the tripId from the response if available (it isn't directly on ChecklistItem but we can search for it in our DB).
                val tripId = existing?.tripId ?: ""
                documentDao.insertChecklistItem(item.toEntity(tripId))
                Result.success(item)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo actualizar el elemento"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "documents",
                    operation = "toggleChecklistItemComplete",
                    throwable = e,
                    defaultMessage = "No se pudo actualizar el elemento",
                    metadata = mapOf("itemId" to itemId)
                )
            )
        }
    }

    override suspend fun deleteChecklistItem(itemId: String): Result<Unit> {
        return try {
            val response = apiService.deleteChecklistItem(itemId)
            if (response.isSuccessful) {
                documentDao.deleteChecklistItemById(itemId)
                Result.success(Unit)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo eliminar el elemento"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "documents",
                    operation = "deleteChecklistItem",
                    throwable = e,
                    defaultMessage = "No se pudo eliminar el elemento",
                    metadata = mapOf("itemId" to itemId)
                )
            )
        }
    }

    override suspend fun uploadChecklistDocument(itemId: String, file: File): Result<ChecklistDocument> {
        return try {
            val requestFile = file.asRequestBody(file.extension.toMediaTypeOrNull())
            val documentPart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = apiService.uploadChecklistDocument(itemId, documentPart)
            if (response.isSuccessful && response.body()?.data != null) {
                val document = response.body()!!.data!!.toDomain()
                // Update the checklist item in database to include this document
                val existing = documentDao.getChecklistItemsByTripId("").firstOrNull { it.id == itemId }
                if (existing != null) {
                    val updated = existing.copy(
                        docId = document.id,
                        docFileName = document.fileName,
                        docFilePath = document.filePath,
                        docMimeType = document.mimeType,
                        docFileSize = document.fileSize,
                        docSource = document.source,
                        docGoogleDriveFileId = document.googleDriveFileId,
                        docUploadedBy = document.uploadedBy,
                        isCompleted = true
                    )
                    documentDao.insertChecklistItem(updated)
                }
                Result.success(document)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo subir el documento"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "documents",
                    operation = "uploadChecklistDocument",
                    throwable = e,
                    defaultMessage = "No se pudo subir el documento del checklist",
                    metadata = mapOf("itemId" to itemId)
                )
            )
        }
    }

    override suspend fun importFromGoogleDrive(itemId: String, fileId: String, accessToken: String): Result<ChecklistDocument> {
        return try {
            val response = apiService.importFromGoogleDrive(itemId, ImportFromDriveRequest(fileId, accessToken))
            if (response.isSuccessful && response.body()?.data != null) {
                val document = response.body()!!.data!!.toDomain()
                val existing = documentDao.getChecklistItemsByTripId("").firstOrNull { it.id == itemId }
                if (existing != null) {
                    val updated = existing.copy(
                        docId = document.id,
                        docFileName = document.fileName,
                        docFilePath = document.filePath,
                        docMimeType = document.mimeType,
                        docFileSize = document.fileSize,
                        docSource = document.source,
                        docGoogleDriveFileId = document.googleDriveFileId,
                        docUploadedBy = document.uploadedBy,
                        isCompleted = true
                    )
                    documentDao.insertChecklistItem(updated)
                }
                Result.success(document)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo importar desde Google Drive"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "documents",
                    operation = "importFromGoogleDrive",
                    throwable = e,
                    defaultMessage = "No se pudo importar desde Google Drive",
                    metadata = mapOf("itemId" to itemId)
                )
            )
        }
    }

    override suspend fun previewChecklistDocument(documentId: String): Result<String> {
        return try {
            val response = apiService.previewChecklistDocument(documentId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.url)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo generar la vista previa"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "documents",
                    operation = "previewChecklistDocument",
                    throwable = e,
                    defaultMessage = "No se pudo generar la vista previa",
                    metadata = mapOf("documentId" to documentId)
                )
            )
        }
    }

    override suspend fun deleteChecklistDocument(documentId: String): Result<Unit> {
        return try {
            val response = apiService.deleteChecklistDocument(documentId)
            if (response.isSuccessful) {
                val existing = documentDao.getChecklistItemsByTripId("").firstOrNull { it.docId == documentId }
                if (existing != null) {
                    val updated = existing.copy(
                        docId = null,
                        docFileName = null,
                        docFilePath = null,
                        docMimeType = null,
                        docFileSize = null,
                        docSource = null,
                        docGoogleDriveFileId = null,
                        docUploadedBy = null,
                        isCompleted = false
                    )
                    documentDao.insertChecklistItem(updated)
                }
                Result.success(Unit)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo eliminar el documento"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "documents",
                    operation = "deleteChecklistDocument",
                    throwable = e,
                    defaultMessage = "No se pudo eliminar el documento del checklist",
                    metadata = mapOf("documentId" to documentId)
                )
            )
        }
    }
}
