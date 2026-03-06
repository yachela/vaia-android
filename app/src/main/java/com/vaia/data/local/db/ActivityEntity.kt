package com.vaia.data.local.db

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
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val location: String,
    val cost: Double
)

fun ActivityEntity.toActivity(): Activity = Activity(
    id = id,
    title = title,
    description = description,
    date = date,
    time = time,
    location = location,
    cost = cost
)

fun Activity.toEntity(tripId: String): ActivityEntity = ActivityEntity(
    id = id,
    tripId = tripId,
    title = title,
    description = description,
    date = date,
    time = time,
    location = location,
    cost = cost
)
