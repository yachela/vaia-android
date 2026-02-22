package com.vaia.data.repository

import com.vaia.data.api.VaiaApiService
import com.vaia.domain.model.Document
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
            Result.failure(e)
        }
    }

    override suspend fun uploadDocument(tripId: String, file: File, description: String?): Result<Document> {
        return try {
            val requestFile = file.asRequestBody(file.extension.toMediaTypeOrNull())
            val documentPart = MultipartBody.Part.createFormData("document", file.name, requestFile)

            val descriptionPart = description?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.uploadDocument(tripId, documentPart, descriptionPart)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data as Document)
            } else {
                Result.failure(RuntimeException("Failed to upload document: ${response.errorBody()?.string() ?: "Unknown error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
            Result.failure(e)
        }
    }
}
