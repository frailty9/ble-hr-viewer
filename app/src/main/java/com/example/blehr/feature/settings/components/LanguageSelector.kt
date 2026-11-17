package com.example.blehr.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.blehr.R
import com.example.blehr.data.settings.AppLanguage
import com.example.blehr.ui.components.AnimatedSegmentedSelector
import com.example.blehr.ui.theme.Muted

@Composable
internal fun LanguageSelector(language: AppLanguage, onLanguageSelected: (AppLanguage) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(stringResource(R.string.language_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.language_description),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )
            AnimatedSegmentedSelector(
                labels = listOf(
                    stringResource(R.string.language_system),
                    stringResource(R.string.language_chinese),
                    stringResource(R.string.language_english),
                ),
                keys = listOf("system", "zh", "en"),
                selectedIndex = AppLanguage.entries.indexOf(language),
                onSelect = { onLanguageSelected(AppLanguage.entries[it]) },
                tagPrefix = "language",
            )
        }
    }
}
