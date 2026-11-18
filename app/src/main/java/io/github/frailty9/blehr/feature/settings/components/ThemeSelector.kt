package io.github.frailty9.blehr.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.data.settings.AppTheme
import io.github.frailty9.blehr.ui.components.AnimatedSegmentedSelector
import io.github.frailty9.blehr.ui.theme.LocalAppShapes
import io.github.frailty9.blehr.ui.theme.Muted

@Composable
internal fun ThemeSelector(theme: AppTheme, onThemeSelected: (AppTheme) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.theme_description),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )
            AnimatedSegmentedSelector(
                labels = listOf(
                    stringResource(R.string.theme_system),
                    stringResource(R.string.theme_light),
                    stringResource(R.string.theme_dark),
                ),
                keys = AppTheme.entries.map { it.key },
                selectedIndex = AppTheme.entries.indexOf(theme),
                onSelect = { onThemeSelected(AppTheme.entries[it]) },
                tagPrefix = "theme",
            )
        }
    }
}
