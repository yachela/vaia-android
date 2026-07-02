package com.vaia.data.api.dto

import com.google.gson.annotations.SerializedName
import com.vaia.domain.model.ChecklistDocument
import com.vaia.domain.model.ChecklistItem
import com.vaia.domain.model.Document
import com.vaia.domain.model.DocumentProgress
import com.vaia.domain.model.TripDocumentChecklist

// DTOs de red para documentos y checklist de documentos. La capa domain no conoce Gson.

data class DocumentDto(
    val id: String?,
    @SerializedName("trip_id") val tripId: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("file_path") val filePath: String? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    val description: String? = null,
    val category: String? = null
)

data class TripDocumentChecklistDto(
    val id: String?,
    @SerializedName("trip_id")
    val tripId: String? = null,
    val items: List<ChecklistItemDto>? = null,
    val progress: DocumentProgressDto? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class ChecklistItemDto(
    val id: String?,
    val name: String?,
    @SerializedName("is_default")
    val isDefault: Boolean? = null,
    @SerializedName("is_completed")
    val isCompleted: Boolean? = null,
    val position: Int? = null,
    val document: ChecklistDocumentDto? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class ChecklistDocumentDto(
    val id: String?,
    @SerializedName("checklist_item_id")
    val checklistItemId: String? = null,
    @SerializedName("file_name")
    val fileName: String? = null,
    @SerializedName("file_path")
    val filePath: String? = null,
    @SerializedName("mime_type")
    val mimeType: String? = null,
    @SerializedName("file_size")
    val fileSize: Long? = null,
    val source: String? = null,
    @SerializedName("google_drive_file_id")
    val googleDriveFileId: String? = null,
    @SerializedName("uploaded_by")
    val uploadedBy: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class DocumentProgressDto(
    val completed: Int? = null,
    val total: Int? = null,
    val percentage: Int? = null
)

fun DocumentDto.toDomain(): Document = Document(
    id = id.orEmpty(),
    tripId = tripId.orEmpty(),
    userId = userId.orEmpty(),
    filePath = filePath.orEmpty(),
    fileName = fileName.orEmpty(),
    mimeType = mimeType.orEmpty(),
    fileSize = fileSize ?: 0L,
    description = description,
    category = category
)

fun TripDocumentChecklistDto.toDomain(): TripDocumentChecklist = TripDocumentChecklist(
    id = id.orEmpty(),
    tripId = tripId.orEmpty(),
    items = items?.map { it.toDomain() } ?: emptyList(),
    progress = progress?.toDomain(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ChecklistItemDto.toDomain(): ChecklistItem = ChecklistItem(
    id = id.orEmpty(),
    name = name.orEmpty(),
    isDefault = isDefault ?: false,
    isCompleted = isCompleted ?: false,
    position = position ?: 0,
    document = document?.toDomain(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ChecklistDocumentDto.toDomain(): ChecklistDocument = ChecklistDocument(
    id = id.orEmpty(),
    checklistItemId = checklistItemId.orEmpty(),
    fileName = fileName.orEmpty(),
    filePath = filePath,
    mimeType = mimeType.orEmpty(),
    fileSize = fileSize ?: 0L,
    source = source.orEmpty(),
    googleDriveFileId = googleDriveFileId,
    uploadedBy = uploadedBy,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun DocumentProgressDto.toDomain(): DocumentProgress = DocumentProgress(
    completed = completed ?: 0,
    total = total ?: 0,
    percentage = percentage ?: 0
)
