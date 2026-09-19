package com.tvsencilla.iptv.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Escucha dentro de la propia app, como hace la búsqueda de Leanback en Android TV. En algunos
 * aparatos (Xiaomi) la pantalla de voz de Google se abre, oye y se cierra sin devolver nada a la
 * app; escuchando aquí mismo el texto llega siempre.
 */
class InAppSpeech(
    private val context: Context,
    private val onListening: (Boolean) -> Unit,
    private val onPartial: (String) -> Unit,
    private val onResult: (String) -> Unit,
    /** No se ha podido escuchar aquí: quien llama puede probar con la pantalla de Google. */
    private val onUnavailable: () -> Unit,
    /** Se escuchó pero no se entendió nada. */
    private val onNothingHeard: () -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        stop()
        val created = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        if (created == null) {
            onUnavailable()
            return
        }
        recognizer = created
        created.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { created.startListening(intent) }
            .onSuccess { onListening(true) }
            .onFailure { finish(); onUnavailable() }
    }

    fun stop() {
        recognizer?.let { runCatching { it.cancel(); it.destroy() } }
        recognizer = null
        onListening(false)
    }

    private fun finish() {
        recognizer?.let { runCatching { it.destroy() } }
        recognizer = null
        onListening(false)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults.firstText()?.let(onPartial)
        }

        override fun onResults(results: Bundle?) {
            val text = results.firstText()
            finish()
            if (text != null) onResult(text) else onNothingHeard()
        }

        override fun onError(error: Int) {
            finish()
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> onNothingHeard()
                else -> onUnavailable()
            }
        }
    }

    private fun Bundle?.firstText(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull { it.isNotBlank() }
}
