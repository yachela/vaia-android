package com.vaia.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vaia.domain.model.Activity

@Entity(
    tableName = "activities",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class ActivityEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val title: String? = null,
    val description: String? = null,
    val date: String? = null,
    val time: String? = "",
    val location: String? = null,
    val cost: Double? = 0.0,
    @ColumnInfo(name = "sync_status") val syncStatus: String? = "synced" // "synced", "pending", "error"
)

fun ActivityEntity.toActivity(): Activity = Activity(
    id = id,
    title = title ?: "Actividad sin título",
    description = description ?: "",
    date = date ?: "",
    time = time ?: "",
    location = location ?: "",
    cost = cost ?: 0.0
)

fun Activity.toEntity(tripId: String): ActivityEntity = ActivityEntity(
    id = id,
    tripId = tripId,
    title = title,
    description = description,
    date = date,
    time = time,
    location = location,
    cost = cost,
    syncStatus = "synced"
)
