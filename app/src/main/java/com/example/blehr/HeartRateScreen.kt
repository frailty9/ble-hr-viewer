package com.example.blehr

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.blehr.ble.HeartRateSample
import com.example.blehr.ble.ScannedDevice
import com.example.blehr.ble.SensorContact
import com.example.blehr.overlay.HeartRateOverlayService
import com.example.blehr.overlay.HeartRateOverlayView
import com.example.blehr.overlay.OverlayAppearance
import com.example.blehr.overlay.OverlayColors
import com.example.blehr.overlay.OverlaySettingsStore
import com.example.blehr.ui.theme.Accent
import com.example.blehr.ui.theme.GridLine
import com.example.blehr.ui.theme.Ink
import com.example.blehr.ui.theme.Muted
import com.example.blehr.ui.theme.OkGreen
import com.example.blehr.ui.theme.SurfaceRaised
import com.example.blehr.ui.theme.WarnAmber
import com.example.blehr.ui.theme.ErrorRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun HeartRateScreen(viewModel: HeartRateViewModel) {
    val state by viewModel.ui.collectAsState()
    val context = LocalContext.current
    val openBluetoothSettings: () -> Unit = {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    var overlayPending by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        overlayGranted = Settings.canDrawOverlays(context)
        if (overlayPending && overlayGranted) {
            HeartRateOverlayService.show(context)
            Toast.makeText(context, "悬浮窗已开启", Toast.LENGTH_SHORT).show()
        } else if (overlayPending) {
            Toast.makeText(context, "未授予悬浮窗权限", Toast.LENGTH_SHORT).show()
        }
        overlayPending = false
    }
    val showOverlay: () -> Unit = {
        if (Settings.canDrawOverlays(context)) {
            HeartRateOverlayService.show(context)
            Toast.makeText(context, "悬浮窗已开启，可拖动，点 × 关闭", Toast.LENGTH_SHORT).show()
        } else {
            overlayPending = true
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri(),
                )
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(14.dp),
    ) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                ) {
                    AppHeader(state)
                    Spacer(Modifier.height(10.dp))
                    BpmCard(state)
                    Spacer(Modifier.height(10.dp))
                    TrendCard(state)
                    Spacer(Modifier.height(10.dp))
                    PacketCard(state)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                ) {
                    DeviceCard(state, viewModel, openBluetoothSettings)
                    Spacer(Modifier.height(10.dp))
                    OverlayCard(overlayGranted, showOverlay)
                    Spacer(Modifier.height(10.dp))
                    OverlayAppearanceCard()
                    Spacer(Modifier.height(10.dp))
                    ErrorCard(state)
                    Spacer(Modifier.height(10.dp))
                    HelpCard()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                AppHeader(state)
                Spacer(Modifier.height(10.dp))
                BpmCard(state)
                Spacer(Modifier.height(10.dp))
                TrendCard(state)
                Spacer(Modifier.height(10.dp))
                DeviceCard(state, viewModel, openBluetoothSettings)
                Spacer(Modifier.height(10.dp))
                OverlayCard(overlayGranted, showOverlay)
                Spacer(Modifier.height(10.dp))
                OverlayAppearanceCard()
                Spacer(Modifier.height(10.dp))
                ErrorCard(state)
                Spacer(Modifier.height(10.dp))
                PacketCard(state)
                Spacer(Modifier.height(10.dp))
                HelpCard()
            }
        }
    }
}

@Composable
private fun AppHeader(state: HeartRateUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Text(
                text = "接收 BLE 心率，支持实时悬浮窗",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
        val (label, color) = when (state.connection) {
            ConnectionPhase.SCANNING -> "扫描中" to WarnAmber
            ConnectionPhase.CONNECTING -> "连接中" to WarnAmber
            ConnectionPhase.CONNECTED -> "已连接" to OkGreen
            ConnectionPhase.DISCONNECTED -> "已断开" to Muted
            ConnectionPhase.ERROR -> "错误" to ErrorRed
            ConnectionPhase.IDLE -> "未连接" to Muted
        }
        StatusChip(label, color)
    }
}

