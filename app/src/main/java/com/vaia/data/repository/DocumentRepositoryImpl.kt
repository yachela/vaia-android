package com.vaia.data.repository

import com.vaia.data.api.*
import com.vaia.data.local.ErrorLogger
import com.vaia.domain.model.*
import com.vaia.domain.repository.DocumentRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class DocumentRepositoryImpl constructor(
    private val apiService: VaiaApiService
) : DocumentRepository {

    override suspend fun getDocuments(tripId: String): Result<List<Document>> {
        return try {
            val response = apiService.getDocuments(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data as List<Document>)
            } else {
                Result.failure(RuntimeException("Failed to fetch documents: ${response.errorBody()?.string() ?: "Unknown error"}"))
            }

        } catch (e: Exception) {
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

    override suspend fun uploadDocument(tripId: String, file: File, description: String?, category: String?): Result<Document> {
        return try {
            val requestFile = file.asRequestBody(file.extension.toMediaTypeOrNull())
            val documentPart = MultipartBody.Part.createFormData("document", file.name, requestFile)

            val descriptionPart = description?.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryPart = category?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.uploadDocument(tripId, documentPart, descriptionPart, categoryPart)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data as Document)
            } else {
                Result.failure(RuntimeException("Failed to upload document: ${response.errorBody()?.string() ?: "Unknown error"}"))
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
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("Failed to delete document: ${response.errorBody()?.string()}"))
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

    // Checklist methods
    override suspend fun getChecklist(tripId: String): Result<TripDocumentChecklist> {
        return try {
            val response = apiService.getDocumentChecklist(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(RuntimeException("Failed to fetch checklist: ${response.errorBody()?.string() ?: "Unknown error"}"))
            }
        } catch (e: Exception) {
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

    override suspend fun addChecklistItem(tripId: String, name: String): Result<ChecklistItem> {
        return try {
            val response = apiService.addChecklistItem(tripId, AddChecklistItemRequest(name))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(RuntimeException("Failed to add checklist item: ${response.errorBody()?.string() ?: "Unknown error"}"))
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
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(RuntimeException("Failed to toggle item: ${response.errorBody()?.string() ?: "Unknown error"}"))
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
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("Failed to delete item: ${response.errorBody()?.string()}"))
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
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(RuntimeException("Failed to upload document: ${response.errorBody()?.string() ?: "Unknown error"}"))
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
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(RuntimeException("Failed to import from Google Drive: ${response.errorBody()?.string() ?: "Unknown error"}"))
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
                Result.failure(RuntimeException("Failed to get preview: ${response.errorBody()?.string() ?: "Unknown error"}"))
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
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("Failed to delete document: ${response.errorBody()?.string()}"))
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
