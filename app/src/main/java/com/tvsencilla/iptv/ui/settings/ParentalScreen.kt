package com.tvsencilla.iptv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.model.displayName
import com.tvsencilla.iptv.ui.components.BigButton
import com.tvsencilla.iptv.ui.components.EmptyState
import com.tvsencilla.iptv.ui.components.SectionTitle
import com.tvsencilla.iptv.ui.components.TvScreen

/**
 * Hidden categories disappear from every list, including favourites, and the remaining favourite
 * numbers close up so no gaps appear.
 */
@Composable
fun ParentalScreen(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TvScreen(title = stringResource(R.string.settings_parental)) {
        Column(Modifier.fillMaxSize()) {
            SectionTitle(stringResource(R.string.settings_parental_hidden_categories))
            Spacer(Modifier.height(14.dp))

            if (state.categories.isEmpty()) {
                EmptyState(stringResource(R.string.common_empty))
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.categories, key = { it.id }) { category ->
                    val isHidden = category.id in state.settings.hiddenCategoryIds
                    BigButton(
                        text = displayName(category.name).let { name -> if (isHidden) "✓  $name" else name },
                        onClick = { viewModel.toggleHiddenCategory(category.id) },
                        minHeight = 56.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            BigButton(
                text = stringResource(R.string.common_save),
                onClick = onDone,
                minHeight = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