@Composable
private fun BpmCard(state: HeartRateUiState) {
    val sample = state.sample
    val bpm = sample?.bpm ?: 0
    val contact = sample?.sensorContact ?: SensorContact.NOT_SUPPORTED
    val hasSample = sample != null
    val bpmColor = when {
        !hasSample -> Muted
        bpm <= 0 -> WarnAmber
        contact == SensorContact.NOT_DETECTED -> WarnAmber
        else -> Accent
    }
    val deviceLine = when {
        state.connection == ConnectionPhase.CONNECTED ->
            "${state.connectedName ?: "未知设备"} · ${state.connectedAddress ?: "--"}"
        state.connection == ConnectionPhase.CONNECTING -> "正在连接…"
        state.connection == ConnectionPhase.SCANNING -> "正在扫描附近设备…"
        state.connection == ConnectionPhase.ERROR -> "连接异常，请重试"
        else -> "未连接手环"
    }
    val contactText = when {
        !hasSample -> "佩戴: --"
        contact == SensorContact.NOT_DETECTED -> "佩戴: 未检测"
        bpm == 0 -> "无脉搏信号"
        contact == SensorContact.DETECTED -> "佩戴: 已检测"
        else -> "佩戴: 不支持检测"
    }
    val contactColor = when {
        !hasSample -> Muted
        contact == SensorContact.NOT_DETECTED -> WarnAmber
        bpm == 0 -> WarnAmber
        contact == SensorContact.DETECTED -> OkGreen
        else -> Muted
    }
    val rrText = sample?.rrIntervalsMs?.takeIf { it.isNotEmpty() }?.let { intervals ->
        "RR " + intervals.takeLast(2).joinToString(" / ") { "%.0f".format(Locale.ROOT, it) } + " ms"
    } ?: "RR --"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingHeart(bpm = bpm, contact = contact)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (hasSample) bpm.toString() else "--",
                            fontSize = 84.sp,
                            lineHeight = 86.sp,
                            fontWeight = FontWeight.Black,
                            color = bpmColor,
                        )
                        Text(
                            text = "BPM",
                            modifier = Modifier.padding(start = 6.dp, bottom = 14.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Muted,
                        )
                    }
                    Text(
                        text = deviceLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(contactText, contactColor)
                StatusChip(rrText, Muted)
            }
            if (sample != null && sample.warnings.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "告警: " + sample.warnings.joinToString("; "),
                    style = MaterialTheme.typography.bodySmall,
                    color = WarnAmber,
                )
            }
            if (hasSample && (bpm == 0 || contact == SensorContact.NOT_DETECTED)) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (bpm == 0) {
                        "未检测到脉搏（0 BPM）。实测手环未佩戴时仍可能报告“已检测到佩戴”，请以 0 BPM 为准。原始包仍会被记录。"
                    } else {
                        "手环报告未检测到佩戴，心率数据可能无效。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WarnAmber,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = state.status,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.connection == ConnectionPhase.ERROR) ErrorRed else Muted,
            )
        }
    }
}

@Composable
private fun PulsingHeart(bpm: Int, contact: SensorContact) {
    val beatMillis = if (bpm > 0) (60_000 / bpm / 2).coerceIn(180, 900) else 1_200
    val transition = rememberInfiniteTransition(label = "heartbeat")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = if (bpm > 0) 1.08f else 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = beatMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heartScale",
    )
    val color = when {
        bpm <= 0 -> Muted
        contact == SensorContact.NOT_DETECTED -> WarnAmber
        else -> Accent
    }
    Canvas(
        modifier = Modifier
            .size(70.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        val base = size.minDimension / 38f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val path = Path()
        for (index in 0..71) {
            val angle = index * (2.0 * PI / 72.0)
            val x = 16.0 * sin(angle) * sin(angle) * sin(angle)
            val y = 13.0 * cos(angle) -
                5.0 * cos(2.0 * angle) -
                2.0 * cos(3.0 * angle) -
                cos(4.0 * angle)
            val px = centerX + (x * base).toFloat()
            val py = centerY - (y * base).toFloat()
            if (index == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.close()
        drawPath(path = path, color = color)
    }
}

@Composable
private fun TrendCard(state: HeartRateUiState) {
    val history = state.history
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "心率趋势",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (history.isEmpty()) "-- BPM" else "${history.last()} BPM",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Accent,
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
            ) {
                Sparkline(history, Modifier.fillMaxSize())
                if (history.size < 2) {
                    Text(
                        text = "等待更多心率通知…",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    text = if (history.isEmpty()) "范围 --" else
                        "范围 ${history.minOrNull() ?: 0} – ${history.maxOrNull() ?: 0} BPM",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "最近 ${history.size} 个通知",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                )
            }
        }
    }
}

