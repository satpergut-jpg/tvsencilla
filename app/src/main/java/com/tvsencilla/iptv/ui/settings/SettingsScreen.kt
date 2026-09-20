package com.tvsencilla.iptv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.BuildConfig
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.model.FontSizeOption
import com.tvsencilla.iptv.domain.model.NumberingMode
import com.tvsencilla.iptv.domain.model.SearchInputMode
import com.tvsencilla.iptv.domain.model.SubtitleSizeOption
import com.tvsencilla.iptv.ui.components.BigButton
import com.tvsencilla.iptv.ui.components.MessageBanner
import com.tvsencilla.iptv.ui.components.PinDialog
import com.tvsencilla.iptv.ui.components.SectionTitle
import com.tvsencilla.iptv.ui.components.TvScreen
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    onSourceCleared: () -> Unit,
    onOpenParental: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sourceCleared by viewModel.sourceCleared.collectAsStateWithLifecycle()

    LaunchedEffect(sourceCleared) {
        if (sourceCleared) onSourceCleared()
    }

    TvScreen(title = stringResource(R.string.settings_title)) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SectionTitle(stringResource(R.string.settings_simple_mode))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        if (state.settings.simpleMode) {
                            R.string.settings_simple_mode_on
                        } else {
                            R.string.settings_simple_mode_off
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                BigButton(
                    text = stringResource(
                        if (state.settings.simpleMode) {
                            R.string.settings_simple_mode_off
                        } else {
                            R.string.settings_simple_mode_on
                        },
                    ),
                    onClick = {
                        if (state.settings.simpleMode) {
                            viewModel.requestSimpleModeOff()
                        } else {
                            viewModel.enableSimpleMode()
                        }
                    },
                    minHeight = 56.dp,
                    modifier = Modifier.fillMaxWidth(),
                )

                SettingsGroup(stringResource(R.string.settings_font_size)) {
                    OptionRow(
                        options = FontSizeOption.entries.map { option ->
                            option to stringResource(option.labelRes())
                        },
                        selected = state.settings.fontSize,
                        onSelect = viewModel::setFontSize,
                    )
                }

                SettingsGroup(stringResource(R.string.settings_subtitle_size)) {
                    OptionRow(
                        options = SubtitleSizeOption.entries.map { option ->
                            option to stringResource(option.labelRes())
                        },
                        selected = state.settings.subtitleSize,
                        onSelect = viewModel::setSubtitleSize,
                    )
                }

                SettingsGroup(stringResource(R.string.settings_numbering)) {
                    OptionRow(
                        options = NumberingMode.entries.map { mode ->
                            mode to stringResource(mode.labelRes())
                        },
                        selected = state.settings.numberingMode,
                        onSelect = viewModel::setNumberingMode,
                    )
                }

                SettingsGroup(stringResource(R.string.settings_start_on_last_channel)) {
                    BigButton(
                        text = stringResource(
                            if (state.settings.startOnLastChannel) R.string.common_yes else R.string.common_no,
                        ),
                        onClick = { viewModel.setStartOnLastChannel(!state.settings.startOnLastChannel) },
                        minHeight = 56.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SettingsGroup(stringResource(R.string.settings_search_mode)) {
                    OptionRow(
                        options = SearchInputMode.entries.map { mode ->
                            mode to stringResource(mode.labelRes())
                        },
                        selected = state.settings.searchInputMode,
                        onSelect = viewModel::setSearchInputMode,
                    )
                }

                SettingsGroup(stringResource(R.string.settings_voice_auto_tune)) {
                    BigButton(
                        text = stringResource(
                            if (state.settings.voiceAutoTune) R.string.common_yes else R.string.common_no,
                        ),
                        onClick = { viewModel.setVoiceAutoTune(!state.settings.voiceAutoTune) },
                        minHeight = 56.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SettingsGroup(stringResource(R.string.settings_parental)) {
                    BigButton(
                        text = stringResource(R.string.settings_parental_hidden_categories),
                        onClick = viewModel::requestParental,
                        minHeight = 56.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    BigButton(
                        text = stringResource(R.string.settings_change_pin),
                        onClick = viewModel::requestChangePin,
                        minHeight = 56.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.showAdvanced) {
                    SettingsGroup(stringResource(R.string.settings_epg_grid)) {
                        BigButton(
                            text = stringResource(
                                if (state.settings.showEpgGrid) R.string.common_yes else R.string.common_no,
                            ),
                            onClick = { viewModel.setShowEpgGrid(!state.settings.showEpgGrid) },
                            minHeight = 56.dp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    SettingsGroup(stringResource(R.string.settings_audio_language)) {
                        OptionRow(
                            options = LANGUAGES,
                            selected = state.settings.preferredAudioLanguage,
                            onSelect = viewModel::setAudioLanguage,
                        )
                    }

                    SettingsGroup(stringResource(R.string.settings_subtitle_language)) {
                        OptionRow(
                            options = LANGUAGES,
                            selected = state.settings.preferredSubtitleLanguage,
                            onSelect = viewModel::setSubtitleLanguage,
                        )
                    }
                }

                SettingsGroup(stringResource(R.string.settings_about)) {
                    Text(
                        text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    BigButton(
                        text = stringResource(R.string.settings_refresh_epg),
                        onClick = viewModel::refreshEpgNow,
                        minHeight = 56.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    BigButton(
                        text = stringResource(R.string.settings_clear_cache),
                        onClick = viewModel::clearCache,
                        minHeight = 56.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    BigButton(
                        text = stringResource(R.string.settings_change_source),
                        onClick = viewModel::requestChangeSource,
                        minHeight = 56.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(26.dp))
            }

            state.notice?.let { notice ->
                LaunchedEffect(notice) {
                    delay(8_000L)
                    viewModel.dismissNotice()
                }
                MessageBanner(
                    message = stringResource(notice),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                )
            }
        }
    }

    if (state.pinPurpose != null) {
        PinDialog(
            title = stringResource(R.string.pin_title),
            errorMessage = state.pinError?.let { stringResource(it) },
            hint = stringResource(R.string.pin_default_notice),
            onSubmit = { pin ->
                viewModel.submitPin(pin) { purpose ->
                    when (purpose) {
                        PinPurpose.OPEN_PARENTAL -> onOpenParental()
                        PinPurpose.CHANGE_SOURCE -> viewModel.clearSource()
                        PinPurpose.LEAVE_SIMPLE_MODE, PinPurpose.CHANGE_PIN -> Unit
                    }
                }
            },
            onDismiss = viewModel::dismissPin,
        )
    }

    if (state.isChangingPin) {
        PinDialog(
            title = stringResource(R.string.pin_create_title),
            errorMessage = state.pinError?.let { stringResource(it) },
            onSubmit = viewModel::saveNewPin,
            onDismiss = viewModel::dismissPin,
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(26.dp))
    SectionTitle(title)
    Spacer(Modifier.height(10.dp))
    content()
}

@Composable
private fun <T> OptionRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { (value, label) ->
            BigButton(
                // The chosen option is marked in words, not by colour alone.
                text = if (value == selected) "✓  $label" else label,
                onClick = { onSelect(value) },
                minHeight = 56.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun FontSizeOption.labelRes(): Int = when (this) {
    FontSizeOption.NORMAL -> R.string.settings_font_normal
    FontSizeOption.LARGE -> R.string.settings_font_large
    FontSizeOption.XLARGE -> R.string.settings_font_xlarge
}

private fun SubtitleSizeOption.labelRes(): Int = when (this) {
    SubtitleSizeOption.NORMAL -> R.string.settings_font_normal
    SubtitleSizeOption.LARGE -> R.string.settings_font_large
    SubtitleSizeOption.XLARGE -> R.string.settings_font_xlarge
}

private fun NumberingMode.labelRes(): Int = when (this) {
    NumberingMode.FAVORITES -> R.string.settings_numbering_favorites
    NumberingMode.FULL_LIST -> R.string.settings_numbering_full
}

private fun SearchInputMode.labelRes(): Int = when (this) {
    SearchInputMode.VOICE -> R.string.settings_search_mode_voice
    SearchInputMode.TEXT -> R.string.settings_search_mode_text
    SearchInputMode.BOTH -> R.string.settings_search_mode_both
}

private val LANGUAGES = listOf(
    "es" to "Español",
    "en" to "English",
    "fr" to "Français",
)
