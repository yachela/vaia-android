package com.vaia.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Document(
    val id: String,
    val tripId: String,
    val userId: String,
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val description: String? = null,
    val category: String? = null
) : Parcelable

@Parcelize
data class TripDocumentChecklist(
    val id: String,
    val tripId: String,
    val items: List<ChecklistItem> = emptyList(),
    val progress: DocumentProgress? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) : Parcelable

@Parcelize
data class ChecklistItem(
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val document: ChecklistDocument? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) : Parcelable

@Parcelize
data class ChecklistDocument(
    val id: String,
    val checklistItemId: String,
    val fileName: String,
    val filePath: String? = null,
    val mimeType: String,
    val fileSize: Long,
    val source: String,
    val googleDriveFileId: String? = null,
    val uploadedBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) : Parcelable

@Parcelize
data class DocumentProgress(
    val completed: Int,
    val total: Int,
    val percentage: Int
) : Parcelable
