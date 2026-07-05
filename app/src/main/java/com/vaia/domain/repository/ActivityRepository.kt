package com.vaia.domain.repository

import com.vaia.domain.model.Activity
import com.vaia.domain.model.ActivitySuggestion

interface ActivityRepository {
    suspend fun getActivities(tripId: String): Result<List<Activity>>
    suspend fun getActivity(tripId: String, activityId: String): Result<Activity>
    suspend fun createActivity(tripId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity>
    suspend fun updateActivity(tripId: String, activityId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity>
    suspend fun deleteActivity(tripId: String, activityId: String): Result<Unit>
    suspend fun getSuggestions(tripId: String): Result<List<ActivitySuggestion>>
}