@Composable
private fun Sparkline(values: List<Int>, modifier: Modifier) {
    Canvas(modifier) {
        val width = size.width
        val height = size.height
        for (index in 1..3) {
            val y = height * index / 4f
            drawLine(
                color = GridLine,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
            )
        }
        if (values.size < 2) return@Canvas

        val minimum = values.minOrNull() ?: 0
        val maximum = values.maxOrNull() ?: 0
        val span = max(10, maximum - minimum).toFloat()
        val low = minimum - span * 0.15f
        val high = maximum + span * 0.15f
        val denominator = (values.size - 1).toFloat()

        fun xAt(index: Int): Float = width * index / denominator
        fun yAt(value: Int): Float = height - height * (value - low) / (high - low)

        val path = Path()
        values.forEachIndexed { index, value ->
            if (index == 0) {
                path.moveTo(xAt(index), yAt(value))
            } else {
                path.lineTo(xAt(index), yAt(value))
            }
        }
        drawPath(
            path = path,
            color = Accent,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        drawCircle(
            color = Accent,
            radius = 3.5.dp.toPx(),
            center = Offset(xAt(values.lastIndex), yAt(values.last())),
        )
    }
}

@Composable
private fun DeviceCard(
    state: HeartRateUiState,
    viewModel: HeartRateViewModel,
    openBluetoothSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "附近设备",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                if (state.scanning) {
                    StatusChip("扫描中", WarnAmber)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = {
                    if (state.scanning) viewModel.stopScan() else viewModel.startScan()
                }) {
                    Text(if (state.scanning) "停止扫描" else "扫描 BLE")
                }
                if (state.connection == ConnectionPhase.CONNECTED ||
                    state.connection == ConnectionPhase.CONNECTING
                ) {
                    OutlinedButton(onClick = viewModel::disconnect) {
                        Text("断开")
                    }
                }
            }
            TextButton(onClick = openBluetoothSettings) {
                Text("打开蓝牙设置")
            }
            if (state.scanning) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = WarnAmber,
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(238.dp),
            ) {
                if (state.devices.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (state.scanning) "正在搜索…" else "未发现设备",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "确认手机蓝牙已打开，手环已开启心率广播",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.devices, key = { it.address }) { device ->
                            DeviceRow(
                                device = device,
                                selected = state.connection == ConnectionPhase.CONNECTED &&
                                    device.address == state.connectedAddress,
                                onClick = { viewModel.connect(device) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: ScannedDevice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Accent.copy(alpha = 0.12f) else SurfaceRaised,
        border = if (selected) BorderStroke(1.dp, Accent) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "未命名设备",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Muted,
                )
            }
            if (device.hasHeartRateService) {
                StatusChip("HR", OkGreen)
            }
            Text(
                text = "${device.rssi} dBm",
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
            )
        }
    }
}

@Composable
private fun PacketCard(state: HeartRateUiState) {
    val sample = state.sample
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "最近原始包",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "0x2A37",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Muted,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.lastRawHex.ifBlank { "--" },
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = Ink,
            )
            if (sample != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = buildString {
                        append("flags=0x%02x".format(Locale.ROOT, sample.flags))
                        append(" · ")
                        append("energy=")
                        append(sample.energyExpendedKj?.let { "$it kJ" } ?: "--")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Muted,
                )
                sample.warnings.forEach { warning ->
                    Text(
                        text = "! $warning",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarnAmber,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayCard(granted: Boolean, onShow: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "悬浮窗心率",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                StatusChip(
                    text = if (granted) "权限已授予" else "需授权",
                    color = if (granted) OkGreen else WarnAmber,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "小型悬浮窗只显示静态心形和 BPM，数值区固定预留三位数字的宽度。"
                    + "需要“显示在其他应用上层”权限；拖动移动，长按关闭；点击不响应，避免游戏误触。",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "建议先连接手环再开启；未连接或未佩戴时显示 -- / 0。",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onShow) {
                Text(if (granted) "开启悬浮窗" else "授权并开启")
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
                    text = "悬浮窗外观",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = store::reset) {
                    Text("恢复默认")
                }
            }
            OverlayPreview(appearance)
            Spacer(Modifier.height(10.dp))

            AppearanceLabel("背景颜色")
            Spacer(Modifier.height(6.dp))
            ColorSwatchRow(
                selected = appearance.backgroundColor,
                colors = OverlayColors.BackgroundPresets,
                onSelect = store::setBackgroundColor,
            )

            Spacer(Modifier.height(10.dp))
            AppearanceLabel(
                "背景/边框透明度 ${(appearance.backgroundAlpha * 100).roundToInt()}%"
            )
            Slider(
                value = appearance.backgroundAlpha,
                onValueChange = store::setBackgroundAlpha,
                modifier = Modifier.fillMaxWidth(),
                valueRange = 0f..1f,
            )

            AppearanceLabel("字体颜色（同时应用到心形）")
            Spacer(Modifier.height(6.dp))
            ColorSwatchRow(
                selected = appearance.fontColor,
                colors = OverlayColors.FontPresets,
                onSelect = store::setFontColor,
            )

            Spacer(Modifier.height(10.dp))
            AppearanceLabel(
                "字体透明度 ${(appearance.fontAlpha * 100).roundToInt()}%"
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
private fun ColorSwatchRow(
    selected: Int,
    colors: List<Int>,
    onSelect: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

@Composable
private fun ErrorCard(state: HeartRateUiState) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "错误包",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                StatusChip(
                    text = state.parseErrorCount.toString(),
                    color = if (state.parseErrorCount > 0) ErrorRed else Muted,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (state.errors.isEmpty()) {
                Text(
                    text = "暂未收到无法解析的通知包。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            } else {
                state.errors.forEach { entry ->
                    Text(
                        text = "${formatter.format(Date(entry.atMillis))}  ${entry.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed,
                    )
                    Text(
                        text = "raw ${entry.rawHex}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun HelpCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "使用提示",
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
            )
            Spacer(Modifier.height(8.dp))
            DetailLine("1. 手环：设置 → 心率广播（运动心率广播）→ 开启。")
            DetailLine("2. 本 App 只接收标准 GATT 心率服务 0x180D / 0x2A37。")
            DetailLine("3. 未佩戴时可能收到 0 BPM、接触状态“未检测”或长时间无通知。")
            DetailLine("4. 错误包会显示在“错误包”卡片中，不会中断监听。")
        }
    }
}

@Composable
private fun DetailLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Muted,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
