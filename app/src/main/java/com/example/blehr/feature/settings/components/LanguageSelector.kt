package com.example.blehr.feature.settings.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.blehr.R
import com.example.blehr.data.settings.AppLanguage
import com.example.blehr.ui.theme.Accent
import com.example.blehr.ui.theme.GridLine
import com.example.blehr.ui.theme.Ink
import com.example.blehr.ui.theme.Muted
import com.example.blehr.ui.theme.SurfaceRaised

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
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceRaised)
                    .border(1.dp, GridLine, RoundedCornerShape(8.dp)),
            ) {
                val segmentWidth = maxWidth / AppLanguage.entries.size
                val indicatorOffset by animateDpAsState(
                    targetValue = segmentWidth * AppLanguage.entries.indexOf(language),
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "languageIndicator",
                )
                Box(
                    Modifier.offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                        .width(segmentWidth).fillMaxHeight()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Accent.copy(alpha = 0.18f))
                        .testTag("language_indicator"),
                )
                Row(Modifier.fillMaxSize().selectableGroup()) {
                    AppLanguage.entries.forEach { option ->
                        val selected = option == language
                        val color by animateColorAsState(
                            if (selected) Accent else Ink,
                            animationSpec = tween(160),
                            label = "languageLabel",
                        )
                        Box(
                            Modifier.weight(1f).fillMaxHeight()
                                .testTag("language_${option.tag.ifEmpty { "system" }}")
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onLanguageSelected(option) },
                                )
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = when (option) {
                                    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                                    AppLanguage.CHINESE -> stringResource(R.string.language_chinese)
                                    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                                },
                                color = color,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}
