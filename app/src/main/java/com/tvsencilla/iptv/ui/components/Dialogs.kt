package com.tvsencilla.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R

/**
 * A blocking question the user has to answer. It never closes by itself and never on a timer, so
 * nothing can disappear while it is being read.
 */
@Composable
fun ConfirmDialog(
    title: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // The safe answer takes focus, so a stray OK press cannot do the destructive thing.
    val dismissFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { dismissFocus.requestFocus() }

    DialogSurface {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigButton(
                text = dismissText,
                onClick = onDismiss,
                modifier = Modifier.weight(1f).focusRequester(dismissFocus),
            )
            BigButton(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * PIN entry. Remote number keys work, and so does the on-screen pad, because a great many TV
 * remotes no longer have digits at all.
 */
@Composable
fun PinDialog(
    title: String,
    errorMessage: String?,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    hint: String = stringResource(R.string.pin_hint),
) {
    var digits by remember { mutableStateOf("") }

    NumberPadDialog(
        title = title,
        hint = hint,
        errorMessage = errorMessage,
        value = digits,
        masked = true,
        maxLength = PIN_LENGTH,
        onValueChange = { digits = it },
        onSubmit = {
            onSubmit(digits)
            digits = ""
        },
        onDismiss = onDismiss,
    )
}

/** "Poner en el número…": the user types a favourite position with the remote. */
@Composable
fun NumberEntryDialog(
    title: String,
    hint: String,
    maxLength: Int,
    onSubmit: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var digits by remember { mutableStateOf("") }

    NumberPadDialog(
        title = title,
        hint = hint,
        errorMessage = null,
        value = digits,
        masked = false,
        maxLength = maxLength,
        onValueChange = { digits = it },
        onSubmit = { digits.toIntOrNull()?.let(onSubmit) },
        onDismiss = onDismiss,
    )
}

@Composable
private fun NumberPadDialog(
    title: String,
    hint: String,
    errorMessage: String?,
    value: String,
    masked: Boolean,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstKey.requestFocus() }

    val append: (Int) -> Unit = { digit ->
        if (value.length < maxLength) onValueChange(value + digit)
    }

    DialogSurface(
        modifier = Modifier.onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val digit = event.key.asDigit()
            if (digit != null) {
                append(digit)
                true
            } else {
                false
            }
        },
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        Text(
            text = if (masked) "●".repeat(value.length) else value,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { columnIndex, digit ->
                        val isFirst = rowIndex == 0 && columnIndex == 0
                        BigButton(
                            text = digit.toString(),
                            onClick = { append(digit) },
                            modifier = Modifier
                                .size(72.dp)
                                .then(if (isFirst) Modifier.focusRequester(firstKey) else Modifier),
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton(
                    text = stringResource(R.string.common_back),
                    onClick = { if (value.isEmpty()) onDismiss() else onValueChange(value.dropLast(1)) },
                    modifier = Modifier.width(72.dp).height(72.dp),
                )
                BigButton(
                    text = "0",
                    onClick = { append(0) },
                    modifier = Modifier.size(72.dp),
                )
                BigButton(
                    text = stringResource(R.string.common_ok),
                    onClick = onSubmit,
                    enabled = value.isNotEmpty(),
                    modifier = Modifier.width(72.dp).height(72.dp),
                )
            }
        }
    }
}

@Composable
private fun DialogSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = modifier
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

internal fun Key.asDigit(): Int? = when (this) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}

private const val PIN_LENGTH = 4
