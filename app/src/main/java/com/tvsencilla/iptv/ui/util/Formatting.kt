package com.tvsencilla.iptv.ui.util

import androidx.annotation.StringRes
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.provider.ProviderException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Failures are only ever shown as one plain sentence, never as a code or a stack trace. */
@StringRes
fun Throwable.userMessageRes(): Int {
    val reason = (this as? ProviderException)?.reason ?: ProviderException.Reason.UNKNOWN
    return when (reason) {
        ProviderException.Reason.NO_NETWORK -> R.string.error_no_internet
        ProviderException.Reason.UNREACHABLE -> R.string.error_source_unreachable
        ProviderException.Reason.BAD_CREDENTIALS -> R.string.error_bad_credentials
        ProviderException.Reason.EMPTY -> R.string.error_empty_list
        ProviderException.Reason.UNKNOWN -> R.string.error_generic
    }
}

private val hourMinute = SimpleDateFormat("HH:mm", Locale.getDefault())

fun formatHourMinute(millis: Long): String = hourMinute.format(Date(millis))

fun formatDurationMinutes(minutes: Int?): String? {
    if (minutes == null || minutes <= 0) return null
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours > 0) "${hours}h ${rest}min" else "${rest}min"
}
