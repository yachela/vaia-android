package com.vaia.domain.usecase

import com.vaia.domain.repository.TripRepository
import javax.inject.Inject

class GetTripsUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke() = tripRepository.getTrips()
}

class GetTripUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String) = tripRepository.getTrip(tripId)
}

class CreateTripUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(title: String, destination: String, startDate: String, endDate: String, budget: Double) =
        tripRepository.createTrip(title, destination, startDate, endDate, budget)
}

class UpdateTripUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String, title: String, destination: String, startDate: String, endDate: String, budget: Double) =
        tripRepository.updateTrip(tripId, title, destination, startDate, endDate, budget)
}

class DeleteTripUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String) = tripRepository.deleteTrip(tripId)
}