package com.tvsencilla.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.NowNext
import com.tvsencilla.iptv.domain.model.displayName
import com.tvsencilla.iptv.ui.components.ProgressStripe
import com.tvsencilla.iptv.ui.components.RemoteImage
import com.tvsencilla.iptv.ui.theme.FocusYellow
import com.tvsencilla.iptv.ui.util.formatHourMinute

/**
 * The strip shown for five seconds after a channel change, and again on the Info key: the number
 * the remote responds to, the channel, what is on now with its end time, and what follows.
 */
@Composable
fun ChannelBanner(
    channel: Channel,
    remoteNumber: Int?,
    nowNext: NowNext,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.86f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (remoteNumber != null) {
            Text(
                text = remoteNumber.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = FocusYellow,
            )
            Spacer(Modifier.width(18.dp))
        }

        RemoteImage(
            url = channel.logoUrl,
            contentDescription = channel.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 92.dp, height = 58.dp).clip(RoundedCornerShape(6.dp)),
        )

        Spacer(Modifier.width(18.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = channel.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val now = nowNext.now
            if (now != null) {
                Text(
                    text = "${now.title} · ${stringResource(R.string.live_until, formatHourMinute(now.endMillis))}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                ProgressStripe(
                    progress = now.progressAt(System.currentTimeMillis()),
                    modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(5.dp)),
                )
            } else {
                Text(
                    text = stringResource(R.string.live_no_epg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            val next = nowNext.next
            if (next != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${stringResource(R.string.live_next)}: " +
                        "${formatHourMinute(next.startMillis)} · ${next.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
