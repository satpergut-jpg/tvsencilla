package com.tvsencilla.iptv.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.ui.components.BigButton
import com.tvsencilla.iptv.ui.components.LoadingState
import com.tvsencilla.iptv.ui.components.SectionTitle
import com.tvsencilla.iptv.ui.components.TvScreen
import com.tvsencilla.iptv.ui.components.TvTextField

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isDone) {
        if (state.isDone) onSetupComplete()
    }

    if (state.isChecking) {
        LoadingState(message = stringResource(R.string.setup_checking))
        return
    }

    BackHandler(enabled = state.method != null) { viewModel.backToMethods() }

    TvScreen(title = stringResource(R.string.setup_title)) {
        when (val method = state.method) {
            null -> MethodChooser(onChoose = viewModel::chooseMethod)
            else -> LoginForm(method = method, state = state, viewModel = viewModel)
        }
    }
}

/** Every sign-in route is one large button, so the choice itself needs no typing. */
@Composable
private fun MethodChooser(onChoose: (LoginMethod) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(stringResource(R.string.setup_choose_title))
        BigButton(
            text = stringResource(R.string.setup_type_xtream),
            icon = Icons.Default.Person,
            onClick = { onChoose(LoginMethod.XTREAM) },
            minHeight = 76.dp,
            modifier = Modifier.fillMaxWidth().focusRequester(first),
        )
        BigButton(
            text = stringResource(R.string.setup_type_xtream_auto),
            icon = Icons.Default.Person,
            onClick = { onChoose(LoginMethod.XTREAM_AUTO) },
            minHeight = 76.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        BigButton(
            text = stringResource(R.string.setup_type_xtream_link),
            icon = Icons.Default.Link,
            onClick = { onChoose(LoginMethod.XTREAM_LINK) },
            minHeight = 76.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        BigButton(
            text = stringResource(R.string.setup_type_m3u),
            icon = Icons.AutoMirrored.Filled.List,
            onClick = { onChoose(LoginMethod.M3U) },
            minHeight = 76.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LoginForm(method: LoginMethod, state: SetupUiState, viewModel: SetupViewModel) {
    val firstField = remember { FocusRequester() }
    LaunchedEffect(method) { firstField.requestFocus() }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        SectionTitle(
            stringResource(
                when (method) {
                    LoginMethod.XTREAM -> R.string.setup_type_xtream
                    LoginMethod.XTREAM_AUTO -> R.string.setup_type_xtream_auto
                    LoginMethod.XTREAM_LINK -> R.string.setup_type_xtream_link
                    LoginMethod.M3U -> R.string.setup_type_m3u
                },
            ),
        )
        Spacer(Modifier.height(14.dp))

        when (method) {
            LoginMethod.XTREAM -> {
                TvTextField(
                    label = stringResource(R.string.setup_field_server),
                    value = state.host,
                    onValueChange = viewModel::onHostChange,
                    keyboardType = KeyboardType.Uri,
                    focusRequester = firstField,
                )
                Spacer(Modifier.height(14.dp))
                TvTextField(
                    label = stringResource(R.string.setup_field_user),
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                )
                Spacer(Modifier.height(14.dp))
                TvTextField(
                    label = stringResource(R.string.setup_field_password),
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    isPassword = true,
                )
            }

            LoginMethod.XTREAM_AUTO -> {
                Text(
                    text = stringResource(R.string.setup_xtream_auto_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                TvTextField(
                    label = stringResource(R.string.setup_field_user),
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                    focusRequester = firstField,
                )
                Spacer(Modifier.height(14.dp))
                TvTextField(
                    label = stringResource(R.string.setup_field_password),
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    isPassword = true,
                )
            }

            LoginMethod.XTREAM_LINK -> {
                TvTextField(
                    label = stringResource(R.string.setup_field_xtream_link),
                    value = state.xtreamLink,
                    onValueChange = viewModel::onXtreamLinkChange,
                    keyboardType = KeyboardType.Uri,
                    focusRequester = firstField,
                )
            }

            LoginMethod.M3U -> {
                TvTextField(
                    label = stringResource(R.string.setup_field_m3u_url),
                    value = state.m3uUrl,
                    onValueChange = viewModel::onM3uUrlChange,
                    keyboardType = KeyboardType.Uri,
                    focusRequester = firstField,
                )
                Spacer(Modifier.height(14.dp))
                TvTextField(
                    label = stringResource(R.string.setup_field_epg_url),
                    value = state.epgUrl,
                    onValueChange = viewModel::onEpgUrlChange,
                    keyboardType = KeyboardType.Uri,
                )
            }
        }

        state.errorMessage?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BigButton(
                text = stringResource(R.string.common_back),
                onClick = viewModel::backToMethods,
                modifier = Modifier.weight(1f),
            )
            BigButton(
                text = stringResource(R.string.setup_connect),
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
