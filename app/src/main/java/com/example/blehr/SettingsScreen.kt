package com.example.blehr

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.blehr.ble.HeartRateSample
import com.example.blehr.ble.SensorContact
import com.example.blehr.overlay.HeartRateOverlayView
import com.example.blehr.overlay.OverlayAppearance
import com.example.blehr.overlay.OverlayColors
import com.example.blehr.overlay.OverlaySettingsStore
import com.example.blehr.settings.AppLanguage
import com.example.blehr.ui.theme.Accent
import com.example.blehr.ui.theme.Ink
import com.example.blehr.ui.theme.Muted
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.back),
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        LanguageCard(language, onLanguageSelected)
        OverlayAppearanceCard()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LanguageCard(language: AppLanguage, onLanguageSelected: (AppLanguage) -> Unit) {
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

@Composable
private fun OverlayAppearanceCard() {
    val context = LocalContext.current
    val store = remember { OverlaySettingsStore.get(context) }
    val appearance by store.appearance.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.overlay_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = store::reset) {
                    Text(stringResource(R.string.reset_defaults))
                }
            }
            OverlayPreview(appearance)
            Spacer(Modifier.height(10.dp))

            AppearanceLabel(stringResource(R.string.background_color))
            Spacer(Modifier.height(6.dp))
            ColorSwatchRow(
                selected = appearance.backgroundColor,
                colors = OverlayColors.BackgroundPresets,
                onSelect = store::setBackgroundColor,
            )

            Spacer(Modifier.height(10.dp))
            AppearanceLabel(
                stringResource(R.string.background_opacity, (appearance.backgroundAlpha * 100).roundToInt())
            )
            Slider(
                value = appearance.backgroundAlpha,
                onValueChange = store::setBackgroundAlpha,
                modifier = Modifier.fillMaxWidth(),
                valueRange = 0f..1f,
            )

            AppearanceLabel(stringResource(R.string.font_color))
            Spacer(Modifier.height(6.dp))
            ColorSwatchRow(
                selected = appearance.fontColor,
                colors = OverlayColors.FontPresets,
                onSelect = store::setFontColor,
            )

            Spacer(Modifier.height(10.dp))
            AppearanceLabel(
                stringResource(R.string.font_opacity, (appearance.fontAlpha * 100).roundToInt())
            )
            Slider(
                value = appearance.fontAlpha,
                onValueChange = store::setFontAlpha,
                modifier = Modifier.fillMaxWidth(),
                valueRange = 0f..1f,
            )
        }
    }
}

@Composable
private fun AppearanceLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Muted,
    )
}

@Composable
private fun OverlayPreview(appearance: OverlayAppearance) {
    val previewState = remember {
        HeartRateUiState(
            sample = HeartRateSample(
                bpm = 123,
                sensorContact = SensorContact.NOT_SUPPORTED,
                energyExpendedKj = null,
                rrIntervalsRaw = emptyList(),
                flags = 0,
                raw = byteArrayOf(),
                warnings = emptyList(),
                receivedAtMillis = 0L,
            ),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { context ->
                HeartRateOverlayView(context, onClose = {}, onDrag = { _, _ -> }).apply {
                    bindState(previewState)
                }
            },
            update = { view -> view.bindAppearance(appearance) },
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ColorSwatchRow(
    selected: Int,
    colors: List<Int>,
    onSelect: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = if (color == selected) 2.dp else 1.dp,
                        color = if (color == selected) Accent else Muted,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(color) },
            )
        }
    }
}

