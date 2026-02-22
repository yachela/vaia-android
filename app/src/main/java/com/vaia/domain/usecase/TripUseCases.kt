package com.vaia.domain.usecase

import com.vaia.domain.repository.TripRepository

class GetTripsUseCase(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke() = tripRepository.getTrips()
}

class GetTripUseCase(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String) = tripRepository.getTrip(tripId)
}

class CreateTripUseCase(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(title: String, destination: String, startDate: String, endDate: String, budget: Double) =
        tripRepository.createTrip(title, destination, startDate, endDate, budget)
}

class UpdateTripUseCase(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String, title: String, destination: String, startDate: String, endDate: String, budget: Double) =
        tripRepository.updateTrip(tripId, title, destination, startDate, endDate, budget)
}

class DeleteTripUseCase(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String) = tripRepository.deleteTrip(tripId)
}