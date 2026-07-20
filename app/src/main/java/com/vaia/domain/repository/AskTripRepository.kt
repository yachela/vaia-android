package com.vaia.domain.repository

import com.vaia.domain.model.TripInsight
import com.vaia.domain.model.TripQuestion

interface AskTripRepository {
    /**
     * Consulta al backend una pregunta que necesita modelo. Nunca lanza: los
     * fallos vuelven como [TripInsight.Unavailable] para que la conversación
     * muestre un mensaje en vez de romperse.
     */
    suspend fun ask(tripId: String, question: TripQuestion): TripInsight
}
