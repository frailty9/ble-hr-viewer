package io.github.frailty9.blehr.ui.legal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.ui.theme.Ink
import io.github.frailty9.blehr.ui.theme.Muted
import io.github.frailty9.blehr.ui.theme.PageMargin

@Composable
fun LegalDocumentScreen(document: LegalDocument, onBack: () -> Unit) {
    val context = LocalContext.current
    val text = remember(context, document) { LegalDocuments.read(context, document) }
    Box(
        Modifier.fillMaxSize().testTag("legal_screen")
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = PageMargin),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = PageMargin),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back),
                    )
                }
                Text(
                    text = stringResource(document.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (text == null) {
                Text(
                    text = stringResource(R.string.legal_load_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    modifier = Modifier.padding(vertical = PageMargin),
                )
            } else {
                SelectionContainer(
                    Modifier.weight(1f).fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = PageMargin),
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                    )
                }
            }
        }
    }
}
