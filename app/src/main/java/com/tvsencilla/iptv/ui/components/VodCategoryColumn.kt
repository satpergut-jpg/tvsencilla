package com.tvsencilla.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.model.Category
import com.tvsencilla.iptv.domain.model.displayName

/** Categoría virtual con lo marcado como "Me gusta", en Películas y en Series. */
const val LIKED_CATEGORY_ID = "__me_gusta__"

/**
 * La columna de categorías de Películas y Series: "Me gusta" primero, después "Todos" y las
 * categorías del proveedor. La elegida lleva una marca escrita, no solo un color.
 */
@Composable
fun VodCategoryColumn(
    categories: List<Category>,
    selectedCategoryId: String?,
    allLabel: String,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun label(text: String, selected: Boolean) = if (selected) "✓  $text" else text

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            BigButton(
                text = label(stringResource(R.string.likes_category), selectedCategoryId == LIKED_CATEGORY_ID),
                icon = Icons.Default.Favorite,
                onClick = { onSelect(LIKED_CATEGORY_ID) },
                minHeight = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            BigButton(
                text = label(allLabel, selectedCategoryId == null),
                onClick = { onSelect(null) },
                minHeight = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        items(categories, key = { it.id }) { category ->
            BigButton(
                text = label(displayName(category.name), selectedCategoryId == category.id),
                onClick = { onSelect(category.id) },
                minHeight = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
