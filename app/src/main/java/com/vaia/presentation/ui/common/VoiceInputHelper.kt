package com.vaia.presentation.ui.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceInputHelper(private val context: Context) {

    sealed class State {
        object Idle : State()
        object Listening : State()
        object Processing : State()
        data class Success(val text: String) : State()
        data class Error(val message: String) : State()
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = State.Listening
        }

        override fun onBeginningOfSpeech() {
            _state.value = State.Listening
        }

        override fun onRmsChanged(rmsdB: Float) {
            _rmsDb.value = rmsdB
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = State.Processing
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Error de audio."
                SpeechRecognizer.ERROR_CLIENT -> "Error de cliente nativo."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos de micrófono insuficientes."
                SpeechRecognizer.ERROR_NETWORK -> "Error de red."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red agotado."
                SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó coincidencia. Por favor, habla más claro."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El servicio de voz está ocupado."
                SpeechRecognizer.ERROR_SERVER -> "Error en el servidor de voz de Google."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz. Tiempo de espera agotado."
                else -> "Error de voz desconocido."
            }
            _state.value = State.Error(message)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                _state.value = State.Success(text)
            } else {
                _state.value = State.Error("No se pudo entender la voz.")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = State.Error("El reconocimiento de voz no está disponible en este dispositivo.")
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        try {
            speechRecognizer?.startListening(intent)
            _state.value = State.Listening
        } catch (e: Exception) {
            _state.value = State.Error("No se pudo iniciar la escucha: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _rmsDb.value = 0f
    }

    fun reset() {
        stopListening()
        _state.value = State.Idle
    }
}
