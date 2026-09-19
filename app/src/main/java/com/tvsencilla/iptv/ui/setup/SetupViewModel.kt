package com.tvsencilla.iptv.ui.setup

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.model.ContentSource
import com.tvsencilla.iptv.domain.model.SourceType
import com.tvsencilla.iptv.domain.repository.SourceRepository
import com.tvsencilla.iptv.ui.util.userMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The ways a user can sign in, all of them typed with the remote. */
enum class LoginMethod { XTREAM, XTREAM_LINK, M3U }

data class SetupUiState(
    /** Null while the user is still choosing how to sign in. */
    val method: LoginMethod? = null,
    val host: String = "",
    val username: String = "",
    val password: String = "",
    val xtreamLink: String = "",
    val m3uUrl: String = "",
    val epgUrl: String = "",
    val isChecking: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    val isDone: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isChecking && when (method) {
            LoginMethod.XTREAM -> host.isNotBlank() && username.isNotBlank() && password.isNotBlank()
            LoginMethod.XTREAM_LINK -> xtreamLink.isNotBlank()
            LoginMethod.M3U -> m3uUrl.isNotBlank()
            null -> false
        }
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun chooseMethod(method: LoginMethod) =
        _state.update { it.copy(method = method, errorMessage = null) }

    fun backToMethods() = _state.update { it.copy(method = null, errorMessage = null) }

    fun onHostChange(value: String) = _state.update { it.copy(host = value) }

    fun onUsernameChange(value: String) = _state.update { it.copy(username = value) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value) }

    fun onXtreamLinkChange(value: String) = _state.update { it.copy(xtreamLink = value) }

    fun onM3uUrlChange(value: String) = _state.update { it.copy(m3uUrl = value) }

    fun onEpgUrlChange(value: String) = _state.update { it.copy(epgUrl = value) }

    fun submit() {
        val current = _state.value
        val source = when (current.method) {
            LoginMethod.XTREAM -> ContentSource(
                type = SourceType.XTREAM,
                host = current.host.trim(),
                username = current.username.trim(),
                password = current.password,
            )

            LoginMethod.XTREAM_LINK -> parseXtreamLink(current.xtreamLink.trim()) ?: run {
                _state.update { it.copy(errorMessage = R.string.setup_error_bad_link) }
                return
            }

            LoginMethod.M3U -> ContentSource(
                type = SourceType.M3U,
                m3uUrl = current.m3uUrl.trim(),
                epgUrl = current.epgUrl.trim().ifBlank { null },
            )

            null -> return
        }

        viewModelScope.launch {
            _state.update { it.copy(isChecking = true, errorMessage = null) }
            runCatching { sourceRepository.save(source) }
                .onSuccess { _state.update { it.copy(isChecking = false, isDone = true) } }
                .onFailure { error ->
                    _state.update { it.copy(isChecking = false, errorMessage = error.userMessageRes()) }
                }
        }
    }

    /**
     * Providers usually hand out a single link such as
     * `http://server:8080/get.php?username=u&password=p&type=m3u_plus`. Signing in through the
     * Xtream API with it gives movies and series too, which the plain playlist would not.
     */
    private fun parseXtreamLink(link: String): ContentSource? {
        val uri = runCatching { Uri.parse(link) }.getOrNull() ?: return null
        val scheme = uri.scheme ?: return null
        val authority = uri.encodedAuthority ?: return null
        val username = uri.getQueryParameter("username")?.takeIf { it.isNotBlank() } ?: return null
        val password = uri.getQueryParameter("password")?.takeIf { it.isNotBlank() } ?: return null
        return ContentSource(
            type = SourceType.XTREAM,
            host = "$scheme://$authority",
            username = username,
            password = password,
        )
    }
}
