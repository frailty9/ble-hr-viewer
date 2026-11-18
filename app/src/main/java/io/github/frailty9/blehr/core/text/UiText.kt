package io.github.frailty9.blehr.core.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.ble.DiagnosticCode
import io.github.frailty9.blehr.ble.HeartRateDiagnostic

/** Keep state language-neutral; resolve it against the current UI or service resources. */
data class UiText(
    @StringRes val resourceId: Int,
    val arguments: List<Any> = emptyList(),
) {
    fun resolve(context: Context): String = context.getString(
        resourceId,
        *arguments.map { if (it is UiText) it.resolve(context) else it }.toTypedArray(),
    )

    @Composable
    fun asString(): String = stringResource(
        resourceId,
        *arguments.map { if (it is UiText) it.asString() else it }.toTypedArray(),
    )
}

fun uiText(@StringRes resourceId: Int, vararg arguments: Any): UiText =
    UiText(resourceId, arguments.toList())

fun HeartRateDiagnostic.asUiText(): UiText = UiText(
    resourceId = when (code) {
        DiagnosticCode.EMPTY_NOTIFICATION -> R.string.diagnostic_empty
        DiagnosticCode.TRUNCATED_HEART_RATE -> R.string.diagnostic_truncated_hr
        DiagnosticCode.RESERVED_CONTACT -> R.string.diagnostic_contact_reserved
        DiagnosticCode.TRUNCATED_ENERGY -> R.string.diagnostic_truncated_energy
        DiagnosticCode.UNKNOWN_ENERGY -> R.string.diagnostic_energy_unknown
        DiagnosticCode.MISSING_RR -> R.string.diagnostic_rr_missing
        DiagnosticCode.ODD_RR_BYTE -> R.string.diagnostic_rr_odd
        DiagnosticCode.TRAILING_BYTES -> R.string.diagnostic_trailing
        DiagnosticCode.RESERVED_FLAGS -> R.string.diagnostic_reserved_flags
        DiagnosticCode.IMPLAUSIBLE_HEART_RATE -> R.string.diagnostic_implausible_hr
        DiagnosticCode.ZERO_HEART_RATE -> R.string.diagnostic_zero_hr
    },
    arguments = arguments,
)
