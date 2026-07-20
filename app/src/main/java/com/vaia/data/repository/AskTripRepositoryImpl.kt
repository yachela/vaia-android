package com.vaia.data.repository

import com.vaia.data.api.AskRequest
import com.vaia.data.api.VaiaApiService
import com.vaia.data.network.ConnectivityObserver
import com.vaia.domain.model.TripInsight
import com.vaia.domain.model.TripQuestion
import com.vaia.domain.model.UnavailableReason
import com.vaia.domain.repository.AskTripRepository
import javax.inject.Inject

class AskTripRepositoryImpl @Inject constructor(
    private val apiService: VaiaApiService,
    private val connectivity: ConnectivityObserver
) : AskTripRepository {

    override suspend fun ask(tripId: String, question: TripQuestion): TripInsight {
        val apiId = question.apiId
            ?: return TripInsight.Unavailable(UnavailableReason.SERVICE_ERROR)

        // Sin conexión ni siquiera se intenta: la llamada tardaría 90 s en fallar.
        if (!connectivity.isConnected()) {
            return TripInsight.Unavailable(UnavailableReason.OFFLINE)
        }

        return try {
            val response = apiService.askTrip(tripId, AskRequest(apiId))
            val answer = response.body()?.data?.answer

            when {
                response.isSuccessful && !answer.isNullOrBlank() ->
                    TripInsight.Generated(answer)

                // El throttle del backend responde 429 con su propio mensaje.
                response.code() == 429 -> TripInsight.Unavailable(UnavailableReason.RATE_LIMITED)

                else -> TripInsight.Unavailable(UnavailableReason.SERVICE_ERROR)
            }
        } catch (e: Exception) {
            // Timeout, DNS o cualquier fallo de red: mismo tratamiento amigable.
            TripInsight.Unavailable(UnavailableReason.SERVICE_ERROR)
        }
    }
}
