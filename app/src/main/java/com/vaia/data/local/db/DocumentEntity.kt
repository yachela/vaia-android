package com.vaia.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vaia.domain.model.ChecklistDocument
import com.vaia.domain.model.ChecklistItem
import com.vaia.domain.model.Document

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val userId: String,
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val description: String?,
    val category: String?
)

fun DocumentEntity.toDocument(): Document = Document(
    id = id,
    tripId = tripId,
    userId = userId,
    filePath = filePath,
    fileName = fileName,
    mimeType = mimeType,
    fileSize = fileSize,
    description = description,
    category = category
)

fun Document.toEntity(): DocumentEntity = DocumentEntity(
    id = id,
    tripId = tripId,
    userId = userId,
    filePath = filePath,
    fileName = fileName,
    mimeType = mimeType,
    fileSize = fileSize,
    description = description,
    category = category
)

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val name: String,
    val isDefault: Boolean,
    val isCompleted: Boolean,
    val position: Int,
    val docId: String?,
    val docFileName: String?,
    val docFilePath: String?,
    val docMimeType: String?,
    val docFileSize: Long?,
    val docSource: String?,
    val docGoogleDriveFileId: String?,
    val docUploadedBy: String?,
    val createdAt: String?,
    val updatedAt: String?
)

fun ChecklistItemEntity.toChecklistItem(): ChecklistItem = ChecklistItem(
    id = id,
    name = name,
    isDefault = isDefault,
    isCompleted = isCompleted,
    position = position,
    document = if (docId != null) {
        ChecklistDocument(
            id = docId,
            checklistItemId = id,
            fileName = docFileName ?: "",
            filePath = docFilePath,
            mimeType = docMimeType ?: "",
            fileSize = docFileSize ?: 0L,
            source = docSource ?: "",
            googleDriveFileId = docGoogleDriveFileId,
            uploadedBy = docUploadedBy,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    } else null,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ChecklistItem.toEntity(tripId: String): ChecklistItemEntity = ChecklistItemEntity(
    id = id,
    tripId = tripId,
    name = name,
    isDefault = isDefault,
    isCompleted = isCompleted,
    position = position,
    docId = document?.id,
    docFileName = document?.fileName,
    docFilePath = document?.filePath,
    docMimeType = document?.mimeType,
    docFileSize = document?.fileSize,
    docSource = document?.source,
    docGoogleDriveFileId = document?.googleDriveFileId,
    docUploadedBy = document?.uploadedBy,
    createdAt = createdAt,
    updatedAt = updatedAt
)
