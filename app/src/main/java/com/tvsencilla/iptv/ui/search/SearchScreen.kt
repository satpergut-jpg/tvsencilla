package com.tvsencilla.iptv.ui.search

import android.Manifest
import android.app.Activity
import android.app.SearchManager
import android.content.pm.PackageManager
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.ui.components.BigButton
import com.tvsencilla.iptv.ui.components.ChannelListItem
import com.tvsencilla.iptv.ui.components.EmptyState
import com.tvsencilla.iptv.ui.components.PosterCard
import com.tvsencilla.iptv.ui.components.PosterWidth
import com.tvsencilla.iptv.ui.components.SectionTitle
import com.tvsencilla.iptv.ui.components.TvScreen
import com.tvsencilla.iptv.ui.components.TvTextField
import com.tvsencilla.iptv.ui.util.formatHourMinute
import java.util.Locale

/**
 * Voice is the headline option, because dictating a title is far easier than spelling it out with
 * a D-pad. The keyboard stays available for anyone who prefers it or has no microphone.
 */
@Composable
fun SearchScreen(
    onPlayChannel: (Channel) -> Unit,
    onOpenMovie: (String) -> Unit,
    onOpenSeries: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var voiceUnavailable by remember { mutableStateOf(false) }
    val voiceButton = remember { FocusRequester() }
    val textField = remember { FocusRequester() }

    // Fire TV y otros aparatos no ofrecen reconocimiento de voz a las apps. Allí el botón no
    // serviría de nada, así que se quita y se explica cómo dictar con el propio mando.
    val canRecognizeSpeech = remember {
        context.packageManager
            .queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
            .isNotEmpty()
    }

    // Al entrar, el cursor ya está donde se empieza: en "Buscar hablando", o en el campo de texto
    // si el aparato no permite buscar hablando.
    LaunchedEffect(Unit) {
        runCatching { if (canRecognizeSpeech) voiceButton.requestFocus() else textField.requestFocus() }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        // Cada fabricante devuelve lo dicho en un sitio distinto, y alguno sin RESULT_OK: si hay
        // texto, se usa.
        val data = result.data ?: return@rememberLauncherForActivityResult
        val spoken = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull { it.isNotBlank() }
            ?: data.getStringExtra(SearchManager.QUERY)
            ?: data.dataString?.takeIf { result.resultCode == Activity.RESULT_OK }
            ?: return@rememberLauncherForActivityResult
        viewModel.onVoiceResult(spoken)
    }

    fun launchGoogleVoiceScreen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.search_by_voice_hint))
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            voiceUnavailable = true
        }
    }

    var isListening by remember { mutableStateOf(false) }
    var heardText by remember { mutableStateOf("") }
    var nothingHeard by remember { mutableStateOf(false) }
    val inAppSpeech = remember {
        InAppSpeech(
            context = context,
            onListening = { isListening = it },
            onPartial = { heardText = it },
            onResult = { heardText = ""; viewModel.onVoiceResult(it) },
            onUnavailable = { heardText = ""; launchGoogleVoiceScreen() },
            onNothingHeard = { heardText = ""; nothingHeard = true },
        )
    }
    DisposableEffect(Unit) { onDispose { inAppSpeech.stop() } }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) inAppSpeech.start() else launchGoogleVoiceScreen() }

    fun startVoice() {
        voiceUnavailable = false
        nothingHeard = false
        when {
            !inAppSpeech.isAvailable -> launchGoogleVoiceScreen()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> inAppSpeech.start()
            else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) { viewModel.tuneTo.collect(onPlayChannel) }

    TvScreen(title = stringResource(R.string.search_title)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (!canRecognizeSpeech) {
                Text(
                    text = stringResource(R.string.search_voice_with_remote),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else BigButton(
                text = if (isListening) {
                    heardText.ifBlank { stringResource(R.string.search_listening) }
                } else {
                    stringResource(R.string.search_by_voice)
                },
                icon = Icons.Default.Mic,
                // Pulsar otra vez mientras escucha deja de escuchar.
                onClick = { if (isListening) inAppSpeech.stop() else startVoice() },
                minHeight = 64.dp,
                modifier = Modifier.fillMaxWidth().focusRequester(voiceButton),
            )

            if (nothingHeard) {
                Text(
                    text = stringResource(R.string.search_nothing_heard),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            if (voiceUnavailable) {
                Text(
                    text = stringResource(R.string.search_voice_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            Spacer(Modifier.height(18.dp))

            TvTextField(
                label = stringResource(R.string.search_by_keyboard),
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                focusRequester = textField,
            )

            Spacer(Modifier.height(20.dp))

            if (state.hasQuery && state.isEmpty && !state.isSearching) {
                EmptyState(stringResource(R.string.search_no_results, state.query))
                return@Column
            }

            if (state.programs.isNotEmpty()) {
                SectionTitle(stringResource(R.string.search_results_programs))
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val now = System.currentTimeMillis()
                    state.programs.forEach { match ->
                        val program = match.program
                        ChannelListItem(
                            number = match.channel.favoriteNumber ?: match.channel.listNumber,
                            name = match.channel.name,
                            logoUrl = match.channel.logoUrl,
                            isFavorite = match.channel.isFavorite,
                            nowTitle = if (program.isLiveAt(now)) {
                                stringResource(R.string.search_program_now, program.title)
                            } else {
                                stringResource(
                                    R.string.search_program_at,
                                    formatHourMinute(program.startMillis),
                                    program.title,
                                )
                            },
                            onClick = { onPlayChannel(match.channel) },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            if (state.channels.isNotEmpty()) {
                SectionTitle(stringResource(R.string.search_results_channels))
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.channels.take(6).forEach { channel ->
                        ChannelListItem(
                            number = channel.favoriteNumber ?: channel.listNumber,
                            name = channel.name,
                            logoUrl = channel.logoUrl,
                            isFavorite = channel.isFavorite,
                            onClick = { onPlayChannel(channel) },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            if (state.movies.isNotEmpty()) {
                SectionTitle(stringResource(R.string.search_results_movies))
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(state.movies, key = { it.id }) { movie ->
                        PosterCard(
                            title = movie.title,
                            subtitle = movie.year,
                            posterUrl = movie.posterUrl,
                            onClick = { onOpenMovie(movie.id) },
                            modifier = Modifier.width(PosterWidth),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            if (state.series.isNotEmpty()) {
                SectionTitle(stringResource(R.string.search_results_series))
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(state.series, key = { it.id }) { series ->
                        PosterCard(
                            title = series.title,
                            subtitle = series.year,
                            posterUrl = series.posterUrl,
                            onClick = { onOpenSeries(series.id) },
                            modifier = Modifier.width(PosterWidth),
                        )
                    }
                }
            }
        }
    }
}
