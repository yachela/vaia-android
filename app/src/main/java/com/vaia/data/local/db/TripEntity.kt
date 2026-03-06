package com.vaia.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vaia.domain.model.Trip

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val budget: Double,
    val totalExpenses: Double = 0.0,
    val activitiesCount: Int = 0,
    val expensesCount: Int = 0
)

fun TripEntity.toTrip(): Trip = Trip(
    id = id,
    title = title,
    destination = destination,
    startDate = startDate,
    endDate = endDate,
    budget = budget,
    totalExpenses = totalExpenses,
    activitiesCount = activitiesCount,
    expensesCount = expensesCount
)

fun Trip.toEntity(): TripEntity = TripEntity(
    id = id,
    title = title,
    destination = destination,
    startDate = startDate,
    endDate = endDate,
    budget = budget,
    totalExpenses = totalExpenses,
    activitiesCount = activitiesCount,
    expensesCount = expensesCount
)
