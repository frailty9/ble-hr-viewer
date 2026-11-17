package com.example.blehr.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.blehr.R
import com.example.blehr.data.settings.AppLanguage
import com.example.blehr.ui.theme.Accent
import com.example.blehr.ui.theme.Muted

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppLanguage.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = language == option,
                        onClick = { onLanguageSelected(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, AppLanguage.entries.size),
                        icon = {},
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Accent.copy(alpha = 0.16f),
                            activeContentColor = Accent,
                        ),
                    ) {
                        Text(
                            text = when (option) {
                                AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                                AppLanguage.CHINESE -> stringResource(R.string.language_chinese)
                                AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
